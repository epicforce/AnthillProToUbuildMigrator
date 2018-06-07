package net.epicforce.migrate.ahp.toucb.source.config.plugin;

/*
 * GitPlugin.java
 *
 * This handles the AHP GIT source plugin.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.script.WorkDirScript;
import net.epicforce.migrate.ahp.toucb.source.AbstractSourceConfig;
import net.epicforce.migrate.ahp.toucb.source.config.PluginSourceConfigMigrate;

import com.urbancode.anthill3.domain.property.PropertyValueGroup;
import com.urbancode.anthill3.domain.source.plugin.PluginSourceConfig;
import com.urbancode.ubuild.client.Factories;
import com.urbancode.ubuild.client.plugin.Plugin;
import com.urbancode.ubuild.client.repository.Repository;
import com.urbancode.ubuild.client.script.workdir.WorkingDirectoryScript;


public class GitPlugin extends PluginSourceConfigMigrate
{
    /**
     * Run method for plugin
     *
     * @param context       Our context
     * @param sc            Source configuration
     * @param plugin        An appropriate UCB plugin object.
     * @throws MigrateException on failure.
     */
    public LinkedList<TemplateRepoPair>
           run(UcbContext context, PluginSourceConfig sc, Plugin plugin)
           throws MigrateException
    {
        // No need to check plugin -- PluginSourceConfigMigrate checked for us.
        // Start constructing our template.
        LinkedList<TemplateRepoPair> ret = new LinkedList<>();

        try {
            // Work Dir Script
            WorkingDirectoryScript workDir = null;

            if(sc.getWorkDirScript() != null) {
                workDir = WorkDirScript.get(sc.getWorkDirScript());
            }

            // Create a template for each property group
            for(PropertyValueGroup pvg : sc.getPropertyValueGroups()) {
                // Check name, skip if we have it
                String templateName;

                try {
                    templateName = generateName(pvg);
                } catch(AbstractSourceConfig.NameInUse e) {
                    continue; // nothing to do
                }

                /* Each one of these is an individual checkout, which
                 * is a source template in UCB terms.
                 * AM-21 : we could probably reuse source templates
                 *        better, maybe construct some sort of ID hash
                 *        or use properties to make more common templates.
                 *        This is going to make a LOT of extra source templates
                 */
                HashMap<String, String> props = new HashMap<>(6);

                // this must be set
                props.put("remoteUrl", pvg.getPropertyValue("remoteUrl")
                                          .getValue()
                );

                // These are optional
                for(String p : Arrays.asList("dirOffset", "branch",
                                             "remoteName", "revision",
                                             "excludeUsers", "fileFilters")) {
                    if((pvg.getPropertyValue(p) != null) &&
                       (pvg.getPropertyValue(p).getValue().length() > 0)) {
                        props.put(p, pvg.getPropertyValue(p).getValue());
                    }
                }

                // Create the source template, add it to our return.
                ret.add(
                    new TemplateRepoPair(
                        loadRepository(
                            context,
                            pvg.getPropertyValue("repo").getValue(),
                            plugin
                        ),
                        Factories.getSourceTemplateFactory()
                                 .createSourceTemplate(
                                    plugin,
                                    templateName,
                                    workDir,
                                    props
                        )
                    )
                );
            }
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) { // UCB
            throw new MigrateException("Failure from UCB", e);
        }

        return ret;
    }

    /**
     * (@inheritdoc)
     */
    @Override
    protected HashMap<String, String> getRepoProperties(PropertyValueGroup pvg)
    {
        HashMap<String, String> ret = new HashMap<>(4);

        // AHP and UCB don't really line up here.
        // AM-23 : Anything I need to do for triggers?  I'm not sure if
        // this is doable on my end or if this needs installation on the
        // repo.  Maybe warn if a trigger is set?

        if(pvg.getPropertyValue("commandPath") != null) {
            ret.put("commandPath", pvg.getPropertyValue("commandPath")
                                      .getValue()
            );
        }

        if(pvg.getPropertyValue("repoBaseUrl") != null) {
            ret.put("repoBaseUrl", pvg.getPropertyValue("repoBaseUrl")
                                       .getValue()
            );
        }

        if(pvg.getPropertyValue("username") != null) {
            ret.put("username", pvg.getPropertyValue("username")
                                   .getValue()
            );
        }

        if(pvg.getPropertyValue("password") != null) {
            ret.put("password", pvg.getPropertyValue("password")
                                   .getValue()
            );
        }

        // Https no verify, Changelog user field, and source viewer
        // are not supported by UCB.
        return ret;
    }
}
