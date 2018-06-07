package net.epicforce.migrate.ahp.toucb.ahp.domain.publisher.changelog;

/*
 * ChangeLogPublisherStepConfigMigrate.java
 *
 * There is no analog to this step in UCB as far as I can tell, and
 * I did extensive searches.
 *
 * @author sconley (sconley@epicforce.net)
 */

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class ChangeLogPublisherStepConfigMigrate extends UcbStep
{
    private final static Logger LOG =
            LoggerFactory.getLogger(ChangeLogPublisherStepConfigMigrate.class);

    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context)
    {
        LOG.warn("Ignored Change Log Publisher step - no UCB analog");
    }
}
