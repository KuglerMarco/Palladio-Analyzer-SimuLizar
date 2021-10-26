package org.palladiosimulator.simulizar.qualitygate.visualization.piechart;

import java.util.HashMap;
import java.util.Map;

import org.jfree.data.general.AbstractDataset;
import org.jfree.data.general.DefaultPieDataset;
import org.palladiosimulator.edp2.datastream.IDataSource;
import org.palladiosimulator.edp2.datastream.IDataStream;
import org.palladiosimulator.edp2.util.MetricDescriptionUtility;
import org.palladiosimulator.edp2.visualization.jfreechart.input.pie.PieChartVisualizationInput;
import org.palladiosimulator.measurementframework.TupleMeasurement;
import org.palladiosimulator.metricspec.BaseMetricDescription;
import org.palladiosimulator.metricspec.Identifier;
import org.palladiosimulator.metricspec.Scale;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;

/**
 * Visualization of the Violation to Success rate and the Severity
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateViolationVisualizationInput extends PieChartVisualizationInput {

    @Override
    protected AbstractDataset generateDataset() {

        final DefaultPieDataset dataset = new DefaultPieDataset();
        final Map<Comparable<?>, Integer> bins = new HashMap<Comparable<?>, Integer>();
        final IDataSource datasource = getInputs().get(0)
            .getDataSource();
        final IDataStream<TupleMeasurement> datastream = datasource.getDataStream();

        for (final TupleMeasurement tuple : datastream) {

            final Comparable<?> state = (Comparable<?>) ((Identifier) tuple.asArray()[1].getValue()).getLiteral();
            if (!bins.containsKey(state)) {
                bins.put(state, 0);
            }
            int current = bins.get(state);
            bins.put(state, current + 1);

        }

        for (final Comparable<?> o : bins.keySet()) {
            dataset.setValue(o, bins.get(o)
                .doubleValue());
        }

        return dataset;

    }

    @Override
    public boolean canAccept(final IDataSource dataSource) {

        final BaseMetricDescription[] subMetricDescriptions = MetricDescriptionUtility
            .toBaseMetricDescriptions(dataSource.getMetricDesciption());
        if (subMetricDescriptions.length != 2) {
            return false; // two-dimensional data needed
        }

        if (!subMetricDescriptions[0].getId()
            .equals(MetricDescriptionConstants.POINT_IN_TIME_METRIC.getId())) {
            return false;
        }
        if (!subMetricDescriptions[1].getName()
            .equals("QualitygateViolation")) {
            return false;
        }

        return subMetricDescriptions[1].getScale()
            .compareTo(Scale.ORDINAL) <= 0;

    }

    /*
     * (non-Javadoc)
     * 
     * @see org.palladiosimulator.edp2.visualization.jfreechart.input.JFreeChartVisualizationInput#
     * getName()
     */
    @Override
    public String getName() {
        return "Qualitygate Results";
    }

}
