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

public class SeverityProbe extends TriggeredProbe {

    private Identifier identifier;
    
    protected SeverityProbe(MetricDescription metricDescription) {
        super(metricDescription);
    }
    

    
    @Override
    protected ProbeMeasurement doMeasure(RequestContext measurementContext) {
        final BasicMeasurement<Identifier, Dimensionless> resultMeasurement = new BasicMeasurement<Identifier, Dimensionless>(
                IdentifierMeasure.valueOf(identifier, Unit.ONE), (BaseMetricDescription) this.getMetricDesciption());
        
        return new ProbeMeasurement(resultMeasurement, this, measurementContext);
    }
    
    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }
    
    ProbeMeasurement doMeasure(Identifier qualitygateResult, RequestContext measurementContext) {
        
        
        final BasicMeasurement<Identifier, Dimensionless> resultMeasurement = new BasicMeasurement<Identifier, Dimensionless>(
                IdentifierMeasure.valueOf(qualitygateResult, Unit.ONE), (BaseMetricDescription) this.getMetricDesciption());
        
        return new ProbeMeasurement(resultMeasurement, this, measurementContext);
        
        
    }

}
