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
import org.palladiosimulator.simulizar.qualitygate.interpreter.preprocessing.StereotypeQualitygateExternalCallPreprocessingSwitch;
import org.palladiosimulator.simulizar.qualitygate.interpreter.preprocessing.StereotypeQualitygateProvidedRolePreprocessingSwitch;

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
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringPointRepository;
import org.palladiosimulator.edp2.models.measuringpoint.MeasuringpointPackage;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.qualitygate.SignatureScope;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.common.util.URI;

/**
 * Creates the necessary Monitors in the MonitorRepository for every Qualitygate, so that the
 * according Calculators are available within the simulation.
 * 
 * @author Marco Kugler
 *
 */
public class QualitygateResponseTimeCalculatorJob implements IBlackboardInteractingJob<MDSDBlackboard> {

    Logger LOGGER = Logger.getLogger(QualitygateResponseTimeCalculatorJob.class);

    private MDSDBlackboard blackboard;
    private final StereotypeQualitygateExternalCallPreprocessingSwitch.Factory externalCallPreprocessingSwitch;
    private final StereotypeQualitygateProvidedRolePreprocessingSwitch.Factory rolePreprocessingSwitch;
    private org.palladiosimulator.pcm.system.System systemRepo;
    private MeasuringPointRepository measuringPointRepo;
    private MonitorRepository monitorRepo;
    private MetricDescriptionRepository metricDescRepo;

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

        this.loadRepositories();

        this.attachQualitygateAtExternalCall();
        this.attachMonitorsAtProvidedRole();
        this.attachMonitorsAtExternalCall();

        LOGGER.debug("Following MeasurementSpecification are in the repository.");
        for (Monitor e : monitorRepo.getMonitors()) {
            LOGGER.debug(e.getMeasuringPoint()
                .getStringRepresentation());

            for (MeasurementSpecification i : (e.getMeasurementSpecifications())) {
                LOGGER.debug(i.getMetricDescription()
                    .getTextualDescription());
            }
        }
    }

    /**
     * Loads the necessary repositories for preprocessing.
     * 
     */
    private void loadRepositories() {
        ResourceSet resourceSet = blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getResourceSet();

        // Loading CommonMetrics-model
        URI uri = URI.createURI(MetricDescriptionConstants.PATHMAP_METRIC_SPEC_MODELS_COMMON_METRICS_METRICSPEC);
        metricDescRepo = (MetricDescriptionRepository) resourceSet.getResource(uri, true)
            .getContents()
            .get(0);

        if (blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY)
            .isEmpty()) {
            throw new IllegalArgumentException("No MeasuringPointRepository found!");
        }

        // Loading MeasuringPointRepository
        measuringPointRepo = (MeasuringPointRepository) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MeasuringpointPackage.Literals.MEASURING_POINT_REPOSITORY)
            .get(0);

        if (blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY)
            .isEmpty()) {
            throw new IllegalArgumentException("No MonitorRepository found!");
        }

        // Loading the MonitorRepository
        monitorRepo = (MonitorRepository) blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(MonitorRepositoryPackage.Literals.MONITOR_REPOSITORY)
            .get(0);

        if (blackboard.getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(SystemPackage.Literals.SYSTEM)
            .isEmpty()) {

            throw new IllegalArgumentException("No System Model found!");
        }

        systemRepo = (org.palladiosimulator.pcm.system.System) blackboard
            .getPartition(ConstantsContainer.DEFAULT_PCM_INSTANCE_PARTITION_ID)
            .getElement(SystemPackage.Literals.SYSTEM)
            .get(0);

    }

    /**
     * Attaches Qualitygates at each ExternalCall with the interface, is the RequiredRole has a
     * Qualitygate annotation.
     * 
     */
    private void attachQualitygateAtExternalCall() {

        for (AssemblyContext assembly : systemRepo.getAssemblyContexts__ComposedStructure()) {

            for (RequiredRole role : assembly.getEncapsulatedComponent__AssemblyContext()
                .getRequiredRoles_InterfaceRequiringEntity()) {

                if (this.hasQualityGate(role)) {

                    if(LOGGER.isDebugEnabled()) {
                        LOGGER.debug("The RequiredRole " + ((RequiredRole) role).getEntityName()
                                + " has a qualitygate-application");
                    }

                    EList<Stereotype> stereotypes = StereotypeAPI.getAppliedStereotypes(role);

                    Stereotype qualitygateElement = stereotypes.stream()
                        .filter(stereotype -> stereotype.getName()
                            .equals("QualitygateElement"))
                        .findAny()
                        .orElseThrow(() -> new IllegalStateException("No Qualitygate stereotype found."));

                    EList<QualityGate> qualitygates = StereotypeAPI.getTaggedValue(role, "qualitygate",
                            "QualitygateElement");

                    for (QualityGate qualitygate : qualitygates) {

                        Signature signature = ((SignatureScope) qualitygate.getScope()).getSignature();

                        for (ServiceEffectSpecification seff : ((BasicComponent) assembly
                            .getEncapsulatedComponent__AssemblyContext())
                                .getServiceEffectSpecifications__BasicComponent()) {

                            for (AbstractAction abstractAction : ((ResourceDemandingSEFF) seff).getSteps_Behaviour()) {

                                if (abstractAction instanceof ExternalCallAction) {

                                    if (((ExternalCallAction) abstractAction).getCalledService_ExternalService()
                                        .equals(signature)
                                            && ((ExternalCallAction) abstractAction).getRole_ExternalService()
                                                .equals(role)) {

                                        StereotypeAPI.applyStereotype(abstractAction, qualitygateElement);

                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    /**
     * Creating and adding the Monitors at ProvidedRole.
     */
    private void attachMonitorsAtProvidedRole() {

        for (AssemblyContext assembly : systemRepo.getAssemblyContexts__ComposedStructure()) {

            for (ProvidedRole role : assembly.getEncapsulatedComponent__AssemblyContext()
                .getProvidedRoles_InterfaceProvidingEntity()) {

                if (this.hasQualityGate(role)) {

                    if(LOGGER.isDebugEnabled()) {
                        LOGGER.debug("The ProvidedRole " + ((ProvidedRole) role).getEntityName()
                                + " has a qualitygate-application");
                    }

                    // Generated Monitors for the Qualitygates.
                    List<Monitor> qualitygateMonitors = rolePreprocessingSwitch.create(metricDescRepo, assembly)
                        .createMonitors(role);

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

    /**
     * Creating and adding the Monitors at ExternalCall.
     */
    private void attachMonitorsAtExternalCall() {

        for (AssemblyContext assembly : systemRepo.getAssemblyContexts__ComposedStructure()) {

            for (ServiceEffectSpecification seff : ((BasicComponent) assembly
                .getEncapsulatedComponent__AssemblyContext()).getServiceEffectSpecifications__BasicComponent()) {

                for (AbstractAction abstractAction : ((ResourceDemandingSEFF) seff).getSteps_Behaviour()) {

                    if (this.hasQualityGate(abstractAction)) {

                        if (abstractAction instanceof ExternalCallAction) {

                            if(LOGGER.isDebugEnabled()) {
                                LOGGER.debug("The ExternalCall " + ((ExternalCallAction) abstractAction).getEntityName()
                                        + " has a qualitygate-application");
                            }

                            // List of generated Monitors for the attached Qualitygates
                            List<Monitor> qualitygateMonitors = externalCallPreprocessingSwitch
                                .create(metricDescRepo, assembly)
                                .createMonitors(abstractAction);

                            // Adding the generated Monitors to the repositories
                            for (Monitor monitor : qualitygateMonitors) {

                                if (!this.isMonitorPresent(monitor)) {
                                    measuringPointRepo.getMeasuringPoints()
                                        .add(monitor.getMeasuringPoint());

                                    monitorRepo.getMonitors()
                                        .add(monitor);

                                    monitor.getMeasuringPoint()
                                        .setMeasuringPointRepository(measuringPointRepo);

                                    monitor.setMonitorRepository(monitorRepo);
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
     * Checking whether Monitor is already in MonitorRepository.
     * 
     * @param qualitygateMonitor
     * @return
     */
    private boolean isMonitorPresent(Monitor qualitygateMonitor) {

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
    private boolean hasQualityGate(Entity object) {

        for (Stereotype stereotype : StereotypeAPI.getAppliedStereotypes(object)) {

            if (stereotype.getName()
                .equals("QualitygateElement")
                    && stereotype.getProfile()
                        .getName()
                        .equals("QualitygateProfile")) {
                return true;
            }
        }
        return false;
    }
}
