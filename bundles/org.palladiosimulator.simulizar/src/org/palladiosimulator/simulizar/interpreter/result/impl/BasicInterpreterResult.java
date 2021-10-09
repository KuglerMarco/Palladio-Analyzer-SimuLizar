package org.palladiosimulator.simulizar.interpreter.result.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class BasicInterpreterResult implements InterpreterResult {
    public List<InterpretationIssue> issues;
    

    @Override
    public boolean hasNoIssues() {
        return issues.isEmpty();
    }

    @Override
    public Iterable<InterpretationIssue> getIssues() {
        return ImmutableList.copyOf(issues);
    }
    
    public static BasicInterpreterResult of(InterpretationIssue issue) {
        var result = new BasicInterpreterResult();
        result.issues = new ArrayList<>(Collections.singletonList(issue));
        return result;                
    }
    
    public static BasicInterpreterResult of(Iterable<InterpretationIssue> iterable) {
        var result = new BasicInterpreterResult();
        result.issues = new ArrayList<>();
        Iterables.addAll(result.issues, iterable);
        return result;                
    }
    
    @Override
    public boolean removeIssue(InterpretationIssue issue) {
        return issues.remove(issue);
    }

    @Override
    public boolean addIssue(InterpretationIssue issue) {
        return issues.add(issue);
    }


    

}
