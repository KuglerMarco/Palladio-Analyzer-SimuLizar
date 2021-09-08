package org.palladiosimulator.simulizar.qualitygate.visualization.piechart;

import org.eclipse.ui.IMemento;
import org.palladiosimulator.edp2.datastream.configurable.IPropertyConfigurable;
import org.palladiosimulator.edp2.visualization.jfreechart.input.JFreeChartVisualizationInputFactory;

public class QualitygatePieChartVisualizationInputFactory extends JFreeChartVisualizationInputFactory {

    public static final String FACTORY_ID = QualitygatePieChartVisualizationInputFactory.class.getCanonicalName();
    
    @Override
    protected IPropertyConfigurable createElementInternal(IMemento memento) {
        return new QualitygatePieChartVisualizationInput();
    }

}
