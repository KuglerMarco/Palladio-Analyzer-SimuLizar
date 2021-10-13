package org.palladiosimulator.simulizar.monitorrepository.feedthrough;

import java.util.Objects;

import javax.measure.quantity.Quantity;
import javax.measure.unit.Unit;

import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.measurementframework.MeasuringValue;
import org.palladiosimulator.measurementframework.listener.IMeasurementSourceListener;
import org.palladiosimulator.metricspec.BaseMetricDescription;
import org.palladiosimulator.metricspec.NumericalBaseMetricDescription;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.runtimemeasurement.RuntimeMeasurementModel;
import org.palladiosimulator.simulizar.metrics.PRMRecorder;

/**
 * A recorder that directly writes through the measurements from the observed calculator, or
 * other measurement source.
 * @author stier
 *
 */
public class FeedThroughRecorder extends PRMRecorder implements IMeasurementSourceListener {
    private BaseMetricDescription expectedMetric;

	public FeedThroughRecorder(final BaseMetricDescription expectedMetric,
    		final RuntimeMeasurementModel rmModel, final MeasurementSpecification measurementSpecification, 
    		final MeasuringPoint measuringPoint) {
        super(Objects.requireNonNull(rmModel), Objects.requireNonNull(measurementSpecification),
                Objects.requireNonNull(measuringPoint));
        this.expectedMetric = expectedMetric;
    }

    @SuppressWarnings("unchecked")
    @Override
	public void newMeasurementAvailable(MeasuringValue newMeasurement) {
        var expectedUnit = expectedMetric instanceof NumericalBaseMetricDescription ? ((NumericalBaseMetricDescription)expectedMetric).getDefaultUnit() : Unit.ONE;
        super.updateMeasurementValue(newMeasurement.getMeasureForMetric(this.expectedMetric)
                .doubleValue((Unit<Quantity>) expectedUnit));
	}

	@Override
	public void preUnregister() {
		// TODO Auto-generated method stub
		
	}
}
