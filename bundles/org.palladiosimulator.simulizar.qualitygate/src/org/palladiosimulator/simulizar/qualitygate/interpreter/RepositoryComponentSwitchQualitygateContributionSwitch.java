package org.palladiosimulator.simulizar.qualitygate.interpreter;

import java.util.ArrayList;
import java.util.List;

import javax.measure.Measure;
import javax.measure.quantity.Quantity;

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
import org.palladiosimulator.failuremodel.failuretype.Failure;
import org.palladiosimulator.failuremodel.failuretype.SWContentFailure;
import org.palladiosimulator.failuremodel.failuretype.SWCrashFailure;
import org.palladiosimulator.failuremodel.failuretype.SWTimingFailure;
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
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.CorruptContentBehavior;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.CrashBehavior;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.DelayBehavior;
import org.palladiosimulator.simulizar.interpreter.CallScope;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchStereotypeContributionFactory.RepositoryComponentSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.StereotypeSwitch;
import org.palladiosimulator.simulizar.interpreter.preinterpretation.PreInterpretationBehavior;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.qualitygate.event.QualitygatePassedEvent;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ParameterIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ResponseTimeIssue;
import org.palladiosimulator.simulizar.qualitygate.measurement.QualitygateViolationProbeRegistry;
import org.palladiosimulator.simulizar.qualitygate.propagation.QualitygatePropagationRecorder;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.uka.ipd.sdq.simucomframework.variables.stackframe.SimulatedStackframe;

/**
 * Switch to process the qualitygates attached at elements of ExternalCalls.
 * 
 * @author Marco Kugler
 *
 */
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
    private Entity stereotypedObject;
    private PCMRandomVariable premise;
    private AssemblyContext assembly;
    private ProvidedRole providedRole;

    private static MeasuringValue responseTime;

    private final BasicInterpreterResultMerger merger;
    private boolean atRequestMetricCalcAdded = false;
    private QualitygateViolationProbeRegistry probeRegistry;
    private QualitygatePropagationRecorder recorder;
    private static final Logger LOGGER = Logger.getLogger(RepositoryComponentSwitchQualitygateContributionSwitch.class);

    @AssistedInject
    public RepositoryComponentSwitchQualitygateContributionSwitch(@Assisted final InterpreterDefaultContext context,
            @Assisted final AssemblyContext assemblyContext, @Assisted final Signature signature,
            @Assisted final ProvidedRole providedRole,
            @Assisted RepositoryComponentSwitchStereotypeElementDispatcher parentSwitch,
            BasicInterpreterResultMerger merger, ProbeFrameworkContext frameworkContext,
            PCMPartitionManager partManager, QualitygateViolationProbeRegistry probeRegistry,
            QualitygatePropagationRecorder recorder) {

        this.interpreterDefaultContext = context;
        this.operationSignature = signature;
        this.providedRole = providedRole;

        // Injected
        this.merger = merger;
        this.frameworkContext = frameworkContext;
        this.partManager = partManager;
        this.assembly = assemblyContext;
        this.probeRegistry = probeRegistry;
        this.recorder = recorder;

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
     * Entry-Point to process the attached stereotypes.
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
                    "Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
        }

        // Processing all the attached Qualitygates
        for (QualityGate e : taggedValues) {
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

        responseTime = (newMeasurement.getMeasuringValueForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC));
        LOGGER.debug("Added a new Measurement at " + stereotypedObject.getEntityName());

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
        if (qualitygate.getAssemblyContext() == null || qualitygate.getAssemblyContext()
            .equals(this.assembly)) {
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
        InterpreterResult result = InterpreterResult.OK;

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

                ParameterIssue issue = new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.interpreterDefaultContext.getStack()
                            .currentStackFrame()
                            .getContents(), true);

                result = BasicInterpreterResult.of(issue);
                

                // triggering probe to measure Success-To-Failure-Rate case violated
                probeRegistry.triggerProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext, false, null));

                probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext, false, qualitygate.getSeverity()));

                recorder.recordQualitygateIssue(qualitygate, stereotypedObject, issue);
                
                if(qualitygate.getImpact() != null) {
                    result = merger.merge(result, this.handleImpact(qualitygate.getImpact().getFailure(), interpreterDefaultContext));
                }

            } else {
                // triggering probe to measure Success-To-Failure-Rate case successful
                probeRegistry.triggerProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext, true, null));
            }

        }
        return result;

    }

    /**
     * Processing the ResultParameterScope.
     */
    @Override
    public InterpreterResult caseResultParameterScope(ResultParameterScope object) {

        Signature signatureOfQualitygate = object.getSignature();
        InterpreterResult result = InterpreterResult.OK;

        if (callScope.equals(CallScope.RESPONSE) && (signatureOfQualitygate == (this.operationSignature))) {

            if (!((boolean) interpreterDefaultContext.evaluate(premise.getSpecification(),
                    this.interpreterDefaultContext.getCurrentResultFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + premise.getSpecification()
                            + " because resultframe is: " + this.interpreterDefaultContext.getCurrentResultFrame()
                                .toString());
                }

                ParameterIssue issue = new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.interpreterDefaultContext.getCurrentResultFrame()
                            .getContents(), false);

                result = BasicInterpreterResult.of(issue);
                

                // triggering probe to measure Success-To-Failure-Rate case successful
                probeRegistry.triggerProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext, false, null));

                probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext, false, qualitygate.getSeverity()));

                
                recorder.recordQualitygateIssue(qualitygate, stereotypedObject, issue);
                
                if(qualitygate.getImpact() != null) {
                    
                    result = merger.merge(result, this.handleImpact(qualitygate.getImpact().getFailure(), interpreterDefaultContext));
                    
                }

            } else {
                // triggering probe to measure Success-To-Failure-Rate case successful
                probeRegistry.triggerProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext, true, null));
            }
        }
        return result;
    }

    @Override
    public InterpreterResult caseRequestMetricScope(RequestMetricScope object) {

        InterpreterResult result = InterpreterResult.OK;
        List<Failure> failureImpactList = new ArrayList<Failure>();


        // Checking whether this qualitygate is evaluated at the right point in model
        if (this.operationSignature.equals(object.getSignature()) && this.providedRole.equals(stereotypedObject)
                && (qualitygate.getAssemblyContext() == null || qualitygate.getAssemblyContext()
                    .equals(this.assembly))) {

            // Registering at the Calculator in Request-Scope
            if (this.callScope.equals(CallScope.REQUEST)) {

                // Loading CommonMetrics-model
                URI uri = URI
                    .createURI(MetricDescriptionConstants.PATHMAP_METRIC_SPEC_MODELS_COMMON_METRICS_METRICSPEC);
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

                result = InterpreterResult.OK;

            } else {
                

                // set temporary stack for evaluation
                final SimulatedStackframe<Object> frame = this.interpreterDefaultContext.getStack().createAndPushNewStackFrame();
                Measure<Object, Quantity> measuringValue = responseTime
                    .getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC);
                
                // TODO Premise noch zuerst auf den originalen Stack auswerten, falls Parameter drauf sind (?)
                
                frame.addValue("ResponseTime.VALUE", (Double) measuringValue.getValue());

                if (!((boolean) interpreterDefaultContext.evaluate(premise.getSpecification(), this.interpreterDefaultContext.getStack()
                        .currentStackFrame()))) {
                    LOGGER.debug("Reponsetime Qualitygate broken: " + responseTime);
                    
                    

                    ResponseTimeIssue issue = new ResponseTimeIssue((Entity) this.stereotypedObject, qualitygate, false);

                    result = BasicInterpreterResult.of(issue);

                    // triggering probe to measure Success-To-Failure-Rate case violation
                    probeRegistry
                        .triggerProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext, false, null));

                    probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(qualitygate,
                            interpreterDefaultContext, false, qualitygate.getSeverity()));

                    recorder.recordQualitygateIssue(qualitygate, stereotypedObject, issue);
                    
                    if(qualitygate.getImpact() != null) {
                        
                        failureImpactList.addAll(qualitygate.getImpact().getFailure());
                        
                    }

                } else {
                    // triggering probe to measure Success-To-Failure-Rate case successful
                    probeRegistry
                        .triggerProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext, true, null));
                }
                
                // pop temporary stack
                this.interpreterDefaultContext.getStack().removeStackFrame();
                
                result = merger.merge(result, this.handleImpact(failureImpactList, interpreterDefaultContext));
            }

        }

        return result;
    }
    

    private InterpreterResult handleImpact(List<Failure> failureList, InterpreterDefaultContext interpreterDefaultContext) {
        
        
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
    

}
