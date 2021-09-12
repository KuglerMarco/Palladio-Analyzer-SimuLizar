package org.palladiosimulator.simulizar.qualitygate.interpreter;

import java.util.List;

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
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.pcm.seff.CallAction;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcmmeasuringpoint.ExternalCallActionMeasuringPoint;
import org.palladiosimulator.probeframework.ProbeFrameworkContext;
import org.palladiosimulator.probeframework.calculator.Calculator;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.CorruptContentBehavior;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.CrashBehavior;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.DelayBehavior;
import org.palladiosimulator.simulizar.interpreter.CallScope;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.StereotypeSwitch;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchStereotypeContributionFactory.RDSeffSwitchElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.preinterpretation.PreInterpretationBehavior;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.qualitygate.event.QualitygatePassedEvent;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ParameterIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ResponseTimeProxyIssue;
import org.palladiosimulator.simulizar.qualitygate.measurement.QualitygateViolationProbeRegistry;
import org.palladiosimulator.simulizar.qualitygate.propagation.QualitygatePropagationRecorder;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager;
import org.palladiosimulator.simulizar.utils.SimulatedStackHelper;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

/**
 * Switch to process the qualitygates attached at ExternalCalls.
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

    // Information about the stereotype it is processing
    private final String stereotypeName = "QualitygateElement";
    private final String profileName = "QualitygateProfile";

    // Information about the simulation-context
    private final InterpreterDefaultContext context;
    private final ProbeFrameworkContext frameworkContext;
    private Signature operationSignature;
    private final PCMPartitionManager partManager;

    // Information about the qualitygate-processing
    private static MeasuringValue responseTime;
    private QualityGate qualitygate;
    private PCMRandomVariable premise;
    private Entity stereotypedObject;
    private CallScope callScope = CallScope.REQUEST;
    private boolean atRequestMetricCalcAdded = false;

    private final BasicInterpreterResultMerger merger;

    private static final Logger LOGGER = Logger.getLogger(RDSeffSwitchQualitygateContributionSwitch.class);

    // Probe-Registry
    private QualitygateViolationProbeRegistry probeRegistry;
    private AssemblyContext assembly;
    private QualitygatePropagationRecorder recorder;

    @AssistedInject
    public RDSeffSwitchQualitygateContributionSwitch(@Assisted final InterpreterDefaultContext context,
            @Assisted RDSeffSwitchElementDispatcher parentSwitch, BasicInterpreterResultMerger merger,
            ProbeFrameworkContext frameworkContext, PCMPartitionManager partManager,
            QualitygateViolationProbeRegistry probeRegistry, QualitygatePropagationRecorder recorder) {

        this.context = context;

        // Injected
        this.merger = merger;
        this.partManager = partManager;
        this.frameworkContext = frameworkContext;
        this.probeRegistry = probeRegistry;
        this.assembly = context.getAssemblyContextStack()
            .get(1);
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

        this.operationSignature = ((ExternalCallAction) theEObject).getCalledService_ExternalService();
        this.stereotypedObject = (Entity) theEObject;
        this.callScope = callScope;

        EList<QualityGate> qualitygates = StereotypeAPI.getTaggedValue(theEObject, "qualitygate", stereotype.getName());

        // Model validation
        if (qualitygates.isEmpty()) {
            throw new IllegalArgumentException(
                    "Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
        }

        // Processing all the attached Qualitygates
        for (QualityGate qualitygate : qualitygates) {

            LOGGER.debug("RepositoryCompoonent: " + qualitygate.getPremise()
                .getSpecification());

            if (theEObject instanceof ExternalCallAction) {
                result = merger.merge(result, this.doSwitch(qualitygate));
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
        responseTime = newMeasurement.getMeasuringValueForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC);
        LOGGER.debug("Added a new Measurement: " + responseTime);
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
        this.premise = qualitygate.getPremise();
        return this.doSwitch(qualitygate.getScope());

    }

    /**
     * Processing in RequestMetricScope.
     *
     */
    @Override
    public InterpreterResult caseRequestMetricScope(RequestMetricScope object) {

        InterpreterResult result = InterpreterResult.OK;

        if (qualitygate.getAssemblyContext() == null || qualitygate.getAssemblyContext()
            .equals(this.assembly)) {

            // Registering at the Calculator in Request-Scope
            if (callScope.equals(CallScope.REQUEST)) {

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
                    if (e instanceof ExternalCallActionMeasuringPoint) {
                        if (((ExternalCallActionMeasuringPoint) e).getExternalCall()
                            .equals(stereotypedObject)) {

                            measPoint = (ExternalCallActionMeasuringPoint) e;

                        }
                    }
                }

                // TODO wenn kein measuringPoint vorliegt, dann eventuell einfach kein Monitor gesetzt?
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

            } else {
                /*
                 * Response-Time is checked, when the measurements are available - Processing-Proxy
                 * is added as Issue
                 */
                LOGGER.debug("New ResponseTimeProxyIssue.");
                result = InterpreterResult
                    .of(new ResponseTimeProxyIssue(premise, this, qualitygate, stereotypedObject, this.context, true));

            }
        }

        return result;

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
                    ParameterIssue issue = new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                            this.context.getStack()
                                .currentStackFrame()
                                .getContents(),
                            true);

                    result = BasicInterpreterResult.of(issue);

                    // triggering probe to measure Success-To-Failure-Rate case violated
                    probeRegistry.triggerProbe(new QualitygatePassedEvent(qualitygate, context, false, null));

                    probeRegistry.triggerSeverityProbe(
                            new QualitygatePassedEvent(qualitygate, context, false, qualitygate.getSeverity()));

                    recorder.recordQualitygateIssue(qualitygate, stereotypedObject, issue);
                    
                    if(qualitygate.getImpact() != null) {
                        
                        result = merger.merge(result, this.handleImpact(qualitygate.getImpact().getFailure(), context));
                        
                    }

                } else {
                    // triggering probe to measure Success-To-Failure-Rate case successful
                    probeRegistry.triggerProbe(new QualitygatePassedEvent(qualitygate, context, true, null));
                }

                // Removing Stack again
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
        InterpreterResult result = InterpreterResult.OK;

        if (callScope.equals(CallScope.RESPONSE) && (signatureOfQualitygate == (this.operationSignature))) {

            if (!((boolean) context.evaluate(premise.getSpecification(), this.context.getStack()
                .currentStackFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken at ExternalCall: " + premise.getSpecification()
                            + " because resultframe is: " + this.context.getStack()
                                .currentStackFrame()
                                .toString());
                }

                ParameterIssue issue = new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.context.getCurrentResultFrame()
                            .getContents(),
                        false);

                result = BasicInterpreterResult.of(issue);

                // triggering probe to measure Success-To-Failure-Rate case violation
                probeRegistry.triggerProbe(new QualitygatePassedEvent(qualitygate, context, false, null));

                probeRegistry.triggerSeverityProbe(
                        new QualitygatePassedEvent(qualitygate, context, false, qualitygate.getSeverity()));

                recorder.recordQualitygateIssue(qualitygate, stereotypedObject, issue);
                
                if(qualitygate.getImpact() != null) {
                    
                    result = merger.merge(result, this.handleImpact(qualitygate.getImpact().getFailure(), context));
                    
                }

            } else {
                // triggering probe to measure Success-To-Failure-Rate case successful
                probeRegistry.triggerProbe(new QualitygatePassedEvent(qualitygate, context, true, null));
            }
        }
        return result;
    }
    

    /**
     * Last measurement of the ResponseTime-calculators
     *
     */
    public MeasuringValue getLastResponseTimeMeasure() {

        return RDSeffSwitchQualitygateContributionSwitch.responseTime;

    }

    public boolean isMonitorPresent() {

        // TODO Monitor Überprüfung
        MonitorRepository monitorRepo = (MonitorRepository) partManager.getBlackboard()
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY)
            .get(0);

        for (Monitor monitor : monitorRepo.getMonitors()) {

            if (monitor.getMeasuringPoint() instanceof ExternalCallActionMeasuringPoint) {

                if (((ExternalCallActionMeasuringPoint) monitor.getMeasuringPoint()).getExternalCall()
                    .equals(stereotypedObject)) {

                    for (MeasurementSpecification specification : monitor.getMeasurementSpecifications()) {
                        if (specification.getMetricDescription()
                            .equals(MetricDescriptionConstants.RESPONSE_TIME_METRIC)) {
                            return true;
                        }
                    }

                }

            }

        }
        return false;
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
