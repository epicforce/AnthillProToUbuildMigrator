package net.epicforce.migrate.ahp.toucb.ahp.domain.builder.make;

/*
 * MakeBuildStepConfigMigrate.java
 *
 * Migrate a 'make' build step
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;

import com.urbancode.anthill3.domain.builder.NameValuePair;
import com.urbancode.anthill3.domain.builder.make.MakeBuilder;
import com.urbancode.anthill3.domain.builder.make.MakeBuildStepConfig;
import com.urbancode.ubuild.client.step.Step;


public class MakeBuildStepConfigMigrate extends UcbStep
{
    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        // Require the plugin
        context.ucbHasPlugin("com.urbancode.air.plugin.make", "Make Plugin");

        // Get our bits.
        MakeBuildStepConfig c = (MakeBuildStepConfig)context.getCurrentStep();
        MakeBuilder mb = c.getBuilder();

        try {
            // Collect our details into a hashmap
            HashMap<String, String> props = new HashMap<>(6);

            // Rack 'em up!
            props.put("workDirOffset", mb.getWorkDirOffset());

            // No fields are required on either AHP or UCB
            if(mb.getMakeFileName() != null) {
                props.put("makeFileName", mb.getMakeFileName());
            }

            if(mb.getBuildParams() != null) {
                props.put("makeArgs", mb.getBuildParams());
            }

            if(mb.getTarget() != null) {
                props.put("makeTargets", mb.getTarget());
            }

            // Environment
            StringBuilder sb = new StringBuilder(1024);

            for(NameValuePair nvp : mb.getEnvironmentVariableArray()) {
                sb.append(nvp.getName());
                sb.append("=");
                sb.append(nvp.getValue());
                sb.append("\n");
            }

            // Make looks like it handles these differently
            // Usually it's env-vars
            props.put("envVars", sb.toString());

            // Try to cook it
            Step ucbStep = context.getUcbJob()
                                  .createStep(
                                    "Build/Build Tools/Make/Run Make File",
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
            throw new MigrateException("Error while migrating Make step", e);
        }
    }
}
