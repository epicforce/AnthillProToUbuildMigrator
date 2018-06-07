package net.epicforce.migrate.ahp.toucb;

/*
 * MigrateEngine.java
 *
 * The MigrateEngine does the 'heavy lifting' of actually running a
 * migration of different parts of Anthill to UCB.
 *
 * It manages the thread pool and provides status accessors and
 * controls for some wrapper (such as the GUI or a command line tool)
 *
 * @autor sconley (sconley@epicforce.net)
 */

import java.lang.IllegalArgumentException;
import java.lang.InterruptedException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CancellationException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.TimeUnit;

import net.epicforce.migrate.ahp.Migration;
import net.epicforce.migrate.ahp.exception.MigrateException;

import com.urbancode.ubuild.client.Client;
import com.urbancode.ubuild.client.Factories;
import com.urbancode.ubuild.client.RESTClientFactory;
import com.urbancode.ubuild.client.plugin.Plugin;

import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.loader.UcbLoader;
import net.epicforce.migrate.ahp.toucb.ucb.UcbClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class MigrateEngine
{
    private final static Logger LOG = 
                                LoggerFactory.getLogger(MigrateEngine.class);

    /*****************************************************************
     * PROPERTIES
     ****************************************************************/

    /*
     * UCB runs off a global client, that uses a singleton for login
     * creds.  So from what I can tell, you instance it once and then
     * I guess all threads can use it.
     *
     * We'll see how well that works.
     */
    private Client  client;

    /*
     * We will be instancing potentially many AHP's so we have to keep
     * the credentials around.
     */
    private final String  ahpHost;
    private final int     ahpPort;
    private final String  ahpUser;
    private final String  ahpPassword;
    private final String  ahpKeystorePath;
    private final String  ahpKeystorePassword;

    /*
     * Stuff we're going to use to keep track of our concurrent
     * friends.
     */
    private ExecutorService         threadService = null;
    private List<Future<Migration>> futures = 
                                    new LinkedList<Future<Migration>>();
    private List<Migration>         migrations =
                                    new LinkedList<Migration>();

    /*
     * Let's grab a list of PluginId's which will be used by
     * all the threads.  We'll use this to sync so we only load
     * once.
     */
    private static Set<String>      ucbPlugins = new HashSet<String>();

    /*****************************************************************
     * CONSTRUCTORS
     ****************************************************************/

    /**
     * Uses the AHP and UCB credentials to start the AHP process.
     *
     * @param ahpHost               Anthill host
     * @param ahpPort               Anthill port
     * @param ahpUser               Anthill user
     * @param ahpPassword           Anthill password
     * @param ahpKeystorePath       Anthill Keystore path, may be null/empty
     * @param ahpKeystorePassword   Anthill keystore password, may be null/empty
     * @param ucbUrl                UCB url
     * @param ucbUser               UCB username
     * @param ucbPassword           UCB password
     */
    public MigrateEngine(final String ahpHost, final String ahpPort,
                         final String ahpUser, final String ahpPassword,
                         final String ahpKeystorePath,
                         final String ahpKeystorePassword,
                         final String ucbUrl, final String ucbUser,
                         final String ucbPassword)
        throws MigrateException
    {
        LOG.debug("Constructing MigrateEngine");

        this.ahpHost = ahpHost;
        this.ahpPort = Integer.parseInt(ahpPort);
        this.ahpUser = ahpUser;
        this.ahpPassword = ahpPassword;
        this.ahpKeystorePath = ahpKeystorePath;
        this.ahpKeystorePassword = ahpKeystorePassword;

        // UCB rather uselessly throws 'exception' everywhere.
        // Wrap it into something marginally more useful.
        try {
            client = RESTClientFactory.createClient(ucbUrl);
            client.login(ucbUser, ucbPassword);

            // Cache a list of plugins -- this both checks the UCB connection
            // and gives us something useful we need later.
            synchronized(ucbPlugins) {
                if(ucbPlugins.size() == 0) {
                    for(Plugin p : Factories.getPluginFactory()
                                            .getAllPlugins()) {
                        ucbPlugins.add(p.getPluginId());
                    }
                }
            }
        } catch(Exception e) {
            LOG.error("Error from UCB", e);

            // This sucks, wish UCB threw actual exceptions.
            if(e.getMessage() == null) {
                throw new MigrateException(e.getClass().getName(), e);
            } else {
                throw new MigrateException("Error from UCB: ", e);
            }
        }
    }

    /*****************************************************************
     * METHODS
     ****************************************************************/

    /**
     * Takes a list of AHP workflows and starts a migration of them.
     *
     * The migration will be done by a thread pool, so this method
     * will return pretty quickly.
     *
     * This should never fail, but if it does, its because of programmer
     * error or not checking and it will throw a MigrateException
     *
     * @param workflowIds           The workflows to operate on
     * @param threadCount           The level of concurrency
     * @throws MigrateException on failure.
     */
    public void start(final List<Long> workflowIds, int threadCount)
           throws MigrateException
    {
        LOG.debug("Starting MigrateEngine!  Workflows: {}, Thread count: {}",
                  workflowIds, threadCount
        );

        try {
            // Initialize if we need to
            if(threadService == null) {
                LOG.debug("Created threadService");
                threadService = Executors.newFixedThreadPool(threadCount);
            }

            // Loader is shared for everyone
            UcbLoader loader = new UcbLoader();

            for(Long workflowId : workflowIds) {
                // Context is unique for each thread.
                UcbContext context = new UcbContext(ucbPlugins);

                Migration migrate = new Migration(ahpHost, ahpPort,
                                                  ahpUser, ahpPassword,
                                                  ahpKeystorePath,
                                                  ahpKeystorePassword);
                migrate.setWorkflowId(workflowId);
                migrate.setContext(context);
                migrate.setLoader(loader);

                LOG.debug("Submitting Migration job for workflow: {}",
                          workflowId
                );

                // Queue it up to run
                Future<Migration> future = threadService.submit(migrate,
                                                                migrate);
                futures.add(future);
                migrations.add(migrate);
            }
        } catch(RejectedExecutionException | IllegalArgumentException e) {
            LOG.error("Failed to start threads", e);
            throw new MigrateException("Failed to start threads: ", e);
        }

        LOG.debug("Finished submitting jobs");
    }

    /**
     * This method returns status of the migration process.  It also
     * does book-keeping involved with keeping the whole show running.
     *
     * When all MigrateStatus objects return progress == 100, it can be
     * assumed done and the 'close' method can be called.
     *
     * @returns List of MigrateStatus
     */
    public List<MigrateStatus> check()
    {
        List<MigrateStatus> ret =
                                new ArrayList<MigrateStatus>(migrations.size());

        for(Migration m : migrations) {
            MigrateStatus status = new MigrateStatus();

            status.workflowId = m.getWorkflowId();
            status.progress = m.getProgress();

            if((status.progress == 100) && (m.getError() != null)) {
                status.errorMessage = m.getError().getMessage();
            }

            ret.add(status);
        }

        // Poke our futures -- this is necessary to find failures.
        for(Future<Migration> future : futures) {
            try {
                Migration result = future.get(0, TimeUnit.SECONDS);

                // this one is done -- close it
                result.close();
            } catch(TimeoutException | InterruptedException |
                    CancellationException e) {
                // don't care
            } catch(ExecutionException e) {
                // this is probably a runtime error.
                LOG.error("Fatal thread error: {}", e);

                // Find the appropriate Migrate object and mark it error
                int index = futures.indexOf(future);

                if(index == -1) { // this shouldn't happen
                    LOG.error("The impossible has happened :(  " +
                              "Our future is lost."
                    );
                } else {
                    Migration toDie = migrations.get(index);
                    toDie.setError(
                        new MigrateException("Migration failure",
                                             (Exception)e.getCause())
                    );
                    toDie.close();
                }
            }
        }

        return ret;
    }

    /**
     * Politely shutdown the Migrate Engine.
     *
     * @param immediate         If true, we will hard stop the system.
     *                          Otherwise, it will wait for currently
     *                          running stuff to stop.
     *
     * Default is to wait for things to stop.  Leaving a migration
     * hanging is a good way to leave a dirty system.
     */
    public void close(boolean immediate)
    {
        if(threadService != null) {
            if(immediate) {
                threadService.shutdownNow();
            } else {
                threadService.shutdown();
            }
        }

        while(true) {
            try {
                if(threadService.awaitTermination(1, TimeUnit.DAYS)) {
                    for(Migration m : migrations) {
                        m.close();
                    }

                    migrations.clear();

                    threadService = null;
                    return;
                }
            } catch(InterruptedException e) { }
        }
    }

    public void close()
    {
        close(false);
    }
}
