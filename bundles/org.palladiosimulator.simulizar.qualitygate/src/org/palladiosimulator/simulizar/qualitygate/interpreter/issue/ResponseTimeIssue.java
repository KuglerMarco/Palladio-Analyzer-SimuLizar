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
    
    private Double responseTime;
    

    public ResponseTimeIssue(Entity stereotypedObject, QualityGate qualitygate,
            Double responseTime) {
        super();
        //Factories for EntityReferences
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.pcm.core.entity.Entity> stereotypedObjectFac = new SimuLizarEntityReferenceFactories.Entity();
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.failuremodel.qualitygate.QualityGate> qualitygateFac = new SimuLizarEntityReferenceFactories.Qualitygate();
        
        
        this.stereotypedObjectRef = stereotypedObjectFac.createCached(stereotypedObject);
        
        this.qualitygateRef = qualitygateFac.createCached(qualitygate);
        
        this.responseTime = responseTime;
    }
    

    @Override
    public boolean isHandled() {
        // TODO Auto-generated method stub
        return false;
    }

    @Override
    public EntityReference<Entity> getStereotypedObjectRef() {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public EntityReference<QualityGate> getQualitygateRef() {
        // TODO Auto-generated method stub
        return null;
    }

}
