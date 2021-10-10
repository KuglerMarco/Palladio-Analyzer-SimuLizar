package org.palladiosimulator.simulizar.qualitygate.measurement.probe;

import java.util.LinkedList;
import java.util.List;

import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.measurementframework.measureprovider.IMeasureProvider;
import org.palladiosimulator.measurementframework.measureprovider.MeasurementListMeasureProvider;
import org.palladiosimulator.metricspec.Identifier;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.probeframework.measurement.ProbeMeasurement;
import org.palladiosimulator.probeframework.measurement.RequestContext;
import org.palladiosimulator.probeframework.probes.Probe;
import org.palladiosimulator.probeframework.probes.TriggeredProbe;

/**
 * This Probe allows to take measurements of the Qualitygate evaluation using a
 * TextualMetricDescription.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateCheckingTriggeredProbeList extends TriggeredProbe {

    /** List of subsumed probes. */
    private final List<TriggeredProbe> subsumedProbes;

    // Result of the evaluation of the Qualitygate
    private Identifier identifier;

    public QualitygateCheckingTriggeredProbeList(MetricDescription metricDescription,
            final List<TriggeredProbe> subsumedProbes) {
        super(metricDescription);
        this.subsumedProbes = subsumedProbes;
    }

    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }

    public ProbeMeasurement takeMeasurement(RequestContext measurementContext, Identifier identifier) {
        this.setIdentifier(identifier);
        final ProbeMeasurement newMeasurement = doMeasure(measurementContext);
        notifyMeasurementSourceListener(newMeasurement);
        return newMeasurement;
    }

    public ProbeMeasurement takeMeasurement(Identifier identifier) {
        this.setIdentifier(identifier);
        final ProbeMeasurement newMeasurement = doMeasure(RequestContext.EMPTY_REQUEST_CONTEXT);
        notifyMeasurementSourceListener(newMeasurement);
        return newMeasurement;
    }

    @Override
    protected ProbeMeasurement doMeasure(RequestContext measurementContext) {

        final List<MeasuringValue> childMeasurements = new LinkedList<MeasuringValue>();
        IMeasureProvider subsumedMeasureProvider = null;

        for (final Probe childProbe : subsumedProbes) {

            if (childProbe instanceof QualitygateCheckingProbe) {
                subsumedMeasureProvider = ((QualitygateCheckingProbe) childProbe)
                    .takeMeasurement(measurementContext, identifier)
                    .getMeasureProvider();
            } else {
                subsumedMeasureProvider = ((TriggeredProbe) childProbe).takeMeasurement(measurementContext)
                    .getMeasureProvider();
            }

            if (!(subsumedMeasureProvider instanceof MeasuringValue)) {
                throw new IllegalArgumentException("Subsumed measure providers have to be measurements");
            }

            childMeasurements.add((MeasuringValue) subsumedMeasureProvider);
        }

        final IMeasureProvider measureProvider = new MeasurementListMeasureProvider(childMeasurements);
        return new ProbeMeasurement(measureProvider, this, measurementContext);
    }

}
