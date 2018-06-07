package net.epicforce.migrate.ahp.toucb.script;

/*
 * PreCondScript.java
 *
 * This class manages the step and job pre condition scripts.
 *
 * Note that UCB only supports javascript, so we're using the following
 * logic:
 *
 * * For Step PreCondition scripts below ID 6, the scripts are identical.
 *   So we'll just use what's in UCB.
 * * For others, we'll first check to see if UCB has a precond with
 *   the same name.  If it does, we'll "blindly use it", meaning we
 *   don't really know if they do the same thing but we'll assume
 *   that they do.
 * * If there's not a precondition script there, we'll either throw
 *   an exception or port it over with a warning if it uses Javascript.
 *   The majority of AHP scripts are in beanshell so this is likely
 *   a lost cause at this point.
 *
 * I tried to make UCB parse Beanshell, but they have it locked down
 * really tight and there's no way.  If they didn't explicitly remove
 * bsh.Interpreter from the script environment, I could wrap BSH
 * in JS.
 */

import java.util.HashMap;
import java.util.Map;

import com.urbancode.ubuild.client.script.precondition.job.JobPreConditionScript;
import com.urbancode.ubuild.client.script.precondition.step.StepPreConditionScript;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.ucb.UcbFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PreCondScript
{
    private final static Logger LOG =
                                LoggerFactory.getLogger(PreCondScript.class);

    /*****************************************************************
     * STATIC PROPERTIES
     ****************************************************************/

    /*
     * These are known precondition scripts.  Maps names to
     * UCB Client Pre Condition objects.
     */
    private static Map<String, StepPreConditionScript> knownStepScripts =
                                                                new HashMap<>();
    private static StepPreConditionScript[] firstSixSteps =
                                            new StepPreConditionScript[6];
    private static Map<String, JobPreConditionScript> knownJobScripts =
                                                                new HashMap<>();
    private static JobPreConditionScript[] firstTenSteps =
                                            new JobPreConditionScript[10];


    /*****************************************************************
     * STATIC METHODS
     ****************************************************************/

    /**
     * Loads a UCB StepPreConditionScript object, given an AHP
     * StepPreConditionScript object.
     *
     * Unfortunate naming clash there!
     *
     * AM-19 : Much overlap with Job Pre condition script.  Condense
     *        code so its not so copy-pasta?
     *
     * @param preCond       The AHP script.
     * @return a StepPreConditionScript if possible
     * @throws MigrateException if not possible
     */
    public static StepPreConditionScript getStepPreconditionScript(
      com.urbancode.anthill3.domain.script.step.StepPreConditionScript preCond
    )
           throws MigrateException
    {
        try {
            // Load once
            synchronized(knownStepScripts) {
                // This will only be 0 if we never loaded it
                if(knownStepScripts.size() == 0) {
                    knownStepScripts = new HashMap<String,
                                                   StepPreConditionScript>();

                    for(StepPreConditionScript script :
                            UcbFactory.getStepPreConditionScriptFactory()
                                      .getAllPreConditionScripts()) {
                        knownStepScripts.put(script.getName(), script);

                        // insert first six
                        int scriptId = Integer.parseInt(script.getId());
                        if(scriptId < 6) {
                            firstSixSteps[scriptId] =
                                script;
                        }
                    }
                }
            }

            if(preCond.getId() < 6) {
                // These we can pass straight through
                return firstSixSteps[preCond.getId().intValue()];
            }

            // Otherwise, let's check name and make it if we have to
            synchronized(knownStepScripts) {
                if(knownStepScripts.containsKey(preCond.getName())) {
                    LOG.warn("Blindly used PreCondition script with name {}." +
                             "  This means there was a script in UCB with " +
                             "the same name, and we're assuming they work " +
                             "the same.",
                             preCond.getName()
                    );

                    return knownStepScripts.get(preCond.getName());
                }

                // If it's Javascript, we can try to move it.
                if(!preCond.getLanguage().equals("javascript")) {
                    throw new MigrateException(
                        "This workflow uses a precondition step named" +
                        preCond.getName() +
                        " which uses an unsupported language.  " +
                        "To fix this, create a precondition script " +
                        "with the same exact name in UCB and try " +
                        "again."
                    );
                }

                LOG.warn("Migrated PreCondition script with name {}." +
                         "  This probably won't work out of the box.",
                         preCond.getName()
                );

                StepPreConditionScript newScript =
                    UcbFactory.getStepPreConditionScriptFactory()
                              .createPreConditionScript(
                                preCond.getName(),
                                preCond.getDescription() == null ?
                                    "" : preCond.getDescription(),
                                preCond.getBody()
                );

                knownStepScripts.put(newScript.getName(), newScript);
                return newScript;
            }
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            throw new MigrateException("Error communicating with UCB", e);
        }
    }

    /**
     * Loads a UCB JobPreConditionScript object, given an AHP
     * JobPreConditionScript object.
     *
     * Unfortunate naming clash there!
     *
     * @param preCond       The AHP script.
     * @return a StepPreConditionScript if possible
     * @throws MigrateException if not possible
     */
    public static JobPreConditionScript getJobPreconditionScript(
      com.urbancode.anthill3.domain.script.job.JobPreConditionScript preCond
    )
           throws MigrateException
    {
        try {
            // Load once
            synchronized(knownJobScripts) {
                if(knownJobScripts.size() == 0) {
                    knownJobScripts = new HashMap<String,
                                                   JobPreConditionScript>();

                    for(JobPreConditionScript script :
                            UcbFactory.getJobPreConditionScriptFactory()
                                      .getAllPreConditionScripts()) {
                        knownJobScripts.put(script.getName(), script);

                        // insert negative numbers
                        int scriptId = Integer.parseInt(script.getId());
                        if(scriptId < 0) {
                            firstTenSteps[scriptId*-1] =
                                script;
                        }
                    }
                }
            }

            if(preCond.getId() < 0) {
                // These we can pass straight through
                return firstTenSteps[preCond.getId().intValue()*-1];
            }

            // Otherwise, let's check name and make it if we have to
            synchronized(knownJobScripts) {
                if(knownJobScripts.containsKey(preCond.getName())) {
                    LOG.warn("Blindly used PreCondition script with name {}." +
                             "  This means there was a script in UCB with " +
                             "the same name, and we're assuming they work " +
                             "the same.",
                             preCond.getName()
                    );

                    return knownJobScripts.get(preCond.getName());
                }

                // If it's Javascript, we can try to move it.
                if(!preCond.getLanguage().equals("javascript")) {
                    throw new MigrateException(
                        "This workflow uses a job precondition step named" +
                        preCond.getName() +
                        " which uses an unsupported language.  " +
                        "To fix this, create a precondition script " +
                        "with the same exact name in UCB and try " +
                        "again."
                    );
                }

                LOG.warn("Migrated Job PreCondition script with name {}." +
                         "  This probably won't work out of the box.",
                         preCond.getName()
                );

                JobPreConditionScript newScript =
                    UcbFactory.getJobPreConditionScriptFactory()
                              .createPreConditionScript(
                                preCond.getName(),
                                preCond.getDescription() == null ?
                                    "" : preCond.getDescription(),
                                preCond.getBody()
                );

                knownJobScripts.put(newScript.getName(), newScript);
                return newScript;
            }
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            throw new MigrateException("Error communicating with UCB", e);
        }
    }
}
