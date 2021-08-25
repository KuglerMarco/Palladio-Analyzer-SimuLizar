package org.palladiosimulator.simulizar.qualitygate.interpreter;

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
import org.palladiosimulator.simulizar.utils.PCMPartitionManager;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

public class RepositoryComponentSwitchQualitygateContributionSwitch extends QualitygateSwitch<InterpreterResult>
        implements StereotypeSwitch, IMeasurementSourceListener {

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
    private AssemblyContext assembly;
    private ProvidedRole providedRole;

    // Stack to save the measurements from the calculators
    private static MeasuringValue responseTime2;

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
        this.providedRole = providedRole;

        // Injected
        this.merger = merger;
        this.frameworkContext = frameworkContext;
        this.partManager = partManager;
        this.assembly = assemblyContext;

        LOGGER.setLevel(Level.DEBUG);
//        responseTime2 = new Stack<MeasuringValue>();
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
        responseTime2 = (newMeasurement.getMeasuringValueForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC));
        LOGGER.debug("HERE Added a new Measurement:");
        LOGGER.debug(responseTime2);
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
        if(qualitygate.getAssemblyContext() == null || qualitygate.getAssemblyContext().equals(this.assembly)) {
            return this.doSwitch(qualitygate.getScope());
        }
        return InterpreterResult.OK;
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
        
        // Checking whether this qualitygate is evaluated at the right point in model TODO Assembly
        if(this.operationSignature.equals(object.getSignature()) && this.providedRole.equals(stereotypedObject)) {
            
            // Registering at the Calculator in Request-Scope
            if (this.callScope.equals(CallScope.REQUEST)) {
    
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
                        .equals("Response Time Tuple"))
                    .findFirst()
                    .orElse(null);
    
                // Calculator for this Qualitygate
                Calculator calc = frameworkContext.getCalculatorRegistry()
                    .getCalculatorByMeasuringPointAndMetricDescription(measPoint, respTimeMetricDesc);
    
                LOGGER.debug("MeasuringPoint is: " + measPoint.getStringRepresentation());
    
                if (!this.atRequestMetricCalcAdded) {
                    calc.addObserver(this);
                    LOGGER.debug("Observer added");
                    this.atRequestMetricCalcAdded = true;
                }
    
                LOGGER.debug(calc.toString());
    
                return InterpreterResult.OK;
    
            } else {
                LOGGER.debug("HERE" + qualitygate.getScope() + callScope);
                LOGGER.debug(responseTime2);
    //            LOGGER.debug(responseTime2.getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC));
                return InterpreterResult.OK;
    
            }
        }
        
        return InterpreterResult.OK;
    }



}
