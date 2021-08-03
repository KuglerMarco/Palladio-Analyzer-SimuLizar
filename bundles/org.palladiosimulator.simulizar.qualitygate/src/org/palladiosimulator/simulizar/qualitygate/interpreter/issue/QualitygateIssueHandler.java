package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;


import javax.inject.Inject;

import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;

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
        
        
        if(!Streams.stream(result.getIssues()).allMatch(QualitygateIssue.class::isInstance)) {
            return InterpreterResumptionPolicy.ABORT;
        }
        
        
        return InterpreterResumptionPolicy.CONTINUE;
    }


	@Override
	public boolean supportIssues(InterpretationIssue issue) {
		
		if(issue instanceof QualitygateIssue) {
			return true;
		}
		
		return false;
		
	}

}
