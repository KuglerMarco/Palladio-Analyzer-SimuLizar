package org.palladiosimulator.simulizar.interpreter.stereotype;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.Switch;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.seff.SeffPackage;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;
import org.palladiosimulator.simulizar.interpreter.stereotype.RDSeffSwitchStereotypeContributionFactory.RDSeffSwitchElementDispatcher;

/**
 * Dispatch-Switch, which is searching for a available matching StereotypeSwitch to process the
 * attached Stereotypes of the RDSeff element, if there is one.
 * 
 * @author Marco Kugler
 *
 */
public class StereotypeDispatchRDSeffSwitch extends Switch<InterpreterResult> implements RDSeffSwitchElementDispatcher {

    /**
     * Default-Switch after handling the Stereotypes at the system-element
     */
    private Switch<InterpreterResult> rdseffDispatchSwitch;

    /**
     * Registry of registered StereotypeSwitches
     */
    private final Map<Stereotype, StereotypeSwitch> registry = new HashMap<Stereotype, StereotypeSwitch>();

    /**
     * Set of the available StreotypeSwitches
     */
    private final List<StereotypeSwitch> switches = new ArrayList<StereotypeSwitch>();
    
    protected static SeffPackage modelPackage;
    private final InterpreterResultHandler handler;
    private InterpreterResultMerger merger;
    private static final Logger LOGGER = Logger.getLogger(StereotypeDispatchRDSeffSwitch.class);

    public StereotypeDispatchRDSeffSwitch(InterpreterResultMerger merger, InterpreterResultHandler handler,
            Switch<InterpreterResult> rdseffDispatchSwitch) {

        if (modelPackage == null) {
            modelPackage = SeffPackage.eINSTANCE;
        }

        this.merger = merger;
        this.handler = handler;
        this.rdseffDispatchSwitch = rdseffDispatchSwitch;

    }

    /**
     * Processes attached stereotype before and after calling the ComposedStructureInnerSwitch
     * (Request- and Response-Scope)
     */
    @Override
    public InterpreterResult doSwitch(EClass theEClass, EObject theEObject) {

        InterpreterResult interpreterResult = InterpreterResult.OK;

        // Stereotype-Handling in Request-Scope
        interpreterResult = this.handleAttachedStereotypes(theEObject, CallScope.REQUEST);

        if (handler.handleIssues(interpreterResult)
            .equals(InterpreterResumptionPolicy.CONTINUE)) {
            // Default-Switch
            interpreterResult = merger.merge(interpreterResult, rdseffDispatchSwitch.doSwitch(theEObject));
        }

        // Stereotype-Handling in Response-Scope
        interpreterResult = merger.merge(interpreterResult,
                this.handleAttachedStereotypes(theEObject, CallScope.RESPONSE));

        // called to get access on information about the issues present in the InterpreterResult
        handler.handleIssues(interpreterResult);

        return interpreterResult;

    }

    /**
     * Searches and calls the registered StereotypeSwitches for the attached Stereotypes
     * 
     * @param theEObject
     *            The EObject to be analysed.
     * @param callScope
     *            Request-Scope or Response-Scope
     * @return
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
     * Checks whether there's any StereotypeSwitch for this Stereotype registered.
     * 
     * @param stereotype
     *            Stereotype, for which it needs to find a StereotypeSwitch
     * @return Boolean, whether there's one StereotypeSwitch registered
     */
    public boolean isSwitchRegistered(Stereotype stereotype) {

        return this.findDelegate(stereotype) != null;

    }

    /**
     * Checks whether this is a switch for given package.
     * 
     * @param ePackage
     *            The package in question.
     * @return Boolean, whether this is a switch for the given package.
     */
    @Override
    protected boolean isSwitchFor(EPackage ePackage) {
        return ePackage == modelPackage;
    }

    /**
     * Adds StereotypeSwitch to registered Switches.
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

    /**
     * Finds the right registered Switch for this Stereotype.
     * 
     * @param stereotype
     *            Stereotype, which needs to be handled
     * @return StereotypeSwitch, which can handle the Stereotype
     */
    protected StereotypeSwitch findDelegate(Stereotype stereotype) {

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

}
