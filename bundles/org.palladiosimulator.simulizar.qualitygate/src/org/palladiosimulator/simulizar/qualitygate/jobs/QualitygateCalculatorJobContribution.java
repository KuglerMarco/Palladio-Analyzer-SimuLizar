package org.palladiosimulator.simulizar.qualitygate.jobs;

import javax.inject.Inject;

import org.palladiosimulator.simulizar.launcher.jobs.ModelCompletionJobContributor;

public class QualitygateCalculatorJobContribution implements ModelCompletionJobContributor {

	QualitygateResponseTimeCalculatorJob job;
	
	@Inject
	public QualitygateCalculatorJobContribution(QualitygateResponseTimeCalculatorJob job) {
		this.job = job;
	}
	
	
	@Override
	public void contribute(Facade delegate) {

		delegate.contribute(job);
	}
	
	

}
