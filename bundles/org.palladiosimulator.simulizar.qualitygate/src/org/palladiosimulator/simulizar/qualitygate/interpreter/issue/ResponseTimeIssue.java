package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;


import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.entity.EntityReference;
import org.palladiosimulator.simulizar.entity.SimuLizarEntityReferenceFactories;

public class ResponseTimeIssue implements QualitygateIssue {

    //Reference of the stereotyped Object
    private EntityReference<Entity> stereotypedObjectRef;
    
    //Reference of the Qualitygate-element, which was broken
    private EntityReference<QualityGate> qualitygateRef;
    
    private String qualitygateId;
    
    private Double responseTime;
    
    private boolean isHandled;
    

    public ResponseTimeIssue(Entity stereotypedObject, QualityGate qualitygate, boolean isHandled) {
        super();
        //Factories for EntityReferences
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.pcm.core.entity.Entity> stereotypedObjectFac = new SimuLizarEntityReferenceFactories.Entity();
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.failuremodel.qualitygate.QualityGate> qualitygateFac = new SimuLizarEntityReferenceFactories.Qualitygate();
        
        
        this.stereotypedObjectRef = stereotypedObjectFac.createCached(stereotypedObject);
        
        this.qualitygateRef = qualitygateFac.createCached(qualitygate);
        
        this.qualitygateId = qualitygate.getId();
        
        this.isHandled = isHandled;
    }
    

    @Override
    public boolean isHandled() {
        return this.isHandled;
    }
    

    @Override
    public EntityReference<Entity> getStereotypedObjectRef() {
        return stereotypedObjectRef;
    }

    @Override
    public EntityReference<QualityGate> getQualitygateRef() {
        return qualitygateRef;
    }
    
    public Double getResponseTime() {
        return responseTime;
    }


    @Override
    public String getQualitygateId() {
        return this.qualitygateId;
    }


    @Override
    public void setHandled(boolean handled) {
        this.isHandled = handled;
        
    }

}
