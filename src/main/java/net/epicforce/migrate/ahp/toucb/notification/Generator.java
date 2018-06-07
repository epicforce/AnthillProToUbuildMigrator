package net.epicforce.migrate.ahp.toucb.notification;

/*
 * Generator.java
 *
 * This class handles NotificationRecipientGenerators,
 * and the creation (or retrieval as the case may be) of UCB
 * analogs.
 *
 * Note, this is a private class because using it outside
 * the package could result in a deadlock.  Scheme is the only
 * class that should be using this class.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import com.urbancode.anthill3.domain.notification.FixedNotificationRecipient;
import com.urbancode.anthill3.domain.notification.FixedNotificationRecipientGenerator;
import com.urbancode.anthill3.domain.notification.NotificationRecipientGenerator;
import com.urbancode.anthill3.domain.notification.RoleBasedNotificationRecipientGenerator;
import com.urbancode.anthill3.domain.notification.ScriptedNotificationRecipientGenerator;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.ucb.UcbClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


class Generator
{
    private final static Logger LOG = LoggerFactory.getLogger(Generator.class);

    /*
     * Keep track of our loaded generators.  Maps name to ID.
     */
    private static HashMap<String, String> knownGenerators = new HashMap<>();

    /**
     * This fetches a recipient generator ID from UCB, either loading
     * an existing one or creating a new one.
     *
     * If for some reason we cannot create a recipient generator in UCB
     * (right now scripted aren't supported - AM-58 to resolve), we will
     * throw the NotFoundException.
     *
     * @param generator         The generator to migrate.
     * @return an ID string of the returned generator.
     * @throws MigrateException on error, NotFoundException for reasons
     *         described above.
     */
    public static String get(NotificationRecipientGenerator generator)
           throws MigrateException, NotFoundException
    {
        // Some generators map directly to UCB.  These are defaults.
        // Note that in UCB, you're able to delete these, so we
        // might aught to see if they are there.  It should be an
        // unlikely case to delete them, though.
        if((generator.getId() <= 0) && (generator.getId() > -4)) {
            return String.valueOf(generator.getId());
        }

        // Otherwise, check if it exists; create it if it doesn't.
        synchronized(knownGenerators) {
            if(knownGenerators.size() == 0) {
                refreshGenerators();
            }

            if(knownGenerators.containsKey(generator.getName())) {
                return knownGenerators.get(generator.getName());
            }

            // try to create it -- this will require cid and multistep.
            // We will short circuit here if it's a scripted, becase
            // we can't migrate those.

            if(generator instanceof ScriptedNotificationRecipientGenerator) {
                throw new NotFoundException(
                    "We are unable to import AHP Scripted Recipient " +
                    "Generators into UCB because AHP uses Beanshell " +
                    "and UCB uses Javascript.  You must manually import " +
                    "this generator: " + generator.getName()
                );
            }

            // Groups don't really translate well.  We could add
            // support later if there's a demand.
            if(generator instanceof RoleBasedNotificationRecipientGenerator) {
                throw new NotFoundException(
                    "We are unable to import AHP Role Based Recipient " +
                    "Generators because UCB uses a different construct " +
                    "(groups) instead.  You must manually import this " +
                    "generator: " + generator.getName()
                );
            }

            // Create our client - AM-9 fix this
            UcbClient client = new UcbClient(
                "/tasks/admin/notification/recipientgenerator/NotificationRecipientGeneratorTasks/newNotificationRecipientGenerator"
            );

            // Get our CID - AM-9 fix this
            String cid = client.getCIDFromGet(
                "/tasks/admin/notification/recipientgenerator/NotificationRecipientGeneratorTasks/newNotificationRecipientGenerator",
                null
            );

            // The first step is common.
            HashMap<String, String> props = new HashMap<>(3);

            props.put("Set", "Set");
            props.put("cid", cid);

            if(generator instanceof FixedNotificationRecipientGenerator) {
                props.put("notificationRecipientGeneratorType", "Fixed");
            } else {
                props.put("notificationRecipientGeneratorType", "Group-Based");
            }

            // Post it, and continue.
            client.sendRawPost(props);

            props.clear();

            // Fork based on type
            if(generator instanceof FixedNotificationRecipientGenerator) {
                return migrateFixed(
                        (FixedNotificationRecipientGenerator)generator,
                        client, cid
                );
            } else {
                // NOTE: This doesn't work, but I left it here
                // in case we want to support it later.
                return migrateRole(
                        (RoleBasedNotificationRecipientGenerator)generator,
                        client, cid
                );
            }
        }
    }

    /**
     * This class handles the migration of a Fixed generator
     *
     * @param generator     The generator to migrate
     * @param client        Our UCB client stacked with a CID;
     * @param cid           Our CID
     * @return String       The generated ID
     * @throws MigrateException on any failure.
     */
    private static String migrateFixed(
                                FixedNotificationRecipientGenerator generator,
                                UcbClient client, String cid)
            throws MigrateException
    {
        // Create our props
        HashMap<String, String> props = new HashMap<>(5);

        // Step 1: name and description
        props.put("cid", cid);
        props.put("notificationRecipientGeneratorId", "");
        props.put("Save", "Save");
        props.put("name", generator.getName());

        if(generator.getDescription() == null) {
            props.put("description", "");
        } else {
            props.put("description", generator.getDescription());
        }

        // Post it -- this returns 302
        try {
            // fit AM-9
            client.setUrl(
                "/tasks/admin/notification/recipientgenerator/FixedNotificationRecipientGeneratorTasks/saveGenerator"
            );

            client.sendRawPost(props);
        } catch(MigrateException e) {
            if(!e.getMessage().endsWith("302")) {
                throw e;
            }
        }

        // We should have an ID now.
        refreshGenerators();

        String newId = knownGenerators.get(generator.getName());

        // If we don't have it, big problem
        if(newId == null) {
            throw new MigrateException(
                "Tried to create a Fixed Recipient Generator, and could " +
                "not create it.  The generator in question: " +
                generator.getName()
            );
        }

        // Add our fixed stuff.  Set common props
        props.clear();
        props.put("Add", "Add");
        props.put("cid", cid);
        props.put("notificationRecipientGeneratorId", newId);

        // fit AM-9
        client.setUrl(
            "/tasks/admin/notification/recipientgenerator/FixedNotificationRecipientGeneratorTasks/addFixedRecipient"
        );


        for(FixedNotificationRecipient recip : generator.getRecipients()) {
            // MSN doesn't migrate
            if((recip.getMsnId() != null) && (recip.getMsnId().length() > 0)) {
                LOG.warn(
                    "Recipient Generator " + generator.getName() +
                    " has recipients with MSN ID's.  UCB does not" +
                    " support these, so they are ignored."
                );
            }

            // Email address
            if(recip.getEmailAddress() != null) {
                props.put("email", recip.getEmailAddress());
            } else {
                props.put("email", "");
            }

            // XMPP
            if(recip.getXmppId() != null) {
                props.put("xmpp", recip.getXmppId());
            } else {
                props.put("xmpp", "");
            }

            // Push it -- this returns 302
            try {
                client.sendRawPost(props);
            } catch(Exception e) {
                if(!e.getMessage().endsWith("302")) {
                    throw e;
                }
            }
        }

        return newId;
    }

    /**
     * This class handles the migration of a Role Based generator
     *
     * @param generator     The generator to migrate
     * @param client        Our UCB client stacked with a CID;
     * @param cid           Our CID
     * @return String       The generated ID
     * @throws NotFoundException always -- this is not yet implemented.
     */
    private static String migrateRole(
                            RoleBasedNotificationRecipientGenerator generator,
                            UcbClient client, String cid)
            throws NotFoundException
    {
        throw new NotFoundException(
            "We are unable to import AHP Role Based Recipient " +
            "Generators because UCB uses a different construct " +
            "(groups) instead.  You must manually import this " +
            "generator: " + generator.getName()
        );
    }

    /**
     * This is a method that reloads the knownGenerators list.
     *
     * @throws MigrateException on any failure
     */
    private static void refreshGenerators() throws MigrateException
    {
        // AM-9: Centralize these URLs
        UcbClient fetcher = new UcbClient(
            "/tasks/admin/notification/recipientgenerator/NotificationRecipientGeneratorTasks/viewList"
        );

        knownGenerators.putAll(
            fetcher.harvestPattern(
                "viewNotificationRecipientGenerator\\?notificationRecipientGeneratorId=(-?\\d+)\">([^<]+)</a>",
                null
            )
        );
    }
}
