package net.epicforce.migrate.ahp.toucb.ahp.domain.builder.ant;

/*
 * AntBuildStepConfigMigrate.java
 *
 * Class to migrate ant build steps from AHP to UCB.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;

import com.urbancode.anthill3.domain.builder.NameValuePair;
import com.urbancode.anthill3.domain.builder.ant.AntBuilder;
import com.urbancode.anthill3.domain.builder.ant.AntBuildStepConfig;
import com.urbancode.ubuild.client.step.Step;


public class AntBuildStepConfigMigrate extends UcbStep
{
    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        // Require the plugin
        context.ucbHasPlugin("com.urbancode.air.plugin.Ant", "Ant Plugin");

        // Get our bits.
        AntBuildStepConfig c = (AntBuildStepConfig)context.getCurrentStep();
        AntBuilder ab = c.getBuilder();

        try {
            // Collect our details into a hashmap
            HashMap<String, String> props = new HashMap<>(10);

            // Rack 'em up!
            props.put("workDirOffset", ab.getWorkDirOffset());

            // Move over required fields
            props.put("antFileName", ab.getBuildFilePath());

            // Targets, if we have them
            if(ab.getTarget() != null) {
                props.put("targetNames", ab.getTarget());
            }

            // Properties if we have 'em
            if(ab.getAntParams() != null) {
                props.put("antProperties", ab.getAntParams());
            }

            // Ant home if we have it
            if(ab.getAntHomeVar() != null) {
                props.put("antHome", ab.getAntHomeVar());
            } else { // required in UCB but not AHP
                props.put("antHome", "${env/ANT_HOME}");
            }

            // And jvm
            if(ab.getJavaHomeVar() != null) {
                props.put("javaHome", ab.getJavaHomeVar());
            } else { // This is required in UCB but not AHP
                props.put("javaHome", "${env/JAVA_HOME}");
            }

            // JVM props
            if(ab.getJvmParams() != null) {
                props.put("antOpts", ab.getJvmParams());
            }

            // Script content, if set.
            if(ab.getScriptContent() != null) {
                props.put("scriptContent", ab.getScriptContent());
            }

            // Environment
            StringBuilder sb = new StringBuilder(1024);

            for(NameValuePair nvp : ab.getEnvironmentVariableArray()) {
                sb.append(nvp.getName());
                sb.append("=");
                sb.append(nvp.getValue());
                sb.append("\n");
            }

            props.put("env-vars", sb.toString());

            // "Command line properties"
            sb.setLength(0);

            for(String param : ab.getBuildParamArray()) {
                sb.append(param).append("\n");
            }

            props.put("properties", sb.toString());

            // Try to cook it
            Step ucbStep = context.getUcbJob()
                                  .createStep(
                                    "Build/Build Tools/Ant/Ant",
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
            throw new MigrateException("Error while migrating Ant step", e);
        }
    }
}
