package net.epicforce.migrate.ahp.toucb.source;

/*
 * AbstractSourceConfig.java
 *
 * This is a base class for migrating source configs, which are the
 * basis of Source Templates in UCB.
 *
 * Because each type of source config is a special snowflake, we
 * will have to use a different class for each template approach.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.LinkedList;
import java.util.List;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;

import com.urbancode.anthill3.domain.source.SourceConfig;
import com.urbancode.ubuild.client.Factories;
import com.urbancode.ubuild.client.repository.Repository;
import com.urbancode.ubuild.client.template.SourceTemplate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public abstract class AbstractSourceConfig
{
    private final static Logger LOG =
                            LoggerFactory.getLogger(AbstractSourceConfig.class);

    /*****************************************************************
     * PROPERTIES
     ****************************************************************/

    protected LinkedList<TemplateRepoPair> repoTemplatePairs =
                                                            new LinkedList<>();

    /*****************************************************************
     * ACCESSORS
     ****************************************************************/

    /**
     * @return a linked list of TemplateRepoPair's.
     */
    public List<TemplateRepoPair> getRepoTemplatePairs()
    {
        return repoTemplatePairs;
    }

    /**
     * Add a repo/template pair to our linked list.
     *
     * @param repo          Our repository
     * @param template      Our source template
     */
    protected void addRepoTemplatePair(Repository repo, SourceTemplate template)
    {
        repoTemplatePairs.add(new TemplateRepoPair(repo, template));
    }

    /*****************************************************************
     * METHODS
     ****************************************************************/

    /**
     * This is the method which must be implemented, and will be
     * executed to build the Source Template.
     *
     * @param context       Our UCB context.
     * @throws MigrateException on any failure
     */
    abstract public void run(UcbContext context) throws MigrateException;

    /**
     * Generate and validate a name.
     *
     * This will take a SourceConfig object and make a template name
     * for it.  If the name is in use, it will raise NameInUse which
     * should be an indication there's nothing to do.
     *
     * @param sc            SourceConfig object
     * @param suffix        An optional suffix for the name, a space will
     *                      be injected before the suffix.
     * @return a valid name string
     *
     * Source config ID should be unique for a workflow, so its unlikely
     * multi-threading will cause a name conflict.  If in practice we get
     * problems, I will try to sort it out.
     *
     * @throws MigrateException on error, NameInUse on name already set.
     */
    protected String generateName(SourceConfig sc, String suffix)
              throws MigrateException, NameInUse
    {
        StringBuffer sb = new StringBuffer(sc.getName().length() + 32);

        sb.append(sc.getName())
          .append(" (")
          .append(sc.getId())
          .append(")");

        if(suffix != null) {
            sb.append(" ");
            sb.append(suffix);
        }

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

    protected String generateName(SourceConfig sc)
              throws MigrateException, NameInUse
    {
        return generateName(sc, null);
    }

    /*****************************************************************
     * SUBCLASSES
     ****************************************************************/

    /**
     * Local exception class
     */
    public static class NameInUse extends Exception
    {
    }

    /**
     * This class keeps track of source templates and associated repositories.
     *
     * These pairs are stored in a list on this object, so that we can easily
     * add them to the UCB project later.
     */
    public static class TemplateRepoPair
    {
        /*
         * The repository object from UCB
         */
        private Repository repo;

        /*
         * The source template object
         */
        private SourceTemplate template;

        /*
         * Constructor
         *
         * @param repo the repository object
         * @param template the template object
         */
        public TemplateRepoPair(Repository repo, SourceTemplate template)
        {
            this.repo = repo;
            this.template = template;
        }

        /*
         * Return the repo
         *
         * @return the repo object
         */
        public Repository getRepository()
        {
            return repo;
        }

        /*
         * Return the template
         *
         * @return the template object
         */
        public SourceTemplate getTemplate()
        {
            return template;
        } 
    }
}
