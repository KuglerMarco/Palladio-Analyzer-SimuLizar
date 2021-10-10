package org.palladiosimulator.simulizar.qualitygate.visualization.piechart;

import org.eclipse.ui.IMemento;
import org.palladiosimulator.edp2.datastream.configurable.IPropertyConfigurable;
import org.palladiosimulator.edp2.visualization.jfreechart.input.JFreeChartVisualizationInputFactory;

public class QualitygateViolationVisualizationInputFactory extends JFreeChartVisualizationInputFactory {

    public static final String FACTORY_ID = QualitygateViolationVisualizationInputFactory.class.getCanonicalName();

    @Override
    protected IPropertyConfigurable createElementInternal(IMemento memento) {
        return new QualitygateViolationVisualizationInput();
    }

}
