package org.palladiosimulator.simulizar.qualitygate.di;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.palladiosimulator.simulizar.di.component.core.SimuLizarRuntimeComponent;
import org.palladiosimulator.simulizar.di.component.dependency.SimuComFrameworkComponent;
import org.palladiosimulator.simulizar.di.extension.ExtensionComponent;
import org.palladiosimulator.simulizar.qualitygate.interpreter.StereotypeQualitygateSwitch;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.QualitygateIssueHandler;
import org.palladiosimulator.simulizar.scopes.RuntimeExtensionScope;

import dagger.Component;

@Component(dependencies = { SimuLizarRuntimeComponent.class, SimuComFrameworkComponent.class } //, modules = {QualitygateExtensionModule.class}
)
@RuntimeExtensionScope
public interface QualitygateExtensionComponent extends ExtensionComponent {
	
	StereotypeQualitygateSwitch.Factory stereotypeQualitygateFactory();
	QualitygateIssueHandler issueHandler();
	
	@Component.Factory
	public static interface Factory extends ExtensionComponent.Factory {
		QualitygateExtensionComponent create(SimuLizarRuntimeComponent runtimeComponent, SimuComFrameworkComponent frameworkComponent);
	}
	
	
	public static class EclipseFactory implements IExecutableExtensionFactory {

        @Override
        public Object create() throws CoreException {
            return DaggerQualitygateExtensionComponent.factory();
        }
        
    }

}
