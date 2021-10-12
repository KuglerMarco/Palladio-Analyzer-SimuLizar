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
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.pcmmeasuringpoint.AssemblyOperationMeasuringPoint;
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
import org.palladiosimulator.simulizar.interpreter.stereotype.RepositoryComponentSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.stereotype.StereotypeSwitch;
import org.palladiosimulator.simulizar.interpreter.stereotype.RepositoryComponentSwitchStereotypeContributionFactory.RepositoryComponentSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.qualitygate.event.QualitygatePassedEvent;
import org.palladiosimulator.simulizar.qualitygate.eventbasedcommunication.RequestContextFailureRegistry;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ParameterIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.ProcessingTimeIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.QualitygateIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.proxy.CrashProxyIssue;
import org.palladiosimulator.simulizar.qualitygate.measurement.QualitygateViolationProbeRegistry;
import org.palladiosimulator.simulizar.qualitygate.measurement.eventbasedcommunication.EventBasedCommunicationProbeRegistry;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;
import de.uka.ipd.sdq.simucomframework.variables.stackframe.SimulatedStackframe;

/**
 * Switch to process the qualitygates attached at elements of ProvidedRoles.
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
    private PCMRandomVariable predicate;
    private AssemblyContext assembly;
    private ProvidedRole providedRole;

    private static MeasuringValue responseTime;

    private final BasicInterpreterResultMerger merger;
    private boolean atRequestMetricCalcAdded = false;
    private QualitygateViolationProbeRegistry probeRegistry;
    private EventBasedCommunicationProbeRegistry eventBasedRegistry;
    private RequestContextFailureRegistry failureRegistry;
    private static final Logger LOGGER = Logger.getLogger(RepositoryComponentSwitchQualitygateContributionSwitch.class);

    @AssistedInject
    public RepositoryComponentSwitchQualitygateContributionSwitch(@Assisted final InterpreterDefaultContext context,
            @Assisted final AssemblyContext assemblyContext, @Assisted final Signature signature,
            @Assisted final ProvidedRole providedRole,
            @Assisted RepositoryComponentSwitchStereotypeElementDispatcher parentSwitch,
            BasicInterpreterResultMerger merger, ProbeFrameworkContext frameworkContext,
            PCMPartitionManager partManager, QualitygateViolationProbeRegistry probeRegistry,
            EventBasedCommunicationProbeRegistry eventBasedRegistry, RequestContextFailureRegistry failureRegistry) {

        this.interpreterDefaultContext = context;
        this.operationSignature = signature;
        this.providedRole = providedRole;

        // Injected
        this.merger = merger;
        this.frameworkContext = frameworkContext;
        this.partManager = partManager;
        this.assembly = assemblyContext;
        this.probeRegistry = probeRegistry;
        this.eventBasedRegistry = eventBasedRegistry;
        this.failureRegistry = failureRegistry;

        LOGGER.setLevel(Level.DEBUG);
    }

    /**
     * Returns whether this Switch is appropriate for processing this stereotype. *
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
     * Entry-Point to process the attached qualitygate.
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
        // Nothing to do here
    }

    /**
     * Saving the qualitygate's predicate and the qualitygate-element itself.
     */
    @Override
    public InterpreterResult caseQualityGate(QualityGate qualitygate) {

        this.qualitygate = qualitygate;
        predicate = qualitygate.getPredicate();
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

        InterpreterResult result = InterpreterResult.OK;
        
        LOGGER.debug("Following StoEx is broken: " + predicate.getSpecification()
        + " because stackframe is: " + this.interpreterDefaultContext.getStack()
            .currentStackFrame()
            .toString());

        if (callScope.equals(CallScope.REQUEST) && object.getSignature() == this.operationSignature) {

            if (!((boolean) interpreterDefaultContext.evaluate(predicate.getSpecification(),
                    this.interpreterDefaultContext.getStack()
                        .currentStackFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + predicate.getSpecification()
                            + " because stackframe is: " + this.interpreterDefaultContext.getStack()
                                .currentStackFrame()
                                .toString());
                }

                ParameterIssue issue = new ParameterIssue((Entity) this.stereotypedObject, this.qualitygate,
                        this.interpreterDefaultContext.getStack()
                            .currentStackFrame()
                            .getContents(),
                        false);

                result = BasicInterpreterResult.of(issue);

                failureRegistry.addIssue(interpreterDefaultContext.getThread()
                    .getRequestContext(), issue);

                // triggering probe to measure Success-To-Failure-Rate case violated
                probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext,
                        false, null, this.stereotypedObject, false));

                probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext,
                        false, qualitygate.getSeverity(), this.stereotypedObject, false));

                this.triggerInvolvedIssueProbes(
                        failureRegistry.getInterpreterResult(this.interpreterDefaultContext.getThread()
                            .getRequestContext()));

                if (!qualitygate.getImpact().isEmpty()) {
                    result = merger.merge(result,
                            this.handleImpact(qualitygate.getImpact(), interpreterDefaultContext));
                }

            } else {
                // triggering probe to measure Success-To-Failure-Rate case successful
                probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext,
                        true, null, this.stereotypedObject, false));
            }

        }

        return result;
    }

    /**
     * Processing the ResultParameterScope.
     */
    @Override
    public InterpreterResult caseResultParameterScope(ResultParameterScope object) {

        InterpreterResult result = InterpreterResult.OK;

        if (callScope.equals(CallScope.RESPONSE) && object.getSignature() == this.operationSignature) {

            if (!((boolean) interpreterDefaultContext.evaluate(predicate.getSpecification(),
                    this.interpreterDefaultContext.getCurrentResultFrame()))) {

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StoEx is broken: " + predicate.getSpecification()
                            + " because resultframe is: " + this.interpreterDefaultContext.getCurrentResultFrame()
                                .toString());
                }

                // for potential Crash failures
                result = BasicInterpreterResult
                    .of(new CrashProxyIssue(qualitygate, interpreterDefaultContext, false, qualitygate.getSeverity(),
                            this.stereotypedObject, interpreterDefaultContext.getCurrentResultFrame()
                                .getContents()));

                if (qualitygate.getImpact() != null) {

                    result = merger.merge(result,
                            this.handleImpact(qualitygate.getImpact(), interpreterDefaultContext));

                }

            } else {

                result = BasicInterpreterResult.of(new CrashProxyIssue(qualitygate, interpreterDefaultContext, true,
                        null, this.stereotypedObject, interpreterDefaultContext.getCurrentResultFrame()
                            .getContents()));

            }

        }

        return result;
    }

    /**
     * Processing the RequestMetricScope
     *
     */
    @Override
    public InterpreterResult caseRequestMetricScope(RequestMetricScope object) {

        InterpreterResult result = InterpreterResult.OK;
        List<Failure> failureImpactList = new ArrayList<Failure>();

        if (this.operationSignature.equals(object.getSignature()) && this.providedRole.equals(stereotypedObject)) {

            // Registering at the Calculator in Request-Scope
            if (this.callScope.equals(CallScope.REQUEST)) {

                MetricDescription metricDesc = object.getMetric();

                /*
                 * MeasuringPointRepository needs to be loaded trough the MonitorRepository,
                 * otherwise the MeasuringPointRepository isn't be found at first run
                 */
                MonitorRepository monitorRepo = (MonitorRepository) partManager
                    .findModel(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY);

                MeasuringPointRepository measuringPointRepo = monitorRepo.getMonitors()
                    .get(0)
                    .getMeasuringPoint()
                    .getMeasuringPointRepository();

                // Searching for the Measuring-Point created in preprocessing
                MeasuringPoint measuringPointForQualitygate = null;

                for (MeasuringPoint measuringPoint : measuringPointRepo.getMeasuringPoints()) {
                    if (measuringPoint instanceof AssemblyOperationMeasuringPoint) {
                        if (((AssemblyOperationMeasuringPoint) measuringPoint).getOperationSignature()
                            .equals(object.getSignature())
                                && ((AssemblyOperationMeasuringPoint) measuringPoint).getRole()
                                    .equals(stereotypedObject)) {

                            measuringPointForQualitygate = (AssemblyOperationMeasuringPoint) measuringPoint;

                        }
                    }
                }

                if (measuringPointForQualitygate == null) {
                    throw new IllegalStateException(
                            "No MeasuringPoint found in MeasuringPointRepository for this Qualitygate.");
                }

                // Calculator for this Qualitygate
                Calculator calc = frameworkContext.getCalculatorRegistry()
                    .getCalculatorByMeasuringPointAndMetricDescription(measuringPointForQualitygate, metricDesc);

                // Adding this class as observer
                if (!this.atRequestMetricCalcAdded) {
                    calc.addObserver(this);
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Observer added at: " + measuringPointForQualitygate.getStringRepresentation());
                    }
                    this.atRequestMetricCalcAdded = true;
                }

            } else if (responseTime != null) {

                // evaluation of the measurements in response scope

                // set temporary stack for evaluation
                final SimulatedStackframe<Object> frame = this.interpreterDefaultContext.getStack()
                    .createAndPushNewStackFrame();
                Measure<Object, Quantity> measuringValue = responseTime
                    .getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC);

                String metricName = object.getMetric()
                    .getName()
                    .replace(" ", "")
                    .concat(".VALUE");

                frame.addValue(metricName, (Double) measuringValue.getValue());

                if (!((boolean) interpreterDefaultContext.evaluate(predicate.getSpecification(),
                        this.interpreterDefaultContext.getStack()
                            .currentStackFrame()))) {

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Reponsetime Qualitygate broken: " + responseTime);
                    }

                    // for potential Crash failures
                    result = BasicInterpreterResult.of(new CrashProxyIssue(qualitygate, interpreterDefaultContext,
                            false, qualitygate.getSeverity(), this.stereotypedObject, null));

                    if (qualitygate.getImpact() != null) {
                        failureImpactList.addAll(qualitygate.getImpact());
                    }

                } else {
                    result = BasicInterpreterResult.of(new CrashProxyIssue(qualitygate, interpreterDefaultContext, true,
                            null, this.stereotypedObject, null));
                }

                // pop temporary stack
                this.interpreterDefaultContext.getStack()
                    .removeStackFrame();

                result = merger.merge(result, this.handleImpact(failureImpactList, interpreterDefaultContext));
            }

        }

        return result;
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

    @Override
    public InterpreterResult caseEventBasedCommunicationScope(EventBasedCommunicationScope scope) {

        InterpreterResult result = InterpreterResult.OK;

        if (callScope.equals(CallScope.REQUEST) && scope.getSignature() == this.operationSignature) {

            if (qualitygate.getPredicate() == null) {
                // Start measurement
                eventBasedRegistry.startMeasurement(new ModelElementPassedEvent<QualityGate>(qualitygate,
                        EventType.BEGIN, interpreterDefaultContext));

            } else if (scope.getSignature() == this.operationSignature) {
                // End measurement
                MeasuringValue value = eventBasedRegistry.endMeasurement(new ModelElementPassedEvent<QualityGate>(
                        scope.getQualitygate(), EventType.END, interpreterDefaultContext));

                if (value != null) {

                    Measure<Object, Quantity> measuringValue = value
                        .getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC);

                    // set temporary stack for evaluation
                    final SimulatedStackframe<Object> frame = this.interpreterDefaultContext.getStack()
                        .createAndPushNewStackFrame();

                    List<Failure> failureImpactList = new ArrayList<Failure>();

                    String metricName = "ProcessingTime.VALUE";

                    frame.addValue(metricName, (Double) measuringValue.getValue());

                    if (!((boolean) interpreterDefaultContext.evaluate(predicate.getSpecification(),
                            this.interpreterDefaultContext.getStack()
                                .currentStackFrame()))) {

                        if (LOGGER.isDebugEnabled()) {
                            LOGGER.debug("Reponsetime Qualitygate broken: " + (Double) measuringValue.getValue());
                        }

                        ProcessingTimeIssue issue = new ProcessingTimeIssue((Entity) this.stereotypedObject,
                                this.qualitygate, false);

                        result = BasicInterpreterResult.of(issue);

                        failureRegistry.addIssue(interpreterDefaultContext.getThread()
                            .getRequestContext(), issue);

                        // triggering probe to measure Success-To-Failure-Rate case violated
                        probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(qualitygate,
                                interpreterDefaultContext, false, null, this.stereotypedObject, false));

                        probeRegistry
                            .triggerSeverityProbe(new QualitygatePassedEvent(qualitygate, interpreterDefaultContext,
                                    false, qualitygate.getSeverity(), this.stereotypedObject, false));

                        this.triggerInvolvedIssueProbes(failureRegistry
                                .getInterpreterResult(this.interpreterDefaultContext.getThread()
                                        .getRequestContext()));

                        if (!qualitygate.getImpact().isEmpty()) {
                            failureImpactList.addAll(qualitygate.getImpact());
                        }

                    } else {
                        // triggering probe to measure Success-To-Failure-Rate case successful
                        probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(qualitygate,
                                interpreterDefaultContext, true, null, this.stereotypedObject, false));
                    }

                    // pop temporary stack
                    this.interpreterDefaultContext.getStack()
                        .removeStackFrame();

                    result = merger.merge(result, this.handleImpact(failureImpactList, interpreterDefaultContext));

                }
            }

        }

        return result;
    }

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
