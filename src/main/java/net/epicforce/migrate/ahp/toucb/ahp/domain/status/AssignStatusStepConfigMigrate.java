package net.epicforce.migrate.ahp.toucb.ahp.domain.status;

/*
 * AssignStatusStepConfigMigrate.java
 *
 * Migrate an Assign Status step.
 *
 * This is another built-in step similar to stamp which is poorly supported
 * by the uBuild API.  Therefore, like Stamp, we have to actually use the
 * form post.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.Map;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;
import net.epicforce.migrate.ahp.toucb.context.UcbWorkflow;
import net.epicforce.migrate.ahp.toucb.ucb.DummyStep;
import net.epicforce.migrate.ahp.toucb.ucb.UcbClient;

import com.urbancode.anthill3.domain.status.AssignStatusStepConfig;


public class AssignStatusStepConfigMigrate extends UcbStep
{
    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        AssignStatusStepConfig c = (AssignStatusStepConfig)
                                   context.getCurrentStep();

        // We can't support status selection script.
        if((c.getStatusSelectionScript() != null) &&
           (c.getStatusSelectionScript().length() > 0)) {
            throw new MigrateException(
                "UCB does not support status selection scripts.  " +
                "We cannot migrate this job."
            );
        }

        // Get our status ID
        String statusId = UcbWorkflow.getStatusId(c.getStatus().getName());

        // We have to submit this as POST.  This is pretty straight forward
        Map<String, String> newStep = createCommonPostParams(
            "com.urbancode.ubuild.domain.status.AssignStatusStepConfig",
            context, c
        );

        // add our unique stuff
        newStep.put("statusId", statusId);

        // Ship it
        saveStepClient(newStep, context);
    }
}
