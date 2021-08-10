package org.palladiosimulator.simulizar.di.modules.scoped.thread;

import java.util.Set;

import org.palladiosimulator.simulizar.di.extension.ExtensionLookup;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchContributionFactory;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchStereotypeContributionFactory;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.scopes.SimulatedThreadScope;

import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;

@Module
public interface ExtensionComponentSimulatedThreadExtensionBindings {
    
    @Provides
    @SimulatedThreadScope
    @ElementsIntoSet
    static Set<RDSeffSwitchContributionFactory> provideContributionFactories(ExtensionLookup lookup) {
        return lookup.lookup(RDSeffSwitchContributionFactory.class);
    }
    
    @Provides
    @SimulatedThreadScope
    @ElementsIntoSet
    static Set<ComposedStructureInnerSwitchStereotypeContributionFactory> provideStereotypeContributionFactories(ExtensionLookup lookup) {
        return lookup.lookup(ComposedStructureInnerSwitchStereotypeContributionFactory.class);
    }
    
    @Provides
    @SimulatedThreadScope
    @ElementsIntoSet
    static Set<RepositoryComponentSwitchStereotypeContributionFactory> provideRepositoryComponentSwitchStereotypeContributionFactory(ExtensionLookup lookup) {
        return lookup.lookup(RepositoryComponentSwitchStereotypeContributionFactory.class);
    }
    
    @Provides
    @SimulatedThreadScope
    @ElementsIntoSet
    static Set<InterpreterResultHandler> interpreterResultHandler(ExtensionLookup lookup) {
        return lookup.lookup(InterpreterResultHandler.class);
    }

    
 
}
