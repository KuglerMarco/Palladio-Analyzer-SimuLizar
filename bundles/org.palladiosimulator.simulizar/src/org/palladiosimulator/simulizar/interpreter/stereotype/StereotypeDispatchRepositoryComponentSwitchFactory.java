package org.palladiosimulator.simulizar.interpreter.stereotype;

import org.eclipse.emf.ecore.util.Switch;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

/**
 * Creates the Dispatch Switch for stereotype processing at RepositoryComponent elements.
 * 
 * @author Marco Kugler
 *
 */
public interface StereotypeDispatchRepositoryComponentSwitchFactory {

    Switch<InterpreterResult> create(final InterpreterDefaultContext context, final AssemblyContext assemblyContext,
            final Signature signature, final ProvidedRole providedRole);

}
