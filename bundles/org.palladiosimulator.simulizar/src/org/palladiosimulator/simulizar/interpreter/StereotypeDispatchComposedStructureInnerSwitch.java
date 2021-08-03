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
import org.palladiosimulator.pcm.core.composition.CompositionPackage;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchStereotypeContributionFactory.ComposedStructureInnerSwitchStereotypeElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultHandler;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResultMerger;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;



/**
 * Dispatch searching for the right StereotypeSwitch to handle attached Stereotypes at ComposedStructure-elements.
 * Classes implementing the ComposedStrucutreInnerSwitchStereotypeContributionFactory are registered here and called, if the relevant Stereotype
 * is attached to the element of a ComposedStructure.
 * 
 * @author Marco Kugler
 *
 */
public class StereotypeDispatchComposedStructureInnerSwitch extends Switch<InterpreterResult> implements ComposedStructureInnerSwitchStereotypeElementDispatcher {
    

   
    /**
     * Default-Switch after handling the Stereotypes at the system-element
     */
    private ComposedStructureInnerSwitch composedStructureInnerSwitch;
    
    /**
     * Registry of registered StereotypeSwitches
     */
    private final Map<Stereotype, StereotypeSwitch> registry = new HashMap<Stereotype, StereotypeSwitch>();
    
    /**
     * Set of StreotypeSwitches inheriting from ComposedStructureInnerSwitchContributionFactory
     */
    private final List<StereotypeSwitch> switches = new ArrayList<StereotypeSwitch>();
    
    protected static CompositionPackage modelPackage;
    
    private final InterpreterResultHandler handler;
    
    private InterpreterResultMerger merger;
    
    private static final Logger LOGGER = Logger.getLogger(StereotypeDispatchComposedStructureInnerSwitch.class);
    
    public StereotypeDispatchComposedStructureInnerSwitch(InterpreterResultMerger merger, InterpreterResultHandler handler, ComposedStructureInnerSwitch composedStructureInnerSwitch) {
        
        if (modelPackage == null) {
            modelPackage = CompositionPackage.eINSTANCE;
        }
        
        this.merger = merger;
        this.handler = handler;
        
        LOGGER.setLevel(Level.DEBUG);
        
        this.composedStructureInnerSwitch = composedStructureInnerSwitch;
        
    }
    
    

    
    
    
    
    /**
     * Processes attached stereotype before and after calling the ComposedStructureInnerSwitch (Request- and Response-Scope)
     */
    @Override
    public InterpreterResult doSwitch(EClass theEClass, EObject theEObject) {
        
        InterpreterResult interpreterResult = InterpreterResult.OK;
        
        //Stereotype-Handling in Request-Scope
        interpreterResult = this.handleAttachedStereotypes(theEObject, CallScope.REQUEST);
        
        if(handler.handleIssues(interpreterResult).equals(InterpreterResumptionPolicy.CONTINUE)) {
            //Default-Switch
            interpreterResult = merger.merge(interpreterResult, composedStructureInnerSwitch.doSwitch(theEObject));
        }
        
        
        if(handler.handleIssues(interpreterResult).equals(InterpreterResumptionPolicy.CONTINUE)) {
            //Stereotype-Handling in Response-Scope
            interpreterResult = merger.merge(interpreterResult, this.handleAttachedStereotypes(theEObject, CallScope.RESPONSE));
        }
        
        
//        if (LOGGER.isDebugEnabled()) {
//            ArrayList<InterpretationIssue> list1 = Lists.newArrayList(interpreterResult.getIssues());
//            for(InterpretationIssue e : list1) {
//                if(e instanceof ParameterIssue) {
//                    LOGGER.debug("(StereotypeDispatchComposedStructureInnerSwitch, doSwitch) StackContents der ParameterIssues: " + ((ParameterIssue) e).getStackContent());
//                }
//            }
//        }
        
        return interpreterResult;
        
    }
    
    
    /**
     * Searches and calls the registered StereotypeSwitches for the attached Stereotypes
     * 
     * @param theEObject
     *          The EObject to be analysed.
     * @param callScope
     *          Request-Scope or Response-Scope
     * @return
     */
    public InterpreterResult handleAttachedStereotypes(EObject theEObject, CallScope callScope) {
        
        InterpreterResult interpreterResult = InterpreterResult.OK;
        
        
        if(StereotypeAPI.hasStereotypeApplications(theEObject)) {
            
            EList<Stereotype> appliedStereotypes = StereotypeAPI.getAppliedStereotypes(theEObject);
            
            Iterator<Stereotype> stereotypeIter = appliedStereotypes.iterator();
            
            while(stereotypeIter.hasNext() && !handler.handleIssues(interpreterResult).equals(InterpreterResumptionPolicy.ABORT)) {
                
                Stereotype stereo = stereotypeIter.next();
                
                if(this.isSwitchRegistered(stereo)) {
                    
                    StereotypeSwitch delegate = this.findDelegate(stereo);
                    
                    if (LOGGER.isDebugEnabled()) {
                        LOGGER.debug("Following StereotypeSwitch is called: " + delegate.getClass().getName());
                    }
                    
                    interpreterResult = merger.merge(interpreterResult, delegate.handleStereotype(stereo, theEObject, callScope));
                    
                } 
            }
            
        }
        
        return interpreterResult;
    }
    
    

    
    /**
     * Checks whether there's any StereotypeSwitch for this Stereotype registered.
     * 
     * @param stereotype
     *              Stereotype, for which it needs to find a StereotypeSwitch
     * @return
     *              Boolean, whether there's one StereotypeSwitch registered
     */
    public boolean isSwitchRegistered(Stereotype stereotype) {
        
        return this.findDelegate(stereotype) != null;
        
    }
    
    
    /**
     * Checks whether this is a switch for given package.
     * 
     * @param ePackage 
     *              The package in question.
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
     *              To be added StereotypeSwitch
     */
    public void addSwitch(StereotypeSwitch sw) {
        
        synchronized (switches) {
          if (!switches.contains(sw)){
            switches.add(sw);
          }
        }
        
    }

    
    /**
     * Finds the right registered Switch for this Stereotype.
     * 
     * @param stereotype 
     *              Stereotype, which needs to be handled
     * @return 
     *              StereotypeSwitch, which can handle the Stereotype
     */
    protected StereotypeSwitch findDelegate(Stereotype stereotype) {
      
        synchronized (switches) {
            
            StereotypeSwitch delegate = registry.get(stereotype);
            
            if (delegate == null && !registry.containsKey(stereotype)) {
                
                if(!switches.isEmpty()) {
                    
                    int i = 0;
                    
                    do {
                        delegate = switches.get(i);
                    } while ((!switches.get(i).isSwitchForStereotype(stereotype)) && (++i < switches.size()));
                    
                    if(!delegate.isSwitchForStereotype(stereotype)) {
                        delegate = null;
                    }

                }
                
                //the Switch is registered in registry for next search
                registry.put(stereotype, delegate);
                
                if (LOGGER.isDebugEnabled()) {
                    LOGGER.debug("Following StereotypeSwitch is registered: " + delegate.getClass().getName());
                }
                

            }
            return delegate;
      }
    }

}
