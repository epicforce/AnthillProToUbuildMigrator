package net.epicforce.migrate.ahp.toucb.notification;

/*
 * EventSelector.java
 *
 * This class is for loading and creating EventSelectors.
 *
 * Note, this is a private class because using it outside
 * the package could result in a deadlock.  Scheme is the
 * only class that should be using this class.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import com.urbancode.anthill3.domain.notification.ScriptedWorkflowCaseSelector;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.ucb.UcbClient;


class EventSelector
{
    /*
     * Keep track of our loaded event selectors.  Maps name to ID
     */
    private static HashMap<String, String> knownEvents = new HashMap<>();

    /**
     * Load an EventSelector ID string for a given 
     * ScriptedWorkflowCaseSelector  (the only type supported at the moment)
     *
     * @param event     The event selector to migrate
     * @return an ID string of the returned selector
     * @throws MigrateException on error, NotFoundException if we can't
     *         load the event selector -- because UCB only supports JS
     *         and AHP only supports BSH, this means if the selector
     *         isn't there we can't load it.  AM-58: be able to migrate
     *         this.
     */
    public static String get(ScriptedWorkflowCaseSelector event)
          throws MigrateException, NotFoundException
    {
        // These events map straight across and we don't have to
        // load anything.  -4 through -7 aren't in UCB.
        // These can be deleted out of UCB, but that would be an
        // odd case.
        if((event.getId() <= 0) && (event.getId() > -11) &&
           ((event.getId() > -4) || (event.getId() < -7))) {
            return String.valueOf(event.getId());
        }

        synchronized(knownEvents) {
            if(knownEvents.size() == 0) {
                // Load 'em - AM-9: Centralized theser URLs
                UcbClient fetcher = new UcbClient(
                    "/tasks/admin/notification/caseselector/WorkflowCaseSelectorTasks/viewList"
                );

                knownEvents.putAll(
                    fetcher.harvestPattern(
                        "viewWorkflowCaseSelector\\?workflowCaseSelectorId=(-?\\d+)\">([^<]+)</a>",
                        null
                    )
                );
            }

            if(knownEvents.containsKey(event.getName())) {
                return knownEvents.get(event.getName());
            }
        }

        // We can't migrate these
        throw new NotFoundException(
            "We are unable to import AHP Event Selectors into UCB " +
            "because UCB uses Javascript for these and AHP uses " +
            "beanshell.  You must manually import this Event Selector: " +
            event.getName()
        );
    }
}
