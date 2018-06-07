package net.epicforce.migrate.ahp.toucb.source;

/*
 * SourceFactory.java
 *
 * This loads the appropriate AbstractSourceConfig decendant based
 * on a passed AHP Source Config object.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.lang.ClassNotFoundException;
import java.lang.IllegalAccessException;
import java.lang.InstantiationException;
import java.lang.NoSuchMethodException;
import java.lang.reflect.InvocationTargetException;

import net.epicforce.migrate.ahp.exception.UnsupportedClassException;

import com.urbancode.anthill3.domain.source.SourceConfig;


public class SourceFactory
{
    /**
     * Class loader, returns an AbstractSourceConfig decendant.
     *
     * Classes will be searched for under the 'config' subpackage.
     *
     * @param sc SourceConfig object from AHP
     * @return AbstractSourceConfig decendant
     * @throws UnsupportedClassException on failure
     */
    public static AbstractSourceConfig loadSourceConfig(SourceConfig sc)
           throws UnsupportedClassException
    {
        String className = "net.epicforce.migrate.ahp.toucb.source.config."
                           + sc.getClass().getSimpleName()
                           + "Migrate";

        try {
            return (AbstractSourceConfig)Class.forName(className)
                                              .getDeclaredConstructor()
                                              .newInstance();
        } catch(NoSuchMethodException | InstantiationException |
                IllegalAccessException | InvocationTargetException |
                ClassNotFoundException e) {
            throw new UnsupportedClassException("Unsupported source config", e);
        }
    }
}
