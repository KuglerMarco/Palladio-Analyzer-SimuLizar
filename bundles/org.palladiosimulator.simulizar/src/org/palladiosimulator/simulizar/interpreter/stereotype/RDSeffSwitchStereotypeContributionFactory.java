package org.palladiosimulator.simulizar.interpreter.stereotype;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.simulizar.di.extension.Extension;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

/**
 * In order to provide an additional StereotypeSwitch for the processing of stereotypes at RDSeff
 * elements, the factory for the StereotypeSwitch need to implement this interface.
 * 
 * @author Marco Kugler
 *
 */
public interface RDSeffSwitchStereotypeContributionFactory extends Extension {

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
    public StereotypeSwitch create(final InterpreterDefaultContext context, RDSeffSwitchElementDispatcher parentSwitch);

}
