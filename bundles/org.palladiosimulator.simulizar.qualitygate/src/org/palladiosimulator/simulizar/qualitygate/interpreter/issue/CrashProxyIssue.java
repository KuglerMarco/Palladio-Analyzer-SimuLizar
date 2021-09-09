package org.palladiosimulator.simulizar.qualitygate.interpreter.issue;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;

public class CrashProxyIssue implements InterpretationIssue {

    private QualityGate qualitygate;
    private Entity stereotypedObject;
    private InterpreterDefaultContext context;

    public CrashProxyIssue(QualityGate qualitygate, Entity stereotypedObject, InterpreterDefaultContext context) {

        this.qualitygate = qualitygate;
        this.stereotypedObject = stereotypedObject;
        this.context = context;

    }
    
    public InterpreterDefaultContext getContext() {
        return context;
    }

    @Override
    public boolean isHandled() {
        return false;
    }

    public QualityGate getQualitygate() {
        return qualitygate;
    }

    public Entity getStereotypedObject() {
        return stereotypedObject;
    }

}
