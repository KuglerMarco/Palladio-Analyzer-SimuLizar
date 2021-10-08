package org.palladiosimulator.simulizar.qualitygate.measurement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygatemeasuringpoint.ComponentInterfaceMeasuringPoint;
import org.palladiosimulator.failuremodel.qualitygatemeasuringpoint.QualitygateMeasuringPoint;
import org.palladiosimulator.failuremodel.qualitygatemeasuringpoint.SeverityMeasuringPoint;
import org.palladiosimulator.metricspec.CaptureType;
import org.palladiosimulator.metricspec.DataType;
import org.palladiosimulator.metricspec.Identifier;
import org.palladiosimulator.metricspec.MetricDescriptionRepository;
import org.palladiosimulator.metricspec.MetricSetDescription;
import org.palladiosimulator.metricspec.MetricSpecFactory;
import org.palladiosimulator.metricspec.Scale;
import org.palladiosimulator.metricspec.ScopeOfValidity;
import org.palladiosimulator.metricspec.TextualBaseMetricDescription;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.probeframework.calculator.DefaultCalculatorProbeSets;
import org.palladiosimulator.probeframework.calculator.IGenericCalculatorFactory;
import org.palladiosimulator.simulizar.entity.EntityReference;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.qualitygate.event.QualitygatePassedEvent;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.QualitygateIssue;
import org.palladiosimulator.simulizar.qualitygate.metric.QualitygateMetricDescriptionConstants;
import org.palladiosimulator.simulizar.runtimestate.RuntimeStateEntityManager;
import org.palladiosimulator.simulizar.scopes.RuntimeExtensionScope;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager.Global;

import de.uka.ipd.sdq.simucomframework.model.SimuComModel;
import de.uka.ipd.sdq.simucomframework.probes.TakeCurrentSimulationTimeProbe;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationControl;

/**
 * Registry for the probes used to evaluate the Qualitygates during simulation.
 * 
 * @author Marco Kugler
 *
 */
@RuntimeExtensionScope
public class QualitygateViolationProbeRegistry implements RuntimeStateEntityManager {

    private static final Logger LOGGER = Logger.getLogger(QualitygateViolationProbeRegistry.class);

    private final ISimulationControl simulationControl;
    private final PCMResourceSetPartition pcmPartition;
    private final IGenericCalculatorFactory calculatorFactory;

    // Probes for QualitygateViolationOverTime
    private Map<String, QualitygateCheckingTriggeredProbeList> violationProbes = new HashMap<String, QualitygateCheckingTriggeredProbeList>();
    private Map<String, Boolean> isViolationProbeCreated = new HashMap<String, Boolean>();

    // Probes for QualitygateViolationOverTime at a component interface
    private Map<String, QualitygateCheckingTriggeredProbeList> violationProbesAtInterface = new HashMap<String, QualitygateCheckingTriggeredProbeList>();
    private Map<String, Boolean> isViolationProbeCreatedAtInterface = new HashMap<String, Boolean>();

    // Probes for InvolvedIssuesOverTime
    private Map<String, QualitygateCheckingTriggeredProbeList> propagationProbes = new HashMap<String, QualitygateCheckingTriggeredProbeList>();
    private Map<String, Boolean> isIssueProbeCreated = new HashMap<String, Boolean>();

    // MetricDescriptions for InvolvedIssuesOverTime
    private TextualBaseMetricDescription involvedIssuesMetric;
    private MetricSetDescription involvedIssuesOverTimeMetric;
    private MetricDescriptionRepository metricRepository;
    private Map<String, Identifier> createdIdentifierForInvolvedIssues = new HashMap<String, Identifier>();

    // Probe for SeverityOverTime
    private QualitygateCheckingTriggeredProbeList severityProbe;

    // Probe for SeverityOverTime at an interface of a component
    private Map<String, QualitygateCheckingTriggeredProbeList> severityProbesAtInterface = new HashMap<String, QualitygateCheckingTriggeredProbeList>();
    private Map<String, Boolean> isSeverityProbeCreatedAtInterface = new HashMap<String, Boolean>();

    // Identifier for Severity
    private TextualBaseMetricDescription severityMetric;
    private MetricSetDescription severityOverTimeMetric;
    private Map<String, Identifier> createdIdentifierForSeverity = new HashMap<String, Identifier>();

    private final SimuComModel simucomModel;

    @Inject
    public QualitygateViolationProbeRegistry(@Global final PCMResourceSetPartition pcmPartition,
            IGenericCalculatorFactory calculatorFactory, final ISimulationControl simulationControl,
            SimuComModel simucomModel) {

        this.simucomModel = simucomModel;
        this.pcmPartition = pcmPartition;
        this.calculatorFactory = calculatorFactory;
        this.simulationControl = simulationControl;

        this.metricRepository = MetricSpecFactory.eINSTANCE.createMetricDescriptionRepository();
        this.involvedIssuesMetric = this.createInvolvedIssuesMetric();
        this.involvedIssuesOverTimeMetric = this.createInvolvedIssueMetricSet();

        this.severityMetric = this.createSeverityMetric();
        this.severityOverTimeMetric = this.createSeverityMetricSet();

        LOGGER.setLevel(Level.DEBUG);

    }

    /**
     * Creates the Metric for the involved issues for a Qualitygate violation
     * 
     * @return
     */
    private TextualBaseMetricDescription createInvolvedIssuesMetric() {

        TextualBaseMetricDescription result = MetricSpecFactory.eINSTANCE.createTextualBaseMetricDescription();
        result.setCaptureType(CaptureType.IDENTIFIER);
        result.setDataType(DataType.QUALITATIVE);
        result.setName("InvolvedFailures");
        result.setScale(Scale.NOMINAL);
        result.setScopeOfValidity(ScopeOfValidity.DISCRETE);
        result.setTextualDescription("This Metric represents the correlating failures.");
        // Identifier to count how many times the Qualitygate was broken in general
        Identifier defaultBrokenIdentifier = MetricSpecFactory.eINSTANCE.createIdentifier();
        this.createdIdentifierForInvolvedIssues.put("default", defaultBrokenIdentifier);
        defaultBrokenIdentifier.setLiteral("Number of Occurrence");
        result.getIdentifiers()
            .add(defaultBrokenIdentifier);
        result.setRepository(this.metricRepository);

        return result;

    }

    private TextualBaseMetricDescription createSeverityMetric() {

        TextualBaseMetricDescription result = MetricSpecFactory.eINSTANCE.createTextualBaseMetricDescription();
        result.setCaptureType(CaptureType.IDENTIFIER);
        result.setDataType(DataType.QUALITATIVE);
        result.setName("Severity");
        result.setScale(Scale.NOMINAL);
        result.setScopeOfValidity(ScopeOfValidity.DISCRETE);
        result.setTextualDescription("Represents the severity of a failure.");
        result.setRepository(this.metricRepository);

        return result;
    }

    /**
     * Creates the Metric-Set for the involved issues
     * 
     * @return
     */
    private MetricSetDescription createInvolvedIssueMetricSet() {

        MetricSetDescription result = MetricSpecFactory.eINSTANCE.createMetricSetDescription();
        result.setName("InvolvedFailuresOverTime");
        result.getSubsumedMetrics()
            .add(MetricDescriptionConstants.POINT_IN_TIME_METRIC);
        result.getSubsumedMetrics()
            .add(this.involvedIssuesMetric);
        result.setTextualDescription("Represents correlating failures plus the time of occurrence.");
        result.setRepository(this.metricRepository);

        return result;
    }

    /**
     * Creates the Metric-Set for the involved issues
     * 
     * @return
     */
    private MetricSetDescription createSeverityMetricSet() {

        MetricSetDescription result = MetricSpecFactory.eINSTANCE.createMetricSetDescription();
        result.setName("SeverityOverTime");
        result.getSubsumedMetrics()
            .add(MetricDescriptionConstants.POINT_IN_TIME_METRIC);
        result.getSubsumedMetrics()
            .add(this.severityMetric);
        result.setTextualDescription("Represents the severity of a failure plus the time of occurrence.");
        result.setRepository(this.metricRepository);

        return result;
    }

    /**
     * Triggers the probes for QualitygateViolationOverTime
     * 
     * @param event
     */
    public void triggerViolationProbe(final QualitygatePassedEvent event) {

        triggerViolationProbeAtInterface(event);

        // Create probe and calculator for this qualitygate
        if (!isViolationProbeCreated.containsKey(event.getModelElement()
            .getId())) {

            QualitygateMeasuringPoint measuringPoint = this.findQualitygateMonitorInRepository(event.getModelElement());

            if (measuringPoint != null) {

                // Creating Probes
                QualitygateCheckingProbe probe = new QualitygateCheckingProbe(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC);

                TakeCurrentSimulationTimeProbe timeProbe = new TakeCurrentSimulationTimeProbe(simulationControl);

                QualitygateCheckingTriggeredProbeList probeOverTime = new QualitygateCheckingTriggeredProbeList(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME,
                        Arrays.asList(timeProbe, probe));

                this.violationProbes.put(event.getModelElement()
                    .getId(), probeOverTime);

                this.isViolationProbeCreated.put(event.getModelElement()
                    .getId(), true);

                // Creating and registering the calculator
                this.calculatorFactory.buildCalculator(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME, measuringPoint,
                        DefaultCalculatorProbeSets.createSingularProbeConfiguration(probeOverTime));

            } else {
                this.isViolationProbeCreated.put(event.getModelElement()
                    .getId(), false);
            }
        }

        // trigger probe according to the evaluation
        if (event.isSuccess() && this.isViolationProbeCreated.get(event.getModelElement()
            .getId()) && this.simulationIsRunning() && !event.isCrash()) {
            this.violationProbes.get(event.getModelElement()
                .getId())
                .takeMeasurement(event.getThread()
                    .getRequestContext(), QualitygateMetricDescriptionConstants.SUCCESS);

        } else if (this.isViolationProbeCreated.get(event.getModelElement()
            .getId()) && this.simulationIsRunning() && !event.isCrash()) {

            this.violationProbes.get(event.getModelElement()
                .getId())
                .takeMeasurement(event.getThread()
                    .getRequestContext(), QualitygateMetricDescriptionConstants.VIOLATION);
        } else if (this.isViolationProbeCreated.get(event.getModelElement()
            .getId()) && this.simulationIsRunning()) {

            this.violationProbes.get(event.getModelElement()
                .getId())
                .takeMeasurement(event.getThread()
                    .getRequestContext(), QualitygateMetricDescriptionConstants.CRASH);

        }

    }

    /**
     * Triggers the probes for QualitygateViolationOverTime
     * 
     * @param event
     */
    public void triggerViolationProbeAtInterface(final QualitygatePassedEvent event) {

        // Create probe and calculator for this qualitygate
        if (!isViolationProbeCreatedAtInterface.containsKey(event.getStereotypedObject()
            .getId())) {

            ComponentInterfaceMeasuringPoint measuringPoint = this
                .findInterfaceMonitorInRepository(event.getStereotypedObject());

            if (measuringPoint != null) {

                // Creating Probes
                QualitygateCheckingProbe probe = new QualitygateCheckingProbe(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC);

                TakeCurrentSimulationTimeProbe timeProbe = new TakeCurrentSimulationTimeProbe(simulationControl);

                QualitygateCheckingTriggeredProbeList probeOverTime = new QualitygateCheckingTriggeredProbeList(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME,
                        Arrays.asList(timeProbe, probe));

                this.violationProbesAtInterface.put(event.getStereotypedObject()
                    .getId(), probeOverTime);

                this.isViolationProbeCreatedAtInterface.put(event.getStereotypedObject()
                    .getId(), true);

                // Creating and registering the calculator
                this.calculatorFactory.buildCalculator(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME, measuringPoint,
                        DefaultCalculatorProbeSets.createSingularProbeConfiguration(probeOverTime));

            } else {
                this.isViolationProbeCreatedAtInterface.put(event.getStereotypedObject()
                    .getId(), false);
            }
        }

        // trigger probe according to the evaluation
        if (event.isSuccess() && this.isViolationProbeCreatedAtInterface.get(event.getStereotypedObject()
            .getId()) && this.simulationIsRunning() && !event.isCrash()) {
            this.violationProbesAtInterface.get(event.getStereotypedObject()
                .getId())
                .takeMeasurement(event.getThread()
                    .getRequestContext(), QualitygateMetricDescriptionConstants.SUCCESS);

        } else if (this.isViolationProbeCreatedAtInterface.get(event.getStereotypedObject()
            .getId()) && this.simulationIsRunning() && !event.isCrash()) {

            this.violationProbesAtInterface.get(event.getStereotypedObject()
                .getId())
                .takeMeasurement(event.getThread()
                    .getRequestContext(), QualitygateMetricDescriptionConstants.VIOLATION);
        } else if (this.isViolationProbeCreatedAtInterface.get(event.getStereotypedObject()
            .getId()) && this.simulationIsRunning()) {

            this.violationProbesAtInterface.get(event.getStereotypedObject()
                .getId())
                .takeMeasurement(event.getThread()
                    .getRequestContext(), QualitygateMetricDescriptionConstants.CRASH);

        }

    }

    /**
     * Triggers the probes for SeverityOverTime
     * 
     * @param event
     */
    public void triggerSeverityProbe(final QualitygatePassedEvent event) {

        triggerSeverityProbeAtInterface(event);

        if (this.severityProbe == null) {

            SeverityMeasuringPoint measuringPoint = this.findSeverityMonitorInRepository();

            if (measuringPoint != null) {

                // Creating Probes
                QualitygateCheckingProbe probe = new QualitygateCheckingProbe(this.severityMetric);

                TakeCurrentSimulationTimeProbe timeProbe = new TakeCurrentSimulationTimeProbe(simulationControl);

                QualitygateCheckingTriggeredProbeList probeOverTime = new QualitygateCheckingTriggeredProbeList(
                        this.severityOverTimeMetric, Arrays.asList(timeProbe, probe));

                this.severityProbe = probeOverTime;

                this.calculatorFactory.buildCalculator(this.severityOverTimeMetric, measuringPoint,
                        DefaultCalculatorProbeSets.createSingularProbeConfiguration(probeOverTime));

            }

        }

        if (event.getSeverity() != null && severityProbe != null && this.simulationIsRunning()) {

            if (!this.createdIdentifierForSeverity.containsKey(event.getSeverity()
                .getEntityName())) {

                // Adding Identifier for every issue not yet registered as Identifier
                Identifier identifier = MetricSpecFactory.eINSTANCE.createIdentifier();
                identifier.setLiteral(event.getSeverity()
                    .getEntityName());
                this.severityMetric.getIdentifiers()
                    .add(identifier);
                this.createdIdentifierForSeverity.put(event.getSeverity()
                    .getEntityName(), identifier);

            }

            this.severityProbe.takeMeasurement(this.createdIdentifierForSeverity.get(event.getSeverity()
                .getEntityName()));

        }
    }

    /**
     * Triggers the probes for SeverityOverTime
     * 
     * @param event
     */
    private void triggerSeverityProbeAtInterface(final QualitygatePassedEvent event) {

        // Create probe and calculator for this qualitygate
        if (!isSeverityProbeCreatedAtInterface.containsKey(event.getStereotypedObject()
            .getId())) {

            ComponentInterfaceMeasuringPoint measuringPoint = this
                .findInterfaceMonitorInRepository(event.getStereotypedObject());

            if (measuringPoint != null) {

                // Creating Probes
                QualitygateCheckingProbe probe = new QualitygateCheckingProbe(this.severityMetric);

                TakeCurrentSimulationTimeProbe timeProbe = new TakeCurrentSimulationTimeProbe(simulationControl);

                QualitygateCheckingTriggeredProbeList probeOverTime = new QualitygateCheckingTriggeredProbeList(
                        this.severityOverTimeMetric, Arrays.asList(timeProbe, probe));

                this.severityProbesAtInterface.put(event.getStereotypedObject()
                    .getId(), probeOverTime);

                this.isSeverityProbeCreatedAtInterface.put(event.getStereotypedObject()
                    .getId(), true);

                // Creating and registering the calculator
                this.calculatorFactory.buildCalculator(this.severityOverTimeMetric, measuringPoint,
                        DefaultCalculatorProbeSets.createSingularProbeConfiguration(probeOverTime));

            } else {
                this.isSeverityProbeCreatedAtInterface.put(event.getStereotypedObject()
                    .getId(), false);
            }
        }

        // trigger probe according to the evaluation
        if (!event.isSuccess() && this.isSeverityProbeCreatedAtInterface.get(event.getStereotypedObject()
            .getId()) && this.simulationIsRunning() && event.getSeverity() != null) {
            if (!this.createdIdentifierForSeverity.containsKey(event.getSeverity()
                .getEntityName())) {

                // Adding Identifier for every issue not yet registered as Identifier
                Identifier identifier = MetricSpecFactory.eINSTANCE.createIdentifier();
                identifier.setLiteral(event.getSeverity()
                    .getEntityName());
                this.severityMetric.getIdentifiers()
                    .add(identifier);
                this.createdIdentifierForSeverity.put(event.getSeverity()
                    .getEntityName(), identifier);

            }

            this.severityProbesAtInterface.get(event.getStereotypedObject()
                .getId())
                .takeMeasurement(this.createdIdentifierForSeverity.get(event.getSeverity()
                    .getEntityName()));

        }

    }

    /**
     * Triggers the probes for InvolvedIssuesOverTime
     * 
     * @param event
     */
    public void triggerInvolvedIssuesProbe(List<InterpretationIssue> issuesWhenBroken,
            EntityReference<QualityGate> qualitygateRef) {

        QualityGate qualitygate = qualitygateRef.getModelElement(pcmPartition);

        // in case no probe for this Qualitygate is registered
        if (!propagationProbes.containsKey(qualitygate.getId())) {

            QualitygateMeasuringPoint measuringPoint = this.findQualitygateMonitorInRepository(qualitygate);

            if (measuringPoint != null) {

                // Creating Probes
                QualitygateCheckingProbe probe = new QualitygateCheckingProbe(involvedIssuesMetric);

                TakeCurrentSimulationTimeProbe timeProbe = new TakeCurrentSimulationTimeProbe(simulationControl);

                QualitygateCheckingTriggeredProbeList probeOverTime = new QualitygateCheckingTriggeredProbeList(
                        involvedIssuesOverTimeMetric, Arrays.asList(timeProbe, probe));

                this.propagationProbes.put(qualitygate.getId(), probeOverTime);

                this.isIssueProbeCreated.put(qualitygate.getId(), true);

                // Creating and registering the calculator
                this.calculatorFactory.buildCalculator(involvedIssuesOverTimeMetric, measuringPoint,
                        DefaultCalculatorProbeSets.createSingularProbeConfiguration(probeOverTime));

            } else {
                this.isIssueProbeCreated.put(qualitygate.getId(), false);
            }
        }

        if (this.isIssueProbeCreated.get(qualitygate.getId()) && this.simulationIsRunning()) {

            this.propagationProbes.get(qualitygate.getId())
                .takeMeasurement(createdIdentifierForInvolvedIssues.get("default"));

            for (InterpretationIssue issue : issuesWhenBroken) {

                if (issue instanceof QualitygateIssue) {
                    if (!this.createdIdentifierForInvolvedIssues
                        .containsKey(((QualitygateIssue) issue).getQualitygateId())) {

                        // Adding Identifier for every issue not yet registered as Identifier
                        Identifier identifier = MetricSpecFactory.eINSTANCE.createIdentifier();
                        identifier.setLiteral(((QualitygateIssue) issue).getQualitygateRef()
                            .getModelElement(pcmPartition)
                            .getEntityName());
                        this.involvedIssuesMetric.getIdentifiers()
                            .add(identifier);
                        this.createdIdentifierForInvolvedIssues.put(((QualitygateIssue) issue).getQualitygateId(),
                                identifier);

                    }

                    this.propagationProbes.get(qualitygate.getId())
                        .takeMeasurement(
                                createdIdentifierForInvolvedIssues.get(((QualitygateIssue) issue).getQualitygateId()));

                }
            }
        }
    }

    @Override
    public void cleanup() {
        this.violationProbes.clear();
        this.propagationProbes.clear();
        this.createdIdentifierForInvolvedIssues.clear();
        this.createdIdentifierForSeverity.clear();
        this.isIssueProbeCreated.clear();
        this.isSeverityProbeCreatedAtInterface.clear();
        this.isViolationProbeCreated.clear();
        this.severityProbesAtInterface.clear();
        this.violationProbesAtInterface.clear();
    }

    /**
     * Finds the QualitygateMonitor in repository, returns null if not found.
     * 
     * @param qualitygate
     * @return
     */
    private QualitygateMeasuringPoint findQualitygateMonitorInRepository(final QualityGate qualitygate) {

        MonitorRepository monitorRepo = (MonitorRepository) pcmPartition
            .getElement(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository())
            .stream()
            .findAny()
            .orElseThrow(() -> new IllegalStateException("No MonitorRepository found!"));

        for (Monitor monitor : monitorRepo.getMonitors()) {

            if (monitor.getMeasuringPoint() instanceof QualitygateMeasuringPoint) {

                if (((QualitygateMeasuringPoint) monitor.getMeasuringPoint()).getQualitygate()
                    .getId()
                    .equals(qualitygate.getId())) {
                    return (QualitygateMeasuringPoint) monitor.getMeasuringPoint();
                }
            }
        }

        return null;
    }

    /**
     * Finds the QualitygateMonitor in repository, returns null if not found.
     * 
     * @param qualitygate
     * @return
     */
    private ComponentInterfaceMeasuringPoint findInterfaceMonitorInRepository(final Entity stereotypedObject) {

        MonitorRepository monitorRepo = (MonitorRepository) pcmPartition
            .getElement(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository())
            .stream()
            .findAny()
            .orElseThrow(() -> new IllegalStateException("No MonitorRepository found!"));

        for (Monitor monitor : monitorRepo.getMonitors()) {

            if (monitor.getMeasuringPoint() instanceof ComponentInterfaceMeasuringPoint) {

                if (((ComponentInterfaceMeasuringPoint) monitor.getMeasuringPoint()).getRole()
                    .getId()
                    .equals(stereotypedObject.getId())) {
                    return (ComponentInterfaceMeasuringPoint) monitor.getMeasuringPoint();
                }
            }
        }

        return null;
    }

    /**
     * Finds the SeverityMonitor in repository, returns null if not found.
     * 
     * @param qualitygate
     * @return
     */
    private SeverityMeasuringPoint findSeverityMonitorInRepository() {

        MonitorRepository monitorRepo = (MonitorRepository) pcmPartition
            .getElement(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository())
            .stream()
            .findAny()
            .orElseThrow(() -> new IllegalStateException("No MonitorRepository found!"));

        org.palladiosimulator.pcm.system.System systemModel = (org.palladiosimulator.pcm.system.System) pcmPartition
            .getElement(org.palladiosimulator.pcm.system.SystemPackage.eINSTANCE.getSystem())
            .stream()
            .findAny()
            .orElseThrow(() -> new IllegalStateException("No System found!"));

        for (Monitor monitor : monitorRepo.getMonitors()) {

            if (monitor.getMeasuringPoint() instanceof SeverityMeasuringPoint) {

                if (((SeverityMeasuringPoint) monitor.getMeasuringPoint()).getSystem()
                    .equals(systemModel)) {
                    return (SeverityMeasuringPoint) monitor.getMeasuringPoint();
                }
            }
        }

        return null;
    }

    private boolean simulationIsRunning() {
        return this.simucomModel.getSimulationControl()
            .isRunning();
    }

}
