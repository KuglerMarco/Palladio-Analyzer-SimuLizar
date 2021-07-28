package org.palladiosimulator.simulizar.interpreter.result.impl;

import java.util.ArrayList;

import javax.inject.Inject;

import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;
import org.palladiosimulator.simulizar.interpreter.result.QualitygateIssue;

import com.google.common.collect.Lists;

public class QualitygateIssueHandler implements InterpreterResultHandler {
    
    @Inject
    public QualitygateIssueHandler() {
    }
    
    
    @Override
    public InterpreterResumptionPolicy handleIssues(InterpreterResult result) {
        
        ArrayList<InterpretationIssue> list = Lists.newArrayList(result.getIssues());
        
        for(InterpretationIssue e : list) {
            if(!(e instanceof QualitygateIssue)) {
                return InterpreterResumptionPolicy.ABORT;
            }
        }
        
        return InterpreterResumptionPolicy.CONTINUE;
    }

}
