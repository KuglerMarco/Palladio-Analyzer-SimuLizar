package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.pcm.core.PCMRandomVariable;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.entity.EntityReference;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.qualitygate.interpreter.RDSeffSwitchQualitygateContributionSwitch;



public class ResponseTimeProxyIssue implements InterpretationIssue {
    
    private PCMRandomVariable premise;
    private RDSeffSwitchQualitygateContributionSwitch seffSwitch;
    
    public ResponseTimeProxyIssue(PCMRandomVariable premise, RDSeffSwitchQualitygateContributionSwitch seffSwitch) {
        this.premise = premise;
        this.seffSwitch = seffSwitch;
    }

    public PCMRandomVariable getPremise() {
        return premise;
    }

    public RDSeffSwitchQualitygateContributionSwitch getSeffSwitch() {
        return seffSwitch;
    }

    @Override
    public boolean isHandled() {
        // TODO Auto-generated method stub
        return false;
    }
    
    
    
    
    
    
    
    
    
}
