package net.epicforce.migrate.ahp.toucb.context;

/*
 * UCB Context for migration
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.urbancode.ubuild.client.security.TeamResourceMapping;
import com.urbancode.ubuild.client.workflow.Job;

import net.epicforce.migrate.ahp.context.AbstractContext;
import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.ucb.SourcePluginMap;

public class UcbContext extends AbstractContext
{

    /*****************************************************************
     * PROPERTIES
     ****************************************************************/

    /*
     * This is largely static at the moment -- it should/could be
     * more dynamic in the future.
     *
     * AM-6: We need to properly load these and re-vamp how
     *       teams are handled.
     */
    public List<TeamResourceMapping>   teamMappings;

    /*
     * The list of plugins.  This is treated like a constant
     * by the migration process.
     */
    private final Set<String>       knownPlugins;

    /*
     * Current UCB job
     */
    private Job     ucbJob;

    /*
     * The UCB API says you can use step count -1 to indicate
     * next step.  That's a big damn lie, so we have to do
     * it this way.
     */
    private int     ucbStepCount = 0;

    /*****************************************************************
     * ACCESSORS
     ****************************************************************/

    /*
     * If we change ucb jobs, we need to reset the
     * step counter.
     */
    public void setUcbJob(Job ucb)
    {
        ucbStepCount = 0;
        ucbJob = ucb;
    }

    public Job getUcbJob()
    {
        return ucbJob;
    }

    public int nextUcbStep()
    {
        return ucbStepCount++;
    }

    /*****************************************************************
     * CONSTRUCTORS
     ****************************************************************/

    /**
     * The constructor does some loading of largely static stuff
     * that we need for each context.
     *
     * @param pluginList        A constant list of plugins.
     */
    public UcbContext(final Set<String> pluginList)
    {
        knownPlugins = pluginList;

        teamMappings = new ArrayList<TeamResourceMapping>(1);

        // This is the "System Team" that comes with UCB and is
        // pretty much constant.
        teamMappings.add(
            new TeamResourceMapping(
                "00000000-0000-0000-0000-000000000200",
                null
            )
        );
    }

    /*****************************************************************
     * METHODS
     ****************************************************************/

    /**
     * Check if we have a plugin.  Throws an exception if
     * we do not.  This is a validation step for *many* parts
     * of the system so we want it to be as easy to use
     * as possible.
     *
     * @param plugin        ID (class name) of plugin to check.
     * @param description   "Human Readable" version of plugin
     *                      for error message, optional.
     * @throws MigrateException if it does not exist.
     */
    public void ucbHasPlugin(final String plugin,
                             final String description)
           throws MigrateException
    {
        if(!knownPlugins.contains(plugin)) {
            if(description == null) {
                throw new MigrateException(
                    "UCB needs plugin " + plugin +
                    " in order for this workflow to migrate."
                );
            } else {
                throw new MigrateException(
                    "UCB needs the " + description +
                    " plugin in order for this workflow to " +
                    " migrate."
                );
            }
        }
    }

    /**
     * Check if we have a plugin, but uses a SourcePluginMap Info
     * object for input.  See ucbHasPlugin for more details.
     *
     * @param info      The info object
     * @throws MigrateException if plugin does not exist
     */
    public void ucbHasPlugin(final SourcePluginMap.Info info)
           throws MigrateException
    {
        ucbHasPlugin(info.getPluginId(), info.getName());
    }
}
