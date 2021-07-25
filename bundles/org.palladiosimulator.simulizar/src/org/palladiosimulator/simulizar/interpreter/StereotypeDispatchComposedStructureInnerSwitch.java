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

/**
 * Dispatch for handling the Stereotypes attached to System-Model.
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
     * Calls first the Stereotype-Switches for the Stereotypes attached to the element.
     * Then calls the default ComposedStructureInnerSwitch.
     */
    @Override
    public InterpreterResult doSwitch(EClass theEClass, EObject theEObject) {
        
        InterpreterResult interpreterResult = InterpreterResult.OK;
        
        
        //Stereotype-Handling in Request-Scope
        interpreterResult = this.handleAttachedStereotypes(theEObject, CallScope.REQUEST);
        

        //Default-Switch
        interpreterResult = composedStructureInnerSwitch.doSwitch(theEObject);
        
        
        //Stereotype-Handling in Response-Scope
        interpreterResult = this.handleAttachedStereotypes(theEObject, CallScope.RESPONSE);
        
        
        
        return interpreterResult;
        
    }
    
    
    public InterpreterResult handleAttachedStereotypes(EObject theEObject, CallScope callScope) {
        
        InterpreterResult interpreterResult = InterpreterResult.OK;
        
        //Handling the Stereotypes (Request)
        if(StereotypeAPI.hasStereotypeApplications(theEObject)) {
            
            EList<Stereotype> appliedStereotypes = StereotypeAPI.getAppliedStereotypes(theEObject);
            
            for(Stereotype stereotype : appliedStereotypes) {
                
                if(this.isSwitchRegistered(stereotype)) {
                    
                    StereotypeQualitygateSwitch delegate = (StereotypeQualitygateSwitch) this.findDelegate(stereotype);
                    interpreterResult = delegate.handleStereotype(stereotype, theEObject, callScope);
                    
                } 
            }
        }
        
        return interpreterResult;
    }
    
    

    
    /**
     * Checks whether there's any registered StereotypeSwitch for this Stereotype.
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
     * @param ePackage the package in question.
     * @return whether this is a switch for the given package.
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
