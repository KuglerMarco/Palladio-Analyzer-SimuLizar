//package org.palladiosimulator.simulizar.qualitygate.measurement;
//
//import java.util.LinkedList;
//import java.util.List;
//
//import org.palladiosimulator.measurementframework.MeasuringValue;
//import org.palladiosimulator.measurementframework.measureprovider.IMeasureProvider;
//import org.palladiosimulator.measurementframework.measureprovider.MeasurementListMeasureProvider;
//import org.palladiosimulator.metricspec.Identifier;
//import org.palladiosimulator.metricspec.MetricDescription;
//import org.palladiosimulator.probeframework.measurement.ProbeMeasurement;
//import org.palladiosimulator.probeframework.measurement.RequestContext;
//import org.palladiosimulator.probeframework.probes.Probe;
//import org.palladiosimulator.probeframework.probes.TriggeredProbe;
//
//public class QualitygateTriggeredProbeList extends QualitygateEvaluationProbe {
//
//    
//
//    /** List of subsumed probes. */
//    private final List<Probe> subsumedProbes;
//    
//    
//    public QualitygateTriggeredProbeList(MetricDescription metricSetDescription, List<Probe> subsumedProbes) {
//        super(metricSetDescription);
//        this.subsumedProbes = subsumedProbes;
//    }
//    
//
//    @Override
//    ProbeMeasurement doMeasure(Identifier qualitygateResult, RequestContext measurementContext) {
//        final List<MeasuringValue> childMeasurements = new LinkedList<MeasuringValue>();
//        IMeasureProvider subsumedMeasureProvider = null;
//        
//        for (final Probe childProbe : subsumedProbes) {
//            
//            if(childProbe instanceof QualitygateEvaluationProbe) {
//                subsumedMeasureProvider = ((ConcreteQualitygateEvaluationProbe) childProbe).doMeasure(qualitygateResult, measurementContext)
//                        .getMeasureProvider();
//            } else {
//                subsumedMeasureProvider = ((TriggeredProbe) childProbe).takeMeasurement(measurementContext)
//                    .getMeasureProvider();
//            }
//            
//
//            
//            if (!(subsumedMeasureProvider instanceof MeasuringValue)) {
//                throw new IllegalArgumentException("Subsumed measure providers have to be measurements");
//            }
//
//            // TODO Actually, we should recursively resolve subsumed measurements here because the
//            // subsumed measurement could be a TupleMeasurement. [Lehrig]
//            childMeasurements.add((MeasuringValue) subsumedMeasureProvider);
//        }
//
//        final IMeasureProvider measureProvider = new MeasurementListMeasureProvider(childMeasurements);
//        return new ProbeMeasurement(measureProvider, this, measurementContext);
//    }
//    
//    
//
//}
