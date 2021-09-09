//package org.palladiosimulator.simulizar.qualitygate.interpreter;
//
//import org.apache.log4j.Level;
//import org.apache.log4j.Logger;
//import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
//import org.palladiosimulator.failuremodel.qualitygate.util.QualitygateSwitch;
//import org.palladiosimulator.metricspec.MetricDescriptionRepository;
//import org.palladiosimulator.monitorrepository.Monitor;
//import org.palladiosimulator.pcm.core.composition.AssemblyContext;
//import org.palladiosimulator.pcm.repository.ProvidedRole;
//import org.palladiosimulator.pcm.system.System;
//
//import dagger.assisted.Assisted;
//import dagger.assisted.AssistedFactory;
//import dagger.assisted.AssistedInject;
//
//public class StereotypeQualitygateSystemProvidedRolePreprocessingSwitch extends QualitygateSwitch<Monitor> {
//    
//    @AssistedFactory
//    public static interface Factory {
//        StereotypeQualitygateProvidedRolePreprocessingSwitch create(MetricDescriptionRepository metricRepo,
//                AssemblyContext assembly);
//    }
//
//    Logger LOGGER = Logger.getLogger(StereotypeQualitygateProvidedRolePreprocessingSwitch.class);
//    private ProvidedRole stereotypedRole;
//    private final MetricDescriptionRepository metricRepo;
//    private System system;
//    private QualityGate qualitygate;
//
//    @AssistedInject
//    public StereotypeQualitygateSystemProvidedRolePreprocessingSwitch(@Assisted MetricDescriptionRepository metricRepo,
//            @Assisted System system) {
//        LOGGER.setLevel(Level.DEBUG);
//        this.metricRepo = metricRepo;
//        this.system = system;
//    }
//    
//    
//    
//    
//    
//    
//    
//
//}
