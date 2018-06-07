package net.epicforce.migrate.ahp.toucb.context;

/*
 * UcbStep.java
 *
 * This extends AbstractStep and adds some methods that are super common
 * for UCB.
 *
 * @author sconley
 */

import java.util.HashMap;
import java.util.Map;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.context.AbstractContext;
import net.epicforce.migrate.ahp.migrate.AbstractStep;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.script.PreCondScript;
import net.epicforce.migrate.ahp.toucb.ucb.DummyStep;
import net.epicforce.migrate.ahp.toucb.ucb.UcbClient;
import net.epicforce.migrate.ahp.toucb.ucb.UcbFactory;
import net.epicforce.migrate.ahp.toucb.ucb.fixed.RESTStep;

import com.urbancode.anthill3.domain.step.StepConfig;
import com.urbancode.ubuild.client.step.Step;
import com.urbancode.ubuild.client.script.precondition.step.StepPreConditionScript;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class UcbStep extends AbstractStep
{
    private final static Logger LOG = LoggerFactory.getLogger(UcbStep.class);

    /**
     * Convert the AbstractStep context to a UcbContext
     *
     * Implement this one instead of the AbstractStep run
     *
     * @param context       The migration context
     * @throws MigrateException on any error.
     */
    public abstract void run(UcbContext context) throws MigrateException;

    /**
     * Implement the underlying run and do the type casting on context.
     *
     * @param context       The migration context
     * @throws MigrateException on any error
     */
    public void run(AbstractContext context) throws MigrateException
    {
        run((UcbContext)context);
    }

    /**
     * Some things are poorly or completely unsupported by the UcbClient.
     * For those things, we need to post straight into UCB and hope for
     * the best.
     *
     * This method will produce a basic map which can be added upon by
     * the invoker to build a common Step map.
     *
     * @param type              The UCB type (class name)
     * @param context           The Context
     * @param ahpStep           Anthill StepConfig
     *
     * @return a string, string map suitable for use with the UcbClient raw post
     * @throws MigrateException on any failure (probably precondition)
     */
    public Map<String, String> createCommonPostParams(String type,
                                                      UcbContext context,
                                                      StepConfig ahpStep)
           throws MigrateException
    {
        Map<String, String> ret = new HashMap<>(20);

        ret.put("Save", "Save");
        ret.put("ignore_my_failures",
                String.valueOf(ahpStep.isIgnoreMyFailures())
        );
        ret.put("is_active", "true");
        ret.put("jobConfigId", String.valueOf(context.getUcbJob().getId()));
        ret.put("name", ahpStep.getName());
        ret.put("runInParallel", "false"); // AHP doesn't have this
        ret.put("runInPreflight", String.valueOf(ahpStep.isRunInPreflight()));
        ret.put("runInPreflightOnly",
                String.valueOf(ahpStep.isRunInPreflightOnly())
        );
        ret.put("stepConfigClass", type);
        ret.put("timeout", String.valueOf(ahpStep.getTimeout()));
        ret.put("stepIndex", String.valueOf(context.nextUcbStep()));

        // AM-7 is to figure out what this means
        ret.put("snippet_method", "saveMainTab");

        // These are complex to deal with
        // AM-8 is to migrate them.
        if(ahpStep.getPostProcessScript() == null) {
            ret.put("post_process_script_id", "");
        } else if(ahpStep.getPostProcessScript().getId() >= 0) {
            ret.put("post_process_script_id", "");
            LOG.warn("UCB does not provide an API to migrate " +
                     "custom post process scripts.  You will " +
                     "have to migrate {} yourself for workflow {} " +
                     "job {} step {}",
                     ahpStep.getPostProcessScript().getName(),
                     context.getWorkflow().getName(),
                     context.getCurrentJob().getName(),
                     ahpStep.getName()
            );
        } else {
            ret.put("post_process_script_id",
                    String.valueOf(ahpStep.getPostProcessScript().getId())
            );
        }

        ret.put("snippet_step_condition", copyStepPreCondition(ahpStep, null));

        return ret;
    }

    /**
     * This is used all over the place.  Copy the sort of 'basic elements'
     * of a StepConfig to a UCB Step.
     *
     * @param context           The Context
     * @param ahpStep           Anthill StepConfig
     * @param ucbStep           UCB Step
     *
     * @throws MigrateException (likely comm error)
     *
     * NOTE TO SELF: WorkDirOffset is a common field in UCB but not
     *               in AHP, thus it is not handled here.
     */
    protected void copyCommonBits(UcbContext context, StepConfig ahpStep,
                                  Step ucbStep)
              throws MigrateException
    {
        try {
            ucbStep.setIgnoreFailures(ahpStep.isIgnoreMyFailures());

            // This is kind of involved, so copy it thusly.
            copyStepPreCondition(ahpStep, ucbStep);

            ucbStep.setRunInPreflight(ahpStep.isRunInPreflight());
            ucbStep.setRunInPreflightOnly(ahpStep.isRunInPreflightOnly());
            ucbStep.setTimeout(ahpStep.getTimeout());

            // We can't migrate post process scripts, so we'll check for
            // the defaults
            // AM-8 is to migrate these.
            if((ahpStep.getPostProcessScript() != null) &&
               (ahpStep.getPostProcessScript().getId() >= 0)) {
                LOG.warn("UCB does not provide an API to migrate " +
                         "custom post process scripts.  You will " +
                         "have to migrate {} yourself for workflow {} " +
                         "job {} step {}",
                         ahpStep.getPostProcessScript().getName(),
                         context.getWorkflow().getName(),
                         context.getCurrentJob().getName(),
                         ahpStep.getName()
                );
            }
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            // Cause UCB!
            throw new MigrateException("Error while copying common step items",
                                       e
            );
        }
    }
    
    /**
     * Copy over step precondition script.  This used to be a way bigger
     * mess but I moved it to its own class.
     *
     * @param ahpStep           The AHP step we're working on
     * @param ucbStep           The UCB step we're working on, or null
     *                          if we're not working on one.
     * @return a string representation of the pre condition step.
     * @throws MigrateException on any failure
     */
    protected String copyStepPreCondition(StepConfig ahpStep, Step ucbStep)
              throws MigrateException
    {
        StepPreConditionScript ucbScript =
            PreCondScript.getStepPreconditionScript(
                                            ahpStep.getPreConditionScript()
        );

        if(ucbStep != null) {
            ucbStep.setPreConditionScript(ucbScript);
        }

        return String.valueOf(ucbScript.getId());
    }

    /**
     * It's really hard to not put bad things about the untested mess
     * that is the UCB client in comments.  So I'll try not to.
     *
     * This is another hack to get around a UCB bug.  in this case, the
     * fact that steps don't store preconditions :P
     *
     * @param Step      A UCB step object
     */
    protected void ucbUpdate(Step ucbStep)
              throws MigrateException
    {
        try {
            RESTStep updater = new RESTStep(ucbStep);
            updater.update();
        } catch(Exception e) {
            throw new MigrateException("Error while updating UCB step", e);
        }
    }


    /**
     * This is a "manual override" that allows us to post to UCB's saveStep
     * form processor instead of using the REST API.
     *
     * This is NOT usually needed ... any Step that is implemented by a
     * UCB plugin should be create-able with the UCB Client.  However,
     * UCB's support for UCB built-in commands (like Stamp and AssignStatus)
     * is limited to a handful of source control related commands.
     *
     * As such, those must be pushed manually.  The structure for all
     * of these is the same; use createCommonPostParams to generate the
     * common params, add in the unique params for your step, then push
     * the map here for us to ship it to UCB.
     *
     * This also handles the "dummy step" portion to make sure the
     * UCB Job object doesn't get broken by the use of the manual
     * override.
     *
     * @param newStep           A map representing our new step
     * @param context           Our current context
     * @throws MigrateException on any error
     */
    protected void saveStepClient(Map<String, String> newStep,
                                  UcbContext context)
              throws MigrateException
    {
        try {
            // Try to push it
            UcbClient client = new UcbClient(
                "/tasks/admin/library/jobconfig/LibraryJobConfigTasks/saveStep"
            );

            client.sendRawPost(newStep);

            // Add a dummy step.  This is so that the sequence counter
            // continues to work.
            context.getUcbJob().addStep(new DummyStep());
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            // Rewrap
            throw new MigrateException("Error while transfering to UCB.", e);
        }
    }
}
