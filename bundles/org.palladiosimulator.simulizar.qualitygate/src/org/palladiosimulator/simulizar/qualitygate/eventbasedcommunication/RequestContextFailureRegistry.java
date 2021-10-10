package org.palladiosimulator.simulizar.qualitygate.eventbasedcommunication;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.palladiosimulator.probeframework.measurement.RequestContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
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

    private Map<String, InterpreterResult> requestIssues = new HashMap<String, InterpreterResult>();
    private BasicInterpreterResultMerger merger;

    @Inject
    public RequestContextFailureRegistry(BasicInterpreterResultMerger merger) {
        this.merger = merger;

    }

    public InterpreterResult getInterpreterResult(RequestContext context) {

        RequestContext mostParentContext = this.calcMostParentContext(context);

        return requestIssues.get(mostParentContext.getRequestContextId());

    }
    
    public void addIssue(RequestContext context, InterpretationIssue issue) {

        RequestContext mostParentContext = this.calcMostParentContext(context);
        if(requestIssues.get(mostParentContext.getRequestContextId()) != null) {
            requestIssues.get(mostParentContext.getRequestContextId()).addIssue(issue);
        } else {
            requestIssues.put(mostParentContext.getRequestContextId(), BasicInterpreterResult.of(issue));
        }
        

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
