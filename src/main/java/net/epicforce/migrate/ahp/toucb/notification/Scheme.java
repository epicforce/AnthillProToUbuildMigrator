package net.epicforce.migrate.ahp.toucb.notification;

/*
 * Scheme.java
 *
 * Class to load and cache notification scheme mappings.
 *
 * If a notification scheme doesn't exist, it will try to create
 * it if it can.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import com.urbancode.anthill3.domain.notification.NotificationScheme;
import com.urbancode.anthill3.domain.notification.NotificationSchemeWhoWhen;
import com.urbancode.anthill3.domain.notification.NotificationSchemeWhoWhenMedium;
import com.urbancode.anthill3.domain.notification.ScriptedWorkflowCaseSelector;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.ucb.UcbClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class Scheme
{
    private final static Logger LOG = LoggerFactory.getLogger(Scheme.class);

    /*
     * Keep track of our loaded schemes.  Map name to an ID string.
     */
    private static HashMap<String, String> knownSchemes = new HashMap<>();

    /**
     * Get a notification scheme ID string from a given NotificationScheme
     * object.
     *
     * @param scheme        The notification scheme to load
     * @return an ID string
     *
     * @throws MigrateException on error, NotFoundException if we are not
     *         able to create the scheme for some reason.
     */
    public static String get(NotificationScheme scheme)
           throws MigrateException, NotFoundException
    {
        // If the scheme is -1 or 0, just use it.
        // Should we verify these schemes are still in UCB?
        // You could delete them, though that's probably a bad idea.
        if((scheme.getId() == 0) || (scheme.getId() == -1)) {
            return String.valueOf(scheme.getId());
        }

        synchronized(knownSchemes) {
            if(knownSchemes.size() == 0) { // Load 'em
                refreshSchemes();
            }

            // Return it if we've got it
            if(knownSchemes.containsKey(scheme.getName())) {
                return knownSchemes.get(scheme.getName());
            }

            /*
             * Try to make it if we don't.
             *
             * We need to build our notification scheme
             *
             * Then add who-when's.  For each who-when:
             * * We need a recipient generator,
             * * We need an event selector
             * * And we need a notification template.
             *
             * This is, unfortunately, done with a very complex
             * CID-based process.
             */
            HashMap<String, String> props = new HashMap<>(6);

            props.put("Save", "Save");

            if(scheme.getDescription() != null) {
                props.put("description", scheme.getDescription());
            } else {
                props.put("description", "");
            }

            props.put("name", scheme.getName());
            props.put("notificationSchemeId", "");

            // try to post it.  This will return a 302 on success.
            try {
                // AM-9 : centralize
                UcbClient client = new UcbClient(
                    "/tasks/admin/notification/scheme/NotificationSchemeTasks/saveNotificationScheme"
                );

                String cid = client.getCIDFromGet(
                    "/tasks/admin/notification/scheme/NotificationSchemeTasks/newNotificationScheme",
                    null
                );

                props.put("cid", cid);
                client.sendRawPost(props);
            } catch(MigrateException e) {
                if(!e.getMessage().endsWith("302")) {
                    throw e; // 302 isn't an error
                }
            }

            // Refresh
            refreshSchemes();

            // We should now have a Notification Scheme ID
            String nsId = knownSchemes.get(scheme.getName());

            if(nsId == null) {
                throw new MigrateException(
                    "Was unable to create notification scheme: " +
                    scheme.getName()
                );
            }

            // Loop over who/when's and migrate as needed.
            for(NotificationSchemeWhoWhen ww : scheme.getWhoWhenArray()) {
                // Event selector
                String workflowCaseId;

                try {
                    workflowCaseId =
                        EventSelector.get(
                            (ScriptedWorkflowCaseSelector)ww.getWhenSelector()
                        );
                } catch(NotFoundException e) {
                    LOG.warn(
                        "We could not migrate a Who/When for Notification " +
                        "Scheme {} - this is because it requires the " +
                        "creation of an Event Selector script that does not " +
                        "exist in UCB.  We cannot migrate AHP Event " +
                        "Selector scripts because the languages are " +
                        "incompatible.  You will have to manually correct " +
                        "this.  The problem event selector script is {}.",
                        scheme.getName(), ww.getWhenSelector().getName()
                    );
                    continue; // skip this iteration.
                }

                // Recipient Generators
                String whoGenerator = null;
                String bccGenerator = null;
                String ccGenerator = null;

                if(ww.getWhoGenerator() != null) {
                    try {
                        whoGenerator = Generator.get(ww.getWhoGenerator());
                    } catch(NotFoundException e) {
                        LOG.warn(
                            "For Notification Scheme " + scheme.getName() +
                            ": " + e.getMessage()
                        );

                        // This one is fatal, we have to skip if we
                        // can't migrate the 'who'.  The other two
                        // are optional.
                        continue;
                    }
                }

                if(ww.getWhoBccGenerator() != null) {
                    try {
                        bccGenerator = Generator.get(ww.getWhoBccGenerator());
                    } catch(NotFoundException e) {
                        LOG.warn(
                            "For Notification Scheme " + scheme.getName() +
                            ": " + e.getMessage()
                        );
                    }
                }

                if(ww.getWhoCcGenerator() != null) {
                    try {
                        ccGenerator = Generator.get(ww.getWhoCcGenerator());
                    } catch(NotFoundException e) {
                        LOG.warn(
                            "For Notification Scheme " + scheme.getName() +
                            ": " + e.getMessage()
                        );
                    }
                }

                // And finally, the templates.  Let's create them
                // if we need to.
                for(NotificationSchemeWhoWhenMedium medium :
                    ww.getMediumTemplateArray()) {
                    Template.get(medium.getTemplate());
                }

                // Now put it all together.  Grab a CID and port it
                // over
                UcbClient client = new UcbClient(
                    "/tasks/admin/notification/scheme/NotificationSchemeTasks/saveWhoWhen"
                );

                props.clear();
                props.put("notificationSchemeId", nsId);

                String cid = client.getCIDFromGet(
                    "/tasks/admin/notification/scheme/NotificationSchemeTasks/viewNotificationScheme",
                    props
                );

                // Poke new Who When
                props.put("cid", cid);
                cid = client.getCIDFromGet(
                    "/tasks/admin/notification/scheme/NotificationSchemeTasks/newWhoWhen",
                    props
                );

                // Now, post who/when
                props.clear();
                props.put("cid", cid);
                props.put("notificationSchemeId", nsId);
                props.put("Save", "Save");
                props.put("workflowCaseSelectorId", workflowCaseId);
                props.put("notificationRecipientGeneratorId", whoGenerator);

                // Set these if they're available
                if(bccGenerator != null) {
                    props.put("notificationRecipientBccGeneratorId",
                              bccGenerator
                    );
                } else {
                    props.put("notificationRecipientBccGeneratorId", "");
                }

                if(ccGenerator != null) {
                    props.put("notificationRecipientCcGeneratorId",
                              ccGenerator
                    );
                } else {
                    props.put("notificationRecipientCcGeneratorId", "");
                }

                // Push it -- this returns 302
                try {
                    client.sendRawPost(props);
                } catch(MigrateException e) {
                    if(!e.getMessage().endsWith("302")) {
                        throw e;
                    }
                }

                // Load the next screen
                props.clear();
                props.put("cid", cid);
                cid = client.getCIDFromGet(
                    "/tasks/admin/notification/scheme/NotificationSchemeTasks/viewWhoWhen",
                    props
                );

                // Now push our medium-templates.
                props.clear();
                props.put("cid", cid);
                props.put("Save", "Save");
                props.put("notificationSchemeId", nsId);

                client.setUrl(
                    "/tasks/admin/notification/scheme/NotificationSchemeTasks/saveMediumTemplate"
                );

                // For poking the CID
                HashMap<String, String> pokeProps = new HashMap<>(2);
                pokeProps.put("cid", cid);
                pokeProps.put("notificationSchemeId", nsId);

                for(NotificationSchemeWhoWhenMedium medium :
                    ww.getMediumTemplateArray()) {
                    // Poke this
                    cid = client.getCIDFromGet(
                        "/tasks/admin/notification/scheme/NotificationSchemeTasks/newMediumTemplate",
                        pokeProps
                    );

                    String templateId = Template.get(medium.getTemplate());

                    // set us up the bomb
                    props.put("template_id", templateId);

                    // This is either EMAIL or JABBERIM or we can't
                    // do it.
                    if(medium.getMediumEnum()
                             .getName()
                             .equalsIgnoreCase("Email")) {
                        props.put("notificationMediumName", "Email");
                    } else {
                        props.put("notificationMediumName", "IM");
                    }

                    // this will be a 302
                    try {
                        client.sendRawPost(props);
                    } catch(MigrateException e) {
                        if(!e.getMessage().endsWith("302")) {
                            throw e;
                        }
                    }
                }
            }

            return nsId;
        }
    }

    /**
     * This refreshes the 'knownSchemes' map.  This is called a couple
     * different places, so we need to have it isolated.
     *
     * @throws MigrateException on the unlikely event of failure.
     */
    private static void refreshSchemes() throws MigrateException
    {
        // AM-9: Centralize these URLs
        UcbClient fetcher = new UcbClient(
            "/tasks/admin/notification/scheme/NotificationSchemeTasks/viewList"
        );

        knownSchemes.putAll(
            fetcher.harvestPattern(
                "viewNotificationScheme\\?notificationSchemeId=(-?\\d+)\">([^<]+)</a>",
                null
            )
        );
    }
}
