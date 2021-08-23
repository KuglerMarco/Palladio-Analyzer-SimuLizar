//package org.palladiosimulator.simulizar.qualitygate.measurement;
//
//
//import javax.measure.Measure;
//import javax.measure.quantity.Dimensionless;
//import javax.measure.unit.Unit;
//
//import org.palladiosimulator.measurementframework.measure.IdentifierMeasure;
//import org.palladiosimulator.probeframework.measurement.ProbeMeasurement;
//import org.palladiosimulator.probeframework.measurement.RequestContext;
//import org.palladiosimulator.probeframework.probes.Probe;
//import org.palladiosimulator.simulizar.qualitygate.metric.QualitygateMetricDescriptionConstants;
//import org.palladiosimulator.metricspec.Identifier;
//import org.palladiosimulator.metricspec.MetricDescription;
//
//public abstract class QualitygateEvaluationProbe extends Probe {
//
//    protected QualitygateEvaluationProbe(final MetricDescription metricDescription) {
//        super(metricDescription);
//    }
//    
//    public ProbeMeasurement takeSuccessfulMeasurement(final RequestContext measurementContext) {
//        final ProbeMeasurement newMeasurement = doMeasure(QualitygateMetricDescriptionConstants.SUCCESS, measurementContext);
//        notifyMeasurementSourceListener(newMeasurement);
//        return newMeasurement;
//        
//    }
//    
//    public ProbeMeasurement takeViolatedMeasurement(final RequestContext measurementContext) {
//        final ProbeMeasurement newMeasurement = doMeasure(QualitygateMetricDescriptionConstants.VIOLATION, measurementContext);
//        notifyMeasurementSourceListener(newMeasurement);
//        return newMeasurement;
//        
//    }
//    
//    
//    abstract ProbeMeasurement doMeasure(Identifier qualitygateResult, RequestContext measurementContext);
//    
//    
//    Measure<Identifier, Dimensionless> getBasicMeasure(Identifier qualitygateResult) {
//        final Measure<Identifier, Dimensionless> result = IdentifierMeasure.valueOf(qualitygateResult, Unit.ONE);
//        return result;
//    }
//
//    
//    
//    
//    
//    
//}
