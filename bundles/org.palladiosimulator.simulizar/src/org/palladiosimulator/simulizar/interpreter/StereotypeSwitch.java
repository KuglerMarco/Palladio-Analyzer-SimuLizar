package org.palladiosimulator.simulizar.interpreter;

import org.eclipse.emf.ecore.EObject;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

public interface StereotypeSwitch {
    
    public boolean isSwitchForStereotype(Stereotype stereotype);
    
    public InterpreterResult handleStereotype(Stereotype stereotype, EObject theEObject);

}
