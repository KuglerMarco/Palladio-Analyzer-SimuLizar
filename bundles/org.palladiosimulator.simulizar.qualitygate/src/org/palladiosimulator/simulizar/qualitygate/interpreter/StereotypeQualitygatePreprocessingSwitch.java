package org.palladiosimulator.simulizar.qualitygate.interpreter;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.RequestMetricScope;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.metricspec.MetricDescriptionRepository;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepositoryFactory;
import org.palladiosimulator.monitorrepository.ProcessingType;
import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcmmeasuringpoint.SystemOperationMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.ExternalCallActionMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointFactory;
import org.palladiosimulator.pcm.seff.ExternalCallAction;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

/**
 * Switch to create the necessary Monitors for the Qualitygate-Elements in order to use the
 * calculators within the simulation.
 * 
 * @author Marco Kugler
 *
 */
public class StereotypeQualitygatePreprocessingSwitch extends QualitygateSwitch<Monitor> {

    @AssistedFactory
    public static interface Factory {
        StereotypeQualitygatePreprocessingSwitch create(MetricDescriptionRepository metricRepo, System system);
    }

    Logger LOGGER = Logger.getLogger(StereotypeQualitygatePreprocessingSwitch.class);
    private EObject stereotypedObject;
    private final MetricDescriptionRepository metricRepo;
    private System system;

    @AssistedInject
    public StereotypeQualitygatePreprocessingSwitch(@Assisted MetricDescriptionRepository metricRepo, @Assisted System system) {
        LOGGER.setLevel(Level.DEBUG);
        this.metricRepo = metricRepo;
        this.system = system;
    }

    /**
     * Creates the Monitor to observe the Response-Time at the stereotyped element.
     */
    @Override
    public Monitor caseRequestMetricScope(RequestMetricScope object) {

        Monitor monitor = MonitorRepositoryFactory.eINSTANCE.createMonitor();

        // Activated
        monitor.setActivated(true);
        
        

        if (stereotypedObject instanceof ProvidedRole) {

            // Entity-Name
            monitor.setEntityName(
                    "QualitygateMonitor at ProvidedRole " + ((ProvidedRole) stereotypedObject).getEntityName());

            // Measuring-Point //TODO Optional Reference on Assembly, then AssemblyOperationMeasuringPoint
            SystemOperationMeasuringPoint measuringPoint = PcmmeasuringpointFactory.eINSTANCE
                .createSystemOperationMeasuringPoint();
            
            // Operation-Signature
            measuringPoint.setOperationSignature((OperationSignature) object.getSignature());
            
            measuringPoint.setRole((ProvidedRole)stereotypedObject);
            
            measuringPoint.setSystem(system);
            
            monitor.setMeasuringPoint(measuringPoint);
            

        }
        
        //TODO Monitor setzen für ExternalCall
        
        if(stereotypedObject instanceof ExternalCallAction) {
            
            monitor.setEntityName(
                    "QualitygateMonitor at ExternalCallAction " + ((ExternalCallAction) stereotypedObject).getEntityName());
            
            ExternalCallActionMeasuringPoint measuringPoint = PcmmeasuringpointFactory.eINSTANCE.createExternalCallActionMeasuringPoint();
            
            measuringPoint.setExternalCall((ExternalCallAction) stereotypedObject);
            
            monitor.setMeasuringPoint(measuringPoint);
            
            
        }

        

//        if (stereotypedObject instanceof AssemblyConnector) {
//
//            // Role TODO Required oder Provided?
//            measuringPoint.setRole(((AssemblyConnector) stereotypedObject).getProvidedRole_AssemblyConnector());
//
//            // System
//            measuringPoint.setSystem((System) ((AssemblyConnector) stereotypedObject).getParentStructure__Connector());
//
//        }

        

        // Measurement-Specification
        MeasurementSpecification measurementSpec = MonitorRepositoryFactory.eINSTANCE.createMeasurementSpecification();

        MetricDescription metricDesc = metricRepo.getMetricDescriptions()
                .stream()
                .filter(e -> e.getName()
                    .equals("Response Time"))
                .findFirst()
                .orElse(null);
        
        // Metric-Description
        measurementSpec.setMetricDescription(metricDesc);
        

        // triggers self adaption
        measurementSpec.setTriggersSelfAdaptations(false);

        // Processing Type
        ProcessingType procType = MonitorRepositoryFactory.eINSTANCE.createFeedThrough();

        

        measurementSpec.setProcessingType(procType);
        procType.setMeasurementSpecification(measurementSpec);
        
        
        monitor.getMeasurementSpecifications()
            .add(measurementSpec);
        
        measurementSpec.setMonitor(monitor);

        LOGGER.debug("A monitor was created for: " + monitor.getMeasurementSpecifications()
            .get(0)
            .getMetricDescription()
            .getTextualDescription());

        return monitor;

    }

    @Override
    public Monitor caseQualityGate(QualityGate object) {

        return doSwitch(object.getScope());

    }

    public List<Monitor> handleQualitygate(EObject object) {

        EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(object, "qualitygate", "QualitygateElement");

        stereotypedObject = object;
        

        List<Monitor> monitor = new ArrayList<Monitor>();

        for (QualityGate e : taggedValues) {
            monitor.add(this.doSwitch(e));
        }

        return monitor;

    }

}