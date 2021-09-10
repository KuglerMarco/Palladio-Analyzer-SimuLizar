package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import javax.inject.Inject;
import javax.measure.Measure;
import javax.measure.quantity.Quantity;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.simulizar.failurescenario.interpreter.issue.FailureOccurredIssue;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;
import org.palladiosimulator.simulizar.qualitygate.event.QualitygatePassedEvent;
import org.palladiosimulator.simulizar.qualitygate.measurement.QualitygateViolationProbeRegistry;
import org.palladiosimulator.simulizar.qualitygate.propagation.QualitygatePropagationRecorder;

import com.google.common.collect.Streams;

import de.uka.ipd.sdq.simucomframework.variables.stackframe.SimulatedStackframe;

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
    public QualitygateIssueHandler(QualitygatePropagationRecorder recorder,
            QualitygateViolationProbeRegistry probeRegistry) {
        LOGGER.setLevel(Level.DEBUG);
        this.recorder = recorder;
        this.probeRegistry = probeRegistry;
    }

    @Override
    public InterpreterResumptionPolicy handleIssues(InterpreterResult result) {

        result = this.handleResponseTimeProxy(result);
        result= this.handleCrashProxyIssues(result);
        
        result = this.recordIssues(result);

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

                if (((ResponseTimeProxyIssue) issue).isHandledOnce()) {
                    // Checking the Qualitygate-Premise
                    try {
                        
                     // set temporary stack for evaluation
                        final SimulatedStackframe<Object> frame = ((ResponseTimeProxyIssue) issue).getContext().getStack().createAndPushNewStackFrame();

                        Measure<Object, Quantity> measuringValue = ((ResponseTimeProxyIssue) issue)
                            .getResponseTimeQualitygateSwitch()
                            .getLastResponseTimeMeasure()
                            .getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC);
                        
                        frame.addValue("ResponseTime.VALUE", (Double) measuringValue.getValue());
                        
                        PCMRandomVariable premise = ((ResponseTimeProxyIssue) issue).getPremise();
                        
                        InterpreterDefaultContext interpreterDefaultContext = ((ResponseTimeProxyIssue) issue)
                                .getContext();
                        
                        if (!((boolean) interpreterDefaultContext.evaluate(premise.getSpecification(), interpreterDefaultContext.getStack()
                                .currentStackFrame()))) {
                            
                            
                            ResponseTimeIssue respIssue = new ResponseTimeIssue(
                                    ((ResponseTimeProxyIssue) issue).getStereotypedObject(),
                                    ((ResponseTimeProxyIssue) issue).getQualitygate(), false);

                            result.addIssue(respIssue);

                            recorder.recordQualitygateIssue(((ResponseTimeProxyIssue) issue).getQualitygate(),
                                    ((ResponseTimeProxyIssue) issue).getStereotypedObject(), respIssue);

                            // triggering probe to measure Success-To-Failure-Rate case
                            // violation
                            probeRegistry.triggerProbe(
                                    new QualitygatePassedEvent(((ResponseTimeProxyIssue) issue).getQualitygate(),
                                            interpreterDefaultContext, false, null));

                            probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(
                                    ((ResponseTimeProxyIssue) issue).getQualitygate(), interpreterDefaultContext,
                                    false, ((ResponseTimeProxyIssue) issue).getQualitygate()
                                        .getSeverity()));

                            
                        } else {
                            probeRegistry.triggerProbe(
                                    new QualitygatePassedEvent(((ResponseTimeProxyIssue) issue).getQualitygate(),
                                            interpreterDefaultContext, true, null));
                        }
                        
                        
                        interpreterDefaultContext.getStack().removeStackFrame();
//
//                        
//
//                        InterpreterDefaultContext interpreterDefaultContext = ((ResponseTimeProxyIssue) issue)
//                            .getContext();
//
//                        Double qualitygateResponseTime = (Double) interpreterDefaultContext
//                            .evaluate(premise.getSpecification(), interpreterDefaultContext.getStack()
//                                .currentStackFrame());
//
//                        Double responseTime = (Double) measuringValue.getValue();
//
//                        if (((ResponseTimeProxyIssue) issue).isLateTimingScope()) {
//
//                            if (responseTime > qualitygateResponseTime) {
//
//                                ResponseTimeIssue respIssue = new ResponseTimeIssue(
//                                        ((ResponseTimeProxyIssue) issue).getStereotypedObject(),
//                                        ((ResponseTimeProxyIssue) issue).getQualitygate(), false);
//
//                                result.addIssue(respIssue);
//
//                                recorder.recordQualitygateIssue(((ResponseTimeProxyIssue) issue).getQualitygate(),
//                                        ((ResponseTimeProxyIssue) issue).getStereotypedObject(), respIssue);
//
//                                // triggering probe to measure Success-To-Failure-Rate case
//                                // violation
//                                probeRegistry.triggerProbe(
//                                        new QualitygatePassedEvent(((ResponseTimeProxyIssue) issue).getQualitygate(),
//                                                interpreterDefaultContext, false, null));
//
//                                probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(
//                                        ((ResponseTimeProxyIssue) issue).getQualitygate(), interpreterDefaultContext,
//                                        false, ((ResponseTimeProxyIssue) issue).getQualitygate()
//                                            .getSeverity()));
//
//                                LOGGER.debug("Following StoEx is broken: " + responseTime);
//
//                            } else {
//                                // triggering probe to measure Success-To-Failure-Rate case
//                                // successful
//                                probeRegistry.triggerProbe(
//                                        new QualitygatePassedEvent(((ResponseTimeProxyIssue) issue).getQualitygate(),
//                                                interpreterDefaultContext, true, null));
//
//                            }
//                        } else {
//                            
//                            if (responseTime < qualitygateResponseTime) {
//
//                                ResponseTimeIssue respIssue = new ResponseTimeIssue(
//                                        ((ResponseTimeProxyIssue) issue).getStereotypedObject(),
//                                        ((ResponseTimeProxyIssue) issue).getQualitygate(), false);
//
//                                result.addIssue(respIssue);
//
//                                recorder.recordQualitygateIssue(((ResponseTimeProxyIssue) issue).getQualitygate(),
//                                        ((ResponseTimeProxyIssue) issue).getStereotypedObject(), respIssue);
//
//                                // triggering probe to measure Success-To-Failure-Rate case
//                                // violation
//                                probeRegistry.triggerProbe(
//                                        new QualitygatePassedEvent(((ResponseTimeProxyIssue) issue).getQualitygate(),
//                                                interpreterDefaultContext, false, null));
//
//                                probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(
//                                        ((ResponseTimeProxyIssue) issue).getQualitygate(), interpreterDefaultContext,
//                                        false, ((ResponseTimeProxyIssue) issue).getQualitygate()
//                                            .getSeverity()));
//
//                                LOGGER.debug("Following StoEx is broken: " + responseTime);
//
//                            } else {
//                                // triggering probe to measure Success-To-Failure-Rate case
//                                // successful
//                                probeRegistry.triggerProbe(
//                                        new QualitygatePassedEvent(((ResponseTimeProxyIssue) issue).getQualitygate(),
//                                                interpreterDefaultContext, true, null));
//
//                            }
//                            
//                            
//                            
//                        }

                    } catch (NoSuchElementException e) {
                        // FIXME SimuLizar-Bug: No Measurements after simulation had stopped but
                        // still in
                        // control flow
                        LOGGER.debug("NoElement");

                    }

                    // Removing the Proxy
                    result.removeIssue(issue);

                    iter = result.getIssues()
                        .iterator();
                } else {

                    ((ResponseTimeProxyIssue) issue).setHandledOnce(true);

                }

            }

        }

        return result;

    }

    public InterpreterResult recordIssues(InterpreterResult interpreterResult) {

        InterpreterResult result = interpreterResult;

        // if unhandled issues are on interpreterResult, then persist the issues
        // (issues when the unhandled issues where broken

        // List for the issues which where there, when the qualitygates where broken
        List<InterpretationIssue> issuesWhenBroken = new ArrayList<InterpretationIssue>();

        // handled issues where present when unhandled issues where broken
        for (InterpretationIssue issue : result.getIssues()) {
            if (issue.isHandled()) {
                issuesWhenBroken.add(issue);

            }
        }

        // for every unhandled Issue persist the Issues
        for (InterpretationIssue issue : result.getIssues()) {
            if (!issue.isHandled() && issue instanceof QualitygateIssue) {

                for (InterpretationIssue logIssue : issuesWhenBroken) {
                    if (issue instanceof QualitygateIssue) {
                        LOGGER.debug(((QualitygateIssue) logIssue).getQualitygateId());
                    }
                }

                recorder.recordIssues(issuesWhenBroken, ((QualitygateIssue) issue).getQualitygateRef());

                probeRegistry.triggerIssueProbes(issuesWhenBroken, ((QualitygateIssue) issue).getQualitygateRef());

                if (issue instanceof QualitygateIssue) {
                    ((QualitygateIssue) issue).setHandled(true);
                    LOGGER.debug(((QualitygateIssue) issue).getQualitygateId());
                }
            }
        }

        return result;

    }
    
    public InterpreterResult handleCrashProxyIssues(InterpreterResult result) {
        
        /*
         * Processing the Proxies in Issues
         */
        Iterator<InterpretationIssue> iter = result.getIssues()
            .iterator();

        while (iter.hasNext()) {

            var issue = iter.next();
            
            if(issue instanceof CrashProxyIssue) {
                
                InterpreterDefaultContext interpreterDefaultContext = ((CrashProxyIssue) issue)
                        .getContext();
                
                if (!Streams.stream(result.getIssues()).anyMatch(FailureOccurredIssue.class:: isInstance)) {
                    
                 // triggering probe to measure Success-To-Failure-Rate case
                    // violation
                    probeRegistry.triggerProbe(
                            new QualitygatePassedEvent(((CrashProxyIssue) issue).getQualitygate(),
                                    interpreterDefaultContext, false, null));

                    probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(
                            ((CrashProxyIssue) issue).getQualitygate(), interpreterDefaultContext,
                            false, ((CrashProxyIssue) issue).getQualitygate()
                                .getSeverity()));
                    
                    
                } else {
                    
                    // triggering probe to measure Success-To-Failure-Rate case
                    // successful
                    probeRegistry.triggerProbe(
                            new QualitygatePassedEvent(((CrashProxyIssue) issue).getQualitygate(),
                                    interpreterDefaultContext, true, null));
                    
                }
                
                // Removing the Proxy
                result.removeIssue(issue);

                iter = result.getIssues()
                    .iterator();
                
                
            }
            
        
        
        }
        
        return result;
        
    }

}
