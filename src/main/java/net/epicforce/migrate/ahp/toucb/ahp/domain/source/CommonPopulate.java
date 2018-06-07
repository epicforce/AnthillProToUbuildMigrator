package net.epicforce.migrate.ahp.toucb.ahp.domain.source;

/**
 * CommonPopulate.java
 *
 * AHP uses a different class for every kind of source populate
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

import com.urbancode.anthill3.domain.source.PopulateWorkspaceStepConfig;
import com.urbancode.anthill3.domain.step.StepConfig;
import com.urbancode.ubuild.client.step.Step;

public class CommonPopulate extends CommonSource
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
            // This has a date string to account for.
            HashMap<String, String> prop = new HashMap<String, String>(1);

            // So we need to detect the AHP script type, and then wrap
            // it in the correct ${...} tags
            String dateScript;
            String scriptLanguage = ((PopulateWorkspaceStepConfig)c)
                                           .getWorkspaceDateScriptLanguage();

            switch(scriptLanguage) {
                case "beanshell":
                    dateScript = "${bsh:";
                    break;
                case "groovy":
                    dateScript = "${gvy:";
                    break;
                case "javascript":
                    dateScript = "${js:"; // AM-4 to verify
                    break;
                default:
                    // AHP supports a "class" variety that we don't
                    // support in UCB.
                    throw new MigrateException(
                        "Unsupported Populate Workspace date script type: "
                        + scriptLanguage
                    );
            }

            dateScript = dateScript +
                         ((PopulateWorkspaceStepConfig)c)
                                                    .getWorkspaceDateScript()
                         + "}";

            // This is the only control we have over this particular step.
            prop.put("date-string", dateScript);

            // Push it on out
            Step ucbStep = context.getUcbJob()
                                  .createStep(
                                    "Source/Populate Workspace",
                                    c.getName(),
                                    c.getDescription() == null ?
                                        "" : c.getDescription(),
                                    context.nextUcbStep(),
                                    prop
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
