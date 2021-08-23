//package org.palladiosimulator.simulizar.qualitygate.measurement;
//
//import javax.measure.quantity.Dimensionless;
//
//import org.palladiosimulator.measurementframework.BasicMeasurement;
//import org.palladiosimulator.metricspec.BaseMetricDescription;
//import org.palladiosimulator.metricspec.Identifier;
//import org.palladiosimulator.metricspec.MetricDescription;
//import org.palladiosimulator.probeframework.measurement.ProbeMeasurement;
//import org.palladiosimulator.probeframework.measurement.RequestContext;
//
//public class ConcreteQualitygateEvaluationProbe extends QualitygateEvaluationProbe {
//    
//    protected ConcreteQualitygateEvaluationProbe(MetricDescription metricDescription) {
//        super(metricDescription);
//        
//    }
//
//    @Override
//    ProbeMeasurement doMeasure(Identifier qualitygateResult, RequestContext measurementContext) {
//        
//        
//        final BasicMeasurement<Identifier, Dimensionless> resultMeasurement = new BasicMeasurement<Identifier, Dimensionless>(
//                getBasicMeasure(qualitygateResult), (BaseMetricDescription) this.getMetricDesciption());
//        
//        return new ProbeMeasurement(resultMeasurement, this, measurementContext);
//        
//        
//    }
//
//}
