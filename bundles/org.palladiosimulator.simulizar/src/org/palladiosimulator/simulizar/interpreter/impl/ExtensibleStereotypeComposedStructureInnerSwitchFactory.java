package org.palladiosimulator.simulizar.interpreter.impl;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.emf.ecore.util.Switch;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitch;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.StereotypeComposedStructureInnerSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.StereotypeDispatchComposedStructureInnerSwitch;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;


/**
 * Factory for the StereotypeDispatchComposedStructureInnerSwitch.
 * 
 * @author Marco Kugler
 *
 */
public class ExtensibleStereotypeComposedStructureInnerSwitchFactory
        implements StereotypeComposedStructureInnerSwitchFactory {

    /**
     * Includes the registered ComposedStructureInnerSwitchContributionFactorys
     */
    private final Provider<Set<ComposedStructureInnerSwitchStereotypeContributionFactory>> elementFactoriesProvider;
    
    /**
     * Factory for the default ComposedStructureInnerSwitch, which needs be set in the StereotypeDispatchComposedStructureInnerSwitch.
     */
    private final ComposedStructureInnerSwitch.Factory composedStructureInnerSwitchFactory;
    
    private final InterpreterResultMerger merger;
    
    private final InterpreterResultHandler handler;


    @Inject
    public ExtensibleStereotypeComposedStructureInnerSwitchFactory(Provider<Set<ComposedStructureInnerSwitchStereotypeContributionFactory>> elementFactoriesProvider, ComposedStructureInnerSwitch.Factory composedStructureInnerSwitchFactory, 
            InterpreterResultMerger merger, InterpreterResultHandler handler) {
        this.composedStructureInnerSwitchFactory = composedStructureInnerSwitchFactory;
        this.elementFactoriesProvider = elementFactoriesProvider;
        this.merger = merger;
        this.handler = handler;
    }

    
    
    /**
     * Creates the StereotypeDispatchComposedStructureInnerSwitch.
     */
    @Override
    public Switch<InterpreterResult> create(InterpreterDefaultContext context, final Signature operationSignature,
            final RequiredRole requiredRole) {
        
        //TODO Factory?
        StereotypeDispatchComposedStructureInnerSwitch interpreter = new StereotypeDispatchComposedStructureInnerSwitch(merger, handler, composedStructureInnerSwitchFactory.create(context, operationSignature, requiredRole));
        
        
        var elementFactories = elementFactoriesProvider.get();
        if (elementFactories.isEmpty()) {
            throw new IllegalStateException("No StereotypeSwitches for ComposedStructures are registered.");
        }
        elementFactories.stream().forEach(s -> interpreter.addSwitch(
                s.createStereotypeSwitch(context, operationSignature, requiredRole, interpreter)));
        
        return interpreter;
    }

}
