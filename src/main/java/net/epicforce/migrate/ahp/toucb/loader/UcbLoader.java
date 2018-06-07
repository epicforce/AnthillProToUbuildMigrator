package net.epicforce.migrate.ahp.toucb.loader;

/*
 * UcbLoader.java
 *
 * Class loader for UCB processing steps.  As required by the AHP library.
 *
 * @author sconley (sconley@epicforce.net)
 */

import net.epicforce.migrate.ahp.exception.UnsupportedClassException;
import net.epicforce.migrate.ahp.loader.DefaultLoader;
import net.epicforce.migrate.ahp.migrate.AbstractJob;
import net.epicforce.migrate.ahp.migrate.AbstractStep;
import net.epicforce.migrate.ahp.migrate.AbstractWorkflow;
import net.epicforce.migrate.ahp.toucb.context.UcbJob;
import net.epicforce.migrate.ahp.toucb.context.UcbWorkflow;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UcbLoader extends DefaultLoader
{
    private final static Logger LOG = 
                                LoggerFactory.getLogger(UcbLoader.class);

    /**
     * Overide default workflow loader step, to provide
     * an actual workflow.
     *
     * @return a UcbWorkflow
     */
    @Override
    public AbstractWorkflow loadWorkflowClass()
    {
        return new UcbWorkflow();
    }

    /**
     * Overide default job loader step, to provide
     * an actual job.
     *
     * @return a UcbJob
     */
    @Override
    public AbstractJob loadJobClass()
    {
        return new UcbJob();
    }

    /**
     * Override default loader step, but fallback to default.
     *
     * @param stepName      The step we're trying to load
     * @return AbstractStep to process
     * @throws UnsupportedClassException if we can't load it.
     */
    @Override
    public AbstractStep loadStepClass(final String stepName)
           throws UnsupportedClassException
    {
        try {
            return (AbstractStep)loadClass(
                stepName.replace(
                    "com.urbancode.anthill3.",
                    "net.epicforce.migrate.ahp.toucb.ahp."
                ) + "Migrate"
            );
        } catch(UnsupportedClassException e) {
            LOG.error("Deferring to default due to error", e);
            return super.loadStepClass(stepName);
        }
    }
}
