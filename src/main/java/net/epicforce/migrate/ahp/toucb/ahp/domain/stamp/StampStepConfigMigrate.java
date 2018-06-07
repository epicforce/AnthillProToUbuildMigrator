package net.epicforce.migrate.ahp.toucb.ahp.domain.stamp;

/*
 * StampStepConfigMigrate.java
 *
 * Migrate a Create Stamp step.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.Map;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;

import com.urbancode.anthill3.domain.profile.NewStampGenerator;
import com.urbancode.anthill3.domain.stamp.StampStepConfig;


public class StampStepConfigMigrate extends UcbStep
{
    /**
     * Run a stamp step migrate; pull the data out of AHP and put it
     * in UCB.
     *
     * @param context           The context
     * @throws MigrateException on any error
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        StampStepConfig c = (StampStepConfig)context.getCurrentStep();

        // Get our stamp script
        if(c.getStampStyle() == null) {
            // This isn't spuported by UCB
            throw new MigrateException(
                "Your Anthill stamp script uses a stamping style " +
                "script.  This isn't supported by UCB."
            );
        }

        NewStampGenerator gen = context.getWorkflow()
                                       .getBuildProfile()
                                       .getGeneratorForStyle(
                                            c.getStampStyle()
        );

        // This also isn't supported by UCB
        if(gen.getContextScript() != null) {
            throw new MigrateException(
                "Your Anthill stamping stategy uses a context " +
                "script; UCB does not support this, and so we " +
                "cannot automatically migrate this job."
            );
        }

        // gen.getValue will have our stamp script
        Map<String, String> newStep = createCommonPostParams(
                    "com.urbancode.ubuild.domain.stamp.StampStepConfig",
                    context, c
        );

        // Add our script
        newStep.put("stampValue", gen.getValue());

        saveStepClient(newStep, context);
    }
}
