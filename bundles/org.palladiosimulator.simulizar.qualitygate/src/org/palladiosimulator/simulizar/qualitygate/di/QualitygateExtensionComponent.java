package org.palladiosimulator.simulizar.qualitygate.di;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IExecutableExtensionFactory;
import org.palladiosimulator.simulizar.di.component.core.SimuLizarRuntimeComponent;
import org.palladiosimulator.simulizar.di.component.dependency.QUALComponent;
import org.palladiosimulator.simulizar.di.component.dependency.SimuComFrameworkComponent;
import org.palladiosimulator.simulizar.di.extension.ExtensionComponent;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.qualitygate.interpreter.ComposedStructureSwitchQualitygateContributionSwitch;
import org.palladiosimulator.simulizar.qualitygate.interpreter.RepositoryComponentSwitchQualitygateContributionSwitch;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.QualitygateIssueHandler;
import org.palladiosimulator.simulizar.scopes.RuntimeExtensionScope;

import dagger.Component;

@Component(dependencies = { SimuLizarRuntimeComponent.class, SimuComFrameworkComponent.class, QUALComponent.class })
@RuntimeExtensionScope
public interface QualitygateExtensionComponent extends ExtensionComponent {
	
//	StereotypeQualitygateSwitch.Factory stereotypeQualitygateFactory();
	ComposedStructureSwitchQualitygateContributionSwitch.Factory qualitygateContribution();
	RepositoryComponentSwitchQualitygateContributionSwitch.Factory repositoryQualityGateContribution();
	QualitygateIssueHandler issueHandler();
	
	@Component.Factory
	public static interface Factory extends ExtensionComponent.Factory {
		QualitygateExtensionComponent create(SimuLizarRuntimeComponent runtimeComponent, SimuComFrameworkComponent frameworkComponent, QUALComponent qualComponent);
	}
	
	
	public static class EclipseFactory implements IExecutableExtensionFactory {

        @Override
        public Object create() throws CoreException {
            return DaggerQualitygateExtensionComponent.factory();
        }
        
    }

}
