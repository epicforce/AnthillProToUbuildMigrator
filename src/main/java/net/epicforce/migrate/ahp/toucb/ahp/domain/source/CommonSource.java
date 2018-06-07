package net.epicforce.migrate.ahp.toucb.ahp.domain.source;

/**
 * CommonSource.java
 *
 * Base class for all the Common* classes here under Source.
 *
 * This provides some common methods for getting plugin info.
 *
 * @author sconley (sconley@epicforce.net)
 */

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;
import net.epicforce.migrate.ahp.toucb.ucb.SourcePluginMap;

import com.urbancode.anthill3.domain.repository.plugin.PluginRepository;
import com.urbancode.anthill3.domain.source.plugin.PluginSourceConfig;

public abstract class CommonSource extends UcbStep
{
    /**
     * This extracts an AHP Source Plugin ID from a Context
     * object.
     *
     * Note that if the context object doesn't contain a
     * Plugin*StepConfig object, this will fail in a nasty way.
     *
     * @param c           The context
     * @return a SourcePluginMap info object with the desired info
     */
    protected SourcePluginMap.Info getSourcePluginInfo(UcbContext c)
              throws MigrateException
    {
        if(c.getWorkflow().getBuildProfile() == null) {
            throw new MigrateException("Workflow has no build profile.");
        }

        PluginRepository[] repos = ((PluginSourceConfig)c.getWorkflow()
                                                         .getBuildProfile()
                                                         .getSourceConfig())
                                                         .getRepositoryArray();

        if(repos.length == 0) {
            throw new MigrateException(
                "No repositories associated with workflow."
            );
        }

        // All repositories will be the same type, so grab the first.
        return SourcePluginMap.getInfo(repos[0].getPlugin().getPluginId());
    }
}
