package org.palladiosimulator.simulizar.qualitygate.jobs;

import javax.inject.Inject;

import org.eclipse.emf.common.util.TreeIterator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;


import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;

import org.palladiosimulator.simulizar.launcher.jobs.extensions.DefaultMeasuringPointRepositoryFactory;
import org.palladiosimulator.simulizar.launcher.jobs.extensions.DefaultMonitorRepositoryFactory;
import org.palladiosimulator.simulizar.runconfig.SimuLizarWorkflowConfiguration;

import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;


public class QualitygateResponseTimeCalculatorJob implements IBlackboardInteractingJob<MDSDBlackboard> {
	
	/*
	 * Blackboard enthält die Partitionen mit den Modellen
	 * Hier drin soll das Modell traversiert und die Calculatoren gesetzt werden
	 * Wird dann ausgeführt nachdem die Modelle geladen sind
	 */
	
	Logger LOGGER = Logger.getLogger(QualitygateResponseTimeCalculatorJob.class);
	
	private final MDSDBlackboard blackboard;
	private final SimuLizarWorkflowConfiguration config;
	
	
	@Inject
	public QualitygateResponseTimeCalculatorJob(MDSDBlackboard blackboard, SimuLizarWorkflowConfiguration config) {
		this.blackboard = blackboard;
		this.config = config;
		LOGGER.setLevel(Level.DEBUG);
		
	}
	
	

	@Override
	public void execute(IProgressMonitor monitor) throws JobFailedException, UserCanceledException {
		

		var resSet = blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getResourceSet();
		for(Resource e : resSet.getResources()) {
			LOGGER.debug(e.getURI().toString());
		}
		
		MeasuringPointRepository measuringPointRepo;
		
        /*
         * Merging the MeasurementPoint-Repositories TODO Rücksprache Sebastian
         */
		if(!blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY).isEmpty()) {
			
			measuringPointRepo = (MeasuringPointRepository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY).get(0);

			MeasuringPointRepository genMeasuringPointRepo = DefaultMeasuringPointRepositoryFactory.createDefaultRepository(resSet);
			
			measuringPointRepo.getMeasuringPoints().addAll(genMeasuringPointRepo.getMeasuringPoints());
			
		} else {
			throw new IllegalStateException("No MeasuringPointRepository found.");
		}
		
		
		
		
		
//        var measuringPointRepo = (MeasuringPointRepository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(
//        		MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY).get(0);
  
        
        /*
         * Merging the MonitorRepositories TODO Rücksprache Sebastian
         */
        if(!blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(
                MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY).isEmpty()) {
        	
        	MonitorRepository monitorRepo = (MonitorRepository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(
                    MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY).get(0);
        	
        	MonitorRepository genMonitorRepository = DefaultMonitorRepositoryFactory.createDefaultMonitorRepository(measuringPointRepo);
        	
        	monitorRepo.getMonitors().addAll(genMonitorRepository.getMonitors());
        	
        	
        }
        
        

		PCMResourceSetPartition resPartition = (PCMResourceSetPartition) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);
		
		LOGGER.debug(resPartition.getRepositories().get(0).getEntityName());
		LOGGER.debug(resPartition.getRepositories().get(1).getEntityName());
		
		TreeIterator<EObject> systemIterator = resPartition.getSystem().eAllContents();
		EObject object;
		
		while(systemIterator.hasNext()) {
			
			object = systemIterator.next();
			
			if(!StereotypeAPI.getAppliedStereotypes(object).isEmpty() && StereotypeAPI.getAppliedStereotypes(object).get(0).getName().equals("QualitygateElement")) {
				
				if(object instanceof Connector) {
					LOGGER.debug("The connector " + ((Connector)object).getEntityName() + " has a qualitygate-application");
					
					//TODO hier entsprechenden Eintrag im MonitorRepository prüfen und ggf. hinzufügen
					
					
					
				}
				
				
			}
			
		}
		

		
		

        
        
        
//        LOGGER.debug("Executed1: " + ((MonitorRepository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getResourceSet().getAllContents()));
        LOGGER.debug("Executed1: " + ((MonitorRepository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY).get(0)).getMonitors().size());
        
//		MonitorRepository monitorRepository = (MonitorRepository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(MonitorRepositoryPackage.eINSTANCE.getMonitorRepository()).get(0);
		MonitorRepository monitorRepository = (MonitorRepository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY).get(0);
		// --> kein MonitorRespository in diesem blackboard? --> selber anlegen?
		
		for(Monitor e : monitorRepository.getMonitors()) {
			LOGGER.debug(e.getMeasuringPoint().getStringRepresentation());
			
			for(MeasurementSpecification i : (e.getMeasurementSpecifications())){
				LOGGER.debug(i.getMetricDescription().getTextualDescription());
			}
			
		}
		
		for(String e : blackboard.getPartitionIds()) {
			LOGGER.debug(e);
		}
		
//		blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).
//		config.getMonitorRepositoryFile()
		
		LOGGER.debug(config.getMonitorRepositoryFile());
		//Connectors
//		for(Connector e : systemModel.getConnectors__ComposedStructure()) {
//			
//			
//			if(!StereotypeAPI.getAppliedStereotypes(e).isEmpty() && StereotypeAPI.getAppliedStereotypes(e).get(0).getName().equals("QualitygateElement")) {
//				LOGGER.debug("The connector " + e.getEntityName() + " has a qualitygate-application");
//				
//				
//				
//				//TODO dieser Connector hat ein Qualitygate Stereotype --> Monitor entsprechend setzen, falls noch nicht gesetzt
////				EList<Monitor> monitors = monitorRepository.getMonitors();
////				LOGGER.debug(monitors.get(0).getMeasuringPoint());
//				
//				
//				
//			}
//			
//			
//		}
		
		
		
		
	}

	@Override
	public void cleanup(IProgressMonitor monitor) throws CleanupFailedException {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getName() {
		return "Qualitygate Calculator Completion";
	}


	@Override
	public void setBlackboard(MDSDBlackboard blackboard) {
		// TODO Auto-generated method stub
		
	}





}
