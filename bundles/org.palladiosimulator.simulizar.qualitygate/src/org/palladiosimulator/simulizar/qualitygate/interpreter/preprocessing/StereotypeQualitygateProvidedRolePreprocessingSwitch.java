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
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcmmeasuringpoint.AssemblyOperationMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointFactory;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

/**
 * Switch to create the necessary Monitors for the Qualitygate-Elements at ProvidedRoles in order to
 * use the calculators within the simulation.
 * 
 * @author Marco Kugler
 *
 */
public class StereotypeQualitygateProvidedRolePreprocessingSwitch extends QualitygateSwitch<Monitor> {

    @AssistedFactory
    public static interface Factory {
        StereotypeQualitygateProvidedRolePreprocessingSwitch create(MetricDescriptionRepository metricRepo,
                AssemblyContext assembly);
    }

    Logger LOGGER = Logger.getLogger(StereotypeQualitygateProvidedRolePreprocessingSwitch.class);
    private ProvidedRole stereotypedRole;
    private final MetricDescriptionRepository metricRepo;
    private AssemblyContext assembly;

    @AssistedInject
    public StereotypeQualitygateProvidedRolePreprocessingSwitch(@Assisted MetricDescriptionRepository metricRepo,
            @Assisted AssemblyContext assembly) {
        LOGGER.setLevel(Level.DEBUG);
        this.metricRepo = metricRepo;
        this.assembly = assembly;
    }

    /**
     * Creates the Monitor at AssemblyOperationMeasuringPoint
     */
    @Override
    public Monitor caseRequestMetricScope(RequestMetricScope object) {

        Monitor monitor = MonitorRepositoryFactory.eINSTANCE.createMonitor();

        // Activated
        monitor.setActivated(true);

        // Entity-Name
        monitor.setEntityName("QualitygateMonitor at ProvidedRole " + ((ProvidedRole) stereotypedRole).getEntityName());

        AssemblyOperationMeasuringPoint measuringPoint = PcmmeasuringpointFactory.eINSTANCE
            .createAssemblyOperationMeasuringPoint();

        // Operation-Signature
        measuringPoint.setOperationSignature((OperationSignature) object.getSignature());

        measuringPoint.setRole((ProvidedRole) stereotypedRole);

        measuringPoint.setAssembly(assembly);

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
            .orElseThrow(() -> new IllegalStateException("No Metric found."));

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

        stereotypedRole = (ProvidedRole) object;

        List<Monitor> monitor = new ArrayList<Monitor>();

        for (QualityGate e : taggedValues) {

            if (e.getAssemblyContext() == null || e.getAssemblyContext()
                .equals(this.assembly)) {
                monitor.add(this.doSwitch(e));
            }
        }

        // Remove null-elements
        monitor.removeAll(Collections.singleton(null));

        return monitor;

    }

}
