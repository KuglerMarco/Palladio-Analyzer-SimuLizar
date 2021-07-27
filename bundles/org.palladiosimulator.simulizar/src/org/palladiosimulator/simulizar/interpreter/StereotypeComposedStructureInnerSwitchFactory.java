package org.palladiosimulator.simulizar.interpreter;

import org.eclipse.emf.ecore.util.Switch;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

/**
 * Factory for the superior StereotypeDispatchSwitch.
 * 
 * @author Marco Kugler
 *
 */
public interface StereotypeComposedStructureInnerSwitchFactory {

    //TODO dieses Interface l�schen und das Interface des ComposedStrucutreSwitch nehmen
    Switch<InterpreterResult> create(final InterpreterDefaultContext context, final Signature operationSignature,
            final RequiredRole requiredRole);
    
    

}
