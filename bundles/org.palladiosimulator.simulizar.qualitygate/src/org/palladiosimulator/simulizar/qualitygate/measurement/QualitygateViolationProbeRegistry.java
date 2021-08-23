package org.palladiosimulator.simulizar.qualitygate.measurement;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import java.util.Arrays;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.failuremodel.qualitygatemeasuringpoint.QualitygateMeasuringPoint;
import org.palladiosimulator.failuremodel.qualitygatemeasuringpoint.QualitygatemeasuringpointFactory;
import org.palladiosimulator.probeframework.calculator.DefaultCalculatorProbeSets;
import org.palladiosimulator.probeframework.calculator.IGenericCalculatorFactory;
import org.palladiosimulator.probeframework.probes.TriggeredProbe;
import org.palladiosimulator.simulizar.qualitygate.event.QualitygatePassedEvent;
import org.palladiosimulator.simulizar.qualitygate.metric.QualitygateMetricDescriptionConstants;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager.Global;

import de.uka.ipd.sdq.simucomframework.probes.TakeCurrentSimulationTimeProbe;
import de.uka.ipd.sdq.simulation.abstractsimengine.ISimulationControl;

import org.palladiosimulator.simulizar.runtimestate.RuntimeStateEntityManager;
import org.palladiosimulator.simulizar.scopes.RuntimeExtensionScope;

@RuntimeExtensionScope
public class QualitygateViolationProbeRegistry implements RuntimeStateEntityManager {

    private static final Logger LOGGER = Logger.getLogger(QualitygateViolationProbeRegistry.class);

    private final ISimulationControl simulationControl;
    private final PCMResourceSetPartition pcmPartition;
    private final IGenericCalculatorFactory calculatorFactory;

    // already registered probes for qualitygates
//    private final Map<String, QualitygateTriggeredProbeList> currentTimeProbes = new HashMap<String, QualitygateTriggeredProbeList>();
    
    //new
    private final Map<String, QualitygateCheckingTriggeredProbeList> currentProbes = new HashMap<String, QualitygateCheckingTriggeredProbeList>();

    @Inject
    public QualitygateViolationProbeRegistry(@Global final PCMResourceSetPartition pcmPartition,
            IGenericCalculatorFactory calculatorFactory, final ISimulationControl simulationControl) {
        this.pcmPartition = pcmPartition;
        this.calculatorFactory = calculatorFactory;
        this.simulationControl = simulationControl;

    }

    public void triggerProbe(final QualitygatePassedEvent event) {

        // Create probe and calculator for this qualitygate
        if (!currentProbes.containsKey(event.getModelElement()
            .getId())) {

            // probe
            QualitygateMeasuringPoint measuringPoint = QualitygatemeasuringpointFactory.eINSTANCE
                .createQualitygateMeasuringPoint();
            
            measuringPoint.setQualitygate(event.getModelElement());
            
            var repo = pcmPartition.getElement(MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository())
                .stream()
                .findAny()
                .orElse(null);
            
            ((MeasuringPointRepository) repo).getMeasuringPoints().add(measuringPoint);
            
            measuringPoint.getResourceURIRepresentation();
            
            measuringPoint.setMeasuringPointRepository((MeasuringPointRepository) repo);
            
            QualitygateCheckingProbe probe = new QualitygateCheckingProbe(
                    QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC);

//            final QualitygateTriggeredProbeList probeOverTime = new QualitygateTriggeredProbeList(
//                    QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME,
//                    Arrays.asList(probe, (TriggeredProbe) new TakeCurrentSimulationTimeProbe(simulationControl)));
            
            QualitygateCheckingTriggeredProbeList probeOverTime = new QualitygateCheckingTriggeredProbeList(
                    QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME,
                    Arrays.asList(probe, (TriggeredProbe) new TakeCurrentSimulationTimeProbe(simulationControl)));

            this.currentProbes.put(event.getModelElement()
                .getId(), probeOverTime);

            this.calculatorFactory.buildCalculator(
                    QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME, measuringPoint,
                    DefaultCalculatorProbeSets.createSingularProbeConfiguration(probeOverTime));
        }

        if (event.isSucess()) {
            this.currentProbes.get(event.getModelElement().getId()).setIdentifier(QualitygateMetricDescriptionConstants.SUCCESS);
            this.currentProbes.get(event.getModelElement().getId()).takeMeasurement(event.getThread()
                    .getRequestContext());
            
//            (this.currentTimeProbes.get(event.getModelElement()
//                .getId())).takeSuccessfulMeasurement(event.getThread()
//                    .getRequestContext());
        } else {
//            this.currentTimeProbes.get(event.getModelElement()
//                .getId())
//                .takeViolatedMeasurement(event.getThread()
//                    .getRequestContext());
            
            this.currentProbes.get(event.getModelElement().getId()).setIdentifier(QualitygateMetricDescriptionConstants.VIOLATION);
            this.currentProbes.get(event.getModelElement().getId()).takeMeasurement(event.getThread()
                    .getRequestContext());
        }

    }
    
    @Override
    public void cleanup() {
        currentProbes.clear();
    }
    

}
