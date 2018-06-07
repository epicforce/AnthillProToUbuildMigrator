package net.epicforce.migrate.ahp.toucb.ahp.domain.builder.nant;

/*
 * NantBuildStepConfigMigrate.java
 *
 * Class to migrate nant build steps from AHP to UCB.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;

import com.urbancode.anthill3.domain.builder.NameValuePair;
import com.urbancode.anthill3.domain.builder.nant.NantBuilder;
import com.urbancode.anthill3.domain.builder.nant.NantBuildStepConfig;
import com.urbancode.ubuild.client.step.Step;


public class NantBuildStepConfigMigrate extends UcbStep
{
    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        // Require the plugin
        context.ucbHasPlugin("com.urbancode.air.plugin.nant", "NAnt Plugin");

        // Get our bits.
        NantBuildStepConfig c = (NantBuildStepConfig)context.getCurrentStep();
        NantBuilder nb = c.getBuilder();

        try {
            // Collect our details into a hashmap
            HashMap<String, String> props = new HashMap<>(10);

            // Rack 'em up!
            props.put("workDirOffset", nb.getWorkDirOffset());

            // Move over required fields
            props.put("nantFile", nb.getBuildFilePath());

            // This is required by UCB but optional in AHP.
            // Provide a reasonable default.
            if(nb.getNantHomeVar() != null) {
                props.put("nantHome", nb.getNantHomeVar());
            } else {
                props.put("nantHome", "${NANT_HOME}");
            }

            // Targets, if we have them
            if(nb.getTarget() != null) {
                props.put("targets", nb.getTarget());
            }

            // Properties if we have 'em
            if(nb.getNantParams() != null) {
                props.put("properties", nb.getNantParams());
            }

            // And Mono
            if(nb.getMonoHomeVar() != null) {
                props.put("monoHome", nb.getMonoHomeVar());
            }

            // Script content, if set.
            if(nb.getScriptContent() != null) {
                props.put("scriptContent", nb.getScriptContent());
            }

            // Environment
            StringBuilder sb = new StringBuilder(1024);

            for(NameValuePair nvp : nb.getEnvironmentVariableArray()) {
                sb.append(nvp.getName());
                sb.append("=");
                sb.append(nvp.getValue());
                sb.append("\n");
            }

            props.put("envVars", nb.toString());

            // "Command line properties"
            sb.setLength(0);

            for(String param : nb.getBuildParamArray()) {
                sb.append(param).append("\n");
            }

            props.put("cmdProps", sb.toString());

            // Try to cook it
            Step ucbStep = context.getUcbJob()
                                  .createStep(
                                    "Build/Build Tools/NAnt/Run NAnt",
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
            throw new MigrateException("Error while migrating Nant step", e);
        }
    }
}
