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
import org.palladiosimulator.metricspec.MetricDescriptionRepository;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepositoryFactory;
import org.palladiosimulator.monitorrepository.ProcessingType;
import org.palladiosimulator.pcm.core.composition.AssemblyConnector;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.system.System;
import org.palladiosimulator.pcmmeasuringpoint.SystemOperationMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointFactory;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

/**
 * Switch to create the necessary Monitors for the Qualitygate-Elements within the model.
 * 
 * @author Marco Kugler
 *
 */
public class StereotypeQualitygatePreprocessingSwitch extends QualitygateSwitch<Monitor> {
	
	@AssistedFactory
	public static interface Factory {
		StereotypeQualitygatePreprocessingSwitch create(MetricDescriptionRepository metricRepo);
	}
	
	Logger LOGGER = Logger.getLogger(StereotypeQualitygatePreprocessingSwitch.class);
	private EObject stereotypeObject;
	private final MetricDescriptionRepository metricRepo;
	
	@AssistedInject
	public StereotypeQualitygatePreprocessingSwitch(@Assisted MetricDescriptionRepository metricRepo) {
		LOGGER.setLevel(Level.DEBUG);
		this.metricRepo = metricRepo;
	}
	
	
	
	/**
	 * Creates the Monitor to observe the Response-Time at the stereotyped element.
	 */
	@Override
	public Monitor caseRequestMetricScope(RequestMetricScope object) {
		

		Monitor monitor = MonitorRepositoryFactory.eINSTANCE.createMonitor();
		
		//Activated
		monitor.setActivated(true);
		
		//Entity-Name
		monitor.setEntityName("QualitygateMonitor");
		
		//Measuring-Point
		SystemOperationMeasuringPoint measuringPoint = PcmmeasuringpointFactory.eINSTANCE.createSystemOperationMeasuringPoint();
		
		//Operation-Signature
		measuringPoint.setOperationSignature((OperationSignature) object.getSignature());
		
		if(stereotypeObject instanceof AssemblyConnector) {
			
			//Role
			measuringPoint.setRole(((AssemblyConnector) stereotypeObject).getRequiredRole_AssemblyConnector());
			
			//System
			measuringPoint.setSystem((System) ((AssemblyConnector) stereotypeObject).getParentStructure__Connector());
			
		}
		
		monitor.setMeasuringPoint(measuringPoint);
		
		//Measurement-Specification
		MeasurementSpecification measurementSpec = MonitorRepositoryFactory.eINSTANCE.createMeasurementSpecification();
		
		//Metric-Description
		measurementSpec.setMetricDescription(metricRepo.getMetricDescriptions().stream().filter(e -> e.getName().equals("Response Time")).findFirst().orElse(null));
		
		//triggers self adaption
		measurementSpec.setTriggersSelfAdaptations(false);
		
		//Processing Type
		ProcessingType procType = MonitorRepositoryFactory.eINSTANCE.createFeedThrough();
		
		procType.setMeasurementSpecification(measurementSpec);
		
		measurementSpec.setProcessingType(procType);
		
		monitor.getMeasurementSpecifications().add(measurementSpec);
		
		LOGGER.debug("A monitor was created for: " + monitor.getMeasurementSpecifications().get(0).getMetricDescription().getTextualDescription());
		
		return monitor;
		
	}
	
	
	
	@Override
	public Monitor caseQualityGate(QualityGate object) {
		
		return doSwitch(object.getScope());

	}
	
	
	
	public List<Monitor> handleQualitygate(EObject object) {
		
		EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(object, "qualitygate", "QualitygateElement");
		
		stereotypeObject = object;
		
		List<Monitor> monitor = new ArrayList<Monitor>();
		
		for(QualityGate e : taggedValues) {
			monitor.add(this.doSwitch(e));
		}
		
		return monitor;
		
		
	}

}