package org.palladiosimulator.simulizar.qualitygate.interpreter;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.RequestMetricScope;
import org.palladiosimulator.failuremodel.qualitygate.RequestParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.ResultParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.measurementframework.listener.IMeasurementSourceListener;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.di.modules.component.core.QUALModule_ProvideProbeframeworkContextFactory;
import org.palladiosimulator.simulizar.interpreter.CallScope;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.StereotypeSwitch;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchStereotypeContributionFactory.ComposedStructureInnerSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ParameterIssue;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.probeframework.ProbeFrameworkContext;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

/**
 * Switch processing the Qualitygate-Stereotype.
 * 
 * @author Marco Kugler
 *
 */
public class StereotypeQualitygateSwitch extends QualitygateSwitch<InterpreterResult>
        implements IMeasurementSourceListener, StereotypeSwitch {

    @AssistedFactory
    public interface Factory {
        StereotypeQualitygateSwitch createStereotypeSwitch(final InterpreterDefaultContext context,
                final Signature operationSignature, CallScope callScope, EObject theEObject);
    }


    // Information about the simulation-context
    private final InterpreterDefaultContext context;
    private final Signature operationSignature;

    // Information about the stereotype attachment and processing time
    private QualityGate qualitygate;
    private Entity object;
    private PCMRandomVariable premise;
    private CallScope callScope = CallScope.REQUEST;


    private static final Logger LOGGER = Logger.getLogger(StereotypeQualitygateSwitch.class);

    @AssistedInject
    StereotypeQualitygateSwitch(@Assisted final InterpreterDefaultContext context,
            @Assisted Signature operationSignature, @Assisted CallScope callScope, @Assisted EObject eObject,
            BasicInterpreterResultMerger merger, ProbeFrameworkContext frameworkContext) {

        this.context = context;
        this.operationSignature = operationSignature;
        this.callScope = callScope;
        this.object = (Entity) eObject;

        LOGGER.setLevel(Level.DEBUG);

    }

    @Override
    public InterpreterResult caseRequestMetricScope(RequestMetricScope object) {

        // TODO MetricDescription laden (REsponseTime)
        // TODO MeasuringPoint erstellen

        // TODO this als Observer hinzuf�gen
//    	frameworkContext.getCalculatorRegistry().getCalculatorByMeasuringPointAndMetricDescription(null, null)

        return InterpreterResult.OK;
    }

    /**
     * Processing the attached Qualitygate, Premise and Scope
     */
    @Override
    public InterpreterResult caseQualityGate(QualityGate object) {
        this.qualitygate = object;
        premise = object.getPremise();
        return this.doSwitch(object.getScope());

    }

    /**
     * Checking the values on the parameter-stack against the premise-specification within the
     * Qualitygate.
     */
    @Override
    public InterpreterResult caseRequestParameterScope(RequestParameterScope object) {

        Signature signatureOfQualitygate = object.getSignature();

        if (callScope.equals(CallScope.REQUEST) && (signatureOfQualitygate == (this.operationSignature))) {

            if (!((boolean) context.evaluate(premise.getSpecification(), this.context.getStack()
                .currentStackFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + premise.getSpecification() + " because stackframe is: "
                            + this.context.getStack()
                                .currentStackFrame()
                                .toString());
                }

                return BasicInterpreterResult.of(new ParameterIssue(this.object, this.qualitygate,
                        this.context.getStack()
                            .currentStackFrame()
                            .getContents()));

            }

        }
        return InterpreterResult.OK;

    }

    /**
     * Checking the values on the parameter-stack against the premise-specification within the
     * Qualitygate.
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

                return BasicInterpreterResult
                    .of(new ParameterIssue(this.object, this.qualitygate, this.context.getCurrentResultFrame()
                        .getContents()));
            }
        }
        return InterpreterResult.OK;
    }



//    /**
//     * Handles the Stereotype attached to the element
//     */
//    @Override
//    public InterpreterResult handleStereotype(Stereotype stereotype, EObject theEObject, CallScope callScope) {
//
//        InterpreterResult result = InterpreterResult.OK;
//
//        this.callScope = callScope;
//        this.object = (Entity) theEObject;
//
//        EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());
//
//        // Model validation
//        if (taggedValues.isEmpty()) {
//            throw new IllegalArgumentException(
//                    "Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
//        }
//
//        // Processing all the attached Qualitygates
//        for (QualityGate e : taggedValues) {
//
//            this.qualitygate = e;
//
//            result = merger.merge(result, this.doSwitch(e));
//
//            if (LOGGER.isDebugEnabled()) {
//                LOGGER.debug("Qualitygate " + e.getEntityName() + " was processed with premise "
//                        + this.premise.getSpecification());
//            }
//        }
//        return result;
//    }



    @Override
    public void newMeasurementAvailable(MeasuringValue newMeasurement) {
        // TODO Auto-generated method stub

    }

    @Override
    public void preUnregister() {
        // TODO Auto-generated method stub

    }

    @Override
    public boolean isSwitchForStereotype(Stereotype stereotype) {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public InterpreterResult handleStereotype(Stereotype stereotype, EObject theEObject, CallScope callScope) {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getStereotypeName() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public String getProfileName() {
        // TODO Auto-generated method stub
        return null;
    }

}
