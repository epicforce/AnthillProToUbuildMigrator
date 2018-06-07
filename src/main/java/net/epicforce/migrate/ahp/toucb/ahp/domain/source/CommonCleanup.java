package net.epicforce.migrate.ahp.toucb.ahp.domain.source;

/**
 * CommonCleanup.java
 *
 * AHP uses a different class for every kind of source cleanup
 * task.
 *
 * UCB only uses one.
 *
 * This is the common code for all of them.
 *
 * @author sconley
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;

import com.urbancode.anthill3.domain.step.StepConfig;
import com.urbancode.ubuild.client.step.Step;

public class CommonCleanup extends CommonSource
{
    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context)
           throws MigrateException
    {
        StepConfig c = context.getCurrentStep();

        try {
            // This is a pretty simple one
            Step ucbStep = context.getUcbJob()
                                  .createStep(
                                    "Source/Clean Workspace",
                                    c.getName(),
                                    c.getDescription() == null ?
                                        "" : c.getDescription(),
                                    context.nextUcbStep(),
                                    new HashMap<String, String>()
            );

            copyCommonBits(context, c, ucbStep);

            ucbUpdate(ucbStep);
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            // Cause UCB
            throw new MigrateException("Error while migrating step", e);
        }
    }
}
