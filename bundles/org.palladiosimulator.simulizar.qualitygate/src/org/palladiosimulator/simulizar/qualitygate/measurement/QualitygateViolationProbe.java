package org.palladiosimulator.simulizar.qualitygate.measurement;


import javax.measure.Measure;
import javax.measure.quantity.Dimensionless;
import javax.measure.unit.Unit;

import org.palladiosimulator.measurementframework.BasicMeasurement;
import org.palladiosimulator.measurementframework.measure.IdentifierMeasure;
import org.palladiosimulator.probeframework.measurement.ProbeMeasurement;
import org.palladiosimulator.probeframework.measurement.RequestContext;
import org.palladiosimulator.probeframework.probes.Probe;
import org.palladiosimulator.simulizar.qualitygate.metric.QualitygateMetricDescriptionConstants;
import org.palladiosimulator.metricspec.BaseMetricDescription;
import org.palladiosimulator.metricspec.Identifier;

public class QualitygateViolationProbe extends Probe {

    protected QualitygateViolationProbe() {
        super(QualitygateMetricDescriptionConstants.QUALITYGATE_VIOLATION_METRIC);
    }
    
    public ProbeMeasurement takeMeasurement(final Identifier qualitygateResult, final RequestContext measurementContext) {
        
        final ProbeMeasurement newMeasurement = doMeasure(qualitygateResult, measurementContext);
        notifyMeasurementSourceListener(newMeasurement);
        return newMeasurement;
        
    }

    private ProbeMeasurement doMeasure(Identifier qualitygateResult, RequestContext measurementContext) {
        
        final BasicMeasurement<Identifier, Dimensionless> resultMeasurement = new BasicMeasurement<Identifier, Dimensionless>(
                getBasicMeasure(qualitygateResult), (BaseMetricDescription) this.getMetricDesciption());
        
        return new ProbeMeasurement(resultMeasurement, this, measurementContext);
        
        
    }
    
    private Measure<Identifier, Dimensionless> getBasicMeasure(Identifier qualitygateResult) {
        final Measure<Identifier, Dimensionless> result = IdentifierMeasure.valueOf(qualitygateResult, Unit.ONE);
        return result;
    }
    
    
    
    
    
}
