package org.palladiosimulator.simulizar.interpreter;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.di.extension.Extension;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

public interface ComposedStructureInnerSwitchStereotypeContributionFactory extends Extension {
    
    
    //TODO Refactor (Generalize)
    public interface ComposedStructureInnerSwitchStereotypeElementDispatcher {
        InterpreterResult doSwitch(EClass theEClass, EObject theEObject);
        
        default InterpreterResult doSwitch(EObject theEObject) {
            return doSwitch(theEObject.eClass(), theEObject);
        }
    }
    
    /**
     * 
     * @param context
     *              Default context for the pcm interpreter.
     * @param basicComponentInstance
     *              Simulated component
     * @param parentSwitch
     *              The composed switch which is containing the created switch
     * @return a composable switch
     */
    public StereotypeSwitch createComposedStructureInnerSwitch(final InterpreterDefaultContext context, final ComposedStructureInnerSwitchStereotypeElementDispatcher parentSwitch, final Signature operationSignature,
            final RequiredRole requiredRole);
    
    
    
    
    
}
