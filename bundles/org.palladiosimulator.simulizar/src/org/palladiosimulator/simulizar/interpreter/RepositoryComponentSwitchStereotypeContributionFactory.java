package org.palladiosimulator.simulizar.interpreter;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.di.extension.Extension;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

/**
 * Implementing this interface contributes a StereotypeSwitch, which will handle attached
 * Stereotypes to RepositoryComponent-elements.
 * 
 * @author Marco Kugler
 *
 */
public interface RepositoryComponentSwitchStereotypeContributionFactory extends Extension {

    public interface RepositoryComponentSwitchStereotypeElementDispatcher {
        InterpreterResult doSwitch(EClass theEClass, EObject theEObject);

        default InterpreterResult doSwitch(EObject theEObject) {
            return doSwitch(theEObject.eClass(), theEObject);
        }
    }

    public StereotypeSwitch create(final InterpreterDefaultContext context, final AssemblyContext assemblyContext,
            final Signature signature, final ProvidedRole providedRole,
            RepositoryComponentSwitchStereotypeElementDispatcher parentSwitch);

}
