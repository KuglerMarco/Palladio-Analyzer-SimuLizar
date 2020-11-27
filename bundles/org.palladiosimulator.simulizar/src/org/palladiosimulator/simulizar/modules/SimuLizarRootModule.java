package org.palladiosimulator.simulizar.modules;

import javax.inject.Named;

import org.palladiosimulator.simulizar.SimuLizarModelCompletionComponent;
import org.palladiosimulator.simulizar.SimuLizarModelLoadComponent;
import org.palladiosimulator.simulizar.SimuLizarSimulationComponent;
import org.palladiosimulator.simulizar.launcher.SimulizarConstants;
import org.palladiosimulator.simulizar.launcher.jobs.LoadSimuLizarModelsIntoBlackboardJob;
import org.palladiosimulator.simulizar.launcher.jobs.PCMInterpreterRootCompositeJob;
import org.palladiosimulator.simulizar.launcher.jobs.PCMStartInterpretationJob;
import org.palladiosimulator.simulizar.modules.core.ExtensionFactoriesModule;
import org.palladiosimulator.simulizar.modules.custom.CustomMDSDBlackboardProvidingModule;
import org.palladiosimulator.simulizar.modules.custom.CustomSimuLizarExtensionsProvidingModule;

import dagger.Binds;
import dagger.Module;
import de.uka.ipd.sdq.workflow.jobs.IJob;

@Module(subcomponents = { SimuLizarModelLoadComponent.class, SimuLizarModelCompletionComponent.class, SimuLizarSimulationComponent.class }, 
        includes = { CustomSimuLizarExtensionsProvidingModule.class, CustomMDSDBlackboardProvidingModule.class,
                SimuLizarConfigurationModule.class, ExtensionFactoriesModule.class })
public interface SimuLizarRootModule {

    @Binds
    @Named(SimulizarConstants.ROOT_JOB_ID)
    IJob bindRootJob(PCMInterpreterRootCompositeJob impl);

    @Binds
    @Named(SimulizarConstants.MODEL_LOAD_JOB_ID)
    IJob bindModelLoadJob(LoadSimuLizarModelsIntoBlackboardJob impl);

    @Binds
    @Named(SimulizarConstants.INTERPRETER_JOB_ID)
    IJob bindInterpretationJob(PCMStartInterpretationJob impl);

}
