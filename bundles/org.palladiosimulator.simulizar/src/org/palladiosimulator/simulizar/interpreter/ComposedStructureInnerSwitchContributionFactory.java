package org.palladiosimulator.simulizar.interpreter;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.Switch;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.di.extension.Extension;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

public interface ComposedStructureInnerSwitchContributionFactory extends Extension {
    
    public interface ComposedStructureInnerSwitchElementDispatcher {
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
    public Switch<InterpreterResult> create(final InterpreterDefaultContext context, final ComposedStructureInnerSwitchElementDispatcher parentSwitch, final Signature operationSignature,
            final RequiredRole requiredRole);
    
    
    
    
    
}
