package net.epicforce.migrate.ahp.toucb.source.config;

/*
 * PluginSourceConfig.java
 *
 * This class is for handling plugin source configs, which is to say,
 * a source config provided by an AHP plugin.
 *
 * These basically use property settings to define parameters.
 *
 * Plugins will inherit this class, as they have some special needs
 * that other source configs don't necessarily share.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.source.AbstractSourceConfig;
import net.epicforce.migrate.ahp.toucb.source.config.plugin.*;
import net.epicforce.migrate.ahp.toucb.ucb.SourcePluginMap;

import com.urbancode.anthill3.domain.property.PropertyValueGroup;
import com.urbancode.anthill3.domain.property.PropertyValueGroupFactory;
import com.urbancode.anthill3.domain.repository.plugin.PluginRepository;
import com.urbancode.anthill3.domain.source.plugin.PluginSourceConfig;
import com.urbancode.ubuild.client.Factories;
import com.urbancode.ubuild.client.plugin.Plugin;
import com.urbancode.ubuild.client.repository.Repository;
import com.urbancode.ubuild.client.repository.RepositoryBuilder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class PluginSourceConfigMigrate extends AbstractSourceConfig
{
    private final static Logger LOG =
                    LoggerFactory.getLogger(PluginSourceConfigMigrate.class);

    /*****************************************************************
     * STATIC PROPERTIES
     ****************************************************************/

    /**
     * Keep track of plugin repo's that we have already
     * seen, so that we can re-use them.
     *
     * This will map 'repo' column in the PVG's (which is
     * the property value group ID for the repo) to UCB
     * Repository objects.
     */
    private static HashMap<String, Repository> repoMap = new HashMap<>();

    /*****************************************************************
     * METHODS
     ****************************************************************/

    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        // Grab our source config
        PluginSourceConfig sc = (PluginSourceConfig)
                                context.getWorkflow()
                                       .getBuildProfile()
                                       .getSourceConfig();

        // Figure out repo type
        PluginRepository[] repos = sc.getRepositoryArray();

        // If this is null or empty, we're screwed.  This should
        // never happen.
        if((repos == null) || (repos.length == 0)) {
            throw new MigrateException(
                "No repositories associated with source configuration."
            );
        }

        // Get UCB plugin
        Plugin plugin = getUcbPluginFromId(
                     SourcePluginMap.getInfo(repos[0].getPlugin().getPluginId())
                                    .getPluginId()
        );

        // Route accordingly
        switch(repos[0].getPlugin().getPluginId()) {
            case "com.urbancode.anthill3.plugin.Git":
                repoTemplatePairs = (new GitPlugin()).run(context, sc, plugin);
                break;
            default:
                throw new MigrateException(
                    "Unknown AHP source plugin: " +
                    repos[0].getPlugin().getPluginId()
                );
        }
    }

    /**
     * Get a UCB plugin object from a UCB plugin ID.
     *
     * This is unlikely to fail; this has been checked to exist no doubt several
     * times before getting to this point.
     *
     * Throws an exception if plugin doesn't exist or other wire failure
     *
     * @param pluginId              The string plugin ID (class name)
     * @return Plugin object
     * @throws MigrateException on any error
     */
    protected Plugin getUcbPluginFromId(final String pluginId)
              throws MigrateException
    {
        try {
            Plugin ret =  Factories.getPluginFactory()
                                   .getPluginByPluginId(pluginId);

            if(ret == null) {
                throw new MigrateException("UCB needs plugin: " + pluginId);
            }

            return ret;
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) { // UCB's wide-net error handling
            throw new MigrateException("Error from UCB", e);
        }
    }

    /**
     * Plugins get their source config name from the property value group,
     * not the source config.  This makes things nice and confusing.
     *
     * So we have to make a different version of this call.
     *
     * @param pvg       our ProperyValueGroup
     * @return the appropriate name, available for us
     * @throws MigrateException on general failure, NameInUse if name already in
     *         use.
     */
    protected String generateName(PropertyValueGroup pvg)
              throws MigrateException, NameInUse
    {
        StringBuffer sb = new StringBuffer(pvg.getName().length() + 32);

        sb.append(pvg.getName())
          .append(" (")
          .append(pvg.getId())
          .append(")");

        // Check it
        try {
            if(Factories.getSourceTemplateFactory()
                        .getTemplateByName(sb.toString()) == null) {
                // We're good to go
                return sb.toString();
            }

            // So sad!
            LOG.warn("Source Template {} already exists!", sb.toString());
            throw new NameInUse();
        } catch(NameInUse e) {
            throw e;
        } catch(Exception e) { // UCB :P
            throw new MigrateException("Failure communicating with UCB", e);
        }
    }

    /**
     * Creates a HashMap of properties converting a
     * "PropertyValueGroup" object to a map of repository properties
     * for creating a new property.  Run by 'loadRepository'.
     *
     * This should be overriden by plugin children, though because
     * this is also a concrete class I cannot make it abstract.
     *
     * This is a design flaw, I'll admit.  AM-24: resolve design flaw :P
     *
     * @param pvg    Property value group to convert
     * @return HashMap of properties
     * @throws MigrateException on failure
     */
    protected HashMap<String, String> getRepoProperties(PropertyValueGroup pvg)
              throws MigrateException
    {
        throw new MigrateException("Child class must define getRepoProperties");
    }

    /**
     * Look up a repo from a PropertyValueGroup ID.  Used by plugins to load
     * their repo data.
     *
     * @param context our context
     * @param id         our PVG id
     * @param plugin     our plugin object that corresponds to this repo.
     * @return a Repository UCB client object.
     * @throws MigrateException on any failure (probably comm error)
     */
    protected Repository loadRepository(UcbContext context, String id,
                                        Plugin plugin)
              throws MigrateException
    {
        try {
            synchronized(repoMap) {
                if(repoMap.containsKey(id)) {
                    return repoMap.get(id);
                }

                PropertyValueGroup repo =
                        PropertyValueGroupFactory.getInstance()
                                                 .restore(Long.parseLong(id));

                // this shouldn't be null -- programmer error if so probably.
                if(repo == null) {
                    throw new MigrateException(
                        "Unknown PVG Id for repo: " + id
                    );
                }

                // Find out if we have a repo already; if so, use it.
                // Otherwise, make it
                String repoName = repo.getName() + " (" + id + ")";

                Repository ucbRepo = Factories.getRepositoryFactory()
                                              .getRepositoryByName(repoName);

                if(ucbRepo != null) { // add it to our map and return it.
                    repoMap.put(id, ucbRepo);
                    return ucbRepo;
                }

                RepositoryBuilder rb = new RepositoryBuilder();
                rb.name(repoName)
                  .pluginId(plugin.getPluginId())
                  .properties(getRepoProperties(repo));

                // Description field is in the Repository object associated
                // with the PVG.  AM-25: take the time to root it out.  Right
                // now, I just don't care.  Few people use 'description'
                // anyway.

                // Create repo
                ucbRepo = Factories.getRepositoryFactory()
                                   .createRepository(rb, context.teamMappings);

                // add it
                repoMap.put(id, ucbRepo);

                //return it
                return ucbRepo;
            }
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) { // UCB
            throw new MigrateException("Error from UCB: ", e);
        }
    }
}
