package org.palladiosimulator.simulizar.qualitygate.measurement;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
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

import de.uka.ipd.sdq.simucomframework.probes.TakeCurrentSimulationTimeProbe;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationControl;

/**
 * Registry for the probes used to evaluate the qualitygates during simulation.
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
    private Map<String, QualitygateCheckingTriggeredProbeList> qualitygateEvaluationProbe = new HashMap<String, QualitygateCheckingTriggeredProbeList>();
    
    private Map<String, Boolean> isProbeCreated = new HashMap<String, Boolean>();
    
    // Probes for InvolvedIssuesOverTime
    private Map<String, QualitygateCheckingTriggeredProbeList> interpreterResultIssueProbe = new HashMap<String, QualitygateCheckingTriggeredProbeList>();

    // MetricDescriptions for InvolvedIssuesOderTime
    private TextualBaseMetricDescription textMetricDesc;
    private MetricSetDescription metricSetDesc;
    private MetricDescriptionRepository metricRepository;
    private Map<String, Identifier> createdIdentifier = new HashMap<String, Identifier>();
    private Map<String, Boolean> isIssueProbeCreated = new HashMap<String, Boolean>();

    // Probes for SeverityOverTime
    private QualitygateCheckingTriggeredProbeList severityProbe;
    
    // Identifier for Severity
    private TextualBaseMetricDescription textMetricDescForSeverity;
    private Map<String, Identifier> createdIdentifierForSeverity = new HashMap<String, Identifier>();


    @Inject
    public QualitygateViolationProbeRegistry(@Global final PCMResourceSetPartition pcmPartition,
            IGenericCalculatorFactory calculatorFactory, final ISimulationControl simulationControl) {
        this.pcmPartition = pcmPartition;
        this.calculatorFactory = calculatorFactory;
        this.simulationControl = simulationControl;
        this.severityProbe = null;

        this.metricRepository = MetricSpecFactory.eINSTANCE.createMetricDescriptionRepository();
        this.textMetricDesc = this.createInvolvedIssuesMetric();
        this.metricSetDesc = this.createInvolvedIssueMetricSet();
        
        this.textMetricDescForSeverity = this.createSeverityMetric();

        LOGGER.setLevel(Level.DEBUG);

    }

    /**
     * Creates the Metric for the involved issues at a qualitygate break
     * 
     * @return
     */
    public TextualBaseMetricDescription createInvolvedIssuesMetric() {

        TextualBaseMetricDescription result = MetricSpecFactory.eINSTANCE.createTextualBaseMetricDescription();
        result.setCaptureType(CaptureType.IDENTIFIER);
        result.setDataType(DataType.QUALITATIVE);
        result.setName("InvolvedIssues");
        result.setScale(Scale.NOMINAL);
        result.setScopeOfValidity(ScopeOfValidity.DISCRETE);
        result.setTextualDescription(
                "This Metric represents the Issues which are involved when a qualitygate is broken.");
        // Identifier to count how many times the Qualitygate was broken in general
        Identifier defaultBrokenIdentifier = MetricSpecFactory.eINSTANCE.createIdentifier();
        this.createdIdentifier.put("default", defaultBrokenIdentifier);
        defaultBrokenIdentifier.setLiteral("Number of Occurrence");
        result.getIdentifiers()
            .add(defaultBrokenIdentifier);

        result.setRepository(this.metricRepository);

        return result;

    }
    
    public TextualBaseMetricDescription createSeverityMetric() {
        TextualBaseMetricDescription result = MetricSpecFactory.eINSTANCE.createTextualBaseMetricDescription();
        result.setCaptureType(CaptureType.IDENTIFIER);
        result.setDataType(DataType.QUALITATIVE);
        result.setName("Severity");
        result.setScale(Scale.NOMINAL);
        result.setScopeOfValidity(ScopeOfValidity.DISCRETE);
        result.setTextualDescription(
                "This Metric represents severity of simulation runs.");
        return result;
    }

    /**
     * Creates the Metric-Set for the involved issues at a qualitygate break
     * 
     * @return
     */
    public MetricSetDescription createInvolvedIssueMetricSet() {
        MetricSetDescription result = MetricSpecFactory.eINSTANCE.createMetricSetDescription();
        result.setName("InvolvedIssuesOverTime");
        result.getSubsumedMetrics()
            .add(MetricDescriptionConstants.POINT_IN_TIME_METRIC);
        result.getSubsumedMetrics()
            .add(this.textMetricDesc);
        result.setTextualDescription("This metric represents the issue present when the qualitygate was broken.");

        result.setRepository(this.metricRepository);
        return result;
    }
    

    /**
     * Triggers the probes for QualitygateViolationOverTime
     * 
     * @param event
     */
    public void triggerProbe(final QualitygatePassedEvent event) {

        // Create probe and calculator for this qualitygate
        if (!isProbeCreated.containsKey(event.getModelElement()
            .getId())) {

            EcoreUtil.resolveAll(pcmPartition.getResourceSet());

            MeasuringPointRepository repo = (MeasuringPointRepository) pcmPartition
                .getElement(MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository())
                .stream()
                .findAny()
                .orElse(null);

            // TODO hier immer Fehler beim ersten starten -> andere Vorgehensweise um die
            // MeasuringPoints zu erhalten

            QualitygateMeasuringPoint measuringPoint = (QualitygateMeasuringPoint) repo.getMeasuringPoints()
                .stream()
                .filter(e -> (e instanceof QualitygateMeasuringPoint && ((QualitygateMeasuringPoint) e).getQualitygate()
                    .equals(event.getModelElement())))
                .findAny()
                .orElse(null);

            if (measuringPoint != null && this.isQualitygateMonitorInRepository(event.getModelElement())) {

                // Creating Probes
                QualitygateCheckingProbe probe = new QualitygateCheckingProbe(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC);

                TakeCurrentSimulationTimeProbe timeProbe = new TakeCurrentSimulationTimeProbe(simulationControl);

                QualitygateCheckingTriggeredProbeList probeOverTime = new QualitygateCheckingTriggeredProbeList(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME,
                        Arrays.asList(timeProbe, probe));

                this.qualitygateEvaluationProbe.put(event.getModelElement()
                    .getId(), probeOverTime);

                this.isProbeCreated.put(event.getModelElement()
                    .getId(), true);

                // Creating and registering the calculator
                this.calculatorFactory.buildCalculator(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME, measuringPoint,
                        DefaultCalculatorProbeSets.createSingularProbeConfiguration(probeOverTime));

            } else {
                this.isProbeCreated.put(event.getModelElement()
                    .getId(), false);
            }
        }

        // trigger probe according to the evaluation
        if (event.isSuccess() && this.isProbeCreated.get(event.getModelElement()
            .getId()) == true) {
            this.qualitygateEvaluationProbe.get(event.getModelElement()
                .getId())
                .takeMeasurement(event.getThread()
                    .getRequestContext(), QualitygateMetricDescriptionConstants.SUCCESS);

        } else if (this.isProbeCreated.get(event.getModelElement()
            .getId()) == true) {

            this.qualitygateEvaluationProbe.get(event.getModelElement()
                .getId())
                .takeMeasurement(event.getThread()
                    .getRequestContext(), QualitygateMetricDescriptionConstants.VIOLATION);
        }

    }
    
    /**
     * Triggers the probes for SeverityOverTime
     * 
     * @param event
     */
    public void triggerSeverityProbe(final QualitygatePassedEvent event) {

        if (this.severityProbe == null) {

            MeasuringPointRepository repo = (MeasuringPointRepository) pcmPartition
                .getElement(MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository())
                .stream()
                .findAny()
                .orElse(null);

            SeverityMeasuringPoint measuringPoint = (SeverityMeasuringPoint) repo.getMeasuringPoints()
                .stream()
                .filter(e -> (e instanceof SeverityMeasuringPoint))
                .findAny()
                .orElse(null);
            
            // TODO Check ob Monitor definiert

            if (measuringPoint != null) {

                // Creating Probes
                QualitygateCheckingProbe probe = new QualitygateCheckingProbe(QualitygateMetricDescriptionConstants.SEVERITY_METRIC);

                TakeCurrentSimulationTimeProbe timeProbe = new TakeCurrentSimulationTimeProbe(simulationControl);

                QualitygateCheckingTriggeredProbeList probeOverTime = new QualitygateCheckingTriggeredProbeList(
                        QualitygateMetricDescriptionConstants.SEVERITY_METRIC_OVER_TIME,
                        Arrays.asList(timeProbe, probe));

                this.severityProbe = probeOverTime;

                this.calculatorFactory.buildCalculator(QualitygateMetricDescriptionConstants.SEVERITY_METRIC_OVER_TIME,
                        measuringPoint, DefaultCalculatorProbeSets.createSingularProbeConfiguration(probeOverTime));

            }

        }

        if (event.getSeverity() != null && severityProbe != null) {

            if(!this.createdIdentifierForSeverity.containsKey(event.getSeverity().getEntityName())) {
                // First create Identifier
                
                // Adding Identifier for every issue not yet registered as Identifier
                Identifier identifier = MetricSpecFactory.eINSTANCE.createIdentifier();
                identifier.setLiteral(event.getSeverity().getEntityName());
                this.textMetricDescForSeverity.getIdentifiers()
                    .add(identifier);
                this.createdIdentifierForSeverity.put(event.getSeverity().getEntityName(), identifier);
                
                
                
            }
            
            this.severityProbe.takeMeasurement(this.createdIdentifierForSeverity.get(event.getSeverity().getEntityName()));
            
            
//            this.severityProbe.takeMeasurement(event.getThread()
//                .getRequestContext(), event.getSeverity());

        }

    }

    /**
     * Triggers the probes for InvolvedIssuesOverTime
     * 
     * @param event
     */
    public void triggerIssueProbes(List<InterpretationIssue> issuesWhenBroken,
            EntityReference<QualityGate> qualitygateRef) {

        QualityGate qualitygate = qualitygateRef.getModelElement(pcmPartition);

        // in case no probe for htis qualitygate is registered
        if (!interpreterResultIssueProbe.containsKey(qualitygate.getId())) {

            MeasuringPointRepository repo = (MeasuringPointRepository) pcmPartition
                .getElement(MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository())
                .stream()
                .findAny()
                .orElse(null);

            // TODO hier immer Fehler beim ersten starten -> andere Vorgehensweise um die
            // MeasuringPoints zu erhalten

            QualitygateMeasuringPoint measuringPoint = (QualitygateMeasuringPoint) repo.getMeasuringPoints()
                .stream()
                .filter(e -> (e instanceof QualitygateMeasuringPoint && ((QualitygateMeasuringPoint) e).getQualitygate()
                    .equals(qualitygate)))
                .findAny()
                .orElse(null);

            if (measuringPoint != null && this.isQualitygateMonitorInRepository(qualitygate)) {

                // Creating Probes
                QualitygateCheckingProbe probe = new QualitygateCheckingProbe(textMetricDesc);

                TakeCurrentSimulationTimeProbe timeProbe = new TakeCurrentSimulationTimeProbe(simulationControl);

                QualitygateCheckingTriggeredProbeList probeOverTime = new QualitygateCheckingTriggeredProbeList(
                        metricSetDesc, Arrays.asList(timeProbe, probe));

                this.interpreterResultIssueProbe.put(qualitygate.getId(), probeOverTime);

                this.isIssueProbeCreated.put(qualitygate.getId(), true);

                // Creating and registering the calculator
                this.calculatorFactory.buildCalculator(metricSetDesc, measuringPoint,
                        DefaultCalculatorProbeSets.createSingularProbeConfiguration(probeOverTime));
            } else {
                this.isIssueProbeCreated.put(qualitygate.getId(), false);
            }
        }

        if (this.isIssueProbeCreated.get(qualitygate.getId())) {

            this.interpreterResultIssueProbe.get(qualitygate.getId())
                .takeMeasurement(createdIdentifier.get("default"));

            for (InterpretationIssue issue : issuesWhenBroken) {

                if (issue instanceof QualitygateIssue) {
                    if (!this.createdIdentifier.containsKey(((QualitygateIssue) issue).getQualitygateId())) {

                        // Adding Identifier for every issue not yet registered as Identifier
                        Identifier identifier = MetricSpecFactory.eINSTANCE.createIdentifier();
                        identifier.setLiteral(((QualitygateIssue) issue).getQualitygateRef()
                            .getModelElement(pcmPartition)
                            .getEntityName());
                        this.textMetricDesc.getIdentifiers()
                            .add(identifier);
                        this.createdIdentifier.put(((QualitygateIssue) issue).getQualitygateId(), identifier);

                    }

                    this.interpreterResultIssueProbe.get(qualitygate.getId())
                        .takeMeasurement(createdIdentifier.get(((QualitygateIssue) issue).getQualitygateId()));

                }

            }
        }

    }

    @Override
    public void cleanup() {
        qualitygateEvaluationProbe.clear();
        interpreterResultIssueProbe.clear();
    }

    /**
     * Checks whether the a monitor is present in MonitorRepository.
     * 
     * @param qualitygate
     * @return
     */
    public boolean isQualitygateMonitorInRepository(final QualityGate qualitygate) {

        MonitorRepository monitorRepo = (MonitorRepository) pcmPartition
            .getElement(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository())
            .stream()
            .findAny()
            .orElse(null);

        for (Monitor monitor : monitorRepo.getMonitors()) {

            if (monitor.getMeasuringPoint() instanceof QualitygateMeasuringPoint) {

                if (((QualitygateMeasuringPoint) monitor.getMeasuringPoint()).getQualitygate()
                    .getId()
                    .equals(qualitygate.getId())) {
                    return true;
                }
            }
        }

        return false;
    }
    

}
