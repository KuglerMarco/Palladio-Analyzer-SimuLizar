package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;

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
public class CrashIssue implements QualitygateIssue {

    // Stack-Content of the time, the Qualitygate was broken
    private ArrayList<Entry<String, Object>> stackContent;

    // Reference of the stereotyped Object
    private EntityReference<Entity> stereotypedObjectRef;

    // Reference of the Qualitygate-element, which was broken
    private EntityReference<QualityGate> qualitygateRef;

    private String qualitygateId;

    // Implies whether this issue was already there, when the last qualitygate broke
    private boolean isHandled;

    private static final Logger LOGGER = Logger.getLogger(CrashIssue.class);

    public CrashIssue(Entity object, QualityGate qualitygate, ArrayList<Entry<String, Object>> stackContent,
            boolean isHandled) {

        // Factories for EntityReferences
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.pcm.core.entity.Entity> stereotypedObjectFac 
        = new SimuLizarEntityReferenceFactories.Entity();
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.failuremodel.qualitygate.QualityGate> qualitygateFac 
        = new SimuLizarEntityReferenceFactories.Qualitygate();

        this.stereotypedObjectRef = stereotypedObjectFac.createCached(object);

        this.qualitygateRef = qualitygateFac.createCached(qualitygate);

        this.qualitygateId = qualitygate.getId();

        this.stackContent = stackContent;

        this.isHandled = isHandled;

        LOGGER.setLevel(Level.DEBUG);

        if (LOGGER.isDebugEnabled()) {
            LOGGER.debug("New ParameterIssue at qualitygate: " + qualitygate.getEntityName());
        }

    }

    public boolean isHandled() {
        return isHandled;
    }

    public ArrayList<Entry<String, Object>> getStackContent() {
        return stackContent;
    }

    public EntityReference<Entity> getStereotypedObjectRef() {
        return stereotypedObjectRef;
    }

    public EntityReference<QualityGate> getQualitygateRef() {
        return qualitygateRef;
    }

    @Override
    public String getQualitygateId() {
        return qualitygateId;
    }

    @Override
    public void setHandled(boolean handled) {
        this.isHandled = handled;

    }

}
