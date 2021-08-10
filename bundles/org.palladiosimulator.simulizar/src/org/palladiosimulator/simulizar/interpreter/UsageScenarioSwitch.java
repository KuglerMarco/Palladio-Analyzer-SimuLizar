package org.palladiosimulator.simulizar.interpreter;

import java.util.Objects;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.usagemodel.AbstractUserAction;
import org.palladiosimulator.pcm.usagemodel.Branch;
import org.palladiosimulator.pcm.usagemodel.BranchTransition;
import org.palladiosimulator.pcm.usagemodel.Delay;
import org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall;
import org.palladiosimulator.pcm.usagemodel.Loop;
import org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour;
import org.palladiosimulator.pcm.usagemodel.UsageScenario;
import org.palladiosimulator.pcm.usagemodel.UsagemodelPackage;
import org.palladiosimulator.pcm.usagemodel.util.UsagemodelSwitch;
import org.palladiosimulator.simulizar.exceptions.PCMModelInterpreterException;
import org.palladiosimulator.simulizar.interpreter.listener.EventType;
import org.palladiosimulator.simulizar.interpreter.listener.ModelElementPassedEvent;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandlerDispatchFactory;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;
import org.palladiosimulator.simulizar.utils.SimulatedStackHelper;
import org.palladiosimulator.simulizar.utils.TransitionDeterminer;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.uka.ipd.sdq.simucomframework.variables.StackContext;
import de.uka.ipd.sdq.simucomframework.variables.stackframe.SimulatedStackframe;

/**
 * Switch for Usage Scenario in Usage Model
 *
 * @author Joachim Meyer
 *
 * @param <T>
 *            return type of switch methods.
 */
public class UsageScenarioSwitch extends UsagemodelSwitch<InterpreterResult> {
    
    @AssistedFactory
    public static interface Factory {
        UsageScenarioSwitch create(final InterpreterDefaultContext context);
    }

    protected static final Logger LOGGER = Logger.getLogger(UsageScenarioSwitch.class.getName());

    private final InterpreterDefaultContext context;
    private final TransitionDeterminer transitionDeterminer;
    private final RepositoryComponentSwitchFactory repositoryComponentSwitchFactory;

    private final EventDispatcher eventHelper;

    private final InterpreterResultMerger resultMerger;
    private final InterpreterResultHandler issueHandler;
    
    /**
     * @see UsageScenarioSwitchFactory#create(InterpreterDefaultContext)
     */
    @AssistedInject
    UsageScenarioSwitch(@Assisted final InterpreterDefaultContext context, RepositoryComponentSwitchFactory repositoryComponentSwitchFactory,
            EventDispatcher eventHelper,
            InterpreterResultHandlerDispatchFactory issueHandler,
            InterpreterResultMerger resultMerger) {
        this.context = context;
        this.repositoryComponentSwitchFactory = repositoryComponentSwitchFactory;
        this.eventHelper = eventHelper;
        this.issueHandler = issueHandler.create();
        this.resultMerger = resultMerger;
        this.transitionDeterminer = new TransitionDeterminer(context);
        LOGGER.setLevel(Level.DEBUG);
    }
    
    /**
     * @see org.palladiosimulator.pcm.usagemodel.util.UsagemodelSwitch#caseScenarioBehaviour(org.palladiosimulator.pcm.usagemodel.ScenarioBehaviour)
     */
    @Override
    public InterpreterResult caseScenarioBehaviour(final ScenarioBehaviour object) {
        final int stacksize = this.context.getStack().size();

        AbstractUserAction currentAction = null;
        // interpret start action
        for (final AbstractUserAction abstractAction : object.getActions_ScenarioBehaviour()) {
            if (abstractAction.eClass() == UsagemodelPackage.Literals.START) {
                firePassedEvent(abstractAction, EventType.BEGIN);
                currentAction = abstractAction.getSuccessor();
                firePassedEvent(abstractAction, EventType.END);
                break;
            }
        }
        if (currentAction == null) {
            throw new PCMModelInterpreterException("Usage Scenario is invalid, it misses a start action");
        }

        InterpreterResult result = InterpreterResult.OK;
        while (issueHandler.handleIssues(result) == InterpreterResumptionPolicy.CONTINUE
                && currentAction.eClass() != UsagemodelPackage.Literals.STOP) {
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("Interpret " + currentAction.eClass().getName() + ": " + currentAction);
            }
            
            this.firePassedEvent(currentAction, EventType.BEGIN);
            result = resultMerger.merge(result, this.doSwitch(currentAction));
            this.firePassedEvent(currentAction, EventType.END);
            currentAction = currentAction.getSuccessor();
        }

        if (this.context.getStack().size() != stacksize) {
            throw new PCMModelInterpreterException("Interpreter did not pop all pushed stackframes");
        }

        return result;
    }

    /**
     * @see org.palladiosimulator.pcm.usagemodel.util.UsagemodelSwitch#caseBranch(org.palladiosimulator.pcm.usagemodel.Branch)
     */
    @Override
    public InterpreterResult caseBranch(final Branch object) {
        // determine branch transition
        final BranchTransition branchTransition = this.transitionDeterminer
                .determineBranchTransition(object.getBranchTransitions_Branch());

        // interpret scenario behaviour of branch transition
        return this.doSwitch(branchTransition.getBranchedBehaviour_BranchTransition());
    }

    /**
     * @see org.palladiosimulator.pcm.usagemodel.util.UsagemodelSwitch#caseDelay(org.palladiosimulator.pcm.usagemodel.Delay)
     */
    @Override
    public InterpreterResult caseDelay(final Delay object) {
        // determine delay
        final double delay = StackContext.evaluateStatic(object.getTimeSpecification_Delay().getSpecification(),
                Double.class);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Start delay " + delay + " @ simulation time "
                    + this.context.getModel().getSimulationControl().getCurrentSimulationTime());
        }
        // hold simulation process
        this.context.getThread().hold(delay);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Continue user @ simulation time "
                    + this.context.getModel().getSimulationControl().getCurrentSimulationTime());
        }
        return InterpreterResult.OK;
    }

    /**
     * @see org.palladiosimulator.pcm.usagemodel.util.UsagemodelSwitch#caseEntryLevelSystemCall(org.palladiosimulator.pcm.usagemodel.EntryLevelSystemCall)
     */
    @Override
    public InterpreterResult caseEntryLevelSystemCall(final EntryLevelSystemCall entryLevelSystemCall) {
        final var providedDelegationSwitch = repositoryComponentSwitchFactory.create(this.context,
                RepositoryComponentSwitch.SYSTEM_ASSEMBLY_CONTEXT,
                entryLevelSystemCall.getOperationSignature__EntryLevelSystemCall(),
                entryLevelSystemCall.getProvidedRole_EntryLevelSystemCall());

        // FIXME We stick to single model elements here even though several would be needed to
        // uniquely identify the measuring point of interest (system + role + signature) [Lehrig]
        eventHelper.firePassedEvent(new ModelElementPassedEvent<OperationSignature>(
                        entryLevelSystemCall.getOperationSignature__EntryLevelSystemCall(), EventType.BEGIN,
                        this.context));

        // create new stack frame for input parameter
        SimulatedStackHelper.createAndPushNewStackFrame(this.context.getStack(),
                entryLevelSystemCall.getInputParameterUsages_EntryLevelSystemCall());
        
        this.context.getResultFrameStack().push(new SimulatedStackframe<>());
        var result = Objects.requireNonNull(providedDelegationSwitch.doSwitch(entryLevelSystemCall.getProvidedRole_EntryLevelSystemCall()));
//        if (LOGGER.isDebugEnabled()) {
//            ArrayList<InterpretationIssue> list1 = Lists.newArrayList(result.getIssues());
//            for(InterpretationIssue e : list1) {
//                if(e instanceof ParameterIssue) {
//                    LOGGER.debug("(UsageScenarioSwitch, caseEntyLevelSystemCall) StackContents der ParameterIssues: " + ((ParameterIssue) e).getStackContent());
//                }
//            }
//        }
        this.context.getStack().removeStackFrame();
        
        SimulatedStackHelper.addParameterToStackFrame(context.getResultFrameStack().pop(),
                entryLevelSystemCall.getOutputParameterUsages_EntryLevelSystemCall(), this.context.getStack().currentStackFrame());

        // FIXME We stick to single model elements here even though several would be needed to
        // uniquely identify the measuring point of interest (system + role + signature) [Lehrig]
        eventHelper.firePassedEvent(new ModelElementPassedEvent<OperationSignature>(
                        entryLevelSystemCall.getOperationSignature__EntryLevelSystemCall(), EventType.END,
                        this.context));

        return result;
    }

    /**
     * @see org.palladiosimulator.pcm.usagemodel.util.UsagemodelSwitch#caseLoop(org.palladiosimulator.pcm.usagemodel.Loop)
     */
    @Override
    public InterpreterResult caseLoop(final Loop object) {
        // determine number of loops
        final int numberOfLoops = StackContext.evaluateStatic(object.getLoopIteration_Loop().getSpecification(),
                Integer.class);
        var result = InterpreterResult.OK;
        for (int i = 0; issueHandler.handleIssues(result) == InterpreterResumptionPolicy.CONTINUE
                && i < numberOfLoops; i++) {
            LOGGER.debug("Interpret loop number " + i);
            result = resultMerger.merge(result, this.doSwitch(object.getBodyBehaviour_Loop()));
            LOGGER.debug("Finished loop number " + i);
        }
        return result;
    }


    /**
     * @see org.palladiosimulator.pcm.usagemodel.util.UsagemodelSwitch#caseAbstractUserAction(org.palladiosimulator.pcm.usagemodel.AbstractUserAction)
     */
    @Override
    public InterpreterResult caseAbstractUserAction(final AbstractUserAction object) {
        throw new UnsupportedOperationException("An unsupported usage model element was encountered: " + object.eClass().getName());
    }

    /**
     * @see org.palladiosimulator.pcm.usagemodel.util.UsagemodelSwitch#caseUsageScenario(org.palladiosimulator.pcm.usagemodel.UsageScenario)
     */
    @Override
    public InterpreterResult caseUsageScenario(final UsageScenario usageScenario) {
        eventHelper.firePassedEvent(
                new ModelElementPassedEvent<UsageScenario>(usageScenario, EventType.BEGIN, this.context));
        final int stacksize = this.context.getStack().size();
        var result = this.doSwitch(usageScenario.getScenarioBehaviour_UsageScenario());
        if (this.context.getStack().size() != stacksize) {
            throw new PCMModelInterpreterException("Interpreter did not pop all pushed stackframes");
        }
        if (!this.context.getResultFrameStack().isEmpty()) {
            throw new PCMModelInterpreterException("Interpreter missbehaving, not all result stack frames were properly removed.");
        }
        eventHelper.firePassedEvent(
                new ModelElementPassedEvent<UsageScenario>(usageScenario, EventType.END, this.context));
        return result;
    }
    
    private <T extends AbstractUserAction> void firePassedEvent(final T abstractAction, final EventType eventType) {
        eventHelper.firePassedEvent(new ModelElementPassedEvent<T>(abstractAction, eventType, this.context));
    }
}
