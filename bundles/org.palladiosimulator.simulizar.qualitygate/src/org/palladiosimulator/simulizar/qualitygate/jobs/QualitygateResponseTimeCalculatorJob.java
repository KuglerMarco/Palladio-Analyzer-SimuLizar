package org.palladiosimulator.simulizar.qualitygate.jobs;

import java.util.List;

import javax.inject.Inject;

import org.eclipse.emf.common.util.TreeIterator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.metricspec.MetricDescriptionRepository;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;

import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;

import org.palladiosimulator.simulizar.launcher.jobs.extensions.DefaultMeasuringPointRepositoryFactory;
import org.palladiosimulator.simulizar.launcher.jobs.extensions.DefaultMonitorRepositoryFactory;
import org.palladiosimulator.simulizar.qualitygate.interpreter.StereotypeQualitygatePreprocessingSwitch;

import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;
import org.palladiosimulator.pcm.core.composition.Connector;
import org.palladiosimulator.pcm.system.SystemPackage;
import org.palladiosimulator.pcm.repository.OperationSignature;
import org.palladiosimulator.pcm.repository.Role;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryComponent;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.pcmmeasuringpoint.OperationReference;
import org.palladiosimulator.pcmmeasuringpoint.SystemOperationMeasuringPoint;
import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPoint;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.eclipse.emf.common.util.URI;

/**
 * Creates the necessary Monitor-elements in the MonitorRepository, so that the according
 * Calculators are available within the simulation.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateResponseTimeCalculatorJob implements IBlackboardInteractingJob<MDSDBlackboard> {

    Logger LOGGER = Logger.getLogger(QualitygateResponseTimeCalculatorJob.class);

    private MDSDBlackboard blackboard;
    private final StereotypeQualitygatePreprocessingSwitch.Factory preprocessingSwitch;

    @Inject
    public QualitygateResponseTimeCalculatorJob(MDSDBlackboard blackboard,
            StereotypeQualitygatePreprocessingSwitch.Factory preprocessingSwitch) {
        this.blackboard = blackboard;
        this.preprocessingSwitch = preprocessingSwitch;

        LOGGER.setLevel(Level.DEBUG);

    }

    /**
     * Adds the required Monitors to the MonitorRepository.
     */
    @Override
    public void execute(IProgressMonitor monitor) throws JobFailedException, UserCanceledException {

        ResourceSet resourceSet = blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getResourceSet();
        
        for(Resource e : resourceSet.getResources()) {
            LOGGER.debug(e.toString());
        }

        // Loading CommonMetrics-model
        URI uri = URI.createURI("pathmap://METRIC_SPEC_MODELS/models/commonMetrics.metricspec");
        MetricDescriptionRepository res = (MetricDescriptionRepository) resourceSet.getResource(uri, false)
            .getContents()
            .get(0);
        
        org.palladiosimulator.pcm.system.System system = (org.palladiosimulator.pcm.system.System) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
                .getElement(SystemPackage.Literals.SYSTEM).get(0);

        /*
         * Merging the MeasurementPoint-Repositories TODO Rücksprache Sebastian
         */
        if (blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY)
            .isEmpty()) {
            throw new IllegalArgumentException("No MeasuringPointRepository found!");

        }

        // Current
        MeasuringPointRepository measuringPointRepo = (MeasuringPointRepository) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY)
            .get(0);
        // Default
        MeasuringPointRepository genMeasuringPointRepo = DefaultMeasuringPointRepositoryFactory
            .createDefaultRepository(resourceSet);
        // added to the current
        measuringPointRepo.getMeasuringPoints()
            .addAll(genMeasuringPointRepo.getMeasuringPoints());

        /*
         * Merging the MonitorRepositories TODO Rücksprache Sebastian TODO FIXME the problem of
         * duplicates in defaultRepository
         */

        if (blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY)
            .isEmpty()) {
            throw new IllegalArgumentException("No MonitorRepository found!");
        }
        // Current
        MonitorRepository monitorRepo = (MonitorRepository) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY)
            .get(0);
        // Default
        MonitorRepository genMonitorRepository = DefaultMonitorRepositoryFactory
            .createDefaultMonitorRepository(measuringPointRepo);

        // (for every Measuring Point one response time monitor will be added in defaultRepository)
        // TODO falls Monitor bereits spezifiziert, keinen mehr hinzufügen, ansonsten zwei
        // Calculators
        monitorRepo.getMonitors()
            .addAll(genMonitorRepository.getMonitors());

        /*
         * Traversing the System-model to find Qualitygate-Elements TODO Auf Repository-Ebene
         * ebenefalls machen
         */
        PCMResourceSetPartition resPartition = (PCMResourceSetPartition) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID);

        LOGGER.debug(blackboard
                .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(RepositoryPackage.Literals.REPOSITORY).size());
        
        Repository repo = (Repository) blackboard
                .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID).getElement(RepositoryPackage.Literals.REPOSITORY).get(1);
        
        LOGGER.debug(repo.getComponents__Repository().get(0));

        EObject object;

        for(RepositoryComponent e : repo.getComponents__Repository()) {
            for(ProvidedRole i : e.getProvidedRoles_InterfaceProvidingEntity()) {
                
                LOGGER.debug("Iterated over: " + i.getEntityName());
                object = i;
                if (!StereotypeAPI.getAppliedStereotypes(object)
                        .isEmpty() && StereotypeAPI.getAppliedStereotypes(object)
                            .get(0)
                            .getName()
                            .equals("QualitygateElement")) {

                        if (object instanceof ProvidedRole) {
                            LOGGER.debug(
                                    "The ProvidedRole " + ((ProvidedRole) object).getEntityName() + " has a qualitygate-application");

                            // List of generated Monitors for the attached Qualitygates (could be more than
                            // one)
                            List<Monitor> qualitygateMonitors = preprocessingSwitch.create(res, system)
                                .handleQualitygate(object);
                            // Removing the Null-elements, because not every Qualitygate needs a calculator
                            while (qualitygateMonitors.remove(null));

                            // Adding the generated Monitors to the repositories
                            for (Monitor j : qualitygateMonitors) {

                                if (!this.isMeasurementSpecificationPresent(j)) {
                                    measuringPointRepo.getMeasuringPoints()
                                        .add(j.getMeasuringPoint());
                                    
                                    monitorRepo.getMonitors()
                                        .add(j);
                                    
                                    j.setMonitorRepository(monitorRepo);
                                }

                            }
                        }
                    }
                
            }
        }
//        // Iterating over System-model
//        while (repoIterator.hasNext()) {
//
//            object = repoIterator.next();
//            LOGGER.debug("Iterated: " + object.eClass().getName());
//
//            if (!StereotypeAPI.getAppliedStereotypes(object)
//                .isEmpty() && StereotypeAPI.getAppliedStereotypes(object)
//                    .get(0)
//                    .getName()
//                    .equals("QualitygateElement")) {
//
//                if (object instanceof ProvidedRole) {
//                    LOGGER.debug(
//                            "The connector " + ((Connector) object).getEntityName() + " has a qualitygate-application");
//
//                    // List of generated Monitors for the attached Qualitygates (could be more than
//                    // one)
//                    List<Monitor> qualitygateMonitors = preprocessingSwitch.create(res, system)
//                        .handleQualitygate(object);
//                    // Removing the Null-elements, because not every Qualitygate needs a calculator
//                    while (qualitygateMonitors.remove(null));
//
//                    // Adding the generated Monitors to the repositories
//                    for (Monitor e : qualitygateMonitors) {
//
//                        if (!this.isMeasurementSpecificationPresent(e)) {
//                            measuringPointRepo.getMeasuringPoints()
//                                .add(e.getMeasuringPoint());
//                            
//                            monitorRepo.getMonitors()
//                                .add(e);
//                            
//                            e.setMonitorRepository(monitorRepo);
//                        }
//
//                    }
//                }
//            }
//        }

        // Only for debug reasons
        MonitorRepository monitorRepository = (MonitorRepository) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY)
            .get(0);

        for (Monitor e : monitorRepository.getMonitors()) {
            LOGGER.debug(e.getMeasuringPoint()
                .getStringRepresentation());

            for (MeasurementSpecification i : (e.getMeasurementSpecifications())) {
                LOGGER.debug(i.getMetricDescription()
                    .getTextualDescription());
            }
        }
    }

    @Override
    public void cleanup(IProgressMonitor monitor) throws CleanupFailedException {
        // Nothing to clean up
    }

    @Override
    public String getName() {
        return "Qualitygate Calculator Completion";
    }

    @Override
    public void setBlackboard(MDSDBlackboard blackboard) {
        this.blackboard = blackboard;

    }

    /**
     * Testing whether MeasurementSpecification is already in MonitorRepository.
     * 
     * @param qualitygateMonitor
     * @return
     */
    public boolean isMeasurementSpecificationPresent(Monitor qualitygateMonitor) {

        MonitorRepository monitorRepo = (MonitorRepository) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY)
            .get(0);
        OperationSignature signature = ((SystemOperationMeasuringPoint) qualitygateMonitor.getMeasuringPoint())
            .getOperationSignature();
        Role role = ((SystemOperationMeasuringPoint) qualitygateMonitor.getMeasuringPoint()).getRole();

        for (Monitor e : monitorRepo.getMonitors()) {
            MeasuringPoint measPoint = e.getMeasuringPoint();
            if (measPoint instanceof SystemOperationMeasuringPoint) {
                if (((OperationReference) measPoint).getOperationSignature()
                    .equals(signature)
                        && ((OperationReference) measPoint).getRole()
                            .equals(role)) {
                    // Same Measuring-Point

                    for (MeasurementSpecification i : e.getMeasurementSpecifications()) {
                        for (MeasurementSpecification j : qualitygateMonitor.getMeasurementSpecifications()) {
                            if (i.getMetricDescription()
                                .equals(j.getMetricDescription())
                                    && i.getProcessingType()
                                        .equals(j.getProcessingType())
                                    && !i.isTriggersSelfAdaptations()) {
                                // Same MeasurementSpecification
                                return true;
                            }
                        }
                    }

                }
            }

        }
        return false;
    }

}
