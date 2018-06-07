package net.epicforce.migrate.ahp.toucb.ahp.domain.source.plugin;

/*
 * PluginGetChangelogStepConfigMigrate.java
 *
 * Migrate a Source Plugin get changelog step.  This is mostly to make
 * sure an analog plugin is installed on UCB, as UCB handles these
 * steps generically.
 *
 * @author sconley (sconley@epicforce.net)
 */

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.ahp.domain.source.CommonGetChangelog;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;


public class PluginGetChangelogStepConfigMigrate extends CommonGetChangelog
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
