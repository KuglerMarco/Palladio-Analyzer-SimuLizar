package org.palladiosimulator.simulizar.interpreter.result;

import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.simulizar.interpreter.CallScope;

public class ParameterIssue implements QualitygateIssue {
    
    

    PCMRandomVariable premise;
    CallScope callScope;
    int stackValue;
    
    
    
    
    
    
    public ParameterIssue(PCMRandomVariable premise, CallScope callScope, int stackValue) {
        
        this.premise = premise;
        this.callScope = callScope;
        this.stackValue = stackValue;
        
        
    }






    @Override
    public PCMRandomVariable getPremise() {
        // TODO Auto-generated method stub
        return premise;
    }
}
