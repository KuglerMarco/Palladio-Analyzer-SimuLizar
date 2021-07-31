package org.palladiosimulator.simulizar.interpreter.result;

import java.util.ArrayList;
import java.util.Map.Entry;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.entity.EntityReference;
import org.palladiosimulator.simulizar.entity.SimuLizarEntityReferenceFactories;

/**
 * Records the broken Qualitygates with Request- or ResponseParameterScope.
 * 
 * @author Marco Kugler
 *
 */
public class ParameterIssue implements QualitygateIssue {

    //Stack-Content of the time, the Qualitygate was broken
    private ArrayList<Entry<String, Object>> stackContent;
    
    //Reference of the stereotyped Object
    private EntityReference<Entity> stereotypedObjectRef;
    
    //Reference of the Qualitygate-element, which was broken
    private EntityReference<QualityGate> qualitygateRef;
    
    
    private static final Logger LOGGER = Logger.getLogger(ParameterIssue.class);

    
    public ParameterIssue(Entity object, QualityGate qualitygate, ArrayList<Entry<String, Object>> stackContent) {
        
        //Factories for EntityReferences
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.pcm.core.entity.Entity> stereotypedObjectFac = new SimuLizarEntityReferenceFactories.Entity();
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.failuremodel.qualitygate.QualityGate> qualitygateFac = new SimuLizarEntityReferenceFactories.Qualitygate();
        
        
        this.stereotypedObjectRef = stereotypedObjectFac.createCached(object);
        
        this.qualitygateRef = qualitygateFac.createCached(qualitygate);
        
        this.stackContent = stackContent;
        
        LOGGER.setLevel(Level.DEBUG);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("New ParameterIssue StackContent: " + stackContent);
        }
        
    }



    @Override
    public boolean isHandled() {
        // TODO Auto-generated method stub
        return false;
    }



    public ArrayList<Entry<String, Object>> getStackContent() {
        return stackContent;
    }



    public EntityReference<Entity> getAssemblyConnectorRef() {
        return stereotypedObjectRef;
    }



    public EntityReference<QualityGate> getQualitygateRef() {
        return qualitygateRef;
    }


}
