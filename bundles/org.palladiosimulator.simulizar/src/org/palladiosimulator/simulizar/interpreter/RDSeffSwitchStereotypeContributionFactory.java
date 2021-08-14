package org.palladiosimulator.simulizar.interpreter;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.di.extension.Extension;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

public interface RDSeffSwitchStereotypeContributionFactory extends Extension {

    // TODO Refactor (Generalize)
    public interface RDSeffSwitchElementDispatcher {
        InterpreterResult doSwitch(EClass theEClass, EObject theEObject);

        default InterpreterResult doSwitch(EObject theEObject) {
            return doSwitch(theEObject.eClass(), theEObject);
        }
    }

    /**
     * @param context
     * @param operationSignature
     * @param requiredRole
     * @return StereotypeSwitch
     */
    public StereotypeSwitch create(final InterpreterDefaultContext context,
            RDSeffSwitchElementDispatcher parentSwitch);

}
