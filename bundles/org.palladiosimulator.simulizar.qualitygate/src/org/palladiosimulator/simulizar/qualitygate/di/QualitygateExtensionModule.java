package org.palladiosimulator.simulizar.qualitygate.di;

import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.QualitygateIssueHandler;
import org.palladiosimulator.simulizar.scopes.RuntimeExtensionScope;
import org.palladiosimulator.simulizar.scopes.SimulatedThreadScope;

import dagger.Binds;
import dagger.Module;

@Module
public interface QualitygateExtensionModule {
	@Binds
	@SimulatedThreadScope
	InterpreterResultHandler bindResultHandler(QualitygateIssueHandler impl);
}
