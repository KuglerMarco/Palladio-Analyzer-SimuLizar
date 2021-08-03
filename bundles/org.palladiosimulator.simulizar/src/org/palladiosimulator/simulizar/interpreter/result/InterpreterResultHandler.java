package org.palladiosimulator.simulizar.interpreter.result;

import org.palladiosimulator.simulizar.di.extension.Extension;

/**
 * Issue handler process the issues which occur during interpretation and ultimately determine how
 * the current interpreter execution flow should proceed.
 * 
 * @author Sebastian Krach
 *
 */
public interface InterpreterResultHandler extends Extension {

    /**
     * Invoke the required issue handling logic and determine how the interpreter should proceed.
     */
    InterpreterResumptionPolicy handleIssues(InterpreterResult result);
    
    boolean supportIssues(InterpretationIssue issue);

}
