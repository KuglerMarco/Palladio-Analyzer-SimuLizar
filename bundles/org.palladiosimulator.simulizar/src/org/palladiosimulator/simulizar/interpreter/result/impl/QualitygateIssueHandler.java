package org.palladiosimulator.simulizar.interpreter.result.impl;

import java.util.ArrayList;

import javax.inject.Inject;

import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;
import org.palladiosimulator.simulizar.interpreter.result.QualitygateIssue;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;

/**
 * Handler to process the impact of QualitygateIssues in the InterpreterResult.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateIssueHandler implements InterpreterResultHandler {
    

    @Inject
    public QualitygateIssueHandler() {
    }
    
    
    /**
     * To this time: Checks whether in Issue list is only consisting of QualitygateIssues, later: impact of QualitygateIssues
     */
    @Override
    public InterpreterResumptionPolicy handleIssues(InterpreterResult result) {
        
        //TODO Streams.of
        ArrayList<InterpretationIssue> list = Lists.newArrayList(result.getIssues());
        Streams.stream(result.getIssues()).filter(QualitygateIssue.class::isInstance).findAny();
        
        for(InterpretationIssue e : list) {
            if(!(e instanceof QualitygateIssue)) {
                return InterpreterResumptionPolicy.ABORT;
            }
        }
        
        return InterpreterResumptionPolicy.CONTINUE;
    }

}
