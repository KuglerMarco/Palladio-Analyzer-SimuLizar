package org.palladiosimulator.simulizar.qualitygate.interpreter.preprocessing;

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
 * Switch to create the necessary Monitors for the Qualitygate-Elements at ExternalCalls in order to
 * use the calculators within the simulation.
 * 
 * @author Marco Kugler
 *
 */
public class StereotypeQualitygateExternalCallPreprocessingSwitch extends QualitygateSwitch<Monitor> {

    @AssistedFactory
    public static interface Factory {
        StereotypeQualitygateExternalCallPreprocessingSwitch create(MetricDescriptionRepository metricRepo,
                AssemblyContext assembly);
    }

    Logger LOGGER = Logger.getLogger(StereotypeQualitygateExternalCallPreprocessingSwitch.class);
    private EObject stereotypedObject;
    private final MetricDescriptionRepository metricRepo;
    private final AssemblyContext assembly;

    @AssistedInject
    public StereotypeQualitygateExternalCallPreprocessingSwitch(@Assisted MetricDescriptionRepository metricRepo,
            @Assisted AssemblyContext assembly) {
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

        monitor.setEntityName(
                "QualitygateMonitor at ExternalCallAction " + ((ExternalCallAction) stereotypedObject).getEntityName());

        ExternalCallActionMeasuringPoint measuringPoint = PcmmeasuringpointFactory.eINSTANCE
            .createExternalCallActionMeasuringPoint();

        measuringPoint.setExternalCall((ExternalCallAction) stereotypedObject);

        monitor.setMeasuringPoint(measuringPoint);

        // Measurement-Specification
        MeasurementSpecification measurementSpec = MonitorRepositoryFactory.eINSTANCE.createMeasurementSpecification();

        String metricName = object.getMetric()
            .getName()
            .replace(" Tuple", "");

        MetricDescription metricDesc = metricRepo.getMetricDescriptions()
            .stream()
            .filter(e -> e.getName()
                .equals(metricName))
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("No Metric Description found."));

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

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("A monitor was created for: " + monitor.getMeasurementSpecifications()
                .get(0)
                .getMetricDescription()
                .getTextualDescription());
        }

        return monitor;

    }

    @Override
    public Monitor caseQualityGate(QualityGate object) {

        return doSwitch(object.getScope());

    }

    public List<Monitor> createMonitors(EObject object) {

        EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(object, "qualitygate", "QualitygateElement");

        if (taggedValues.isEmpty()) {

            /*
             * The containment reference on the Qualitygate cannot be transferred to the
             * ExternalCall during Preprocessing; thus, if ExternalCall has no Qualitygate, the
             * RequiredRole needs to be checked
             */
            taggedValues = StereotypeAPI.getTaggedValue(((ExternalCallAction) object).getRole_ExternalService(),
                    "qualitygate", "QualitygateElement");

            // if still empty -> invalid model
            if (taggedValues.isEmpty()) {
                throw new IllegalArgumentException(
                        "Qualitygate-Model not valid: Qualitygate-Stereotype needs to have at least one Qualitygate element.");
            }

        }

        stereotypedObject = object;

        List<Monitor> monitor = new ArrayList<Monitor>();

        for (QualityGate qualitygate : taggedValues) {

            // Only monitors for the assembly, which is optionally referred to
            if (qualitygate.getAssemblyContext() == null || qualitygate.getAssemblyContext()
                .equals(this.assembly)) {

                monitor.add(this.doSwitch(qualitygate));

            }

        }
        // remove null elements
        monitor.removeAll(Collections.singleton(null));

        return monitor;

    }

}