package net.epicforce.migrate.ahp.toucb.context;

/*
 * UcbJob.java
 *
 * The job class allows us an injection point at the top / bottom of
 * a job loop, to set up our UCB job.
 */

import net.epicforce.migrate.ahp.context.AbstractContext;
import net.epicforce.migrate.ahp.migrate.AbstractJob;
import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.exception.SkipException;

import com.urbancode.anthill3.domain.jobconfig.JobConfig;
import com.urbancode.ubuild.client.Factories;
import com.urbancode.ubuild.client.workflow.Job;


public class UcbJob extends AbstractJob
{
    /**
     * preRun
     *
     * @param context           Context of the migration
     *
     * @throws MigrateException on failure
     * @throws SkipException is we should skip this job.
     */
    public void preRun(UcbContext context)
           throws MigrateException, SkipException
    {
        try {
            // We're really interested in the job config here
            JobConfig jc = context.getCurrentJob().getJobConfig();

            // Load the job if we've got it
            StringBuilder sb = new StringBuilder(jc.getName());

            sb.append(" (");
            sb.append(String.valueOf(jc.getId()));
            sb.append(")");

            final String jobName = sb.toString();

            if(Factories.getJobFactory().getJobByName(jobName) != null) {
                throw new SkipException("Job " + jobName + 
                                        " already exists!"
                );
            }

            // Create it
            context.setUcbJob(Factories.getJobFactory()
                                       .createJob(jobName,
                                                  jc.getDescription(),
                                                  context.teamMappings
                                        )
            );
        } catch(MigrateException e) {
            throw e; // just rethrow these
        } catch(Exception e) {
            // Cause UCB throws Exception
            throw new MigrateException("Error while migrating job", e);
        }
    }

    /**
     * (@inheritdoc)
     */
    @Override
    public void preRun(AbstractContext context)
           throws MigrateException
    {
        preRun((UcbContext)context);
    }

    /**
     * postRun
     *
     * @param context       Context of the migration
     *
     * This is run after all the job's steps are migrated.
     */
    public void postRun(UcbContext context)
    {

    }

    /**
     * (@inheritdoc)
     */
    @Override
    public void postRun(AbstractContext context)
           throws MigrateException
    {
        postRun((UcbContext)context);
    }

}
