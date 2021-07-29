package org.palladiosimulator.simulizar.entity;

import javax.inject.Inject;

import org.palladiosimulator.simulizar.entity.EntityReference.AbstractEntityReferenceFactory;

/**
 * This class contains specialized entity reference factory implementations for the relevant model
 * element types used within SimuLizar.
 *
 */
public final class SimuLizarEntityReferenceFactories {
    private SimuLizarEntityReferenceFactories() {
        // No instance should be constructible, as this is a utility class
    }

    public static final class ResourceContainer
            extends AbstractEntityReferenceFactory<org.palladiosimulator.pcm.resourceenvironment.ResourceContainer> {
        @Inject
        public ResourceContainer() {
        }

        @Override
        public EntityReference<org.palladiosimulator.pcm.resourceenvironment.ResourceContainer> create(String id) {
            return new ResourceContainerReference(id);
        }
    }

    public static final class LinkingResource
            extends AbstractEntityReferenceFactory<org.palladiosimulator.pcm.resourceenvironment.LinkingResource> {
        @Inject
        public LinkingResource() {
        }

        @Override
        public EntityReference<org.palladiosimulator.pcm.resourceenvironment.LinkingResource> create(String id) {
            return new LinkingResourceReference(id);
        }
    }

    public static final class UsageScenario
            extends AbstractEntityReferenceFactory<org.palladiosimulator.pcm.usagemodel.UsageScenario> {
        @Inject
        public UsageScenario() {
        }

        @Override
        public EntityReference<org.palladiosimulator.pcm.usagemodel.UsageScenario> create(String id) {
            return new UsageScenarioReference(id);
        }
    }
    
    //Added by Marco (BA)
    public static final class Entity
    extends AbstractEntityReferenceFactory<org.palladiosimulator.pcm.core.entity.Entity> {
        @Inject
        public Entity() {
        }

        @Override
        public EntityReference<org.palladiosimulator.pcm.core.entity.Entity> create(String id) {
            return new EntityReference<org.palladiosimulator.pcm.core.entity.Entity>(id, org.palladiosimulator.pcm.core.entity.Entity.class);
        }
        

    }
    
    //Added by Marco (BA)
    public static final class Qualitygate
    extends AbstractEntityReferenceFactory<org.palladiosimulator.failuremodel.qualitygate.QualityGate> {
        @Inject
        public Qualitygate() {
        }

        @Override
        public EntityReference<org.palladiosimulator.failuremodel.qualitygate.QualityGate> create(String id) {
            return new EntityReference<org.palladiosimulator.failuremodel.qualitygate.QualityGate>(id, org.palladiosimulator.failuremodel.qualitygate.QualityGate.class);
        }
        

    }

}
