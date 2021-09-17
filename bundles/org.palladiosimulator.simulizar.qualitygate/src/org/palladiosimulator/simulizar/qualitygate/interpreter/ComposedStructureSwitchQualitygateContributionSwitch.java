package org.palladiosimulator.simulizar.qualitygate.interpreter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.RequestParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.ResultParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.stereotype.CallScope;
import org.palladiosimulator.simulizar.interpreter.stereotype.ComposedStructureInnerSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.stereotype.StereotypeSwitch;
import org.palladiosimulator.simulizar.interpreter.stereotype.ComposedStructureInnerSwitchStereotypeContributionFactory.ComposedStructureInnerSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ParameterIssue;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

/**
 * Switch to process the qualitygates attached at AssemblyConnectors.
 * 
 * @author Marco Kugler
 *
 */
public class ComposedStructureSwitchQualitygateContributionSwitch extends QualitygateSwitch<InterpreterResult>
        implements StereotypeSwitch {

    @AssistedFactory
    public interface Factory extends ComposedStructureInnerSwitchStereotypeContributionFactory {
        @Override
        ComposedStructureSwitchQualitygateContributionSwitch createStereotypeSwitch(
                final InterpreterDefaultContext context, final Signature operationSignature,
                final RequiredRole requiredRole, ComposedStructureInnerSwitchStereotypeElementDispatcher parentSwitch);
    }

    // Information about the stereotype it is processing
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";

    // Information about the simulation-context
    private final InterpreterDefaultContext context;
    private final Signature operationSignature;

    // Information about the qualitygate-processing
    private QualityGate qualitygate;
    private PCMRandomVariable premise;
    private CallScope callScope = CallScope.REQUEST;
    private Entity stereotypedObject;

    private static final Logger LOGGER = Logger.getLogger(ComposedStructureSwitchQualitygateContributionSwitch.class);
    private final BasicInterpreterResultMerger merger;

    @AssistedInject
    ComposedStructureSwitchQualitygateContributionSwitch(@Assisted final InterpreterDefaultContext context,
            @Assisted Signature operationSignature, @Assisted RequiredRole requiredRole,
            @Assisted ComposedStructureInnerSwitchStereotypeElementDispatcher parentSwitch,
            BasicInterpreterResultMerger merger) {

        this.operationSignature = operationSignature;
        this.context = context;
        
        // Injected
        this.merger = merger;

        LOGGER.setLevel(Level.DEBUG);

    }

    /**
     * Returns whether this Switch is responsible for processing this stereotype.
     *
     */
    @Override
    public boolean isSwitchForStereotype(Stereotype stereotype) {

        boolean result = stereotype.getProfile()
            .getName()
            .equals(profileName);
        if (result) {
            return stereotype.getName()
                .equals(stereotypeName);
        }
        return result;
    }

    /**
     * Entry-Point to process the attached stereotype.
     *
     */
    @Override
    public InterpreterResult handleStereotype(Stereotype stereotype, EObject theEObject, CallScope callScope) {

        InterpreterResult result = InterpreterResult.OK;

        EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());

        this.callScope = callScope;
        this.stereotypedObject = (Entity) theEObject;

        if (taggedValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "Qualitygate-Stereotype not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
        }

        // Processing all the attached Qualitygates
        for (QualityGate e : taggedValues) {

            LOGGER.debug("Following StoEx was processed at " + this.stereotypedObject.getEntityName()
                    + " with the StoEx: " + e.getPredicate()
                        .getSpecification());

            result = merger.merge(result, this.doSwitch(e));

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
     * Saving the qualitygate's premise and the qualitygate-element itself.
     */
    @Override
    public InterpreterResult caseQualityGate(QualityGate qualitygate) {
        this.qualitygate = qualitygate;
        this.premise = qualitygate.getPredicate();
        return this.doSwitch(qualitygate.getScope());

    }

    /**
     * Processing the RequestParameterScope
     *
     */
    @Override
    public InterpreterResult caseRequestParameterScope(RequestParameterScope requestParameterScope) {

        Signature signatureOfQualitygate = requestParameterScope.getSignature();

        if (callScope.equals(CallScope.REQUEST) && (signatureOfQualitygate == (this.operationSignature))) {

            if (!((boolean) context.evaluate(premise.getSpecification(), this.context.getStack()
                .currentStackFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + premise.getSpecification() + " because stackframe is: "
                            + this.context.getStack()
                                .currentStackFrame()
                                .toString());
                }

                return BasicInterpreterResult.of(new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.context.getStack()
                            .currentStackFrame()
                            .getContents(),
                        true));

            }

        }
        return InterpreterResult.OK;

    }

    /**
     * Processing the ResultParameterScope
     */
    @Override
    public InterpreterResult caseResultParameterScope(ResultParameterScope object) {

        Signature signatureOfQualitygate = object.getSignature();

        if (callScope.equals(CallScope.RESPONSE) && (signatureOfQualitygate == (this.operationSignature))) {
            if (!((boolean) context.evaluate(premise.getSpecification(), this.context.getCurrentResultFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + premise.getSpecification()
                            + " because resultframe is: " + this.context.getCurrentResultFrame()
                                .toString());
                }

                return BasicInterpreterResult.of(new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.context.getCurrentResultFrame()
                            .getContents(),
                        false));
            }
        }

        return InterpreterResult.OK;
    }

}
