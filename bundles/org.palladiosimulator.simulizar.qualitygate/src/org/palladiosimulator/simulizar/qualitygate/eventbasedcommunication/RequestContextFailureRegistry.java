package org.palladiosimulator.simulizar.qualitygate.eventbasedcommunication;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.palladiosimulator.probeframework.measurement.RequestContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.runtimestate.RuntimeStateEntityManager;
import org.palladiosimulator.simulizar.scopes.RuntimeExtensionScope;

/**
 * Captures the issues at in request context.
 * 
 * @author Marco Kugler
 *
 */
@RuntimeExtensionScope
public class RequestContextFailureRegistry implements RuntimeStateEntityManager {

    private Map<String, InterpreterResult> interpreterResults = new HashMap<String, InterpreterResult>();

    @Inject
    public RequestContextFailureRegistry() {
    }

    /**
     * Returns current InterpreterResult for this RequestContext
     * 
     * @param context
     * @return
     */
    public InterpreterResult getInterpreterResult(RequestContext context) {

        RequestContext mostParentContext = this.calcMostParentContext(context);

        return interpreterResults.get(mostParentContext.getRequestContextId());

    }
    
    /**
     * Adds InterpretationIssue to
     * 
     * @param context
     * @param issue
     */
    public void addIssue(RequestContext context, InterpretationIssue issue) {

        RequestContext mostParentContext = this.calcMostParentContext(context);
        if(interpreterResults.get(mostParentContext.getRequestContextId()) != null) {
            interpreterResults.get(mostParentContext.getRequestContextId()).addIssue(issue);
        } else {
            interpreterResults.put(mostParentContext.getRequestContextId(), BasicInterpreterResult.of(issue));
        }
    }
    
    public void putInterpreterResult(RequestContext context, InterpreterResult result) {
        
        interpreterResults.put(calcMostParentContext(context).getRequestContextId(), result);
        
    }

    /**
     * To handle fork actions.
     * 
     * @param context
     * @return
     */
    private RequestContext calcMostParentContext(RequestContext context) {

        RequestContext result = context;

        while (result.getParentContext() != null) {
            result = result.getParentContext();
        }

        return result;

    }

}
