package org.palladiosimulator.simulizar.interpreter;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.di.extension.Extension;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

/**
 * Interface to contribute StereotypeSwitches to handle attached Stereotypes to the ComposedStructure-elements.
 * 
 * @author Marco Kugler
 *
 */
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
     * 
     * @param context
     *              Default context for the PCM-interpreter.
     * @param basicComponentInstance
     *              Simulated component
     * @param parentSwitch
     *              The Dispatch-Switch which is containing the created switch
     * @return StereotypeSwitch
     */
    public StereotypeSwitch createStereotypeSwitch(final InterpreterDefaultContext context);
    
    
    
    
    
}
