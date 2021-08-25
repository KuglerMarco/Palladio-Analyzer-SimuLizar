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
import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcmmeasuringpoint.AssemblyOperationMeasuringPoint;
import org.palladiosimulator.pcmmeasuringpoint.PcmmeasuringpointFactory;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedFactory;
import dagger.assisted.AssistedInject;

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
    private QualityGate qualitygate;

    @AssistedInject
    public StereotypeQualitygateProvidedRolePreprocessingSwitch(@Assisted MetricDescriptionRepository metricRepo,
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

        // Entity-Name
        monitor.setEntityName("QualitygateMonitor at ProvidedRole " + ((ProvidedRole) stereotypedRole).getEntityName());

        // Measuring-Point //TODO Optional Reference on Assembly, then
        // AssemblyOperationMeasuringPoint
        AssemblyOperationMeasuringPoint measuringPoint = PcmmeasuringpointFactory.eINSTANCE
            .createAssemblyOperationMeasuringPoint();

        // Operation-Signature
        measuringPoint.setOperationSignature((OperationSignature) object.getSignature());

        measuringPoint.setRole((ProvidedRole) stereotypedRole);

        measuringPoint.setAssembly(assembly);

        monitor.setMeasuringPoint(measuringPoint);

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

        this.qualitygate = object;
        if(object.getAssemblyContext() == null || this.assembly.equals(object.getAssemblyContext())) {
           return doSwitch(object.getScope()); 
        }
        return null;

    }

    public List<Monitor> handleQualitygate(EObject object) {

        EList<QualityGate> taggedValues = StereotypeAPI.getTaggedValue(object, "qualitygate", "QualitygateElement");

        stereotypedRole = (ProvidedRole) object;

        List<Monitor> monitor = new ArrayList<Monitor>();

        for (QualityGate e : taggedValues) {
            monitor.add(this.doSwitch(e));
        }
        monitor.removeAll(Collections.singleton(null));

        return monitor;

    }

}
