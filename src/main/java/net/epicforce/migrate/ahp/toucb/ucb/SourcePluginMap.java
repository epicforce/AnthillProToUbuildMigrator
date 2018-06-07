package net.epicforce.migrate.ahp.toucb.ucb;

/*
 * SourcePluginMap.java
 *
 * This is a map of Anthill source plugins to equivalent UCB
 * plugins for validation purposes (used by Plugin*StepConfigMigrate)
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import net.epicforce.migrate.ahp.exception.MigrateException;


public final class SourcePluginMap
{
    /*
     * Store a map of info bits, mapping AHP plugin ID to UCB info.
     */
    private static final Map<String, Info> pluginMap;

    static
    {
        Map<String, Info> temp = new HashMap<String, Info>(1);

        // Load static data
        temp.put("com.urbancode.anthill3.plugin.Git",
                 new Info("com.urbancode.air.plugin.git", "GIT SCM Plugin")
        );

        // Add it
        pluginMap = Collections.unmodifiableMap(temp);
    }

    /**
     * Load info or throw MigrateException if unknown plugin.
     *
     * @param id                AHP Plugin ID to look for
     * @return  an Info object with UCB info
     * @throws MigrateException if plugin ID is unknown
     */
    public static Info getInfo(final String id) throws MigrateException
    {
        if(!pluginMap.containsKey(id)) {
            throw new MigrateException(
                "Unknown source plugin: " + id
            );
        }

        return pluginMap.get(id);
    }

    /*
     * This holds the "info pair" of UCB plugin ID and a human-readable
     * plugin name.
     */
    public static final class Info
    {
        private final String pluginId;
        private final String name;

        /**
         * Basic constructor to load name and plugin ID
         *
         * @param pluginId  the plugin ID
         * @param name      a human readable name
         */
        public Info(final String pluginId, final String name)
        {
            this.pluginId = pluginId;
            this.name = name;
        }

        /*
         * Accessors
         */
        public final String getPluginId()
        {
            return pluginId;
        }

        public final String getName()
        {
            return name;
        }
    }
}
