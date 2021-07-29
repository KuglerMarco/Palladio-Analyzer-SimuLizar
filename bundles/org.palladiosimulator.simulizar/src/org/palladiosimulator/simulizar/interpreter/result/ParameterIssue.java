package org.palladiosimulator.simulizar.interpreter.result;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.util.EcoreUtil;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.entity.EntityReference;
import org.palladiosimulator.simulizar.entity.SimuLizarEntityReferenceFactories;

/**
 * Records the broken Qualitygates with Request- and ResponseParameterScope.
 * 
 * @author Marco Kugler
 *
 */
public class ParameterIssue implements QualitygateIssue {

    private String stackContent;
    private EntityReference<Entity> stereotypedObjectRef;
    private EntityReference<QualityGate> qualitygateRef;
    private static final Logger LOGGER = Logger.getLogger(ParameterIssue.class);

    
    public ParameterIssue(Entity object, Stereotype stereotype, String stackContent) {
        
        //Factories for EntityReferences
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.pcm.core.entity.Entity> ref = new SimuLizarEntityReferenceFactories.Entity();
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.failuremodel.qualitygate.QualityGate> qualitygateFac = new SimuLizarEntityReferenceFactories.Qualitygate();
        
        this.stereotypedObjectRef = ref.createCached(object);
        
        
        
        EList<EObject> taggedValues = StereotypeAPI.getTaggedValue(object, "qualitygate", stereotype.getName());
        QualityGate qualitygate = (QualityGate) taggedValues.get(0);
        
        this.qualitygateRef = qualitygateFac.createCached(qualitygate);
        
        this.stackContent = stackContent;
        
        LOGGER.setLevel(Level.DEBUG);
        
        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("ParameterIssue includes StackContent: " + stackContent);
        }
        
    }



    @Override
    public boolean isHandled() {
        // TODO Auto-generated method stub
        return false;
    }



    public String getStackContent() {
        return stackContent;
    }



    public EntityReference<Entity> getAssemblyConnectorRef() {
        return stereotypedObjectRef;
    }



    public EntityReference<QualityGate> getQualitygateRef() {
        return qualitygateRef;
    }


}
