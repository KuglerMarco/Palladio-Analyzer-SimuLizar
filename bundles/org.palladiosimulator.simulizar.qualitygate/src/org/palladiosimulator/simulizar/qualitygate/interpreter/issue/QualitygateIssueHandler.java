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
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;
import org.palladiosimulator.simulizar.qualitygate.event.QualitygatePassedEvent;
import org.palladiosimulator.simulizar.qualitygate.measurement.QualitygateViolationProbeRegistry;
import org.palladiosimulator.simulizar.qualitygate.propagation.QualitygatePropagationRecorder;

import com.google.common.collect.Streams;

/**
 * Handler to process the impact of QualitygateIssues in the InterpreterResult.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateIssueHandler implements InterpreterResultHandler {

    private static final Logger LOGGER = Logger.getLogger(QualitygateIssueHandler.class);
    private QualitygatePropagationRecorder recorder;
    private QualitygateViolationProbeRegistry probeRegistry;

    @Inject
    public QualitygateIssueHandler(QualitygatePropagationRecorder recorder, QualitygateViolationProbeRegistry probeRegistry) {
        LOGGER.setLevel(Level.DEBUG);
        this.recorder = recorder;
        this.probeRegistry = probeRegistry;
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
                    
                    InterpreterDefaultContext interpreterDefaultContext = ((ResponseTimeProxyIssue) issue).getContext();

                    Double qualitygateResponseTime = (Double) interpreterDefaultContext.evaluate(premise.getSpecification(), interpreterDefaultContext.getStack().currentStackFrame());

                    Double responseTime = (Double) measuringValue.getValue();
                    
                    if (responseTime > qualitygateResponseTime) {

                        ResponseTimeIssue respIssue = new ResponseTimeIssue(((ResponseTimeProxyIssue) issue).getStereotypedObject(),
                                ((ResponseTimeProxyIssue) issue).getQualitygate(), responseTime);
                        
                        result.addIssue(respIssue);
                        
                        recorder.recordQualitygateIssue(((ResponseTimeProxyIssue) issue).getQualitygate(), ((ResponseTimeProxyIssue) issue).getStereotypedObject(), respIssue);

                     // triggering probe to measure Success-To-Failure-Rate case violation
                        probeRegistry
                            .triggerProbe(new QualitygatePassedEvent(((ResponseTimeProxyIssue) issue).getQualitygate(), interpreterDefaultContext, false, null));
                        
                        probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(((ResponseTimeProxyIssue) issue).getQualitygate(), interpreterDefaultContext, false, ((ResponseTimeProxyIssue) issue).getQualitygate().getCriticality()));
                        
                        LOGGER.debug("Following StoEx is broken: " + responseTime);

                    } else {
                     // triggering probe to measure Success-To-Failure-Rate case successful
                        probeRegistry
                            .triggerProbe(new QualitygatePassedEvent(((ResponseTimeProxyIssue) issue).getQualitygate(), interpreterDefaultContext, true, null));
                        
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
