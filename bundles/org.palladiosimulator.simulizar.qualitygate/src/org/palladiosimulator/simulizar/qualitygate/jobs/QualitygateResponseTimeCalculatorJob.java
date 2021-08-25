package org.palladiosimulator.simulizar.qualitygate.jobs;

import java.util.List;

import javax.inject.Inject;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.analyzer.workflow.ConstantsContainer;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.metricspec.MetricDescriptionRepository;
import org.palladiosimulator.metricspec.constants.MetricDescriptionConstants;
import org.palladiosimulator.monitorrepository.MeasurementSpecification;

import org.palladiosimulator.monitorrepository.Monitor;
import org.palladiosimulator.monitorrepository.MonitorRepository;
import org.palladiosimulator.monitorrepository.MonitorRepositoryPackage;

import org.palladiosimulator.simulizar.qualitygate.interpreter.StereotypeQualitygateExternalCallPreprocessingSwitch;
import org.palladiosimulator.simulizar.qualitygate.interpreter.StereotypeQualitygateProvidedRolePreprocessingSwitch;

import de.uka.ipd.sdq.workflow.jobs.CleanupFailedException;
import de.uka.ipd.sdq.workflow.jobs.IBlackboardInteractingJob;
import de.uka.ipd.sdq.workflow.jobs.JobFailedException;
import de.uka.ipd.sdq.workflow.jobs.UserCanceledException;
import de.uka.ipd.sdq.workflow.mdsd.blackboard.MDSDBlackboard;

import org.palladiosimulator.pcm.core.composition.AssemblyContext;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.pcm.system.SystemPackage;
import org.palladiosimulator.pcm.repository.BasicComponent;
import org.palladiosimulator.pcm.seff.AbstractAction;
import org.palladiosimulator.pcm.seff.ExternalCallAction;
import org.palladiosimulator.pcm.seff.ResourceDemandingSEFF;
import org.palladiosimulator.pcm.seff.ServiceEffectSpecification;
import org.palladiosimulator.pcm.repository.ProvidedRole;
import org.palladiosimulator.pcm.repository.Repository;
import org.palladiosimulator.pcm.repository.RepositoryComponent;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.eclipse.emf.common.util.URI;

/**
 * Creates the necessary Monitor-elements in the MonitorRepository for every Qualitygate, so that
 * the according Calculators are available within the simulation.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateResponseTimeCalculatorJob implements IBlackboardInteractingJob<MDSDBlackboard> {

    Logger LOGGER = Logger.getLogger(QualitygateResponseTimeCalculatorJob.class);

    private MDSDBlackboard blackboard;
    private final StereotypeQualitygateExternalCallPreprocessingSwitch.Factory externalCallPreprocessingSwitch;
    private StereotypeQualitygateProvidedRolePreprocessingSwitch.Factory rolePreprocessingSwitch;
    private org.palladiosimulator.pcm.system.System systemRepo;
    private MeasuringPointRepository measuringPointRepo;
    private MonitorRepository monitorRepo;
    private MetricDescriptionRepository metricDescRepo;
    private Repository repo;

    @Inject
    public QualitygateResponseTimeCalculatorJob(MDSDBlackboard blackboard,
            StereotypeQualitygateExternalCallPreprocessingSwitch.Factory preprocessingSwitch,
            StereotypeQualitygateProvidedRolePreprocessingSwitch.Factory rolePreprocessingSwitch) {

        this.blackboard = blackboard;
        this.externalCallPreprocessingSwitch = preprocessingSwitch;
        this.rolePreprocessingSwitch = rolePreprocessingSwitch;

        LOGGER.setLevel(Level.DEBUG);

    }

    /**
     * Adds the required Monitors to the MonitorRepository before simulation.
     */
    @Override
    public void execute(IProgressMonitor monitor) throws JobFailedException, UserCanceledException {

        ResourceSet resourceSet = blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getResourceSet();

        // Loading CommonMetrics-model
        URI uri = URI.createURI(MetricDescriptionConstants.PATHMAP_METRIC_SPEC_MODELS_COMMON_METRICS_METRICSPEC);
        metricDescRepo = (MetricDescriptionRepository) resourceSet.getResource(uri, false)
            .getContents()
            .get(0);

        if (blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY)
            .isEmpty()) {
            throw new IllegalArgumentException("No MeasuringPointRepository found!");
        }

        measuringPointRepo = (MeasuringPointRepository) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY)
            .get(0);

        if (blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY)
            .isEmpty()) {
            throw new IllegalArgumentException("No MonitorRepository found!");
        }

        monitorRepo = (MonitorRepository) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY)
            .get(0);

        repo = (Repository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(RepositoryPackage.Literals.REPOSITORY)
            .get(1);

        /*
         * Iterating over all the assemblies to create the monitors for potentially attached
         * Qualitygates at the ProvidedRoles. Iterating over assemblies to take into account that
         * some Qualitygates are only attached to specific assembly.
         */
        systemRepo = (org.palladiosimulator.pcm.system.System) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(SystemPackage.Literals.SYSTEM)
            .get(0);
        
        this.attachMonitorsAtProvidedRole();
        
        this.attachMonitorsAtExternalCall();

        // Only for debug reasons
        MonitorRepository monitorRepository = (MonitorRepository) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY)
            .get(0);

        LOGGER.debug("Following Monitors are in the MonitorRepository.");
        for (Monitor e : monitorRepository.getMonitors()) {
            LOGGER.debug(e.getMeasuringPoint()
                .getStringRepresentation());

            for (MeasurementSpecification i : (e.getMeasurementSpecifications())) {
                LOGGER.debug(i.getMetricDescription()
                    .getTextualDescription());
            }
        }
    }
    
    public void attachMonitorsAtProvidedRole() {
        for (AssemblyContext assembly : systemRepo.getAssemblyContexts__ComposedStructure()) {

            for (ProvidedRole role : assembly.getEncapsulatedComponent__AssemblyContext()
                .getProvidedRoles_InterfaceProvidingEntity()) {

                if (this.hasQualityGate(role)) {

                    LOGGER.debug("The ProvidedRole " + ((ProvidedRole) role).getEntityName()
                            + " has a qualitygate-application");

                    // Generated Monitors for the Qualitygates.
                    List<Monitor> qualitygateMonitors = rolePreprocessingSwitch.create(metricDescRepo, assembly)
                        .handleQualitygate(role);

                    // Adding the generated Monitors to the repositories
                    for (Monitor j : qualitygateMonitors) {

                        if (!this.isMonitorPresent(j)) {
                            measuringPointRepo.getMeasuringPoints()
                                .add(j.getMeasuringPoint());

                            monitorRepo.getMonitors()
                                .add(j);

                            j.getMeasuringPoint()
                                .setMeasuringPointRepository(measuringPointRepo);

                            j.setMonitorRepository(monitorRepo);
                        }
                    }
                }
            }
        }
    }
    
    public void attachMonitorsAtExternalCall() {
        
        // Creating and adding Qualitygate-Monitors
        for (RepositoryComponent e : repo.getComponents__Repository()) {

            for (ServiceEffectSpecification seff : ((BasicComponent) e)
                .getServiceEffectSpecifications__BasicComponent()) {

                for (AbstractAction abstractAction : ((ResourceDemandingSEFF) seff).getSteps_Behaviour()) {
                    if (!StereotypeAPI.getAppliedStereotypes(abstractAction)
                        .isEmpty() && StereotypeAPI.getAppliedStereotypes(abstractAction)
                            .get(0)
                            .getName()
                            .equals("QualitygateElement")) {

                        if (abstractAction instanceof ExternalCallAction) {

                            LOGGER.debug("The ExternalCall " + ((ExternalCallAction) abstractAction).getEntityName()
                                    + " has a qualitygate-application");

                            // List of generated Monitors for the attached Qualitygates
                            List<Monitor> qualitygateMonitors = externalCallPreprocessingSwitch.create(metricDescRepo)
                                .handleQualitygate(abstractAction);

                            // Adding the generated Monitors to the repositories
                            for (Monitor j : qualitygateMonitors) {

                                if (!this.isMonitorPresent(j)) {
                                    measuringPointRepo.getMeasuringPoints()
                                        .add(j.getMeasuringPoint());

                                    monitorRepo.getMonitors()
                                        .add(j);

                                    j.getMeasuringPoint()
                                        .setMeasuringPointRepository(measuringPointRepo);

                                    j.setMonitorRepository(monitorRepo);
                                }

                            }

                        }

                    }
                }
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
     * Testing whether Monitor is already in MonitorRepository.
     * 
     * @param qualitygateMonitor
     * @return
     */
    public boolean isMonitorPresent(Monitor qualitygateMonitor) {

        MonitorRepository monitorRepo = (MonitorRepository) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY)
            .get(0);

        // Comparing for each Monitor the MeasuringPoint and the MeasurmentSpecification
        for (Monitor monitor : monitorRepo.getMonitors()) {

            if (monitor.getMeasuringPoint()
                .equals(qualitygateMonitor.getMeasuringPoint())) {

                for (MeasurementSpecification spec : monitor.getMeasurementSpecifications()) {

                    if (spec.equals(qualitygateMonitor.getMeasurementSpecifications()
                        .get(0))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /**
     * Checks whether object has a Qualitygate attached.
     * 
     * @param object
     * @return
     */
    public boolean hasQualityGate(Entity object) {

        for (Stereotype stereotype : StereotypeAPI.getAppliedStereotypes(object)) {

            if (stereotype.getName()
                .equals("QualitygateElement")) {
                return true;
            }
        }
        return false;
    }
}
