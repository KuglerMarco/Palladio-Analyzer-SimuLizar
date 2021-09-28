package org.palladiosimulator.simulizar.qualitygate.interpreter;

import java.util.List;

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
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcmmeasuringpoint.ExternalCallActionMeasuringPoint;
import org.palladiosimulator.probeframework.ProbeFrameworkContext;
import org.palladiosimulator.probeframework.calculator.Calculator;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.CorruptContentBehavior;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.CrashBehavior;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.DelayBehavior;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.preinterpretation.PreInterpretationBehavior;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.stereotype.CallScope;
import org.palladiosimulator.simulizar.interpreter.stereotype.RDSeffSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.stereotype.StereotypeSwitch;
import org.palladiosimulator.simulizar.interpreter.stereotype.RDSeffSwitchStereotypeContributionFactory.RDSeffSwitchElementDispatcher;
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

    // Information about the stereotype
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
    private PCMRandomVariable predicate;
    private Entity stereotypedObject;
    private RequiredRole requiredRole;
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
     * Entry-Point to process the attached qualitygates at the model element.
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
            result = InterpreterResult
                .of(new ResponseTimeProxyIssue(predicate, this, qualitygate, stereotypedObject, this.context, this.requiredRole));
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

        if (callScope.equals(CallScope.REQUEST) && (signatureOfQualitygate == (this.operationSignature))) {

            /*
             * Pushes the new frame for evaluation, because in RDSeffSwitch, stack is only pushed
             * after this stereotype processing
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
                        true);

                result = BasicInterpreterResult.of(issue);

                // triggering probe to measure Success-To-Failure-Rate case violated
                probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(qualitygate, context, false, null, this.requiredRole));

                probeRegistry.triggerSeverityProbe(
                        new QualitygatePassedEvent(qualitygate, context, false, qualitygate.getSeverity(), this.requiredRole));

                recorder.recordQualitygateIssue(qualitygate, stereotypedObject, issue);

                if (qualitygate.getImpact() != null) {

                    result = merger.merge(result, this.handleImpact(qualitygate.getImpact(), context));

                }

            } else {
                // triggering probe to measure Success-To-Failure-Rate case successful
                probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(qualitygate, context, true, null, this.stereotypedObject));
            }

            // Removing Stack again
            this.context.getStack()
                .removeStackFrame();

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

            if (!((boolean) context.evaluate(predicate.getSpecification(), this.context.getStack()
                .currentStackFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken at ExternalCall: " + predicate.getSpecification()
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
                probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(qualitygate, context, false, null, this.requiredRole));

                probeRegistry.triggerSeverityProbe(
                        new QualitygatePassedEvent(qualitygate, context, false, qualitygate.getSeverity(), this.requiredRole));

                recorder.recordQualitygateIssue(qualitygate, stereotypedObject, issue);

                if (qualitygate.getImpact() != null) {
                    result = merger.merge(result, this.handleImpact(qualitygate.getImpact(), context));
                }

            } else {
                // triggering probe to measure Success-To-Failure-Rate case successful
                probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(qualitygate, context, true, null, this.stereotypedObject));
            }
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

}
