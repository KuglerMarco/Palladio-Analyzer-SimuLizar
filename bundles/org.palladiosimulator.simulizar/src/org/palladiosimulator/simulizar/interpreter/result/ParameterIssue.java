package org.palladiosimulator.simulizar.interpreter.result;

import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.simulizar.interpreter.CallScope;

public class ParameterIssue implements QualitygateIssue {
    
    

    private final EObject stereotypedObject;
    private final CallScope callScope;
    private final int valueOnStack;
    
    
    public ParameterIssue(EObject stereotypedObject, CallScope callScope, int valueOnStack) {
        
        this.stereotypedObject = stereotypedObject;
        this.callScope = callScope;
        this.valueOnStack = valueOnStack;

    }

    //TODO URI speichern nicht Pointer
    public EObject getStereotypedObject() {
        return stereotypedObject;
    }

    public CallScope getCallScope() {
        return callScope;
    }
    

    public int getValueOnStack() {
        return valueOnStack;
    }
}
