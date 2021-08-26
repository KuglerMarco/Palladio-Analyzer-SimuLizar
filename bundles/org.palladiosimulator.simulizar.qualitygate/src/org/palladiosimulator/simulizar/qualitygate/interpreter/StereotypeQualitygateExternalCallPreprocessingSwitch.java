package org.palladiosimulator.simulizar.qualitygate.interpreter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.RequestMetricScope;
import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.metricspec.MetricDescription;
import org.palladiosimulator.metricspec.MetricDescriptionRepository;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;
import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepositoryFactory;
import org.palladiosimulator.monitorrepository.ProcessingType;
import org.palladiosimulator.pcmmeasuringpoint.ExternalCallActionMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointFactory;
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.seff.ExternalCallAction;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

/**
 * Switch to create the necessary Monitors for the Qualitygate-Elements in order to use the
 * calculators within the simulation.
 * 
 * @author Marco Kugler
 *
 */
public class StereotypeQualitygateExternalCallPreprocessingSwitch extends QualitygateSwitch<Monitor> {

    @AssistedFactory
    public static interface Factory {
        StereotypeQualitygateExternalCallPreprocessingSwitch create(MetricDescriptionRepository metricRepo, AssemblyContext assembly);
    }

    Logger LOGGER = Logger.getLogger(StereotypeQualitygateExternalCallPreprocessingSwitch.class);
    private EObject stereotypedObject;
    private final MetricDescriptionRepository metricRepo;
    private final AssemblyContext assembly;

    @AssistedInject
    public StereotypeQualitygateExternalCallPreprocessingSwitch(@Assisted MetricDescriptionRepository metricRepo, @Assisted AssemblyContext assembly) {
        LOGGER.setLevel(Level.DEBUG);
        this.metricRepo = metricRepo;
        this.assembly = assembly;
    }

    /**
     * Creates the Monitor to observe the Response-Time at the stereotyped element.
     */
    @Override
    public Monitor caseRequestMetricScope(RequestMetricScope object) {

        Monitor monitor = MonitorRepositoryFactory.eINSTANCE.createMonitor();

        // Activated
        monitor.setActivated(true);

        if (stereotypedObject instanceof ExternalCallAction) {

            monitor.setEntityName("QualitygateMonitor at ExternalCallAction "
                    + ((ExternalCallAction) stereotypedObject).getEntityName());

            ExternalCallActionMeasuringPoint measuringPoint = PcmmeasuringpointFactory.eINSTANCE
                .createExternalCallActionMeasuringPoint();

            measuringPoint.setExternalCall((ExternalCallAction) stereotypedObject);

            monitor.setMeasuringPoint(measuringPoint);

        }

        // Measurement-Specification
        MeasurementSpecification measurementSpec = MonitorRepositoryFactory.eINSTANCE.createMeasurementSpecification();

        MetricDescription metricDesc = metricRepo.getMetricDescriptions()
            .stream()
            .filter(e -> e.getName()
                .equals("Response Time"))
            .findFirst()
            .orElse(null);

        // Metric-Description
        measurementSpec.setMetricDescription(metricDesc);

        // triggers self adaption
        measurementSpec.setTriggersSelfAdaptations(false);

        // Processing Type
        ProcessingType procType = MonitorRepositoryFactory.eINSTANCE.createFeedThrough();

        measurementSpec.setProcessingType(procType);
        procType.setMeasurementSpecification(measurementSpec);

        monitor.getMeasurementSpecifications()
            .add(measurementSpec);

        measurementSpec.setMonitor(monitor);

        LOGGER.debug("A monitor was created for: " + monitor.getMeasurementSpecifications()
            .get(0)
            .getMetricDescription()
            .getTextualDescription());

        return monitor;

    }

    @Override
    public Monitor caseQualityGate(QualityGate object) {

        return doSwitch(object.getScope());

    }

    public List<Monitor> handleQualitygate(EObject object) {

        EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(object, "qualitygate", "QualitygateElement");

        stereotypedObject = object;

        List<Monitor> monitor = new ArrayList<Monitor>();

        for (QualityGate e : taggedValues) {
            
            // Only monitors added to the assembly, which is optionally reffered to
            if (e.getAssemblyContext() == null || e.getAssemblyContext()
                .equals(this.assembly)) {
                monitor.add(this.doSwitch(e));
            }

        }
        monitor.removeAll(Collections.singleton(null));

        return monitor;

    }

}