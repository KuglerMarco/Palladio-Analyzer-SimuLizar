package org.palladiosimulator.simulizar.interpreter;

import org.eclipse.emf.ecore.util.Switch;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

public interface RepositoryComponentSwitchFactory {
    Switch<InterpreterResult> create(final InterpreterDefaultContext context, final AssemblyContext assemblyContext,
            final Signature signature, final ProvidedRole providedRole);
}
