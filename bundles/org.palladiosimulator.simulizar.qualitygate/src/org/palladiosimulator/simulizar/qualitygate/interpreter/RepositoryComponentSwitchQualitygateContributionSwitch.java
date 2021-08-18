package org.palladiosimulator.simulizar.qualitygate.interpreter;

import java.util.Collection;
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
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.pcmmeasuringpoint.AssemblyOperationMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.SystemOperationMeasuringPoint;
import org.palladiosimulator.probeframework.ProbeFrameworkContext;
import org.palladiosimulator.probeframework.calculator.Calculator;
import org.palladiosimulator.simulizar.interpreter.CallScope;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchStereotypeContributionFactory.RepositoryComponentSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.StereotypeSwitch;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ParameterIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ResponseTimeProxyIssue;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

public class RepositoryComponentSwitchQualitygateContributionSwitch extends QualitygateSwitch<InterpreterResult>
        implements StereotypeSwitch, IMeasurementSourceListener, ResponseTimeQualitygateSwitch {

    @AssistedFactory
    public interface Factory extends RepositoryComponentSwitchStereotypeContributionFactory {

        @Override
        RepositoryComponentSwitchQualitygateContributionSwitch create(final InterpreterDefaultContext context,
                final AssemblyContext assemblyContext, final Signature signature, final ProvidedRole providedRole,
                RepositoryComponentSwitchStereotypeElementDispatcher parentSwitch);

    }

    // The stereotype, which the Switch is processing
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";

    // Information about the simulation-context
    private final InterpreterDefaultContext interpreterDefaultContext;
    private final Signature operationSignature;
    private final ProbeFrameworkContext frameworkContext;
    private final PCMPartitionManager partManager;

    // Information about the stereotype-processing
    private QualityGate qualitygate;
    private CallScope callScope = CallScope.REQUEST;
    private EObject stereotypedObject;
    private PCMRandomVariable premise;

    // Stack to save the measurements from the calculators
    private static Stack<MeasuringValue> responseTime;

    private final BasicInterpreterResultMerger merger;
    private boolean atRequestMetricCalcAdded = false;
    private static final Logger LOGGER = Logger.getLogger(RepositoryComponentSwitchQualitygateContributionSwitch.class);

    @AssistedInject
    public RepositoryComponentSwitchQualitygateContributionSwitch(@Assisted final InterpreterDefaultContext context,
            @Assisted final AssemblyContext assemblyContext, @Assisted final Signature signature,
            @Assisted final ProvidedRole providedRole,
            @Assisted RepositoryComponentSwitchStereotypeElementDispatcher parentSwitch,
            BasicInterpreterResultMerger merger, ProbeFrameworkContext frameworkContext,
            PCMPartitionManager partManager) {

        this.interpreterDefaultContext = context;
        this.operationSignature = signature;
        
        //Injected
        this.merger = merger;
        this.frameworkContext = frameworkContext;
        this.partManager = partManager;

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

        EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());

        this.callScope = callScope;
        this.stereotypedObject = theEObject;
        

        // Model validation
        if (taggedValues.isEmpty()) {
            throw new IllegalArgumentException(
                    "Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
        }

        // Processing all the attached Qualitygates
        for (QualityGate e : taggedValues) {

            LOGGER.debug("RepositoryCompoonent: " + e.getPremise()
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

    /**
     * Processing the RequestParameterScope.
     */
    @Override
    public InterpreterResult caseRequestParameterScope(RequestParameterScope object) {

        Signature signatureOfQualitygate = object.getSignature();

        if (callScope.equals(CallScope.REQUEST) && (signatureOfQualitygate == (this.operationSignature))) {

            if (!((boolean) interpreterDefaultContext.evaluate(premise.getSpecification(),
                    this.interpreterDefaultContext.getStack()
                        .currentStackFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + premise.getSpecification() + " because stackframe is: "
                            + this.interpreterDefaultContext.getStack()
                                .currentStackFrame()
                                .toString());
                }

                return BasicInterpreterResult.of(new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.interpreterDefaultContext.getStack()
                            .currentStackFrame()
                            .getContents()));

            }

        }
        return InterpreterResult.OK;

    }

    /**
     * Processing the ResultParameterScope.
     */
    @Override
    public InterpreterResult caseResultParameterScope(ResultParameterScope object) {

        Signature signatureOfQualitygate = object.getSignature();

        if (callScope.equals(CallScope.RESPONSE) && (signatureOfQualitygate == (this.operationSignature))) {
            if (!((boolean) interpreterDefaultContext.evaluate(premise.getSpecification(),
                    this.interpreterDefaultContext.getCurrentResultFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + premise.getSpecification()
                            + " because resultframe is: " + this.interpreterDefaultContext.getCurrentResultFrame()
                                .toString());
                }

                return BasicInterpreterResult.of(new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.interpreterDefaultContext.getCurrentResultFrame()
                            .getContents()));
            }
        }
        return InterpreterResult.OK;
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
                if (e instanceof AssemblyOperationMeasuringPoint) {
                    if (((AssemblyOperationMeasuringPoint) e).getOperationSignature()
                        .equals(object.getSignature())
                            && ((AssemblyOperationMeasuringPoint) e).getRole()
                                .equals(stereotypedObject)) {

                        measPoint = (AssemblyOperationMeasuringPoint) e;

                    }
                }
            }

            if (measPoint == null) {
                throw new IllegalStateException(
                        "No MeasuringPoint found in MeasuringPointRepository for this Qualitygate.");
            }

            MetricDescription respTimeMetricDesc = res.getMetricDescriptions()
                .stream()
                .filter(e -> e.getName()
                    .equals("Response Time"))
                .findFirst()
                .orElse(null);


            // Calculator for this Qualitygate TODO Noch MetricDescription laden
            Collection<Calculator> calculator = frameworkContext.getCalculatorRegistry()
                .getCalculatorsForMeasuringPoint(measPoint);

            LOGGER.debug("MeasuringPoint is: " + measPoint.getStringRepresentation());

            Calculator calc = calculator.stream()
                .findFirst()
                .orElse(null);

            if (!this.atRequestMetricCalcAdded ) {
                calc.addObserver(this);
                LOGGER.debug("Observer added");
                this.atRequestMetricCalcAdded = true;
            }

            LOGGER.debug(calc.toString());

            return InterpreterResult.OK;

        } else {
            LOGGER.debug("New ResponseTimeProxyIssue.");
            return InterpreterResult
                .of(new ResponseTimeProxyIssue(premise, this, qualitygate, (Entity) stereotypedObject));

        }
    }

    @Override
    public MeasuringValue getLastResponseTimeMeasure() {
        
        return RepositoryComponentSwitchQualitygateContributionSwitch.responseTime.firstElement();
        
    }

}
