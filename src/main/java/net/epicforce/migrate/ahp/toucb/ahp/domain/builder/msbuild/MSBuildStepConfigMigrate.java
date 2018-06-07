package net.epicforce.migrate.ahp.toucb.ahp.domain.builder.msbuild;

/*
 * MSBuildStepConfigMigrate.java
 *
 * Class to migrate MS build steps from AHP to UCB.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;

import com.urbancode.anthill3.domain.builder.NameValuePair;
import com.urbancode.anthill3.domain.builder.msbuild.MSBuildStepConfig;
import com.urbancode.ubuild.client.step.Step;


public class MSBuildStepConfigMigrate extends UcbStep
{
    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        // Require the plugin
        context.ucbHasPlugin("com.urbancode.air.plugin.MSBuild",
                             "MSBuild Plugin"
        );

        // Get our bits.
        MSBuildStepConfig mb = (MSBuildStepConfig)context.getCurrentStep();

        try {
            // Collect our details into a hashmap
            HashMap<String, String> props = new HashMap<>(10);

            // Rack 'em up!
            props.put("workDirOffset", mb.getWorkDirOffset());

            // Move over required fields
            props.put("buildFile", mb.getBuildFilePath());

            // Everything else is optional

            // Command Path
            if(mb.getMSBuildExePath() != null) {
                props.put("cmdPath", mb.getMSBuildExePath());
            }

            // Targets, if we have them
            if(mb.getTargets() != null) {
                props.put("targets", mb.getTargets());
            }

            // Script content, if set.
            if(mb.getScriptContent() != null) {
                props.put("scriptContent", mb.getScriptContent());
            }

            // Verbosity
            if(mb.getVerbosity() != null) {
                props.put("verbosity", mb.getVerbosity()
                                         .getName()
                                         .toLowerCase()
                );
            } else {
                props.put("verbosity", "default");
            }

            // Environment
            StringBuilder sb = new StringBuilder(1024);

            for(NameValuePair nvp : mb.getEnvironmentVariableArray()) {
                sb.append(nvp.getName());
                sb.append("=");
                sb.append(nvp.getValue());
                sb.append("\n");
            }

            props.put("envVars", sb.toString());

            // "Command line properties"
            sb.setLength(0);

            for(String param : mb.getBuildParamArray()) {
                sb.append(param).append("\n");
            }

            props.put("buildParams", sb.toString());

            // "Build properties"
            sb.setLength(0);

            for(NameValuePair nvp : mb.getPropertyArray()) {
                sb.append(nvp.getName());
                sb.append("=");
                sb.append(nvp.getValue());
                sb.append("\n");
            }

            props.put("buildProps", sb.toString());

            // Try to cook it
            Step ucbStep = context.getUcbJob()
                                  .createStep(
                                    "Build/Build Tools/MSBuild/Run MSBuild",
                                    mb.getName(),
                                    mb.getDescription() == null ?
                                        "" : mb.getDescription(),
                                    context.nextUcbStep(),
                                    props
            );

            // Add the common stuff
            copyCommonBits(context, mb, ucbStep);

            // Save it
            ucbUpdate(ucbStep);
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            throw new MigrateException("Error while migrating MSBuild step", e);
        }
    }
}
