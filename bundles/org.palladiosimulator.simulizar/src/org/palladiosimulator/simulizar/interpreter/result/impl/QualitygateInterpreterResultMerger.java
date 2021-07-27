package org.palladiosimulator.simulizar.interpreter.result.impl;


import javax.inject.Inject;

import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;

import com.google.common.collect.Iterables;

/**
 * Merger for QualitygateInterpreterResults.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateInterpreterResultMerger implements InterpreterResultMerger {

    @Inject
    public QualitygateInterpreterResultMerger() {
        
    }
    
    /**
     * Merges beside the InterpreterIssues, also the QualitygateIssues.
     */
    @Override
    public InterpreterResult merge(InterpreterResult previousResult, InterpreterResult newResult) {
        
        
        //TODO Performance verbessern
        
        
        QualitygateInterpreterResult interpreterResult = new QualitygateInterpreterResult();
        
        
        if(previousResult instanceof QualitygateInterpreterResult) {
            Iterables.addAll(interpreterResult.qualitygateIssues, ((QualitygateInterpreterResult) previousResult).getQualitygateIssues());
        }
        if(newResult instanceof QualitygateInterpreterResult) {
            Iterables.addAll(interpreterResult.qualitygateIssues, ((QualitygateInterpreterResult) newResult).getQualitygateIssues());
        }
        

        
        Iterables.addAll(interpreterResult.issues, previousResult.getIssues());
        Iterables.addAll(interpreterResult.issues, newResult.getIssues());
        
        
        return interpreterResult;
    }

}
