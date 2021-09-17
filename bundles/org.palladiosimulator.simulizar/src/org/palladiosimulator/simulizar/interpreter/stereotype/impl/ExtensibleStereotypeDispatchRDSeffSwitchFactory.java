package org.palladiosimulator.simulizar.interpreter.stereotype.impl;

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;

import org.eclipse.emf.ecore.util.Switch;
import org.palladiosimulator.simulizar.di.modules.scoped.thread.StandardSwitch;
import org.palladiosimulator.simulizar.interpreter.ComposedRDSeffSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.InterpreterDefaultContext;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandlerDispatchFactory;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.stereotype.RDSeffSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.stereotype.StereotypeDispatchRDSeffSwitch;

/**
 * Factory for the StereotypeDispatchRDSeffSwitch.
 * 
 * @author Marco Kugler
 *
 */
public class ExtensibleStereotypeDispatchRDSeffSwitchFactory implements ComposedRDSeffSwitchFactory {

    private final Provider<Set<RDSeffSwitchStereotypeContributionFactory>> elementFactoriesProvider;

    private final ComposedRDSeffSwitchFactory composedRDSeffSwitchFactory;

    private final InterpreterResultMerger merger;

    private final InterpreterResultHandler handler;

    @Inject
    public ExtensibleStereotypeDispatchRDSeffSwitchFactory(
            Provider<Set<RDSeffSwitchStereotypeContributionFactory>> elementFactoriesProvider,
            @StandardSwitch ComposedRDSeffSwitchFactory composedRDSeffSwitchFactory, InterpreterResultMerger merger,
            InterpreterResultHandlerDispatchFactory handler) {

        this.composedRDSeffSwitchFactory = composedRDSeffSwitchFactory;
        this.elementFactoriesProvider = elementFactoriesProvider;
        this.merger = merger;
        this.handler = handler.create();

    }

    /**
     * Creates the StereotypeDispatchRDSeffSwitch.
     */
    @Override
    public Switch<InterpreterResult> createRDSeffSwitch(InterpreterDefaultContext context) {

        var interpreter = new StereotypeDispatchRDSeffSwitch(merger, handler,
                composedRDSeffSwitchFactory.createRDSeffSwitch(context));

        var elementFactories = elementFactoriesProvider.get();
        elementFactories.stream()
            .forEach(s -> interpreter.addSwitch(s.create(context, interpreter)));

        return interpreter;
    }

}
