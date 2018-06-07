package net.epicforce.migrate.ahp.toucb.ahp.domain.source.git;

/*
 * GitCleanupStepConfigMigrate.java
 *
 * Migrate GIT Cleanup Step
 *
 * @author sconley (sconley@epicforce.net)
 */

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.ahp.domain.source.CommonCleanup;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;

public class GitCleanupStepConfigMigrate extends CommonCleanup
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
