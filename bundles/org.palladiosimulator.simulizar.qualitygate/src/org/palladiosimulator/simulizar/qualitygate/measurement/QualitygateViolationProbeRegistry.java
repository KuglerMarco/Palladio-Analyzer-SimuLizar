package org.palladiosimulator.simulizar.qualitygate.measurement;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.ecore.util.EcoreUtil;

import java.util.Arrays;

import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.failuremodel.qualitygatemeasuringpoint.QualitygateMeasuringPoint;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;
import org.palladiosimulator.probeframework.calculator.DefaultCalculatorProbeSets;
import org.palladiosimulator.probeframework.calculator.IGenericCalculatorFactory;
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

    private Map<String, QualitygateCheckingTriggeredProbeList> currentProbes = new HashMap<String, QualitygateCheckingTriggeredProbeList>();
    
    private Map<String, Boolean> isProbeCreated = new HashMap<String, Boolean>();

    @Inject
    public QualitygateViolationProbeRegistry(@Global final PCMResourceSetPartition pcmPartition,
            IGenericCalculatorFactory calculatorFactory, final ISimulationControl simulationControl) {
        this.pcmPartition = pcmPartition;
        this.calculatorFactory = calculatorFactory;
        this.simulationControl = simulationControl;
        
        LOGGER.setLevel(Level.DEBUG);

    }

    public void triggerProbe(final QualitygatePassedEvent event) {

        
        
        // Create probe and calculator for this qualitygate
        if (!isProbeCreated.containsKey(event.getModelElement()
            .getId())) {
            
            EcoreUtil.resolveAll(pcmPartition.getResourceSet());
            
            // TODO hier �berpr�fen ob QualitygateMonitor im Repository vorliegt

            MeasuringPointRepository repo = (MeasuringPointRepository) pcmPartition
                .getElement(MeasuringpointPackage.eINSTANCE.getMeasuringPointRepository())
                .stream()
                .findAny()
                .orElse(null);
            
            // TODO hier immer Fehler beim ersten starten -> andere Vorgehensweise um die MeasuringPoints zu erhalten
            
            QualitygateMeasuringPoint measuringPoint = (QualitygateMeasuringPoint) repo.getMeasuringPoints()
                .stream()
                .filter(e -> (e instanceof QualitygateMeasuringPoint && ((QualitygateMeasuringPoint) e).getQualitygate()
                    .equals(event.getModelElement())))
                .findAny()
                .orElse(null);
            
            if(measuringPoint != null) {

                // Creating Probes
                QualitygateCheckingProbe probe = new QualitygateCheckingProbe(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC);
                
                TakeCurrentSimulationTimeProbe timeProbe = new TakeCurrentSimulationTimeProbe(simulationControl);

                QualitygateCheckingTriggeredProbeList probeOverTime = new QualitygateCheckingTriggeredProbeList(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME,
                        Arrays.asList(timeProbe, probe));
    
                this.currentProbes.put(event.getModelElement()
                    .getId(), probeOverTime);
                
                this.isProbeCreated.put(event.getModelElement().getId(), true);
                
                // Creating and registering the calculator
                this.calculatorFactory.buildCalculator(
                        QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME, measuringPoint,
                        DefaultCalculatorProbeSets.createSingularProbeConfiguration(probeOverTime));
            
            } else {
                this.isProbeCreated.put(event.getModelElement().getId(), false);
            }
        }

        // trigger probe according to the evaluation
        if (event.isSuccess() && this.isProbeCreated.get(event.getModelElement().getId()) == true) {
            this.currentProbes.get(event.getModelElement()
                .getId())
                .setIdentifier(QualitygateMetricDescriptionConstants.SUCCESS);
            this.currentProbes.get(event.getModelElement()
                .getId())
                .takeMeasurement(event.getThread()
                    .getRequestContext());

        } else if (this.isProbeCreated.get(event.getModelElement().getId()) == true) {

            this.currentProbes.get(event.getModelElement()
                .getId())
                .setIdentifier(QualitygateMetricDescriptionConstants.VIOLATION);
            this.currentProbes.get(event.getModelElement()
                .getId())
                .takeMeasurement(event.getThread()
                    .getRequestContext());
        }

    }

    @Override
    public void cleanup() {
        currentProbes.clear();
    }
    
    
    // TODO sp�ter f�r die �berpr�fung nutzen
    public boolean isQualitygateMonitorInRepository(final QualitygatePassedEvent event) {
        
        MonitorRepository monitorRepo = (MonitorRepository) pcmPartition
                .getElement(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository())
                .stream()
                .findAny()
                .orElse(null);
        
        for(Monitor monitor : monitorRepo.getMonitors()) {
            
            if(monitor.getMeasuringPoint() instanceof QualitygateMeasuringPoint) {
                
                if(((QualitygateMeasuringPoint) monitor.getMeasuringPoint()).getQualitygate().equals(event.getModelElement())) {
                    
                    for(MeasurementSpecification spec : monitor.getMeasurementSpecifications()) {
                        
                        if(spec.getMetricDescription().equals(QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC_OVER_TIME)) {
                            return true;
                        }
                    }
                }
            }
        }
        
        return false;
    }

}
