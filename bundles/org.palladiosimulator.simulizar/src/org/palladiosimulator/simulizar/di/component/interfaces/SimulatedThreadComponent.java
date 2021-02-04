package org.palladiosimulator.simulizar.di.component.interfaces;

import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.InterpreterFacade;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext.ParentContext;

import dagger.BindsInstance;
import de.uka.ipd.sdq.simucomframework.SimuComSimProcess;

public interface SimulatedThreadComponent {
    InterpreterFacade interpreterFacade();
    
    public static interface Factory {
        SimulatedThreadComponent create(@BindsInstance @ParentContext InterpreterDefaultContext parentContext,
                @BindsInstance SimuComSimProcess simProcess);
    }
}
