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
import org.palladiosimulator.failuremodel.failuretype.Failure;
import org.palladiosimulator.failuremodel.failuretype.SWContentFailure;
import org.palladiosimulator.failuremodel.failuretype.SWCrashFailure;
import org.palladiosimulator.failuremodel.failuretype.SWTimingFailure;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.CorruptContentBehavior;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.CrashBehavior;
import org.palladiosimulator.simulizar.failurescenario.interpreter.behavior.preinterpretation.DelayBehavior;
import org.palladiosimulator.simulizar.failurescenario.interpreter.issue.FailureOccurredIssue;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.preinterpretation.PreInterpretationBehavior;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.qualitygate.event.QualitygatePassedEvent;
import org.palladiosimulator.simulizar.qualitygate.measurement.QualitygateViolationProbeRegistry;
import org.palladiosimulator.simulizar.qualitygate.metric.QualitygateMetricDescriptionConstants;
import org.palladiosimulator.simulizar.qualitygate.propagation.QualitygatePropagationRecorder;
import org.palladiosimulator.failuremodel.qualitygate.RequestMetricScope;
import org.palladiosimulator.failuremodel.qualitygate.RequestParameterScope;
import org.palladiosimulator.failuremodel.qualitygate.ResultParameterScope;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;

import com.google.common.collect.Streams;

import de.uka.ipd.sdq.simucomframework.variables.stackframe.SimulatedStackframe;

/**
 * Handler to process QualitygateIssues in the InterpreterResult.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateIssueHandler implements InterpreterResultHandler {

    private static final Logger LOGGER = Logger.getLogger(QualitygateIssueHandler.class);
    private QualitygatePropagationRecorder recorder;
    private QualitygateViolationProbeRegistry probeRegistry;
    private BasicInterpreterResultMerger merger;

    @Inject
    public QualitygateIssueHandler(QualitygatePropagationRecorder recorder,
            QualitygateViolationProbeRegistry probeRegistry, BasicInterpreterResultMerger merger) {
        LOGGER.setLevel(Level.DEBUG);
        this.recorder = recorder;
        this.probeRegistry = probeRegistry;
        this.merger = merger;
    }

    @Override
    public InterpreterResumptionPolicy handleIssues(InterpreterResult result) {

        result = this.handleResponseTimeProxy(result);
        result = this.handleCrashProxy(result);
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

    public InterpreterResult handleCrashProxy(InterpreterResult resultPrevious) {

        InterpreterResult resultNew = resultPrevious;

        if (Streams.stream(resultPrevious.getIssues())
            .anyMatch(FailureOccurredIssue.class::isInstance)) {

            // Crash occurred
            for (InterpretationIssue issue : resultPrevious.getIssues()) {

                if (issue instanceof CrashProxyIssue) {

                    probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(
                            ((CrashProxyIssue) issue).getModelElement(), ((CrashProxyIssue) issue).getContext(),
                            ((CrashProxyIssue) issue).isSuccess(), ((CrashProxyIssue) issue).getSeverity(),
                            ((CrashProxyIssue) issue).getStereotypedObject(), true));
                    probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(
                            ((CrashProxyIssue) issue).getModelElement(), ((CrashProxyIssue) issue).getContext(),
                            ((CrashProxyIssue) issue).isSuccess(), ((CrashProxyIssue) issue).getSeverity(),
                            ((CrashProxyIssue) issue).getStereotypedObject(), true));

                    ParameterIssue issueNew = new ParameterIssue(
                            (Entity) ((CrashProxyIssue) issue).getStereotypedObject(),
                            ((CrashProxyIssue) issue).getModelElement(), ((CrashProxyIssue) issue).getStackContent(),
                            false);

                    resultNew = merger.merge(resultNew, BasicInterpreterResult.of(issueNew));
                    
                    if(((CrashProxyIssue) issue).getModelElement().getImpact() != null) {
                        resultNew = merger.merge(resultNew, this.handleImpact(((CrashProxyIssue) issue).getModelElement().getImpact(), ((CrashProxyIssue) issue).getContext()));
                    }
                    
                    resultNew.removeIssue(issue); 
                }
                
            }
            
            

        } else {
            // no Crash occurred
            for (InterpretationIssue issue : resultPrevious.getIssues()) {

                if (issue instanceof CrashProxyIssue) {
                    
                    if (!((CrashProxyIssue) issue).isSuccess()) {
                        
                        probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(
                                ((CrashProxyIssue) issue).getModelElement(), ((CrashProxyIssue) issue).getContext(),
                                ((CrashProxyIssue) issue).isSuccess(), ((CrashProxyIssue) issue).getSeverity(),
                                ((CrashProxyIssue) issue).getStereotypedObject(), false));
                        probeRegistry.triggerSeverityProbe(new QualitygatePassedEvent(
                                ((CrashProxyIssue) issue).getModelElement(), ((CrashProxyIssue) issue).getContext(),
                                ((CrashProxyIssue) issue).isSuccess(), ((CrashProxyIssue) issue).getSeverity(),
                                ((CrashProxyIssue) issue).getStereotypedObject(), false));
                        
                        if(((CrashProxyIssue) issue).getModelElement().getScope() instanceof RequestParameterScope) {
                            
                            ParameterIssue issueNew = new ParameterIssue(
                                (Entity) ((CrashProxyIssue) issue).getStereotypedObject(),
                                ((CrashProxyIssue) issue).getModelElement(), ((CrashProxyIssue) issue).getStackContent(),
                                false);

                            resultNew = merger.merge(resultNew, BasicInterpreterResult.of(issueNew));
                            
                        } else {
                            
                            ResponseTimeIssue issueNew = new ResponseTimeIssue((Entity) ((CrashProxyIssue) issue).getStereotypedObject(),
                                ((CrashProxyIssue) issue).getModelElement(), false);
                            resultNew = merger.merge(resultNew, BasicInterpreterResult.of(issueNew));
                        }
                        
                        
                        
                    } else {
                        
                        probeRegistry.triggerViolationProbe(new QualitygatePassedEvent(
                                ((CrashProxyIssue) issue).getModelElement(), ((CrashProxyIssue) issue).getContext(),
                                ((CrashProxyIssue) issue).isSuccess(), ((CrashProxyIssue) issue).getSeverity(),
                                ((CrashProxyIssue) issue).getStereotypedObject(), false));
                        
                    }
                    
                    
                   resultNew.removeIssue(issue); 

                }
                
            }
        }
        
        
        
        return resultNew;
    }

    public InterpreterResult handleResponseTimeProxy(InterpreterResult result) {

        /*
         * Processing the Proxies in Issues
         */
        Iterator<InterpretationIssue> iter = result.getIssues()
            .iterator();

        List<Failure> failureImpactList = new ArrayList<Failure>();

        while (iter.hasNext()) {

            var issue = iter.next();

            if (issue instanceof ResponseTimeProxyIssue) {

                // one time passing the handler, then response time is available
                if (((ResponseTimeProxyIssue) issue).isHandledOnce()) {
                    // Checking the Qualitygate-Premise
                    try {

                        // set temporary stack for evaluation
                        final SimulatedStackframe<Object> frame = ((ResponseTimeProxyIssue) issue).getContext()
                            .getStack()
                            .createAndPushNewStackFrame();

                        Measure<Object, Quantity> measuringValue = ((ResponseTimeProxyIssue) issue)
                            .getResponseTimeQualitygateSwitch()
                            .getLastResponseTimeMeasure()
                            .getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC);

                        String metricName = ((RequestMetricScope) ((ResponseTimeProxyIssue) issue).getQualitygate()
                            .getScope()).getMetric()
                                .getName()
                                .replace(" ", "")
                                .concat(".VALUE");

                        frame.addValue(metricName, (Double) measuringValue.getValue());

                        PCMRandomVariable premise = ((ResponseTimeProxyIssue) issue).getPremise();

                        InterpreterDefaultContext interpreterDefaultContext = ((ResponseTimeProxyIssue) issue)
                            .getContext();

                        if (!((boolean) interpreterDefaultContext.evaluate(premise.getSpecification(),
                                interpreterDefaultContext.getStack()
                                    .currentStackFrame()))) {


                            // triggering probe to measure Success-To-Failure-Rate case
                            // violation
                            
                            result = merger.merge(result, BasicInterpreterResult.of(new CrashProxyIssue(((ResponseTimeProxyIssue) issue).getQualitygate(), interpreterDefaultContext, false, ((ResponseTimeProxyIssue) issue).getQualitygate()
                                    .getSeverity(), ((ResponseTimeProxyIssue) issue).getStereotypedObject(), null)));

                            

                            if (((ResponseTimeProxyIssue) issue).getQualitygate()
                                .getImpact() != null) {

                                failureImpactList.addAll(((ResponseTimeProxyIssue) issue).getQualitygate()
                                    .getImpact());

                            }

                        } else {
                            result = merger.merge(result, BasicInterpreterResult.of(new CrashProxyIssue(((ResponseTimeProxyIssue) issue).getQualitygate(), interpreterDefaultContext, true, ((ResponseTimeProxyIssue) issue).getQualitygate()
                                    .getSeverity(), ((ResponseTimeProxyIssue) issue).getStereotypedObject(), null)));
                        }

                        interpreterDefaultContext.getStack()
                            .removeStackFrame();

                        result = merger.merge(result, this.handleImpact(failureImpactList, interpreterDefaultContext));

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

                    // Skipping one handler in StereotypeDispatch
                    ((ResponseTimeProxyIssue) issue).setHandledOnce(true);

                }

            }

        }

        return result;

    }

    private InterpreterResult handleImpact(List<Failure> failureList,
            InterpreterDefaultContext interpreterDefaultContext) {

        InterpreterResult result = InterpreterResult.OK;

        for (Failure failure : failureList) {

            if (failure instanceof SWTimingFailure) {

                PreInterpretationBehavior behavior = new DelayBehavior(((SWTimingFailure) failure).getDelay()
                    .getSpecification());

                result = merger.merge(result, behavior.execute(interpreterDefaultContext));

            } else if (failure instanceof SWContentFailure) {

                PreInterpretationBehavior behavior = new CorruptContentBehavior(
                        ((SWContentFailure) failure).getDegreeOfCorruption()
                            .getSpecification());

                result = merger.merge(result, behavior.execute(interpreterDefaultContext));

            } else if (failure instanceof SWCrashFailure) {

                PreInterpretationBehavior behavior = new CrashBehavior(failure);
                result = merger.merge(result, behavior.execute(interpreterDefaultContext));

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

                probeRegistry.triggerInvolvedIssuesProbe(issuesWhenBroken,
                        ((QualitygateIssue) issue).getQualitygateRef());

                if (issue instanceof QualitygateIssue) {
                    ((QualitygateIssue) issue).setHandled(true);
                    LOGGER.debug(((QualitygateIssue) issue).getQualitygateId());
                }
            }
        }

        return result;

    }

}
