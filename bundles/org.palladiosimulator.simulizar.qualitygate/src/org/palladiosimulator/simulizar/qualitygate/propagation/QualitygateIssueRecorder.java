package org.palladiosimulator.simulizar.qualitygate.propagation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.entity.EntityReference;
import org.palladiosimulator.simulizar.entity.SimuLizarEntityReferenceFactories;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.QualitygateIssue;

public class QualitygateIssueRecorder {
    
    // Qualitygate for which the issues are recorded
    private final EntityReference<QualityGate> qualitygateRef;
    
    // Entity which was attached with the Qualitygate
    private EntityReference<Entity> stereotypedObjectRef;
    
    // Different qualitygate-issues at the Qualitygate
    private List<QualitygateIssue> issueList;
    
    // the String means here the id of the qualitygate, from which the issue was emerging
    private Map<String, Integer> issueCounter = new HashMap<String, Integer>();
    
    private Integer numberOfBreaking;
    

    public QualitygateIssueRecorder(QualityGate qualitygate, Entity stereotypedObject) {
        
      //Factories for EntityReferences
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.pcm.core.entity.Entity> stereotypedObjectFac = new SimuLizarEntityReferenceFactories.Entity();
        EntityReference.AbstractEntityReferenceFactory<org.palladiosimulator.failuremodel.qualitygate.QualityGate> qualitygateFac = new SimuLizarEntityReferenceFactories.Qualitygate();
        
        this.qualitygateRef = qualitygateFac.createCached(qualitygate);
        this.stereotypedObjectRef = stereotypedObjectFac.createCached(stereotypedObject);
        this.issueList = new ArrayList<QualitygateIssue>();
        this.numberOfBreaking = 0;
        
    }
    
    public void addIssue(QualitygateIssue issue) {
        
        if(!issueCounter.containsKey(issue.getQualitygateId())) {
            // Create new entry for this issue
            issueCounter.put(issue.getQualitygateId(), 1);
            issueList.add(issue);
            this.numberOfBreaking++;
        } else {
            int currentCount = issueCounter.get(issue.getQualitygateId()) + 1;
            issueCounter.put(issue.getQualitygateId(), currentCount);
            this.numberOfBreaking++;
        }
    }
    
    public int getCountOfIssue(QualitygateIssue issue) {
        return this.issueCounter.get(issue.getQualitygateId());
    }
    
    public double getPercentageOfPresenceForIssue(QualitygateIssue issue) {
        double result = issueCounter.get(issue.getQualitygateId());
        return (result / numberOfBreaking) * 100;
    }    
    
    public List<QualitygateIssue> getIssueList() {
        return issueList;
    }

    public EntityReference<QualityGate> getQualitygateRef() {
        return qualitygateRef;
    }

    public EntityReference<Entity> getStereotypedObjectRef() {
        return stereotypedObjectRef;
    }
    
    public Integer getNumberOfBreaking() {
        return numberOfBreaking;
    }
    


}
