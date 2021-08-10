package org.palladiosimulator.simulizar.di.modules.scoped.thread;

import java.util.Set;

import org.palladiosimulator.simulizar.interpreter.ComposedRDSeffSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitch;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.RDSeffPerformanceSwitch;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitch;
import org.palladiosimulator.simulizar.interpreter.RDSeffSwitchContributionFactory;
import org.palladiosimulator.simulizar.interpreter.StereotypeDispatchRepositoryComponentSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.impl.ExtensibleComposedRDSeffSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.impl.ExtensibleStereotypeComposedStructureInnerSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.impl.ExtensibleStereotypeDispatchRepositoryComponentSwitchFactory;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.result.impl.BasicInterpreterResultMerger;
import org.palladiosimulator.simulizar.scopes.SimulatedThreadScope;


import com.google.common.collect.ImmutableSet;

import dagger.Binds;
import dagger.Module;
import dagger.Provides;
import dagger.multibindings.ElementsIntoSet;

@Module
public interface CoreSimulatedThreadBindings {
    
    
    @Binds
    @SimulatedThreadScope
    InterpreterResultMerger bindResultMerger(BasicInterpreterResultMerger impl);
    
    @Binds
    @SimulatedThreadScope
    ComposedRDSeffSwitchFactory bindComposedRDSeffSwitchFactory(ExtensibleComposedRDSeffSwitchFactory impl);
    
    @Binds
    @SimulatedThreadScope @StandardComposedStructureInnerSwitch
    ComposedStructureInnerSwitchFactory bindComposedStructureInnerSwitchFactory(ComposedStructureInnerSwitch.Factory impl);
    
    
    @Binds
    @SimulatedThreadScope 
    ComposedStructureInnerSwitchFactory bindStereotypeDispatchComposedStructureInnerSwitchFactory(ExtensibleStereotypeComposedStructureInnerSwitchFactory impl);


    @Provides
    @ElementsIntoSet
    static Set<RDSeffSwitchContributionFactory> provideCoreRDSeffSwitchFactories(
            RDSeffPerformanceSwitch.Factory performanceSwitchFactory, RDSeffSwitch.Factory rdseffSwitchFactory) {
        return ImmutableSet.of(rdseffSwitchFactory, performanceSwitchFactory);
    }
    
    @Binds
    @SimulatedThreadScope
    StereotypeDispatchRepositoryComponentSwitchFactory bindStereotypeDispatchRepositoryComponentSwitchFactory(ExtensibleStereotypeDispatchRepositoryComponentSwitchFactory impl);

}
