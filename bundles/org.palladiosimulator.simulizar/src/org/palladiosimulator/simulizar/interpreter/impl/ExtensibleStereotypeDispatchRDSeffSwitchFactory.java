package org.palladiosimulator.simulizar.interpreter.impl;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.emf.ecore.util.Switch;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.di.modules.scoped.thread.StandardSwitch;
import org.palladiosimulator.simulizar.interpreter.ComposedRDSeffSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.StereotypeDispatchRDSeffSwitch;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandlerDispatchFactory;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;

public class ExtensibleStereotypeDispatchRDSeffSwitchFactory implements ComposedRDSeffSwitchFactory {
    
    
    /**
     * Includes the registered ComposedStructureInnerSwitchContributionFactorys
     */
    private final Provider<Set<RDSeffSwitchStereotypeContributionFactory>> elementFactoriesProvider;
    
    private final ComposedRDSeffSwitchFactory composedRDSeffSwitchFactory;
    
    private final InterpreterResultMerger merger;

    private final InterpreterResultHandler handler;

    @Inject
    public ExtensibleStereotypeDispatchRDSeffSwitchFactory(
            Provider<Set<RDSeffSwitchStereotypeContributionFactory>> elementFactoriesProvider,
            @StandardSwitch ComposedRDSeffSwitchFactory composedRDSeffSwitchFactory,
            InterpreterResultMerger merger, InterpreterResultHandlerDispatchFactory handler) {
        
        this.composedRDSeffSwitchFactory = composedRDSeffSwitchFactory;
        this.elementFactoriesProvider = elementFactoriesProvider;
        this.merger = merger;
        this.handler = handler.create();
        
    }
    
    /**
     * Creates the StereotypeDispatchComposedStructureInnerSwitch.
     */
    @Override
    public Switch<InterpreterResult> createRDSeffSwitch(InterpreterDefaultContext context) {

        // TODO Factory?
        var interpreter = new StereotypeDispatchRDSeffSwitch(
                merger, handler, composedRDSeffSwitchFactory.createRDSeffSwitch(context));

        var elementFactories = elementFactoriesProvider.get();
        if (elementFactories.isEmpty()) {
            throw new IllegalStateException("No StereotypeSwitches for ComposedStructures are registered.");
        }
        elementFactories.stream()
            .forEach(s -> interpreter
                .addSwitch(s.create(context, interpreter)));

        return interpreter;
    }



}
