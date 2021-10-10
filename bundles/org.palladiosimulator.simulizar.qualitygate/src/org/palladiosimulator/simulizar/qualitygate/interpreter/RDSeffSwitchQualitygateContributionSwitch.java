package org.palladiosimulator.simulizar.qualitygate.interpreter;

import java.util.ArrayList;
import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Quantity;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.failuremodel.failuretype.Failure;
import org.palladiosimulator.failuremodel.failuretype.SWContentFailure;
import org.palladiosimulator.failuremodel.failuretype.SWCrashFailure;
import org.palladiosimulator.failuremodel.failuretype.SWTimingFailure;
import org.palladiosimulator.failuremodel.qualitygate.EventBasedCommunicationScope;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.RequestMetricScope;
import org.palladiosimulator.failuremodel.qualitygate.RequestParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.ResultParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.measurementframework.listener.IMeasurementSourceListener;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.pcm.seff.CallAction;
import org.palladiosimulator.pcm.seff.CallReturnAction;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcmmeasuringpoint.ExternalCallActionMeasuringPoint;
import org.palladiosimulator.probeframework.ProbeFrameworkContext;
import org.palladiosimulator.probeframework.calculator.Calculator;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.CorruptContentBehavior;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.CrashBehavior;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.DelayBehavior;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.listener.EventType;
import org.palladiosimulator.simulizar.interpreter.listener.ModelElementPassedEvent;
import org.palladiosimulator.simulizar.interpreter.preinterpretation.PreInterpretationBehavior;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.stereotype.CallScope;
import org.palladiosimulator.simulizar.interpreter.stereotype.RDSeffSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.stereotype.StereotypeSwitch;
import org.palladiosimulator.simulizar.interpreter.stereotype.RDSeffSwitchStereotypeContributionFactory.RDSeffSwitchElementDispatcher;
import org.palladiosimulator.simulizar.qualitygate.event.QualitygatePassedEvent;
import org.palladiosimulator.simulizar.qualitygate.eventbasedcommunication.RequestContextFailureRegistry;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ParameterIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ProcessingTimeIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.QualitygateIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.proxy.CrashProxyIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.proxy.ResponseTimeProxyIssue;
import org.palladiosimulator.simulizar.qualitygate.measurement.QualitygateViolationProbeRegistry;
import org.palladiosimulator.simulizar.qualitygate.measurement.eventbasedcommunication.EventBasedCommunicationProbeRegistry;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager;
import org.palladiosimulator.simulizar.utils.SimulatedStackHelper;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.uka.ipd.sdq.simucomframework.variables.stackframe.SimulatedStackframe;

/**
 * Switch to process the Qualitygates attached at ExternalCalls.
 * 
 * @author Marco Kugler
 *
 */
public class RDSeffSwitchQualitygateContributionSwitch extends QualitygateSwitch<InterpreterResult>
        implements StereotypeSwitch, IMeasurementSourceListener {

    @AssistedFactory
    public interface Factory extends RDSeffSwitchStereotypeContributionFactory {

        @Override
        RDSeffSwitchQualitygateContributionSwitch create(final InterpreterDefaultContext context,
                RDSeffSwitchElementDispatcher parentSwitch);

    }

    // Information about the stereotype
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";

    // Information about the simulation-context
    private final InterpreterDefaultContext context;
    private final ProbeFrameworkContext frameworkContext;
    private Signature operationSignature;
    private final PCMPartitionManager partManager;
    private AssemblyContext assembly;

    // Information about the qualitygate-processing
    private static MeasuringValue responseTime;
    private QualityGate qualitygate;
    private PCMRandomVariable predicate;
    private Entity stereotypedObject;
    private RequiredRole requiredRole;
    private CallScope callScope = CallScope.REQUEST;
    private boolean atRequestMetricCalcAdded = false;

    private final BasicInterpreterResultMerger merger;

    private static final Logger LOGGER = Logger.getLogger(RDSeffSwitchQualitygateContributionSwitch.class);

    private QualitygateViolationProbeRegistry probeRegistry;
    private EventBasedCommunicationProbeRegistry eventBasedRegistry;
    private RequestContextFailureRegistry failureRegistry;

    @AssistedInject
    public RDSeffSwitchQualitygateContributionSwitch(@Assisted final InterpreterDefaultContext context,
            @Assisted RDSeffSwitchElementDispatcher parentSwitch, BasicInterpreterResultMerger merger,
            ProbeFrameworkContext frameworkContext, PCMPartitionManager partManager,
            QualitygateViolationProbeRegistry probeRegistry, EventBasedCommunicationProbeRegistry eventBasedRegistry,
            RequestContextFailureRegistry failureRegistry) {

        this.context = context;

        // Injected
        this.merger = merger;
        this.partManager = partManager;
        this.frameworkContext = frameworkContext;
        this.probeRegistry = probeRegistry;
        this.assembly = context.getAssemblyContextStack()
            .get(1);
        this.eventBasedRegistry = eventBasedRegistry;
        this.failureRegistry = failureRegistry;

        LOGGER.setLevel(Level.DEBUG);
    }

    /**
     * Returns whether this Switch is appropriate for processing this stereotype.
     */
    @Override
    public boolean isSwitchForStereotype(Stereotype stereotype) {

        if (stereotype.getProfile()
            .getName()
            .equals(profileName)
                && stereotype.getName()
                    .equals(stereotypeName)) {

            return true;
        }
        return false;
    }

    /**
     * Entry-Point to process the attached Qualitygates at the model element.
     *
     */
    @Override
    public InterpreterResult handleStereotype(Stereotype stereotype, EObject theEObject, CallScope callScope) {

        InterpreterResult result = InterpreterResult.OK;

        this.operationSignature = ((ExternalCallAction) theEObject).getCalledService_ExternalService();
        this.stereotypedObject = (Entity) theEObject;
        this.callScope = callScope;
        this.requiredRole = ((ExternalCallAction) theEObject).getRole_ExternalService();

        EList<QualityGate> qualitygates = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());

        if (qualitygates.isEmpty()) {

            /*
             * The containment reference on the Qualitygate cannot be transferred to the
             * ExternalCall during Preprocessing; thus, if ExternalCall has no Qualitygate, the
             * corresponding RequiredRole needs to be checked
             */
            qualitygates = StereotypeAPI.getTaggedValue(((ExternalCallAction) theEObject).getRole_ExternalService(),
                    "qualitygate", stereotype.getName());

            if (qualitygates.isEmpty()) {
                throw new IllegalArgumentException(
                        "Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
            }

        }

        // Processing all the attached Qualitygates
        for (QualityGate qualitygate : qualitygates) {
            result = merger.merge(result, this.doSwitch(qualitygate));
        }

        return result;
    }

    @Override
    public String getStereotypeName() {
        return this.stereotypeName;
    }

    @Override
    public String getProfileName() {
        return this.profileName;
    }

    /**
     * Captures the ResponseTime measurements at the stereotyped entity
     *
     */
    @Override
    public void newMeasurementAvailable(MeasuringValue newMeasurement) {
        responseTime = newMeasurement.getMeasuringValueForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC);
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("Added a new Measurement: " + responseTime);
        }
    }

    @Override
    public void preUnregister() {
        // Nothing to do here
    }

    @Override
    public InterpreterResult caseQualityGate(QualityGate qualitygate) {

        this.qualitygate = qualitygate;
        this.predicate = qualitygate.getPredicate();
        if (qualitygate.getAssemblyContext() == null || qualitygate.getAssemblyContext()
            .equals(this.assembly)) {
            return this.doSwitch(qualitygate.getScope());
        }

        return InterpreterResult.OK;

    }

    /**
     * Processing in RequestMetricScope.
     *
     */
    @Override
    public InterpreterResult caseRequestMetricScope(RequestMetricScope requestMetricScope) {

        InterpreterResult result = InterpreterResult.OK;

        // Registering at the Calculator in Request-Scope
        if (callScope.equals(CallScope.REQUEST)) {

            MetricDescription respTimeMetricDesc = requestMetricScope.getMetric();

            /*
             * MeasuringPointRepository needs to be loaded trough the MonitorRepository, otherwise
             * the MeasuringPointRepository isn't be found at first run
             */
            MonitorRepository monitorRepo = (MonitorRepository) partManager
                .findModel(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY);

            MeasuringPointRepository measuringPointRepo = monitorRepo.getMonitors()
                .get(0)
                .getMeasuringPoint()
                .getMeasuringPointRepository();

            // Searching for the Measuring-Point created in preprocessing
            MeasuringPoint measPoint = null;

            for (MeasuringPoint e : measuringPointRepo.getMeasuringPoints()) {
                if (e instanceof ExternalCallActionMeasuringPoint) {
                    if (((ExternalCallActionMeasuringPoint) e).getExternalCall()
                        .equals(stereotypedObject)) {
                        measPoint = (ExternalCallActionMeasuringPoint) e;
                    }
                }
            }

            if (measPoint == null) {
                throw new IllegalStateException(
                        "No MeasuringPoint found in MeasuringPointRepository for this Qualitygate.");
            }

            // Calculator for this Qualitygate
            Calculator calc = frameworkContext.getCalculatorRegistry()
                .getCalculatorByMeasuringPointAndMetricDescription(measPoint, respTimeMetricDesc);

            // Adding this class as observer
            if (!this.atRequestMetricCalcAdded) {
                calc.addObserver(this);
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Observer added at: " + measPoint.getStringRepresentation());
                }
                this.atRequestMetricCalcAdded = true;
            }

        } else {
            /*
             * Response-Time is checked, when the measurements are available - Processing-Proxy is
             * added as Issue, which is evaluated at the IssueHandler
             */
            if (LOGGER.isDebugEnabled()) {
                LOGGER.debug("New ResponseTimeProxyIssue at " + this.stereotypedObject.getEntityName());
            }
            result = InterpreterResult.of(new ResponseTimeProxyIssue(predicate, this, qualitygate, stereotypedObject,
                    this.context, this.requiredRole));
        }

        return result;
    }

    /**
     * Processing the RequestParameterScope
     *
     */
    @Override
    public InterpreterResult caseRequestParameterScope(RequestParameterScope scope) {

        InterpreterResult result = InterpreterResult.OK;

        if (callScope.equals(CallScope.REQUEST) && (scope.getSignature() == (this.operationSignature))) {

            /*
             * Pushes the new frame for evaluation, because in RDSeffSwitch, stack is pushed after
             * this stereotype processing
             */
            SimulatedStackHelper.createAndPushNewStackFrame(this.context.getStack(),
                    ((CallAction) stereotypedObject).getInputVariableUsages__CallAction());

            if (!((boolean) context.evaluate(predicate.getSpecification(), this.context.getStack()
                .currentStackFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken at ExternalCall: " + predicate.getSpecification()
                            + " because stackframe is: " + this.context.getStack()
                                .currentStackFrame()
                                .toString());
                }

                ParameterIssue issue = new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.context.getStack()
                            .currentStackFrame()
                            .getContents(),
                        false);

                result = BasicInterpreterResult.of(issue);

                failureRegistry.addIssue(context.getThread()
                    .getRequestContext(), issue);

                this.triggerInvolvedIssueProbes(failureRegistry.getInterpreterResult(this.context.getThread()
                    .getRequestContext()));

                probeRegistry.triggerViolationProbe(
                        new QualitygatePassedEvent(qualitygate, context, false, null, this.requiredRole, false));

                probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(qualitygate, context, false,
                        qualitygate.getSeverity(), this.requiredRole, false));

                if (qualitygate.getImpact() != null) {
                    result = merger.merge(result, this.handleImpact(qualitygate.getImpact(), context));
                }

            } else {
                // triggering probe to measure Success-To-Failure-Rate case successful
                probeRegistry.triggerViolationProbe(
                        new QualitygatePassedEvent(qualitygate, context, true, null, this.stereotypedObject, false));
            }

            // Removing Stack again
            this.context.getStack()
                .removeStackFrame();

        }

        return result;

    }

    /**
     * Processing the ResultParameterScope
     */
    @Override
    public InterpreterResult caseResultParameterScope(ResultParameterScope object) {

        InterpreterResult result = InterpreterResult.OK;

        if (callScope.equals(CallScope.RESPONSE) && object.getSignature() == (this.operationSignature)) {

            SimulatedStackHelper.createAndPushNewStackFrame(context.getStack(),
                    ((CallReturnAction) stereotypedObject).getReturnVariableUsage__CallReturnAction());

            if (!((boolean) context.evaluate(predicate.getSpecification(), this.context.getStack()
                .currentStackFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken at ExternalCall: " + predicate.getSpecification()
                            + " because resultframe is: " + this.context.getStack()
                                .currentStackFrame()
                                .toString());
                }

                result = BasicInterpreterResult.of(new CrashProxyIssue(qualitygate, context, false,
                        qualitygate.getSeverity(), this.requiredRole, context.getCurrentResultFrame()
                            .getContents()));

            } else {

                result = BasicInterpreterResult.of(new CrashProxyIssue(qualitygate, context, true, null,
                        this.requiredRole, context.getCurrentResultFrame()
                            .getContents()));
            }

            this.context.getStack()
                .removeStackFrame();

        }
        return result;
    }

    /**
     * Last measurement of the ResponseTime-calculator
     *
     */
    public MeasuringValue getLastResponseTimeMeasure() {
        return RDSeffSwitchQualitygateContributionSwitch.responseTime;
    }

    /**
     * Processes impact of the Qualitygates.
     * 
     * @param failureList
     * @param interpreterDefaultContext
     * @return
     */
    private InterpreterResult handleImpact(List<Failure> failureList,
            InterpreterDefaultContext interpreterDefaultContext) {

        InterpreterResult result = InterpreterResult.OK;

        for (Failure failure : failureList) {

            if (failure instanceof SWTimingFailure) {

                PreInterpretationBehavior behavior = new DelayBehavior(((SWTimingFailure) failure).getDelay()
                    .getSpecification());

                result = merger.merge(result, behavior.execute(interpreterDefaultContext));

            } else if (failure instanceof SWContentFailure) {

                PreInterpretationBehavior behavior = new CorruptContentBehavior(
                        ((SWContentFailure) failure).getDegreeOfCorruption()
                            .getSpecification());

                result = merger.merge(result, behavior.execute(interpreterDefaultContext));

            } else if (failure instanceof SWCrashFailure) {

                PreInterpretationBehavior behavior = new CrashBehavior(failure);
                result = merger.merge(result, behavior.execute(interpreterDefaultContext));

            }

        }

        return result;
    }

    /**
     * Processing the EventBasedCommunicationScope.
     *
     */
    @Override
    public InterpreterResult caseEventBasedCommunicationScope(EventBasedCommunicationScope scope) {

        InterpreterResult result = InterpreterResult.OK;

        if (callScope.equals(CallScope.REQUEST) && scope.getSignature() == this.operationSignature) {

            if (qualitygate.getPredicate()
                .getSpecification()
                .equals("Start.TYPE")) {

                // Start measurement
                eventBasedRegistry
                    .startMeasurement(new ModelElementPassedEvent<QualityGate>(qualitygate, EventType.BEGIN, context));

            } else if (scope.getSignature() == this.operationSignature) {

                // End measurement
                MeasuringValue value = eventBasedRegistry.endMeasurement(
                        new ModelElementPassedEvent<QualityGate>(scope.getQualitygate(), EventType.END, context));

                Measure<Object, Quantity> measuringValue = value
                    .getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC);

                // set temporary stack for evaluation
                final SimulatedStackframe<Object> frame = this.context.getStack()
                    .createAndPushNewStackFrame();

                List<Failure> failureImpactList = new ArrayList<Failure>();

                String parameterName = "Stop.TYPE";

                frame.addValue(parameterName, (Double) measuringValue.getValue());

                if (!((boolean) context.evaluate(predicate.getSpecification(), this.context.getStack()
                    .currentStackFrame()))) {

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Reponsetime Qualitygate broken: " + responseTime);
                    }

                    ProcessingTimeIssue issue = new ProcessingTimeIssue((Entity) this.stereotypedObject,
                            this.qualitygate, false);

                    result = BasicInterpreterResult.of(issue);

                    failureRegistry.addIssue(context.getThread()
                        .getRequestContext(), issue);

                    // triggering probes to measure Success-To-Failure-Rate case violated
                    probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(qualitygate, context, false, null,
                            this.stereotypedObject, false));

                    probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(qualitygate, context, false,
                            qualitygate.getSeverity(), this.stereotypedObject, false));

                    this.triggerInvolvedIssueProbes(failureRegistry.getInterpreterResult(this.context.getThread()
                            .getRequestContext()));

                    if (qualitygate.getImpact() != null) {
                        failureImpactList.addAll(qualitygate.getImpact());
                    }

                } else {
                    // triggering probes to measure Success-To-Failure-Rate case successful
                    probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(qualitygate, context, true, null,
                            this.stereotypedObject, false));
                }

                // pop temporary stack
                this.context.getStack()
                    .removeStackFrame();

                result = merger.merge(result, this.handleImpact(failureImpactList, context));

            }

        }

        return result;

    }

    /**
     * Triggers the Issues present in InterpreterResult.
     * 
     * @param interpreterResult
     * @return
     */
    private InterpreterResult triggerInvolvedIssueProbes(InterpreterResult interpreterResult) {

        InterpreterResult result = interpreterResult;

        // if unhandled issues are on interpreterResult, then persist the issues
        // (issues when the unhandled issues where broken

        // List for the issues which where there, when the qualitygates where broken
        List<InterpretationIssue> issuesWhenBroken = new ArrayList<InterpretationIssue>();

        // handled issues where present when unhandled issues where broken
        for (InterpretationIssue issue : result.getIssues()) {
            if (issue.isHandled()) {
                issuesWhenBroken.add(issue);

            }
        }

        // for every unhandled Issue persist the Issues
        for (InterpretationIssue issue : result.getIssues()) {
            if (!issue.isHandled() && issue instanceof QualitygateIssue) {

                probeRegistry.triggerInvolvedIssuesProbe(issuesWhenBroken,
                        ((QualitygateIssue) issue).getQualitygateRef());

                ((QualitygateIssue) issue).setHandled(true);
            }
        }

        return result;

    }

}
