package net.epicforce.migrate.ahp.toucb.notification;

/*
 * Template.java
 *
 * This class handles the nmigration of Template classes from
 * AHP to UCB.  Its a private class because improper calls can
 * cause a deadlock; only Scheme should use it.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.ucb.UcbClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class Template
{
    private final static Logger LOG = LoggerFactory.getLogger(Template.class);

    /*
     * Keep track of templates we've already loaded; map template name
     * to template ID.
     */
    private static HashMap<String, String> knownTemplates = new HashMap<>();

    /**
     * Method to get a template based on the provided (unfortunately
     * redundantly named) AHP class.  Will return the ID and create
     * the template if necessary.
     *
     * Templates made in AHP probably won't work in UCB, but we can
     * try to migrate them anyway.  There are a couple differences we
     * will WARN about, but this should not throw NotFoundException.
     *
     * @param template          The template to load
     * @return a string template ID
     * @throws MigrateException on any failure.
     */
    public static String get(
                    com.urbancode.anthill3.domain.template.Template template)
           throws MigrateException
    {
        // We have to do a static mapping of these, because the ID's
        // don't line up.  Note, this assumes the default templates
        // are in UCB ... which may be a bad assumption :)
        switch(template.getId().intValue()) {
            case -3:
                return "-2";
            case 11:
                return "-3";
            case -2:
            case -8: // This is a task template ... I guess map it to
                     // a process template.
                return "-1";
        }

        synchronized(knownTemplates) {
            if(knownTemplates.size() == 0) {
                refreshTemplates();
            }

            if(knownTemplates.containsKey(template.getName())) {
                return knownTemplates.get(template.getName());
            }

            // Otherwise, we must create it.
            if((template.getContextScript() != null) &&
               (template.getContextScript().length() > 0)) {
                LOG.warn(
                    "UCB does not support Context Scripts as part of " +
                    "Notification Templates.  Therefore, the migrated " +
                    "notification template might not work out of the box.  " +
                    "Please double-check it!  The template in question is: " +
                    template.getName()
                );
            }

            // Create our map
            HashMap<String, String> props = new HashMap<>(5);

            props.put("saveTemplate", "Set");
            props.put("name", template.getName());

            if(template.getDescription() != null) {
                props.put("description", template.getDescription());
            } else {
                props.put("description", "");
            }

            props.put("templateText", template.getTemplateText());

            // Push it -- this will return 302
            try {
                UcbClient client = new UcbClient(
                    "/tasks/admin/templates/TemplateTasks/saveTemplate"
                );

                String cid = client.getCIDFromGet(
                    "/tasks/admin/templates/TemplateTasks/newTemplate",
                    null
                );

                props.put("cid", cid);

                // Send a post
                client.sendRawPost(props);
            } catch(MigrateException e) {
                if(!e.getMessage().endsWith("302")) {
                    throw e;
                }
            }

            refreshTemplates();

            if(!knownTemplates.containsKey(template.getName())) {
                throw new MigrateException(
                    "Could not migrate Notification template with name: "
                    + template.getName()
                );
            }

            // Make a note.
            LOG.warn(
                "We have migrated a Notification Template named: " +
                template.getName() +
                "  UCB templates are slightly different from AHP templates." +
                "  It is possible you will need to tweak this template for" +
                " it to work."
            );

            return knownTemplates.get(template.getName());
        }
    }

    /**
     * This is a method that reloads the knownTemplates list.
     *
     * @throws MigrateException on any failure
     */
    private static void refreshTemplates() throws MigrateException
    {
        // AM-9: Centralize these URLs
        UcbClient fetcher = new UcbClient(
            "/tasks/admin/templates/TemplateTasks/viewList"
        );

        knownTemplates.putAll(
            fetcher.harvestPattern(
                "viewTemplate\\?template_id=(-?\\d+)\"\\s*>([^<]+)</a>",
                null
            )
        );
    }

}
