package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;


import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;
import org.palladiosimulator.simulizar.qualitygate.interpreter.StereotypeQualitygateSwitch;

import com.google.common.collect.Streams;

/**
 * Handler to process the impact of QualitygateIssues in the InterpreterResult.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateIssueHandler implements InterpreterResultHandler {
    
    private static final Logger LOGGER = Logger.getLogger(StereotypeQualitygateSwitch.class);

    @Inject
    public QualitygateIssueHandler() {
        LOGGER.setLevel(Level.DEBUG);
    }
    
    
    /**
     * To this time: Checks whether in Issue list is only consisting of QualitygateIssues, later: impact of QualitygateIssues
     */
    @Override
    public InterpreterResumptionPolicy handleIssues(InterpreterResult result) {
        
        //TODO Überprüfung ob Proxy Issue Qulaitygate bricht (zu while Schleife)
        if(Streams.stream(result.getIssues()).anyMatch(ResponseTimeProxyIssue.class::isInstance)) {
            
            ResponseTimeProxyIssue issue = (ResponseTimeProxyIssue) Streams.stream(result.getIssues()).filter(ResponseTimeProxyIssue.class::isInstance).findFirst().orElse(null);

            
            LOGGER.debug(issue.getSeffSwitch().getLastMeasure().getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC));
            
            
            
        }
        
        
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
