package org.palladiosimulator.simulizar.qualitygate.jobs;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.emf.common.util.TreeIterator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.metricspec.MetricDescriptionRepository;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;


import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;

import org.palladiosimulator.simulizar.launcher.jobs.extensions.DefaultMeasuringPointRepositoryFactory;
import org.palladiosimulator.simulizar.launcher.jobs.extensions.DefaultMonitorRepositoryFactory;
import org.palladiosimulator.simulizar.qualitygate.interpreter.StereotypeQualitygatePreprocessingSwitch;

import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.eclipse.emf.common.util.URI;


/**
 * Creates the necessary Monitor-elements in the MonitorRepository, so that the according Calculators
 * are available within the simulation.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateResponseTimeCalculatorJob implements IBlackboardInteractingJob<MDSDBlackboard> {
	
	
	Logger LOGGER = Logger.getLogger(QualitygateResponseTimeCalculatorJob.class);
	
	private MDSDBlackboard blackboard;
	private final StereotypeQualitygatePreprocessingSwitch.Factory preprocessingSwitch;
	
	
	@Inject
	public QualitygateResponseTimeCalculatorJob(MDSDBlackboard blackboard, StereotypeQualitygatePreprocessingSwitch.Factory preprocessingSwitch) {
		this.blackboard = blackboard;
		this.preprocessingSwitch = preprocessingSwitch;
		
		LOGGER.setLevel(Level.DEBUG);
		
	}
	
	

	@Override
	public void execute(IProgressMonitor monitor) throws JobFailedException, UserCanceledException {
		
		
		ResourceSet resourceSet = blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getResourceSet();

		//Loading CommonMetrics-model
		URI uri = URI.createURI("pathmap://METRIC_SPEC_MODELS/models/commonMetrics.metricspec");
		MetricDescriptionRepository res = (MetricDescriptionRepository) resourceSet.getResource(uri, false).getContents().get(0);
		
		
		/*
         * Merging the MeasurementPoint-Repositories TODO Rücksprache Sebastian
         */
		if(blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY).isEmpty()) {
			throw new IllegalArgumentException("No MeasuringPointRepository found!");

		}
		
		//Current
		MeasuringPointRepository measuringPointRepo = (MeasuringPointRepository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY).get(0);
		//Default
		MeasuringPointRepository genMeasuringPointRepo = DefaultMeasuringPointRepositoryFactory.createDefaultRepository(resourceSet);
		//added to the current
		measuringPointRepo.getMeasuringPoints().addAll(genMeasuringPointRepo.getMeasuringPoints());
		
	
		/*
         * Merging the MonitorRepositories TODO Rücksprache Sebastian TODO Fix the problem of duplicates in defaultRepository
         */
		
        
        if(blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY).isEmpty()) {
        	throw new IllegalArgumentException("No MonitorRepository found!");
        }
        //Current
        MonitorRepository monitorRepo = (MonitorRepository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY).get(0);
        //Default
        MonitorRepository genMonitorRepository = DefaultMonitorRepositoryFactory.createDefaultMonitorRepository(measuringPointRepo);
        
        //(for every Measuring Point one response time monitor will be added in defaultRepository)
        //TODO falls Monitor bereits spezifiziert, keinen mehr hinzufügen, ansonsten zwei Calculators
        monitorRepo.getMonitors().addAll(genMonitorRepository.getMonitors());
        	
         

        /*
         * Traversing the System-model to find Qualitygate-Elements
         * TODO Auf Repository-Ebene ebenefalls machen
         */
		PCMResourceSetPartition resPartition = (PCMResourceSetPartition) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);
		
		TreeIterator<EObject> systemIterator = resPartition.getSystem().eAllContents();
		EObject object;
		
		//Iterating over System-model
		while(systemIterator.hasNext()) {
			
			object = systemIterator.next();
			
			if(!StereotypeAPI.getAppliedStereotypes(object).isEmpty() && StereotypeAPI.getAppliedStereotypes(object).get(0).getName().equals("QualitygateElement")) {
				
				if(object instanceof Connector) {
					LOGGER.debug("The connector " + ((Connector)object).getEntityName() + " has a qualitygate-application");
					
					
					//List of generated Monitors for the attached Qualitygates (could be more than one)
					List<Monitor> qualitygateMonitor = preprocessingSwitch.create(res).handleQualitygate(object);
					//Removing the Null-elements, because not every Qualitygate needs a calculator
					while(qualitygateMonitor.remove(null));
					
					//Adding the generated Monitors to the repositories
					for(Monitor e : qualitygateMonitor) {
						measuringPointRepo.getMeasuringPoints().add(e.getMeasuringPoint());
						monitorRepo.getMonitors().add(e);
						e.setMonitorRepository(monitorRepo);
							
					}
				}
			}
		}
		

		//Only for debug reasons
		MonitorRepository monitorRepository = (MonitorRepository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY).get(0);
		
		for(Monitor e : monitorRepository.getMonitors()) {
			LOGGER.debug(e.getMeasuringPoint().getStringRepresentation());
			
			for(MeasurementSpecification i : (e.getMeasurementSpecifications())){
				LOGGER.debug(i.getMetricDescription().getTextualDescription());
			}
		}
	}

	@Override
	public void cleanup(IProgressMonitor monitor) throws CleanupFailedException {
		// Nothing to clean up
	}

	@Override
	public String getName() {
		return "Qualitygate Calculator Completion";
	}


	@Override
	public void setBlackboard(MDSDBlackboard blackboard) {
		this.blackboard = blackboard;
		
	}





}
