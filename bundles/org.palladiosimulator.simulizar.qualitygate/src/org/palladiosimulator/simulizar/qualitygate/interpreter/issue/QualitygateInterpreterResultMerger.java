package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;

import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;

import com.google.common.collect.Iterables;

public class QualitygateInterpreterResultMerger implements InterpreterResultMerger {

    @Override
    public InterpreterResult merge(InterpreterResult previousResult, InterpreterResult newResult) {
        // TODO hier die issues von previousResult in Recorder speichern, wenn Proxy drauf liegt
        
        if (previousResult.hasNoIssues()) return newResult;
        if (newResult.hasNoIssues()) return previousResult;
        if (previousResult instanceof BasicInterpreterResult) {
            Iterables.addAll(((BasicInterpreterResult)previousResult).issues, newResult.getIssues());
            return previousResult;
        } else if (newResult instanceof BasicInterpreterResult) {
            Iterables.addAll(((BasicInterpreterResult)newResult).issues, previousResult.getIssues());
            return newResult;
        } else {
            var result = new BasicInterpreterResult();
            Iterables.addAll(result.issues, previousResult.getIssues());
            Iterables.addAll(result.issues, newResult.getIssues());
            return result;
        }
    }
    
    
    
    
    
    
    

}
