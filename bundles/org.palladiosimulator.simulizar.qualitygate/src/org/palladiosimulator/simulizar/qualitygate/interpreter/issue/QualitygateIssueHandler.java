package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.inject.Inject;
import javax.measure.Measure;
import javax.measure.quantity.Quantity;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
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

    private static final Logger LOGGER = Logger.getLogger(QualitygateIssueHandler.class);

    @Inject
    public QualitygateIssueHandler() {
        LOGGER.setLevel(Level.DEBUG);
    }

    
    @Override
    public InterpreterResumptionPolicy handleIssues(InterpreterResult result) {
        
        result = this.handleResponseTimeProxy(result);

        if (!Streams.stream(result.getIssues())
            .allMatch(QualitygateIssue.class::isInstance)) {
            return InterpreterResumptionPolicy.ABORT;
        }

        return InterpreterResumptionPolicy.CONTINUE;
    }

    @Override
    public boolean supportIssues(InterpretationIssue issue) {

        if (issue instanceof QualitygateIssue) {
            return true;
        }

        return false;

    }
    
    public InterpreterResult handleResponseTimeProxy(InterpreterResult result) {
        
        /*
         * Processing the Proxies in Issues
         */
        Iterator<InterpretationIssue> iter = result.getIssues()
            .iterator();

        while (iter.hasNext()) {

            var issue = iter.next();

            if (issue instanceof ResponseTimeProxyIssue) {

                // Checking the Qualitygate-Premise
                try {

                    Measure<Object, Quantity> measuringValue = ((ResponseTimeProxyIssue) issue)
                        .getResponseTimeQualitygateSwitch()
                        .getLastResponseTimeMeasure()
                        .getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC);

                    PCMRandomVariable premise = ((ResponseTimeProxyIssue) issue).getPremise();

//                    Measure<Integer,Duration> measuringValueCon = Measure.valueOf(Integer.parseInt(premise.getSpecification()), Quantity);

                    Double responseTime = (Double) measuringValue.getValue();
                    
                    Double premiseResponseTime = Double.parseDouble(premise.getSpecification());

                    // TODO nicht optimal: mit JScience und Parser arbeiten
                    if (responseTime > premiseResponseTime) {

                        result.addIssue(new ResponseTimeIssue(((ResponseTimeProxyIssue) issue).getStereotypedObject(),
                                ((ResponseTimeProxyIssue) issue).getQualitygate(), responseTime));

                        LOGGER.debug("Response-Time too long. " + responseTime);

                    }

                } catch (NoSuchElementException e) {
                    // FIXME SimuLizar-Bug: No Measurements after simulation had stopped but still in
                    // control flow
                    LOGGER.debug("NoElement");
                    
                }

                // Removing the Proxy
                result.removeIssue(issue);

                LOGGER.debug(result.getIssues());

                iter = result.getIssues()
                    .iterator();

            }

        }
        
        return result;
        
    }

}
