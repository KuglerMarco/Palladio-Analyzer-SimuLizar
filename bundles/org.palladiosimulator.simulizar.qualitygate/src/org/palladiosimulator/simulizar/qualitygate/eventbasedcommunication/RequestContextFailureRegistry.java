package org.palladiosimulator.simulizar.qualitygate.eventbasedcommunication;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import org.palladiosimulator.probeframework.measurement.RequestContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.runtimestate.RuntimeStateEntityManager;
import org.palladiosimulator.simulizar.scopes.RuntimeExtensionScope;

import com.google.common.collect.Iterables;

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

    public void addInterpreterResult(RequestContext context, InterpreterResult result) {

        RequestContext mostParentContext = this.calcMostParentContext(context);

        BasicInterpreterResult mergedResult = BasicInterpreterResult.of(result.getIssues());
        Iterables.addAll(mergedResult.issues, requestIssues.get(mostParentContext.getRequestContextId()).getIssues());

        requestIssues.put(mostParentContext.getRequestContextId(), mergedResult);

    }
    
    public void putInterpreterResult(RequestContext context, InterpreterResult result) {

        RequestContext mostParentContext = this.calcMostParentContext(context);

        requestIssues.put(mostParentContext.getRequestContextId(), result);

    }

    public InterpreterResult getInterpreterResult(RequestContext context) {

        RequestContext mostParentContext = this.calcMostParentContext(context);

        return requestIssues.get(mostParentContext.getRequestContextId());

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
