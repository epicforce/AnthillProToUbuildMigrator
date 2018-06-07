package net.epicforce.migrate.ahp.toucb.script;

/*
 * WorkDirScript.java
 *
 * Class to handle the conversion of AHP workdir scripts to UCB
 * equivalent code.
 *
 * AM-20: There's a lot that could be done here, but I'm kind of
 *       taking it as I go.  Ultimately, all the AHP helpers need
 *       to probably map to the UCB
 *       com.urbancode.ubuild.runtime.scripting.helpers
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;
import java.util.Map;

import com.urbancode.ubuild.client.Factories;
import com.urbancode.ubuild.client.script.workdir.WorkingDirectoryScript;

import net.epicforce.migrate.ahp.exception.MigrateException;


public class WorkDirScript
{
    /*****************************************************************
     * STATIC PROPERTIES
     ****************************************************************/

    // Keep a list of known work dir scripts.  Update as necessary,
    // but only load once.  Map script name to UCB client object.
    private static Map<String, WorkingDirectoryScript> knownScripts =
                                                                new HashMap<>();

    /*
     * Method that does all the 'leg work' as it were.  Given an AHP
     * WorkDirScript, we'll return a known script or move it over.
     *
     * @param script            The working directory script from AHP
     * @return a WorkingDirectoryScript object for UCB client.
     * @throws MigrateException on any failure.
     */
    public static WorkingDirectoryScript get(
                  com.urbancode.anthill3.domain.workdir.WorkDirScript script)
           throws MigrateException
    {
        try {
            // Only load once
            synchronized(knownScripts) {
                if(knownScripts.size() == 0) {
                    for(WorkingDirectoryScript wds :
                            Factories.getWorkDirScriptFactory()
                                     .getAllWorkingDirectoryScripts()) {
                        knownScripts.put(wds.getName(), wds);
                    }
                }

                // Look it up if we have it.
                if(knownScripts.containsKey(script.getName())) {
                    return knownScripts.get(script.getName());
                }

                // Transform script then save it.
                // AM-20: make this code nicer / more dynamic
                String newScript =
                        script.getPathScript()
                              .replace("WorkflowLookup.getRequested()",
                                       "WorkflowLookup.getCurrent()")
                              .replace("${anthill3/work.dir}",
                                       "${env/AGENT_HOME}");

                // Make it
                WorkingDirectoryScript newUcdScript =
                    Factories.getWorkDirScriptFactory()
                             .createWorkingDirectoryScript(
                                script.getName(),
                                script.getDescription() == null ?
                                    "" : script.getDescription(),
                                newScript
                );

                knownScripts.put(newUcdScript.getName(), newUcdScript);
                return newUcdScript;
            }
        } catch(Exception e) { // Cause UCB
            throw new MigrateException("Error sending Work Dir script to " +
                                       "UCB", e);
        }
    }
}
