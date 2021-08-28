package org.palladiosimulator.simulizar.qualitygate.propagation;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.palladiosimulator.analyzer.workflow.blackboard.PCMResourceSetPartition;
import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.qualitygate.interpreter.issue.QualitygateIssue;
import org.palladiosimulator.simulizar.runtimestate.RuntimeStateEntityManager;
import org.palladiosimulator.simulizar.scopes.RuntimeExtensionScope;
import org.palladiosimulator.simulizar.utils.PCMPartitionManager.Global;

@RuntimeExtensionScope
public class QualitygatePropagationRecorder implements RuntimeStateEntityManager {

    // String means here the id of the qualitygate, for which the issues should be recorded
    Map<String, QualitygateIssueRecorder> issueRecorder = new HashMap<String, QualitygateIssueRecorder>();

    PCMResourceSetPartition pcmPartition;

    @Inject
    public QualitygatePropagationRecorder(@Global final PCMResourceSetPartition pcmPartition) {
        this.pcmPartition = pcmPartition;
    }

    @Override
    public void initialize() {

    }

    public void recordQualitygateIssue(QualityGate qualitygate, Entity stereotypedObject, QualitygateIssue issue) {

        if (!issueRecorder.containsKey(qualitygate.getId())) {
            // Create new QualitygateIssueRecorder for this Qualitygate
            issueRecorder.put(qualitygate.getId(), new QualitygateIssueRecorder(qualitygate, stereotypedObject));
        } else {
            issueRecorder.get(qualitygate.getId())
                .addIssue(issue);
        }

    }

    @Override
    public void cleanup() {

        for (QualitygateIssueRecorder rec : issueRecorder.values()) {


            for (QualitygateIssue issue : rec.getIssueList()) {
                System.out.println("When the qualitygate " + rec.getQualitygateRef()
                    .getModelElement(pcmPartition).getEntityName() + " was broken in ");
                
                System.out.println(rec.getPercentageOfPresenceForIssue(issue) + "%");
                
                System.out.println("the issue of " + issue.getQualitygateRef()
                    .getModelElement(pcmPartition)
                    .getEntityName() + " was present.");
            }

        }

        issueRecorder.clear();
    }

}
