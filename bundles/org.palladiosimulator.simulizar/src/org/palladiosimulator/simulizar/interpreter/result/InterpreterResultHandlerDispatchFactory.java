package org.palladiosimulator.simulizar.interpreter.result;

import org.palladiosimulator.simulizar.interpreter.result.impl.NoIssuesHandler;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

public class InterpreterResultHandlerDispatchFactory {
    
    private final Provider<Set<InterpreterResultHandler>> interpreterResultHandlerProvider;
    
    @Inject
    public InterpreterResultHandlerDispatchFactory(Provider<Set<InterpreterResultHandler>> interpreterResultHandlerProvider) {
        this.interpreterResultHandlerProvider = interpreterResultHandlerProvider;
    }
    
    public InterpreterResultHandler create() {
        
        var elementFactories = interpreterResultHandlerProvider.get();
        
        if(!elementFactories.isEmpty()) {
            return elementFactories.stream().findFirst().orElse(null);
        }
        
        return new NoIssuesHandler();
        
    }

}
