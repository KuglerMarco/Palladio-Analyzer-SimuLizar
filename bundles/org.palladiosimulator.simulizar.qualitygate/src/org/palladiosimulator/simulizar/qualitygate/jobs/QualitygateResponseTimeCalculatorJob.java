package org.palladiosimulator.simulizar.qualitygate.jobs;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.simulizar.launcher.jobs.ModelCompletionJobContributor;


import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import org.palladiosimulator.pcm.core.composition.CompositionPackage;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.system.System;


public class QualitygateResponseTimeCalculatorJob implements IBlackboardInteractingJob<MDSDBlackboard>, ModelCompletionJobContributor {
	
	/*
	 * Blackboard enthält die Partitionen mit den Modellen
	 * Hier drin soll das Modell traversiert und die Calculatoren gesetzt werden
	 * Wird dann ausgeführt nachdem die Modelle geladen sind
	 */
	
	Logger LOGGER = Logger.getLogger(QualitygateResponseTimeCalculatorJob.class);
	
	private final MDSDBlackboard blackboard;
	
	
	@Inject
	public QualitygateResponseTimeCalculatorJob(MDSDBlackboard blackboard) {
		this.blackboard = blackboard;
		LOGGER.setLevel(Level.DEBUG);
		
	}
	
	
		@Override
	public void contribute(Facade delegate) {
		// TODO Auto-generated method stub
		
		//TODO delegate.contribute(IBlackboardInteractingJob<MDSDBlackboard> contribution)
		//BlackboardJob wird aufgerufen wenn Modell geladen, hier die Calculators im MonitorRespository setzen
		
		LOGGER.debug("System-Model loaded: " + blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(CompositionPackage.eINSTANCE.getComposedStructure()).get(0));
		
		System systemModel = (System) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(CompositionPackage.eINSTANCE.getComposedStructure()).get(0);
		LOGGER.debug(systemModel.getConnectors__ComposedStructure().get(0).getEntityName());
		LOGGER.debug(systemModel.getConnectors__ComposedStructure().get(1).getEntityName());
		
		for(Connector e : systemModel.getConnectors__ComposedStructure()) {
			LOGGER.debug(e.getEntityName());
		}
		
		for(String e : blackboard.getPartitionIds()) {
			LOGGER.debug(e);
			
		}
		
		delegate.contribute(this);
	}
	
	
	
	@Override
	public void execute(IProgressMonitor monitor) throws JobFailedException, UserCanceledException {
		// TODO Auto-generated method stub
//		System.out.println("System-Model loaded: " + blackboard.hasPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID));
		
		
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
