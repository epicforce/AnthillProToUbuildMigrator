package net.epicforce.migrate.ahp.toucb.ahp.domain.source.plugin;

/*
 * PluginCleanupStepConfigMigrate.java
 *
 * Migrate a Source Plugin cleanup step.  This is mostly to make
 * sure an analog plugin is installed on UCB, as UCB handles these
 * steps generically.
 *
 * @author sconley (sconley@epicforce.net)
 */

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.ahp.domain.source.CommonCleanup;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;

import com.urbancode.anthill3.domain.repository.plugin.PluginRepository;
import com.urbancode.anthill3.domain.source.plugin.PluginSourceConfig;



public class PluginCleanupStepConfigMigrate extends CommonCleanup
{
    /**
     * Make sure we have the correct plugin, or this will be awkward.
     *
     * @param context           Our context
     * @throws MigrateException on failures
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        context.ucbHasPlugin(getSourcePluginInfo(context));
        super.run(context);
    }
}
