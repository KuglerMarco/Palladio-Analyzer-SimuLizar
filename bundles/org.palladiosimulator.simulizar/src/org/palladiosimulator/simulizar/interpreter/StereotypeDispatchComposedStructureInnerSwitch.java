package org.palladiosimulator.simulizar.interpreter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.util.Switch;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.mdsdprofiles.api.StereotypeAPI;
import org.palladiosimulator.pcm.repository.RequiredRole;
import org.palladiosimulator.pcm.repository.Signature;
import org.palladiosimulator.simulizar.interpreter.ComposedStructureInnerSwitchContributionFactory.ComposedStructureInnerSwitchElementDispatcher;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

import dagger.assisted.Assisted;
import dagger.assisted.AssistedInject;

public class StereotypeDispatchComposedStructureInnerSwitch extends Switch<InterpreterResult> implements ComposedStructureInnerSwitchElementDispatcher {
    

    
    //default-Switch, an den wieder delegiert wird
    private ComposedStructureInnerSwitch composedStructureInnerSwitch;
    
    //identifiziert werden die Switches mithilfe eines Stereotype-Feldes (noch nicht ganz sicher, vllt auch nur String?)
    private final Map<Stereotype, StereotypeSwitch> registry = new HashMap<Stereotype, StereotypeSwitch>();
    
    //hier werden die Switches registriert (Menge an verfügbaren Stereotype-Switches)
    private final List<StereotypeSwitch> switches = new ArrayList<StereotypeSwitch>();
    
    public StereotypeDispatchComposedStructureInnerSwitch() {
    }
    
    public void setDefaultSwitch(ComposedStructureInnerSwitch composedStructureInnerSwitch) {
        this.composedStructureInnerSwitch = composedStructureInnerSwitch;
    }
    
    
    @Override
    public InterpreterResult doSwitch(EClass theEClass, EObject theEObject) {
        /*
         * erstmal nur von hier aus den Default-Switch aufrufen, dann  später die Funktionalität
         * für die Auswahl des richtigen Switches implementieren
         * 
         * if für den angehängten Stereotyp ein Switch vorhanden, rufe den entsprechenden Switch auf und verarbeite diesen
         * falls nicht, default-Switch aufrufen
         * 
         * von dem Qualitygate Switch dann den default Switch aufrufen
         */
        
        /*
         * Hier in einer Schleife alle Switches der Stereotypes aufrufen (Alle Switches aus der Map, für die es ein Stereotype an dem Element gibt
         * Sollte immer wieder zurückkehren
         * Danach wird dann der Default-Switch aufgerufen
         */
        
        StereotypeSwitch delegate = findDelegate(StereotypeAPI.getAppliedStereotypes(theEObject).get(0));
        
        if(StereotypeAPI.hasStereotypeApplications(theEObject)) {
            System.out.println("Has Stereotype." + StereotypeAPI.getAppliedStereotypes(theEObject).get(0));
            
        }
        
        
        return composedStructureInnerSwitch.doSwitch(theEObject);
        
    }

    @Override
    protected boolean isSwitchFor(EPackage ePackage) {
        // TODO Auto-generated method stub
        return false;
    }
    
    

    public void addSwitch(StereotypeSwitch sw) {
        
        synchronized (switches) {
          if (!switches.contains(sw)){
            switches.add(sw);
          }
        }
    }
    
    protected StereotypeSwitch findDelegate(Stereotype stereotype)
    {
      synchronized (switches)
      {
        StereotypeSwitch delegate = registry.get(stereotype);
        if (delegate == null && !registry.containsKey(stereotype))
        {
          for (StereotypeSwitch sw : switches)
          {
            if (sw.isSwitchForStereotype(stereotype))
            {
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
