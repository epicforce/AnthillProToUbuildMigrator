package net.epicforce.migrate.ahp.toucb.ahp.domain.publisher.artifact;

/*
 * ArtifactDeliverStepConfigMigrate.java
 *
 * Migrate the artifact delivery step to UCB
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;

import com.urbancode.anthill3.domain.publisher.artifact.ArtifactDeliver;
import com.urbancode.anthill3.domain.publisher.artifact.ArtifactDeliverStepConfig;
import com.urbancode.ubuild.client.step.Step;


public class ArtifactDeliverStepConfigMigrate extends UcbStep
{
    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        // Staaaaaation!
        context.ucbHasPlugin("com.urbancode.air.plugin.codestation",
                             "Maven Plugin"
        );

        // Our artifact config
        ArtifactDeliverStepConfig c = (ArtifactDeliverStepConfig)
                                      context.getCurrentStep();

        // Deliver info
        ArtifactDeliver ad = c.getArtifactDeliver();

        // Try to push it to UCB
        try {
            // Build a hashmap of details
            HashMap<String, String> props = new HashMap<>(5);

            // Artifact set
            props.put("setName", ad.getArtifactSet().getName());

            // These are required by UCB, but do not have an
            // AHP analog.
            props.put("symlinks", "AS_LINK");
            props.put("directories", "INCLUDE_ALL");

            // For Anthill, this is hard wired.  We'll maintain the
            // behavior.
            props.put("failIfNotFound", "true");

            /* AHP and UCB have totally different ideas of permission
             * migrations.  My reasoning is as follows:
             *
             * * If Preserve File Owner / Preserve File Group is
             *   set, we will consider the permissions super relevant
             * * If Preserve File Permissions is checked, and the
             *   owner preserve is not checked, we'll use BEST_EFFORT
             *   so it won't error if the owners don't match.
             * * Default to FILE_EXECUTE_ONLY, because that is
             *   pretty harmless and I think a good idea.
             */
            if(ad.isIncludeGroup() || ad.isIncludeOwner()) {
                props.put("permissions", "REQUIRED");
            } else if(ad.isIncludePermissions()) {
                props.put("permissions", "BEST_EFFORT");
            } else {
                props.put("permissions", "FILE_EXECUTE_ONLY");
            }

            // Try to cook it
            Step ucbStep =
                        context.getUcbJob()
                               .createStep(
                                 "Build Systems/CodeStation/Upload Artifacts",
                                 c.getName(),
                                 c.getDescription() == null ?
                                     "" : c.getDescription(),
                                 context.nextUcbStep(),
                                 props
            );

            // Add the common stuff
            copyCommonBits(context, c, ucbStep);

            // Save it
            ucbUpdate(ucbStep);
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            throw new MigrateException("Error while migrating Maven step", e);
        }
    }
}
