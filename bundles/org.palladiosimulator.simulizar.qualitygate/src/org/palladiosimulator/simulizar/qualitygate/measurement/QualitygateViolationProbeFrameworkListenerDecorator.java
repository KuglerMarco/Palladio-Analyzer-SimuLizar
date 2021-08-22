package org.palladiosimulator.simulizar.qualitygate.measurement;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.failuremodel.qualitygatemeasuringpoint.QualitygateMeasuringPoint;
import org.palladiosimulator.failuremodel.qualitygatemeasuringpoint.QualitygatemeasuringpointFactory;
import org.palladiosimulator.probeframework.calculator.DefaultCalculatorProbeSets;
import org.palladiosimulator.probeframework.calculator.IGenericCalculatorFactory;
import org.palladiosimulator.simulizar.qualitygate.event.QualitygatePassedEvent;
import org.palladiosimulator.simulizar.qualitygate.metric.QualitygateMetricDescriptionConstants;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager.Global;

import de.uka.ipd.sdq.simulation.ISimulationControl;

public class QualitygateViolationProbeFrameworkListenerDecorator {
    
    private static final Logger LOGGER = Logger.getLogger(QualitygateViolationProbeFrameworkListenerDecorator.class);
    
    private final ISimulationControl simulationControl;
    private final PCMResourceSetPartition pcmPartition;
    private final IGenericCalculatorFactory calculatorFactory;
    
    // already registered probes for qualitygates
    private final Map<String, QualitygateViolationProbe> currentTimeProbes = new HashMap<String, QualitygateViolationProbe>();
    
    @Inject
    public QualitygateViolationProbeFrameworkListenerDecorator(final ISimulationControl simulationControl,
            @Global final PCMResourceSetPartition pcmPartition, 
            IGenericCalculatorFactory calculatorFactory) {
        this.simulationControl = simulationControl;
        this.pcmPartition = pcmPartition;
        this.calculatorFactory = calculatorFactory;
        
    }
    
    public void triggerProbe(final QualitygatePassedEvent event) {
        
        // Create probe and calculator for this qualitygate
        if(!currentTimeProbes.containsKey(event.getModelElement().getId())) {

            // probe
            QualitygateMeasuringPoint measuringPoint = QualitygatemeasuringpointFactory.eINSTANCE
                .createQualitygateMeasuringPoint();
            measuringPoint.setQualitygate(event.getModelElement());
            var repo = pcmPartition.getElement(MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository())
                .stream()
                .findAny()
                .orElse(null);
            measuringPoint.setMeasuringPointRepository((MeasuringPointRepository) repo);
            QualitygateViolationProbe probe = new QualitygateViolationProbe();

            this.currentTimeProbes.put(event.getModelElement()
                .getId(), probe);
            this.calculatorFactory.buildCalculator(QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC,
                    measuringPoint, DefaultCalculatorProbeSets.createSingularProbeConfiguration(probe));
        }
        
        this.currentTimeProbes.get(event.getModelElement().getId()).takeMeasurement(event.getIdentifier(), event.getThread().getRequestContext());
        
    }
    
    

    

}
