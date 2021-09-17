package org.palladiosimulator.simulizar.interpreter.stereotype;

import org.eclipse.emf.ecore.EObject;
import org.modelversioning.emfprofile.Stereotype;
import org.palladiosimulator.simulizar.interpreter.result.InterpreterResult;

/**
 * All contributing Stereotype-Switches need to inherit from this interface.
 * 
 * @author Marco Kugler
 *
 */
public interface StereotypeSwitch {

    /**
     * Checks whether this is the StereotypeSwitch for stereotype.
     * 
     * @param stereotype
     * @return Boolean, whether Switch for Stereotype.
     */
    public boolean isSwitchForStereotype(Stereotype stereotype);

    /**
     * Handles the Stereotype attached to the model element.
     * 
     * @param stereotype
     *            The specific stereotype which needs to be handled by this StereotypeSwitch
     * @param theEObject
     *            The EObject with the attached stereotype
     * @param callScope
     *            The time of evaluation (request or response)
     * @return InterpreterResult
     */
    public InterpreterResult handleStereotype(Stereotype stereotype, EObject theEObject, CallScope callScope);

    /**
     * 
     * @return the name of the processed stereotype
     */
    public String getStereotypeName();

    /**
     * 
     * @return the name of the profile, whose stereotype is processed in this StereotypeSwitch
     */
    public String getProfileName();

}
