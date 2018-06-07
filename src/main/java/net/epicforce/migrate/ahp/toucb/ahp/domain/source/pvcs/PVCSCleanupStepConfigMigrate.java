package net.epicforce.migrate.ahp.toucb.ahp.domain.source.pvcs;

/*
 * PVCS SCM - NOT supported by UCB
 *
 * @author sconley
 */

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;


public class PVCSCleanupStepConfigMigrate extends UcbStep
{
    /**
     * Just throw an exception -- this type of source control is
     * not supported in UCB.
     *
     * @param context
     * @throws MigrateException ALWAYS
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        throw new MigrateException("PVCS SCM is not supported by UCB.");
    }
}
