package org.palladiosimulator.simulizar.qualitygate.measurement;

import javax.measure.quantity.Dimensionless;
import javax.measure.unit.Unit;

import org.palladiosimulator.measurementframework.BasicMeasurement;
import org.palladiosimulator.measurementframework.measure.IdentifierMeasure;
import org.palladiosimulator.metricspec.BaseMetricDescription;
import org.palladiosimulator.metricspec.Identifier;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.probeframework.measurement.ProbeMeasurement;
import org.palladiosimulator.probeframework.measurement.RequestContext;
import org.palladiosimulator.probeframework.probes.TriggeredProbe;

/**
 * This Probe allows to take measurements of the Qualitygate evaluation using a
 * TextualMetricDescription.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateCheckingProbe extends TriggeredProbe {

    private Identifier identifier;

    protected QualitygateCheckingProbe(MetricDescription metricDescription) {
        super(metricDescription);
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
        final BasicMeasurement<Identifier, Dimensionless> resultMeasurement = new BasicMeasurement<Identifier, Dimensionless>(
                IdentifierMeasure.valueOf(identifier, Unit.ONE), (BaseMetricDescription) this.getMetricDesciption());

        return new ProbeMeasurement(resultMeasurement, this, measurementContext);
    }

    private void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }


}
