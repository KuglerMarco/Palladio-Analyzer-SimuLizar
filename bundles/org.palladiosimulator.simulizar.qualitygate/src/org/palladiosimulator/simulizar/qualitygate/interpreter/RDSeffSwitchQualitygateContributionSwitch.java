package org.palladiosimulator.simulizar.qualitygate.interpreter;

import java.util.Stack;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.RequestMetricScope;
import org.palladiosimulator.failuremodel.qualitygate.RequestParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.ResultParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.measurementframework.listener.IMeasurementSourceListener;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.metricspec.MetricDescriptionRepository;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.pcm.seff.CallAction;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcmmeasuringpoint.ExternalCallActionMeasuringPoint;
import org.palladiosimulator.probeframework.ProbeFrameworkContext;
import org.palladiosimulator.probeframework.calculator.Calculator;
import org.palladiosimulator.simulizar.interpreter.CallScope;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.StereotypeSwitch;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchStereotypeContributionFactory.RDSeffSwitchElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ParameterIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ResponseTimeProxyIssue;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager;
import org.palladiosimulator.simulizar.utils.SimulatedStackHelper;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

/**
 * Switch to process the qualitygates attached at elements of RDSeffs.
 * 
 * @author Marco Kugler
 *
 */
public class RDSeffSwitchQualitygateContributionSwitch extends QualitygateSwitch<InterpreterResult>
        implements StereotypeSwitch, IMeasurementSourceListener, ResponseTimeQualitygateSwitch {

    @AssistedFactory
    public interface Factory extends RDSeffSwitchStereotypeContributionFactory {

        @Override
        RDSeffSwitchQualitygateContributionSwitch create(final InterpreterDefaultContext context,
                RDSeffSwitchElementDispatcher parentSwitch);

    }

    // Information about the stereotype it is processing
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";

    // Information about the simulation-context
    private final InterpreterDefaultContext context;
    private final ProbeFrameworkContext frameworkContext;
    private Signature operationSignature;
    private final PCMPartitionManager partManager;

    // Information about the qualitygate-processing
    private static Stack<MeasuringValue> responseTime;
    private QualityGate qualitygate;
    private PCMRandomVariable premise;
    private EObject stereotypedObject;
    private CallScope callScope = CallScope.REQUEST;
    private boolean atRequestMetricCalcAdded = false;

    private final BasicInterpreterResultMerger merger;
    private static final Logger LOGGER = Logger.getLogger(RDSeffSwitchQualitygateContributionSwitch.class);

    @AssistedInject
    public RDSeffSwitchQualitygateContributionSwitch(@Assisted final InterpreterDefaultContext context,
            @Assisted RDSeffSwitchElementDispatcher parentSwitch, BasicInterpreterResultMerger merger,
            ProbeFrameworkContext frameworkContext, PCMPartitionManager partManager) {

        this.context = context;

        // Injected
        this.merger = merger;
        this.partManager = partManager;
        this.frameworkContext = frameworkContext;

        LOGGER.setLevel(Level.DEBUG);
        responseTime = new Stack<MeasuringValue>();
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
     * Entry-Point to process the attached stereotypes.
     *
     */
    @Override
    public InterpreterResult handleStereotype(Stereotype stereotype, EObject theEObject, CallScope callScope) {

        InterpreterResult result = InterpreterResult.OK;

        this.operationSignature = ((ExternalCallAction) theEObject).getCalledService_ExternalService();
        this.stereotypedObject = theEObject;
        this.callScope = callScope;

        EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());

        // Model validation
        if (taggedValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
        }

        // Processing all the attached Qualitygates
        for (QualityGate e : taggedValues) {

            LOGGER.debug("RepositoryCompoonent: " + e.getPremise()
                .getSpecification());

            if (theEObject instanceof ExternalCallAction) {
                result = merger.merge(result, this.doSwitch(e));
            } else {
                throw new IllegalStateException(
                        "The element, which was attached with a qualitygate is not (yet) supported");
            }

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

    @Override
    public void newMeasurementAvailable(MeasuringValue newMeasurement) {
        responseTime.add(newMeasurement.getMeasuringValueForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC));
        LOGGER.debug("Added a new Measurement: " + responseTime.size());

    }

    @Override
    public void preUnregister() {
        // TODO Auto-generated method stub

    }

    /**
     * Saving the qualitygate's premise and the qualitygate-element itself.
     */
    @Override
    public InterpreterResult caseQualityGate(QualityGate qualitygate) {
        this.qualitygate = qualitygate;
        premise = qualitygate.getPremise();
        return this.doSwitch(qualitygate.getScope());

    }
    

    @Override
    public InterpreterResult caseRequestMetricScope(RequestMetricScope object) {

        // Registering at the Calculator in Request-Scope
        if (callScope.equals(CallScope.REQUEST)) {

            // TODO MetricDescription laden (REsponseTime)
            // Loading CommonMetrics-model
            URI uri = URI.createURI("pathmap://METRIC_SPEC_MODELS/models/commonMetrics.metricspec");
            MetricDescriptionRepository res = (MetricDescriptionRepository) partManager.getBlackboard()
                .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
                .getResourceSet()
                .getResource(uri, false)
                .getContents()
                .get(0);

            // Searching for the Measuring-Point
            MeasuringPointRepository measuringPointRepo = (MeasuringPointRepository) partManager
                .findModel(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY);

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

            // Loading the ResponseTime MetricDescription
            MetricDescription respTimeMetricDesc = res.getMetricDescriptions()
                .stream()
                .filter(e -> e.getName()
                    .equals("Response Time Tuple"))
                .findFirst()
                .orElse(null);

            // Calculator for this Qualitygate TODO Noch MetricDescription raussuchen
            Calculator calc = frameworkContext.getCalculatorRegistry()
                .getCalculatorByMeasuringPointAndMetricDescription(measPoint, respTimeMetricDesc);

            LOGGER.debug("MeasuringPoint is: " + measPoint.getStringRepresentation());

            if (!this.atRequestMetricCalcAdded) {
                calc.addObserver(this);
                LOGGER.debug("Observer added");
                this.atRequestMetricCalcAdded = true;
            }

            return InterpreterResult.OK;

        } else {
            /*
             * Response-Time is checked, when the measurements are available - Processing-Proxy is
             * added as Issue
             */
            LOGGER.debug("New ResponseTimeProxyIssue.");
            return InterpreterResult
                .of(new ResponseTimeProxyIssue(premise, this, qualitygate, (Entity) stereotypedObject));

        }
    }

    /**
     * Processing the RequestParameterScope
     *
     */
    @Override
    public InterpreterResult caseRequestParameterScope(RequestParameterScope object) {

        Signature signatureOfQualitygate = object.getSignature();
        InterpreterResult result = InterpreterResult.OK;

        if (stereotypedObject instanceof ExternalCallAction) {

            if (callScope.equals(CallScope.REQUEST) && (signatureOfQualitygate == (this.operationSignature))) {

                // Push the new frame to evaluate
                SimulatedStackHelper.createAndPushNewStackFrame(this.context.getStack(),
                        ((CallAction) stereotypedObject).getInputVariableUsages__CallAction());

                if (!((boolean) context.evaluate(premise.getSpecification(), this.context.getStack()
                    .currentStackFrame()))) {
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Following StoEx is broken at ExternalCall: " + premise.getSpecification()
                                + " because stackframe is: " + this.context.getStack()
                                    .currentStackFrame()
                                    .toString());
                    }
                    result = BasicInterpreterResult.of(new ParameterIssue((Entity) this.stereotypedObject,
                            this.qualitygate, this.context.getStack()
                                .currentStackFrame()
                                .getContents()));
                }

                this.context.getStack()
                    .removeStackFrame();

            }

            return result;

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

            if (!((boolean) context.evaluate(premise.getSpecification(), this.context.getStack()
                .currentStackFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken at ExternalCall: " + premise.getSpecification()
                            + " because resultframe is: " + this.context.getStack()
                                .currentStackFrame()
                                .toString());
                }

                return BasicInterpreterResult.of(new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.context.getCurrentResultFrame()
                            .getContents()));
            }
        }
        return InterpreterResult.OK;
    }

    /**
     * Last measurement of the ResponseTime-calculators
     *
     */
    public MeasuringValue getLastResponseTimeMeasure() {

        return RDSeffSwitchQualitygateContributionSwitch.responseTime.firstElement();

    }

}
