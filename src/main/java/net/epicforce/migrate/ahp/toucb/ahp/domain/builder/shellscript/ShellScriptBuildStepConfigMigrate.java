package net.epicforce.migrate.ahp.toucb.ahp.domain.builder.shellscript;

/*
 * ShellScriptBuildStepConfigMigrate.java
 *
 * Class to migrate shell script build steps from AHP to UCB.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;

import com.urbancode.anthill3.domain.builder.NameValuePair;
import com.urbancode.anthill3.domain.builder.shellscript.ShellScriptBuilder;
import com.urbancode.anthill3.domain.builder.shellscript.ShellScriptBuildStepConfig;
import com.urbancode.ubuild.client.step.Step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ShellScriptBuildStepConfigMigrate extends UcbStep
{
    private final static Logger LOG =
            LoggerFactory.getLogger(ShellScriptBuildStepConfigMigrate.class);

    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        // Require the maven plugin
        context.ucbHasPlugin("com.urbancode.air.plugin.Shell", "Shell Plugin");

        // Get the shell bits
        ShellScriptBuildStepConfig c = (ShellScriptBuildStepConfig)
                                        context.getCurrentStep();
        ShellScriptBuilder sb = c.getBuilder();

        try {
            // Collect our Shell details into a hashmap
            HashMap<String, String> props = new HashMap<>(6);

            // This and environment look like they are common to all
            // build steps.  Should we move them to a common place?
            props.put("workDirOffset", sb.getWorkDirOffset());

            // "Command line" is the script.
            props.put("scriptBody", sb.getCommandLine());

            // This is optional
            if(sb.getInterpreter() != null) {
                props.put("shellInterpreter", sb.getInterpreter());
            }

            // If this is set, we can't use them.
            if(((sb.getUser() != null) && (sb.getUser().length() > 0)) ||
               ((sb.getPassword() != null) &&
                (sb.getPassword().length() > 0))) {
                LOG.warn(
                    "Shell Script Build Step with name: " + c.getName() +
                    " uses a user and password.  This is not supported in" +
                    " UCB.  Workflow: " + context.getWorkflow().getName()
                );
            }

            // This is also optional
            if(sb.getOutputFile() != null) {
                props.put("outputFile", sb.getOutputFile());
            }

            props.put("runAsDaemon", String.valueOf(sb.isDaemon()));

            StringBuilder sbr = new StringBuilder(1024);

            for(NameValuePair nvp : sb.getEnvironmentVariableArray()) {
                sbr.append(nvp.getName());
                sbr.append("=");
                sbr.append(nvp.getValue());
                sbr.append("\n");
            }

            props.put("env-vars", sbr.toString());

            // Try to cook it
            Step ucbStep = context.getUcbJob()
                                  .createStep(
                                    "Scripting/Shell/Shell",
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
            throw new MigrateException("Error while migrating shell step", e);
        }
    }
}
