package net.epicforce.migrate.ahp.toucb.ahp.domain.source.git;

/*
 * GitGetChangelogStepConfigMigrate.java
 *
 * Migrate GIT Get Changelog step
 *
 * @author sconley (sconley@epicforce.net)
 */

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.ahp.domain.source.CommonGetChangelog;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;


public class GitGetChangelogStepConfigMigrate extends CommonGetChangelog
{
    /**
     * Make sure we have the GIT plugin, or this will be awkward.
     *
     * @param context           Our context
     * @throws MigrateException on failures
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        context.ucbHasPlugin("com.urbancode.air.plugin.git", "GIT SCM Plugin");
        super.run(context);
    }
}
