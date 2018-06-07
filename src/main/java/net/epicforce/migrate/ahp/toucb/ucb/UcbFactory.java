package net.epicforce.migrate.ahp.toucb.ucb;

/*
 * UcbFactory.java
 *
 * This is class to override "Factories" from the UCB client.
 *
 * It patches various broken calls that are UCB bugs / deficiencies
 * necessary to do the migration.
 *
 * @author sconley (sconley@epicforce.net)
 */

import com.urbancode.ubuild.client.Factories;
import com.urbancode.ubuild.client.script.precondition.step.StepPreConditionScriptFactory;

// This is lazy, but I'm grumpy and these 'fixed' classes are
// the entire reason we're here.
import net.epicforce.migrate.ahp.toucb.ucb.fixed.*;

public class UcbFactory extends Factories
{
    /*
     * This mimics the internal structure of Factories
     */
    private static RESTStepPreConditionScriptFactory fixedPreFac = null;

    /**
     * (@inheritdoc)
     */
    public static void setStepPreConditionScriptFactory(
                            StepPreConditionScriptFactory x)
    {
        // discard it
        fixedPreFac = new RESTStepPreConditionScriptFactory();
    }

    /**
     * (@inheritdoc)
     */
    public static StepPreConditionScriptFactory
                                            getStepPreConditionScriptFactory()
    {
        if(fixedPreFac == null) {
            setStepPreConditionScriptFactory(null);
        }

        return fixedPreFac;
    }
}
