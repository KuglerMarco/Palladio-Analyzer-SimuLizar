package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;

import java.util.Iterator;
import java.util.NoSuchElementException;

import javax.inject.Inject;
import javax.measure.Measurable;
import javax.measure.Measure;
import javax.measure.quantity.Duration;
import javax.measure.quantity.Quantity;
import javax.measure.unit.SI;
import javax.measure.unit.Unit;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.jscience.geography.coordinates.Time;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.entity.EntityReference;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;
import org.palladiosimulator.simulizar.qualitygate.interpreter.StereotypeQualitygateSwitch;

import com.google.common.collect.Streams;
import com.google.common.collect.Iterables;

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
     * To this time: Checks whether in Issue list is only consisting of QualitygateIssues, later:
     * impact of QualitygateIssues
     */
    @Override
    public InterpreterResumptionPolicy handleIssues(InterpreterResult result) {

        Iterator<InterpretationIssue> iter = result.getIssues()
            .iterator();

        while (iter.hasNext()) {

            var issue = iter.next();

            if (issue instanceof ResponseTimeProxyIssue) {

                // Checking the Qualitygate-Premise
                try {

                    Measure<Object, Quantity> measuringValue = ((ResponseTimeProxyIssue) issue).getSeffSwitch()
                        .getLastMeasure()
                        .getMeasureForMetric(MetricDescriptionConstants.RESPONSE_TIME_METRIC);

                    PCMRandomVariable premise = ((ResponseTimeProxyIssue) issue).getPremise();

                    LOGGER.debug("!!!" + measuringValue.getValue());

                    LOGGER.debug("!!!" + measuringValue.getUnit());

//                    Measure<Integer,Duration> measuringValueCon = Measure.valueOf(Integer.parseInt(premise.getSpecification()), Quantity);

                    Double responseTime = (Double) measuringValue.getValue();

                    // TODO nicht optimal: mit JScience und Parser arbeiten
                    if (responseTime > Double.parseDouble(premise.getSpecification())) {

                        result.addIssue(new ResponseTimeIssue(((ResponseTimeProxyIssue) issue).getStereotypedObject(),
                                ((ResponseTimeProxyIssue) issue).getQualitygate(), responseTime));

                        LOGGER.debug(responseTime);

                    }

                } catch (NoSuchElementException e) {
                    // SimuLizar-Bug: No Measurements after simulation had stopped but still in
                    // control flow
                }

                // Removing the Proxy
                result.removeIssue(issue);

                LOGGER.debug(result.getIssues());

                iter = result.getIssues()
                    .iterator();

            }

        }

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

}
