package org.palladiosimulator.simulizar.qualitygate.interpreter.issue.proxy;

import java.util.ArrayList;
import java.util.Map.Entry;

import org.palladiosimulator.failuremodel.qualitygate.QualityGate;
import org.palladiosimulator.failuremodel.severityhierarchy.Severity;
import org.palladiosimulator.pcm.core.entity.Entity;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;

import de.uka.ipd.sdq.simucomframework.SimuComSimProcess;

/**
 * Proxy for Crashs
 * 
 * @author Marco Kugler
 *
 */
public class CrashProxyIssue implements InterpretationIssue {

    private final QualityGate modelElement;
    private final InterpreterDefaultContext context;
    private final boolean success;
    private final Severity severity;
    private final Entity stereotypedObject;
    // Stack-Content of the time, the Qualitygate was broken
    private ArrayList<Entry<String, Object>> stackContent;

    public CrashProxyIssue(final QualityGate modelElement, final InterpreterDefaultContext context,
            final boolean success, final Severity severity, final Entity stereotypedObject,
            ArrayList<Entry<String, Object>> stackContent) {

        this.modelElement = modelElement;
        this.context = context;
        this.success = success;
        this.severity = severity;
        this.stereotypedObject = stereotypedObject;
        this.stackContent = stackContent;

    }

    /**
     * @return the modelElement
     */
    public QualityGate getModelElement() {
        return this.modelElement;
    }

    /**
     * @return the context
     */
    public InterpreterDefaultContext getContext() {
        return this.context;
    }

    /**
     * @return the thread
     */
    public SimuComSimProcess getThread() {
        return this.context.getThread();
    }

    /**
     * @return whether the qualitygate-evaluation was successful
     */
    public boolean isSuccess() {
        return success;
    }

    public Severity getSeverity() {
        return this.severity;
    }

    public Entity getStereotypedObject() {
        return stereotypedObject;
    }

    @Override
    public boolean isHandled() {
        // TODO Auto-generated method stub
        return false;
    }

    public ArrayList<Entry<String, Object>> getStackContent() {
        return stackContent;
    }

}
