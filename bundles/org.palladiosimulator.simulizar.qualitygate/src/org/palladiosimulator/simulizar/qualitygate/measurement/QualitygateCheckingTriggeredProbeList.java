package org.palladiosimulator.simulizar.qualitygate.measurement;

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

public class QualitygateCheckingTriggeredProbeList extends TriggeredProbe {

    
    /** List of subsumed probes. */
    private final List<TriggeredProbe> subsumedProbes;
    
    // Whether evaluation was success
    private Identifier identifier;
    
    
    protected QualitygateCheckingTriggeredProbeList(MetricDescription metricDescription, final List<TriggeredProbe> subsumedProbes) {
        super(metricDescription);
        this.subsumedProbes = subsumedProbes;
        // TODO Auto-generated constructor stub
    }
    
    public void setIdentifier(Identifier identifier) {
        this.identifier = identifier;
    }
    

    @Override
    protected ProbeMeasurement doMeasure(RequestContext measurementContext) {
        
        final List<MeasuringValue> childMeasurements = new LinkedList<MeasuringValue>();
        IMeasureProvider subsumedMeasureProvider = null;
        
        for (final Probe childProbe : subsumedProbes) {
            
            if(childProbe instanceof QualitygateCheckingProbe) {
                ((QualitygateCheckingProbe) childProbe).setIdentifier(identifier);
                subsumedMeasureProvider = ((QualitygateCheckingProbe) childProbe).doMeasure(identifier, measurementContext)
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
