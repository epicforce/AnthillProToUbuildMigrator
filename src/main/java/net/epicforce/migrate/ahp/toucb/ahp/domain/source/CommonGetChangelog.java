package net.epicforce.migrate.ahp.toucb.ahp.domain.source;

/*
 * CommonGetChangelog.java
 *
 * AHP uses a different class for every kind of source cleanup task.
 * UCB only uses one.  This is common code for all of them.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;

import com.urbancode.anthill3.domain.source.GetChangelogStepConfig;
import com.urbancode.ubuild.client.step.Step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommonGetChangelog extends CommonSource
{
    private final static Logger LOG = 
                            LoggerFactory.getLogger(CommonGetChangelog.class);

    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context)
           throws MigrateException
    {
        GetChangelogStepConfig c = (GetChangelogStepConfig)
                                   context.getCurrentStep();

        try {
            // The hash map is used to set properties on the
            // step, which are in turn used to build the step.
            HashMap<String, String> props = new HashMap<String, String>(3);

            // We can't migrate script instead of "simple selection"
            if((c.getStartStatusScript() != null) &&
               (c.getStartStatusScript().length() != 0)) {
                throw new MigrateException(
                    "The Get Change Log step uses a script to determine " +
                    "starting status.  UCB does not support this, so we " +
                    "cannot migrate this job."
                );
            }

            // We can handle anything else.
            if(c.getStartStatus() != null) {
                props.put("statusName", c.getStartStatus().getName());
            }

            if(c.getStartStampPattern() != null) {
                props.put("stampValue", c.getStartStampPattern());
            }

            // UCB requires at least one of htese
            if((c.getStartStatus() == null) &&
               ((c.getStartStampPattern() == null) ||
                (c.getStartStampPattern().length() == 0))) {
                throw new MigrateException(
                    "Get Changelog step has neither Start Status nor " +
                    "start stamp pattern.  UCB requires one of these be set."
                );
            }

            if(c.isPersistingChanges()) {
                props.put("persistingChanges", "true");
            } else {
                props.put("persistingChanges", "false");
            }

            // Create our UCB step
            Step ucbStep = context.getUcbJob()
                                  .createStep(
                                    "Source/Get Source Changes",
                                    c.getName(),
                                    c.getDescription() == null ?
                                        "" : c.getDescription(),
                                    context.nextUcbStep(),
                                    props
            );

            // Add common stuff
            copyCommonBits(context, c, ucbStep);

            // Save it
            ucbUpdate(ucbStep);
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            // Cause UCB
            throw new MigrateException("Error while migrating step", e);
        }
    }
}
