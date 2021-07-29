package org.palladiosimulator.simulizar.interpreter;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

public interface ElementDispatcher {
    InterpreterResult doSwitch(EClass theEClass, EObject theEObject);
    
    default InterpreterResult doSwitch(EObject theEObject) {
        return doSwitch(theEObject.eClass(), theEObject);
    }
}
