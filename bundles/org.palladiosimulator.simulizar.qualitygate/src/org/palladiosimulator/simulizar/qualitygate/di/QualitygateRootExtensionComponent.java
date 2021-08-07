package org.palladiosimulator.simulizar.qualitygate.di;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.palladiosimulator.simulizar.di.component.core.SimuLizarRootComponent;
import org.palladiosimulator.simulizar.scopes.RootExtensionScope;
import org.palladiosimulator.simulizar.di.extension.ExtensionComponent;
import org.palladiosimulator.simulizar.qualitygate.jobs.QualitygateCalculatorJobContribution;

import dagger.Component;

@Component(dependencies = {SimuLizarRootComponent.class})
@RootExtensionScope
public interface QualitygateRootExtensionComponent extends ExtensionComponent {
	
	QualitygateCalculatorJobContribution qualitygateCompletionJob();
	
	
	@Component.Factory
	public static interface Factory extends ExtensionComponent.Factory {
		QualitygateRootExtensionComponent create(SimuLizarRootComponent rootComponent);
	}
	
	public static class EclipseFactory implements IExecutableExtensionFactory {

        @Override
        public Object create() throws CoreException {
            return DaggerQualitygateRootExtensionComponent.factory();
        }
        
    }

}
