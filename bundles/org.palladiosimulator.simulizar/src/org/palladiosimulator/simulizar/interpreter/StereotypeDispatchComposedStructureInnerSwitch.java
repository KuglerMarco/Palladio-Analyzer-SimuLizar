package org.palladiosimulator.simulizar.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResumptionPolicy;
import org.palladiosimulator.simulizar.interpreter.result.impl.NoIssuesHandler;

/**
 * Dispatch for handling the Stereotypes attached to Composed Structure.
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
    
    
    NoIssuesHandler handler = new NoIssuesHandler();
    
    
    /**
     * Creates instance.
     */
    public StereotypeDispatchComposedStructureInnerSwitch(InterpreterDefaultContext context) {
        if (modelPackage == null) {
            modelPackage = CompositionPackage.eINSTANCE;
        }
        
    }
    
    
    /**
     * Setting the DefaultSwitch, in this case ComposedStructureInnerSwitch.
     * @param composedStructureInnerSwitch
     */
    public void setDefaultSwitch(ComposedStructureInnerSwitch composedStructureInnerSwitch) {
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
            interpreterResult = composedStructureInnerSwitch.doSwitch(theEObject);
        }
        
        if(handler.handleIssues(interpreterResult).equals(InterpreterResumptionPolicy.CONTINUE)) {
            //Stereotype-Handling in Response-Scope
            interpreterResult = this.handleAttachedStereotypes(theEObject, CallScope.RESPONSE);
        }
        
        
        
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
            
            
            for(Stereotype stereotype : appliedStereotypes) {
                
                if(this.isSwitchRegistered(stereotype)) {
                    
                    StereotypeSwitch delegate = this.findDelegate(stereotype);
                    interpreterResult = delegate.handleStereotype(stereotype, theEObject, callScope);
                    
                } 
                
                
                if(handler.handleIssues(interpreterResult).equals(InterpreterResumptionPolicy.ABORT)) {
                    break;
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
     * Finds the right registered Switch to handle the Stereotype.
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
              
                for (StereotypeSwitch sw : switches) {
                    
                if (sw.isSwitchForStereotype(stereotype)) {
                  delegate = sw;
                  break;
                }
              }
              registry.put(stereotype, delegate);
            }
            return delegate;
      }
    }

}
