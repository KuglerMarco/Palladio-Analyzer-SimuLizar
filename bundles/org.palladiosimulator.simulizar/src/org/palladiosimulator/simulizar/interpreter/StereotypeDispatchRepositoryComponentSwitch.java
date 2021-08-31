package org.palladiosimulator.simulizar.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.Switch;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.repository.RepositoryPackage;
import org.palladiosimulator.simulizar.interpreter.RepositoryComponentSwitchStereotypeContributionFactory.RepositoryComponentSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;

/**
 * Dispatch-Switch, which is searching for a matching StereotypeSwitch to process the attached
 * Stereotypes, if there is one.
 * 
 * @author Marco Kugler
 *
 */
public class StereotypeDispatchRepositoryComponentSwitch extends Switch<InterpreterResult>
        implements RepositoryComponentSwitchStereotypeElementDispatcher {

    // Actual Switch to process the element
    private RepositoryComponentSwitch repositoryComponentSwitch;
    // Registry of available StereotypeSwitches
    private final Map<Stereotype, StereotypeSwitch> registry = new HashMap<Stereotype, StereotypeSwitch>();
    // Set of available StereotypeSwitches, before they are registered in registry
    private final List<StereotypeSwitch> switches = new ArrayList<StereotypeSwitch>();

    private final InterpreterResultHandler handler;
    private InterpreterResultMerger merger;
    protected static RepositoryPackage modelPackage;
    private static final Logger LOGGER = Logger.getLogger(StereotypeDispatchRepositoryComponentSwitch.class);

    public StereotypeDispatchRepositoryComponentSwitch(InterpreterResultMerger merger, InterpreterResultHandler handler,
            RepositoryComponentSwitch repositoryComponentSwitch) {
        if (modelPackage == null) {
            modelPackage = RepositoryPackage.eINSTANCE;
        }

        this.merger = merger;
        this.handler = handler;
        this.repositoryComponentSwitch = repositoryComponentSwitch;

        LOGGER.setLevel(Level.DEBUG);
    }

    /**
     * Processes the attached stereotypes in Request and Response Scope.
     *
     */
    @Override
    public InterpreterResult doSwitch(EClass theEClass, EObject theEObject) {

        InterpreterResult interpreterResult = InterpreterResult.OK;

        // Stereotype-Handling in Request-Scope
        interpreterResult = this.handleAttachedStereotypes(theEObject, CallScope.REQUEST);

        if (handler.handleIssues(interpreterResult)
            .equals(InterpreterResumptionPolicy.CONTINUE)) {
            interpreterResult = merger.merge(interpreterResult, repositoryComponentSwitch.doSwitch(theEObject));
        }

        // Stereotype-Handling in Response-Scope
        if (handler.handleIssues(interpreterResult)
            .equals(InterpreterResumptionPolicy.CONTINUE)) {

            interpreterResult = merger.merge(interpreterResult,
                    this.handleAttachedStereotypes(theEObject, CallScope.RESPONSE));
        }
        
        handler.handleIssues(interpreterResult);

        return interpreterResult;

    }

    /**
     * Searches and calls the registered StereotypeSwitches for the attached Stereotypes
     *
     */
    public InterpreterResult handleAttachedStereotypes(EObject theEObject, CallScope callScope) {
        InterpreterResult interpreterResult = InterpreterResult.OK;

        if (StereotypeAPI.hasStereotypeApplications(theEObject)) {

            EList<Stereotype> appliedStereotypes = StereotypeAPI.getAppliedStereotypes(theEObject);

            Iterator<Stereotype> stereotypeIter = appliedStereotypes.iterator();

            while (stereotypeIter.hasNext() && !handler.handleIssues(interpreterResult)
                .equals(InterpreterResumptionPolicy.ABORT)) {

                Stereotype stereo = stereotypeIter.next();

                if (this.isSwitchRegistered(stereo)) {

                    StereotypeSwitch delegate = this.findDelegate(stereo);

                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Following StereotypeSwitch is called: " + delegate.getClass()
                            .getName());
                    }

                    interpreterResult = merger.merge(interpreterResult,
                            delegate.handleStereotype(stereo, theEObject, callScope));

                }
            }

        }

        return interpreterResult;
    }

    /**
     * Adds StereotypeSwitch to available Switches.
     * 
     * @param sw
     *            To be added StereotypeSwitch
     */
    public void addSwitch(StereotypeSwitch sw) {

        synchronized (switches) {
            if (!switches.contains(sw)) {
                switches.add(sw);
            }
        }

    }

    private StereotypeSwitch findDelegate(Stereotype stereotype) {
        synchronized (switches) {

            StereotypeSwitch delegate = registry.get(stereotype);

            if (delegate == null && !registry.containsKey(stereotype)) {

                if (!switches.isEmpty()) {

                    int i = 0;

                    do {
                        delegate = switches.get(i);
                    } while ((!switches.get(i)
                        .isSwitchForStereotype(stereotype)) && (++i < switches.size()));

                    if (!delegate.isSwitchForStereotype(stereotype)) {
                        delegate = null;
                    }

                }

                // the Switch is registered in registry for next search
                registry.put(stereotype, delegate);

                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StereotypeSwitch is registered: " + delegate.getClass()
                        .getName());
                }

            }
            return delegate;
        }
    }

    private boolean isSwitchRegistered(Stereotype stereo) {
        return this.findDelegate(stereo) != null;
    }

    @Override
    protected boolean isSwitchFor(EPackage ePackage) {
        return ePackage == modelPackage;
    }

}
