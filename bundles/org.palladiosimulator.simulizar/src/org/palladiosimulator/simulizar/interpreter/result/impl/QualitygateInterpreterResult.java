package org.palladiosimulator.simulizar.interpreter.result.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.palladiosimulator.simulizar.interpreter.result.InterpretationIssue;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.QualitygateIssue;

import com.google.common.collect.ImmutableList;

public class QualitygateInterpreterResult implements InterpreterResult {

    List<InterpretationIssue> issues;
    
    //List of broken Qualitygates in this simulation run
    List<QualitygateIssue> qualitygateIssues;
    
    QualitygateInterpreterResult(){
        issues = new ArrayList<>();
        qualitygateIssues = new ArrayList<>();
    }
    
    
    
    @Override
    public boolean hasNoIssues() {
        return issues.isEmpty();
    }

    @Override
    public Iterable<InterpretationIssue> getIssues() {
        return ImmutableList.copyOf(issues);
    }
    
    public Iterable<QualitygateIssue> getQualitygateIssues() {
        return ImmutableList.copyOf(qualitygateIssues);
    }
    
    public static QualitygateInterpreterResult of(InterpretationIssue issue) {
        var result = new QualitygateInterpreterResult();
        result.issues = new ArrayList<>(Collections.singletonList(issue));
        return result;                
    }
    
    public static QualitygateInterpreterResult of(QualitygateIssue qualitygateIssue) {
        var result = new QualitygateInterpreterResult();
        result.qualitygateIssues = new ArrayList<>(Collections.singletonList(qualitygateIssue));
        return result;                
    }
    

}
