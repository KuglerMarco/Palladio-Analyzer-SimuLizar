package org.palladiosimulator.simulizar.qualitygate.visualization.barchart.severity;

import org.eclipse.ui.IMemento;
import org.palladiosimulator.edp2.datastream.configurable.IPropertyConfigurable;
import org.palladiosimulator.edp2.visualization.jfreechart.input.JFreeChartVisualizationInputFactory;

public class QualitygateSeverityVisualizationInputFactory extends JFreeChartVisualizationInputFactory {

    public static final String FACTORY_ID = QualitygateSeverityVisualizationInputFactory.class.getCanonicalName();

    @Override
    protected IPropertyConfigurable createElementInternal(IMemento memento) {
        return new QualitygateSeverityVisualizationInput();
    }

}
