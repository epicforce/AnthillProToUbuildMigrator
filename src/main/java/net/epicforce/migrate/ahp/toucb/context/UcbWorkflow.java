package net.epicforce.migrate.ahp.toucb.context;

/*
 * UcbWorkflow.java
 *
 * The workflow class provides an injection point into the migrate
 * process wherein we can perform actions before and after the bulk
 * of the migration starts.
 *
 * This allows us to configure a workflow template, set up artifacts,
 * and other stuff that the steps will require to execute.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.epicforce.migrate.ahp.context.AbstractContext;
import net.epicforce.migrate.ahp.context.JobLayout;
import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.migrate.AbstractWorkflow;
import net.epicforce.migrate.ahp.toucb.notification.NotFoundException;
import net.epicforce.migrate.ahp.toucb.notification.Scheme;
import net.epicforce.migrate.ahp.toucb.script.PreCondScript;
import net.epicforce.migrate.ahp.toucb.script.WorkDirScript;
import net.epicforce.migrate.ahp.toucb.source.AbstractSourceConfig;
import net.epicforce.migrate.ahp.toucb.source.SourceFactory;
import net.epicforce.migrate.ahp.toucb.ucb.UcbClient;
import net.epicforce.migrate.ahp.toucb.ucb.Urls;

import com.urbancode.anthill3.domain.artifacts.ArtifactSet;
import com.urbancode.anthill3.domain.persistent.PersistenceException;
import com.urbancode.anthill3.domain.profile.ArtifactDeliverPatterns;
import com.urbancode.anthill3.domain.profile.BuildProfile;
import com.urbancode.anthill3.domain.project.QuietPeriodConfig;
import com.urbancode.anthill3.domain.project.QuietPeriodConfigChangeLog;
import com.urbancode.anthill3.domain.project.envprops.ProjectEnvironmentProperty;
import com.urbancode.anthill3.domain.project.prop.ProjectProperty;
import com.urbancode.anthill3.domain.servergroup.ServerGroup;
import com.urbancode.anthill3.domain.status.Status;
import com.urbancode.anthill3.domain.status.StatusFactory;
import com.urbancode.anthill3.domain.trigger.Trigger;
import com.urbancode.anthill3.domain.trigger.event.EventTrigger;
import com.urbancode.anthill3.domain.trigger.remoterequest.repository.RepositoryRequestTrigger;
import com.urbancode.anthill3.domain.trigger.scheduled.ScheduledTrigger;
import com.urbancode.anthill3.domain.workflow.JobIterationPlan;
import com.urbancode.anthill3.domain.workflow.WorkflowDefinitionJobConfig;
import com.urbancode.anthill3.domain.workflow.WorkflowProperty;
import com.urbancode.commons.graph.TableDisplayableGraph;
import com.urbancode.commons.graph.Vertex;
import com.urbancode.commons.graph.Graph;;
import com.urbancode.ubuild.client.Factories;
import com.urbancode.ubuild.client.agent.AgentConfig;
import com.urbancode.ubuild.client.agent.AgentLockDuration;
import com.urbancode.ubuild.client.agent.AgentPool;
import com.urbancode.ubuild.client.agent.AgentPoolAgentConfig;
import com.urbancode.ubuild.client.agent.NoAgentAgentConfig;
import com.urbancode.ubuild.client.agent.UseParentAgentConfig;
import com.urbancode.ubuild.client.project.process.source.SourceConfigBuilder;
import com.urbancode.ubuild.client.project.Project;
import com.urbancode.ubuild.client.project.ProjectBuilder;
import com.urbancode.ubuild.client.project.process.Process;
import com.urbancode.ubuild.client.project.process.ProcessBuilder;
import com.urbancode.ubuild.client.project.process.ProcessType;
import com.urbancode.ubuild.client.property.template.PropertyInterfaceTypeEnum;
import com.urbancode.ubuild.client.property.template.PropertyTemplate;
import com.urbancode.ubuild.client.rest.security.RESTTeamParser;
import com.urbancode.ubuild.client.rest.workflow.RESTWorkflow;
import com.urbancode.ubuild.client.script.precondition.job.JobPreConditionScript;
import com.urbancode.ubuild.client.script.precondition.step.StepPreConditionScript;
import com.urbancode.ubuild.client.template.ProcessTemplate;
import com.urbancode.ubuild.client.template.Template;
import com.urbancode.ubuild.client.shared.template.DefaultPreProcessConfig;
import com.urbancode.ubuild.client.workflow.Job;
import com.urbancode.ubuild.client.workflow.SourceConfigWorkDirConfig;
import com.urbancode.ubuild.client.workflow.UseParentWorkDirConfig;
import com.urbancode.ubuild.client.workflow.WorkDirConfig;
import com.urbancode.ubuild.client.workflow.WorkDirScriptWorkDirConfig;
import com.urbancode.ubuild.client.workflow.WorkflowNode;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import org.apache.commons.lang3.StringUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.apache.http.NameValuePair;
import org.apache.http.message.BasicNameValuePair;


public class UcbWorkflow extends AbstractWorkflow
{
    private final static Logger LOG =
                                    LoggerFactory.getLogger(UcbWorkflow.class);

    /*****************************************************************
     * STATIC PROPERTIES
     ****************************************************************/

    /*
     * Keep a running list of known artifact set names.
     * If this list is empty, it will be initialized by
     * pulling from UCB.  This is also used as a mutex
     * for a synchronized block to make sure we don't
     * get redundant / accidental artifact adds.
     */
    private static Set<String>  knownArtifactSets = new HashSet<String>();

    /*
     * Keep track of the statuses that we know about.
     *
     * This is used as a mutex so its only loaded once.  We need the
     * ID's so we will map UCB status names to string representations
     * of the ID (they are really Long's)
     */
    private static Map<String, String> knownStatusMap = new HashMap<>();

    /*
     * What's our workflow name?  We'll compute this once and keep
     * it around for consistency.
     */
    private String workflowName = null;

    /*
     * Map of AHP Job Config ID's to UCB Job objects.
     */
    private static ConcurrentHashMap<Long, Job> knownJobs =
                                            new ConcurrentHashMap<Long, Job>();

    /*
     * Load this once and use it everywhere
     */
    private static Object agentMutex = new Object();
    private static AgentPool allAgentsPool = null;

    /*****************************************************************
     * CLASS PROPERTIES
     ****************************************************************/

    /*
     * Keep track of jobs that have iterations
     */
    private List<WorkflowDefinitionJobConfig> hasIteration = new ArrayList<>();

    /*
     * We will need our process template to build the project.  It is
     * set by migrateWorkflow and used by migrateProject.
     */
    private ProcessTemplate processTemplate = null;

    /*
     * We will need our source config for creating a project.  It is
     * set by postRun and consumed by migrateProject.
     */
    private AbstractSourceConfig ucbSourceConfig = null;

    /*****************************************************************
     * STATIC METHODS
     ****************************************************************/

    /**
     * Return ID of a status in string format, given a status name.
     *
     * @param status                Status name
     * @return the ID in string format
     *
     * @throws MigrateException in the incredibly stupendously unlikely
     *         event that we don't have this status loaded.  That's probably
     *         programmer error.
     *
     * Note: This is read across multiple threads, however it should be
     *       safe because the loading of this data is in a sync block
     *       and all threads should be stopped by that sync block.  This
     *       data should therefore be loaded and ready by the time it
     *       gets read.
     */
    public static String getStatusId(String status) throws MigrateException
    {
        if(!knownStatusMap.containsKey(status.toLowerCase())) {
            throw new MigrateException(
                "Could not locate status " + status + " in known status map."
                + "  This is probably a programming error."
            );
        }

        return knownStatusMap.get(status);
    }

    /*****************************************************************
     * METHODS
     ****************************************************************/

    /**
     * preRun
     *
     * @param context       Context of the migration
     *
     * This is run before the migration.  Context will just contain
     * the AHP workflow and nothing else at this stage.
     *
     * @throws MigrateException on failure
     */
    public void preRun(UcbContext context)
           throws MigrateException
    {
        // Compute our workflow name, make sure its available.
        workflowName = context.getWorkflow().getName() + " (" +
                       String.valueOf(context.getWorkflow().getId())
                       + ")";

        // Does it already exist?
        try {
            if(Factories.getProcessTemplateFactory()
                        .getTemplateByName(workflowName) != null) {
                throw new MigrateException(
                    "Process Template with name " + workflowName +
                    " already exists!"
                );
            }
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            // Cause UCB throws Exception
            throw new MigrateException("Communication falure with UCB", e);
        }

        // Start building it.
        try {
            // Grab our workflow's build profile.  Repo's and
            // artifacts come from there, and we need to configure
            // those first.
            BuildProfile bp = context.getWorkflow().getBuildProfile();

            /*
             * The following things are system global and need to
             * be configured first.  We'll check to be sure they
             * don't already exist to avoid making duplicates.
             *
             * * Artifact Set
             * * Statuses
             */

            // ARTIFACT SETS
            migrateArtifactSets(bp);

            // STATUSES
            migrateStatuses();
        } catch(JSONException e) {
            throw new MigrateException("Failed to process JSON", e);
        } catch(PersistenceException e) {
            throw new MigrateException("Could not communicate with AHP", e);
        }
    }

    /**
     * (@inheritdoc)
     */
    @Override
    public void preRun(AbstractContext context)
           throws MigrateException
    {
        preRun((UcbContext)context);
    }

    /**
     * Method to load artifact sets, just to keep preRun from
     * growing out of control.
     *
     * @param bp            Build Profile
     * @throws  MigrateException or JSONException
     */
    private void migrateArtifactSets(BuildProfile bp)
            throws MigrateException, JSONException
    {
        ArtifactDeliverPatterns[] artPat = bp.getArtifactConfigArray();

        UcbClient artClient = new UcbClient(Urls.ARTIFACTSET);

        // This is a critical section -- because each workflow thread
        // may try to import the same artifact sets otherwise.
        synchronized(knownArtifactSets) {

            // Load artifacts if we don't have them
            if(knownArtifactSets.size() == 0) {
                for(JSONObject a : artClient.getAll()) {
                    // AM-10: Take into account active/inactive?
                    knownArtifactSets.add(a.getString("name"));
                }
            }

            for(ArtifactDeliverPatterns ap : artPat) {
                ArtifactSet as = ap.getArtifactSet();

                // See if its already in UCB.
                if(!knownArtifactSets.contains(as.getName())) {
                    // Add it
                    JSONObject a = new JSONObject();
                    a.put("name", as.getName());

                    if(as.getDescription() == null) {
                        a.put("description", "");
                    } else {
                        a.put("description", as.getDescription());
                    }

                    a.put("active", true);
                    a.put("inactive", false);

                    // send it
                    artClient.sendPostReturnObject(a.toString());

                    // Add it as known
                    knownArtifactSets.add(as.getName());
                }
            }
        }
    }

    /**
     * Method to load status information, again to keep preRun from
     * getting huge.
     *
     * NOTE: UCB Statuses are CASE INSENSITIVE!  We will therefore
     * store and check all status name keys after running them
     * through tolowerCase.
     *
     * @throws MigrateException or JSONException
     */
    private void migrateStatuses()
            throws MigrateException, JSONException, PersistenceException
    {
        synchronized(knownStatusMap) {
            // Nothing to do if its already initialized.
            // This is global so we only need to do this once.
            if(knownStatusMap.size() > 0) {
                return;
            }

            // Otherwise, let's fetch the UCB things and merge in the
            // statuses we don't have.
            UcbClient artClient = new UcbClient(Urls.STATUS);

            for(JSONObject status : artClient.getAll()) {
                // AM-11: Take into account isDeleted ?
                knownStatusMap.put(status.getString("name").toLowerCase(),
                                   String.valueOf(status.getLong("id"))
                );
            }

            /* Iterate over known statuses
             * AM-12: UCB ties cleanup to status, where AHP ties
             * cleanup to Life cycle model.
             *
             * The problem here is collisions; AHP may have many
             * statuses of the same name in different life cycles with
             * different cleanup parameters, whereas UCB congeals it
             * all into one.
             *
             * I am not sure what the preferred way of migrating this
             * would be, so I'm ignoring that aspect for now.
             */
            for(Status status : StatusFactory.getInstance().restoreAll()) {
                if(!knownStatusMap.containsKey(status.getName()
                                                     .toLowerCase())) {
                    // Add it
                    JSONObject a = new JSONObject();
                    a.put("name", status.getName());

                    if(status.getDescription() == null) {
                        a.put("description", "");
                    } else {
                        a.put("description", status.getDescription());
                    }

                    if(status.getColor() == null) {
                        a.put("color", "");
                    } else {
                        a.put("color", status.getColor());
                    }

                    // AHP has a noition of "is archived?" "is failure?"
                    // "Is success?" and "is locked?" that UCB does not
                    // seem to have.

                    // UCB has an "isDeleted" which AHP doesn't seem
                    // to have.
                    a.put("isDeleted", false);

                    // ship it
                    JSONObject r = artClient.sendPostReturnObject(a.toString());

                    // Process out the ID.
                    knownStatusMap.put(status.getName().toLowerCase(),
                                       String.valueOf(r.getLong("id"))
                    );
                }
            }
        }
    }

    /**
     * postRun
     *
     * @param context       Context of the migration
     *
     * This is run after the entire migration.
     * @throws MigrateException on any failure
     */
    public void postRun(UcbContext context) throws MigrateException
    {
        /*
         * After we've migrated our jobs, we still have a lot to do.
         *
         * First, we have to create our process.
         * Then we need a project template.
         * And finally, we make the project itself.
         */
        try {
            // This creates a process template
            migrateWorkflow(context);

            // Now, make source template
            ucbSourceConfig = SourceFactory.loadSourceConfig(
                                        context.getWorkflow()
                                               .getBuildProfile()
                                               .getSourceConfig()
            );

            ucbSourceConfig.run(context);

            // And now, create a project template and project.
            migrateProject(context);
        } catch(JSONException e) {
            throw new MigrateException("Got JSON processing error", e);
        }
    }

    /**
     * Create a project template, and then the project itself.
     *
     * This should be the last thing we have to do.
     *
     * @param context       Migration context
     * @throws MigrateException on any failure
     */
    protected void migrateProject(UcbContext context) throws MigrateException
    {
        // Create our project template if needed
        try {
            Template projectTemplate;
            String templateName = context.getWorkflow().getProject().getName()
                                  + " Template (" +
                                  context.getWorkflow().getProject().getId()
                                  + ")";

            projectTemplate = Factories.getTemplateFactory()
                                       .getTemplateByName(templateName);
            String templateId;

            if(projectTemplate != null) {
                templateId = projectTemplate.getId();
            } else {
                // We need to make the template.
                // Let's create it, and then push modifications via
                // the web form for stuff that it doesn't support.
                // (looks like it may only be 'timeout' that isn't
                // supported)

                projectTemplate =
                    Factories.getTemplateFactory()
                             .createTemplate(
                                templateName,
                                context.getWorkflow().getProject()
                                       .getDescription(),
                                new DefaultPreProcessConfig(),
                                context.teamMappings
                );
                templateId = projectTemplate.getId();

                /* PROPERTY HANDLING
                 *
                 * So AHP has properties per environment, but only
                 * one environment is coupled to the originating workflow.
                 *
                 * We'll migrate properties affixed to the originating
                 * workflow, and the "all" group as well.
                 */

                for(ProjectProperty pp : context.getWorkflow()
                                                .getProject()
                                                .getPropertyList()) {
                    PropertyTemplate newProp;

                    if(pp.isSecure()) {
                        newProp = projectTemplate.createProperty(
                                        pp.getName(),
                                        PropertyInterfaceTypeEnum.TEXT_SECURE
                        );
                    } else {
                        newProp = projectTemplate.createProperty(
                                        pp.getName(),
                                        PropertyInterfaceTypeEnum.TEXT
                        );
                    }

                    // Default to empty string
                    if(pp.getDescription() != null) {
                        newProp.setDescription(pp.getDescription());
                    }

                    // Add property value
                    newProp.setValue(pp.getValue(), pp.isSecure());
                }

                // And now build environment properties
                ServerGroup[] sg = context.getWorkflow().getServerGroupArray();

                // This shouldn't be possible.
                if(sg.length != 1) {
                    throw new MigrateException(
                        "Workflow should have exactly one server group " +
                        "(which is to say environment), for some reason it " +
                        "does not."
                    );
                }

                // Grab our properties and go
                for(ProjectEnvironmentProperty pep :
                                context.getWorkflow()
                                       .getProject()
                                       .getEnvironmentProperties(sg[0])) {
                    // and these are just slightly different.
                    // AHP has so many different properties objects cause
                    // reasons.
                    PropertyTemplate newProp;

                    if(pep.isSecure()) {
                        newProp = projectTemplate.createProperty(
                                        pep.getName(),
                                        PropertyInterfaceTypeEnum.TEXT_SECURE
                        );
                    } else {
                        newProp = projectTemplate.createProperty(
                                        pep.getName(),
                                        PropertyInterfaceTypeEnum.TEXT
                        );
                    }

                    // Default to empty string
                    if(pep.getDescription() != null) {
                        newProp.setDescription(pep.getDescription());
                    }

                    // Add property value
                    newProp.setValue(pep.getValue(), pep.isSecure());
                }

                // Save it
                projectTemplate.update();

                // We only need to configure Build Pre-Processing if
                // our Quiet Period type is QuietPeriodConfigChangeLog
                // That's the only type UCB supports.
                QuietPeriodConfig qpc = context.getWorkflow()
                                               .getProject()
                                               .getQuietPeriodConfig();

                // Set the rest only if its a QuietPeriodConfigChangeLog
                if(qpc instanceof QuietPeriodConfigChangeLog) {
                    QuietPeriodConfigChangeLog qpccl =
                        (QuietPeriodConfigChangeLog)qpc;
                    // Set up our UCB client
                    // AM-9 : put this URL someplace more sensible?
                    UcbClient client = new UcbClient(
                        "/tasks/admin/project/template/ProjectTemplateTasks/saveBuildPreProcess"
                    );

                    // Create our value list.  we can have duplicate
                    // values so we can't use a simple map here.
                    List<NameValuePair> vals = new ArrayList<>(10);

                    vals.add(new BasicNameValuePair("projectTemplateId",
                                                    templateId
                            )
                    );
                    vals.add(new BasicNameValuePair("Save", "Save"));

                    // AHP and UCB have very incompatible means of handling
                    // agent selection.  we'll hardcode this to all agents
                    // for now.
                    // AM-13: Is there a better way?  Should probably at
                    // least not assume pool_1 -- I think there's another
                    // place I hardcode this, so centralize it.
                    vals.add(new BasicNameValuePair("agentPoolId", "pool_1"));
                    vals.add(new BasicNameValuePair("agentPoolTypeRadio",
                                                    "agentPool")
                    );

                    // Put labels
                    for(String label : qpccl.getLabelValues()) {
                        vals.add(new BasicNameValuePair("labelValue", label));
                    }

                    // AHP stores these as milliseconds, and only has
                    // quiet period and not merge period.  Thus, we
                    // will assume the same value for both.
                    long ms = context.getWorkflow()
                                     .getProject()
                                     .getQuietPeriod();

                    if(ms < 1000) {
                        ms = 0;
                    } else {
                        ms = ms / 1000;
                    }

                    vals.add(new BasicNameValuePair("mergePeriodDuration",
                                                    String.valueOf(ms))
                    );
                    vals.add(new BasicNameValuePair("quietPeriodDuration",
                                                    String.valueOf(ms))
                    );
                    vals.add(new BasicNameValuePair("shouldCleanup",
                                 String.valueOf(qpccl.getShouldCleanup()))
                    );

                    // Stamp values
                    for(String stamp : qpccl.getStampValues()) {
                        vals.add(new BasicNameValuePair("stampValue", stamp));
                    }

                    StepPreConditionScript ppc = null;

                    if(qpccl.getPreConditionScript() != null) {
                        ppc = PreCondScript.getStepPreconditionScript(
                                      qpccl.getPreConditionScript()
                       );
                    }

                    // Handle precond
                    if(ppc != null) {
                        vals.add(
                            new BasicNameValuePair(
                                "stepPreConditionScriptId", ppc.getId()
                            )
                        );
                    } else {
                        vals.add(
                            new BasicNameValuePair(
                                "stepPreConditionScriptId", ""
                            )
                        );
                    }

                    // set timeout
                    vals.add(
                        new BasicNameValuePair(
                            "timeoutDuration",
                            String.valueOf(qpccl.getTimeout())
                        )
                    );

                    HashMap<String, String> props = new HashMap<>(1);
                    props.put("projectTemplateId", templateId);

                    synchronized(UcbClient.cidLock) {
                        // Get CID.  AM-9: centarlize this URL?
                        String cid = client.getCIDFromGet(
                            "/tasks/admin/project/template/ProjectTemplateTasks/viewBuildPreProcess",
                            props
                        );

                        // Add CID
                        vals.add(new BasicNameValuePair("cid", cid));

                        // Push it -- 302 is okay in this case
                        try {
                            client.sendRawPost(vals);
                        } catch(MigrateException e) {
                            if(!e.getMessage().endsWith("302")) {
                                throw e;
                            }
                        }
                    }
                }

                // Otherwise, we've done as much as we can here.
                // Re-load our template.
                projectTemplate = Factories.getTemplateFactory()
                                           .getTemplateByName(templateName);
            }

            // Create our project
            ProjectBuilder projBuild = new ProjectBuilder();
            projBuild.name(context.getWorkflow().getProject().getName()
                           + " (" + context.getWorkflow().getProject().getId()
                           + ")"
            );
            projBuild.templateId(templateId);

            if(context.getWorkflow().getProject().getDescription() != null) {
                projBuild.description(context.getWorkflow()
                                             .getProject()
                                             .getDescription()
                );
            }

            // Create a project
            Project proj = Factories.getProjectFactory()
                                    .createProject(projBuild,
                                                   context.teamMappings
            );

            // We need a map of templated properties.  Unfortunately,
            // UCB doesn't seem to pick up default values by.. well,
            // default.
            HashMap<String, String> defaults = new HashMap<>(
                                        processTemplate.getProperties().size()
            );

            for(PropertyTemplate pt : processTemplate.getProperties()) {
                defaults.put(pt.getName(), pt.getValue().getValue());
            }

            // Link the process
            ProcessBuilder procBuilder = new ProcessBuilder();
            procBuilder.name(processTemplate.getName())
                       .active(true)
                       .templateId(processTemplate.getId())
                       .projectId(proj.getId())
                       .type(ProcessType.BUILD)
                       .templatedProperties(defaults);

            if(processTemplate.getDescription() != null) {
                procBuilder.description(processTemplate.getDescription());
            }

            // Create it
            Process process = Factories.getProcessFactory()
                                       .createProcess(procBuilder,
                                                      context.teamMappings
            );

            // Transfer Triggers
            for(Trigger t : context.getWorkflow().getTriggerArray()) {
                // Only migrate active
                if(!t.isEnabled()) {
                    continue;
                }

                // We can't migrate EventTriggers
                if(t instanceof EventTrigger) {
                    StringBuilder warnMessage = new StringBuilder(128);
                    warnMessage.append("Workflow ")
                               .append(context.getWorkflow().getName())
                               .append(" (")
                               .append(context.getWorkflow().getId())
                               .append(" ) has an event trigger named ")
                               .append(t.getName())
                               .append(".  UCB does not support this type of ")
                               .append("trigger.");

                    LOG.warn(warnMessage.toString());
                    continue;
                }

                // Otherwise, migrate.
                HashMap<String, String> triggerParams = new HashMap<>(1);
                HashMap<String, String> triggerProps = new HashMap<>(11);
                triggerParams.put("workflowId", process.getId());

                synchronized(UcbClient.cidLock) {
                    // Get CID.  AM-9: centralize this URL?
                    UcbClient tClient = new UcbClient(
                        "/tasks/admin/project/workflow/WorkflowTasks/viewTriggerTypes"
                    );

                    // Get CID.  AM-9: centralize this URL?
                    String cid = tClient.getCIDFromGet(
                        "/tasks/admin/project/workflow/WorkflowTasks/viewTriggerTypes",
                        triggerParams
                    );

                    // Add common params
                    triggerProps.put("cid", cid);
                    triggerProps.put("workflowId", process.getId());
                    triggerProps.put("Select", "Select");

                    // Start the trigger process.
                    if(t instanceof ScheduledTrigger) {
                        triggerProps.put(
                            "triggerType",
                            "com.urbancode.ubuild.domain.trigger.scheduled.ScheduledTrigger"
                        );
                    } else if(t instanceof RepositoryRequestTrigger) {
                        triggerProps.put(
                            "triggerType",
                            "com.urbancode.ubuild.domain.trigger.remoterequest.repository.RepositoryRequestTrigger"
                        );
                    } else { // error
                        throw new MigrateException(
                            "Unknown/unsupported trigger type: "
                            + t.getClass().getName()
                            + " (this shouldn't happen)"
                        );
                    }

                    // Submit it - AM-9 centralize URL
                    tClient.setUrl(
                        "/tasks/admin/project/workflow/WorkflowTasks/newTrigger"
                    );

                    tClient.sendRawPost(triggerProps);

                    // Verify the results?  Dunno if I care,
                    // I would think UCB would return an error code if
                    // there was a problem which would be an exception.

                    // Clean out what we don't need
                    triggerProps.remove("triggerType");
                    triggerProps.remove("Select");

                    // Add common bits
                    triggerProps.put("saveTrigger", "save");
                    triggerProps.put("enabled", "true");
                    triggerProps.put("name", t.getName());
                    triggerProps.put("priority",
                                     String.valueOf(t.getPriority().getId())
                    );

                    // If it's a trigger type, push a warning.
                    // If it's a schedule type, set the schedule.
                    if(t instanceof ScheduledTrigger) {
                        ScheduledTrigger st = (ScheduledTrigger)t;

                        // Luckily the ID's happen to line up.
                        triggerProps.put("scheduleId",
                            String.valueOf(st.getSchedule().getId())
                        );
                    } else { // we eliminated others above.
                        LOG.warn(
                        "Migrating a Repository Request trigger.  Note that " +
                        "you will need to correct the trigger URL.  This is " +
                        "a manual process; you can get the trigger URL from " +
                        "UCB.  Contact Epic Force for further assistance if " +
                        "you are unsure of how to do this."
                        );
                    }

                    // Any properties we need to push?
                    for(String propName : t.getPropertyNames()) {
                        triggerProps.put(
                            "property:" + propName,
                            t.getProperty(propName)
                        );
                    }

                    // And then push 'em
                    tClient.setUrl(
                        "/tasks/admin/project/workflow/WorkflowTasks/saveTrigger"
                    );

                    try { // This will send a 302 if it succeeds.
                        tClient.sendRawPost(triggerProps);
                    } catch(MigrateException e) {
                        if(!e.getMessage().endsWith("302")) {
                            throw e;
                        }
                    }
                }
            }

            // Push Notification Scheme, Skip-PreProcess, and Priority
            // via edit screen.
            String nsId = "";

            // Get Notification String ID if we need it
            if(context.getWorkflow().getNotificationScheme() != null) {
                try {
                    nsId = Scheme.get(context.getWorkflow()
                                                    .getNotificationScheme()
                    );
                } catch(NotFoundException e) {
                    LOG.warn(
                        "Could not migrate notification scheme from " +
                        "workflow: " + e.getMessage()
                    );
                }
            }

            // Get a CID
            UcbClient nsClient = new UcbClient(
                "/tasks/admin/project/workflow/WorkflowTasks/saveWorkflow"
            );

            HashMap<String, String> sendProps = new HashMap<>(7+defaults.size());

            sendProps.put("workflowId", process.getId());

            // CID probably isn't necessary, I could probably
            // push the workflowID instead.  That works on a lot of
            // these forms -- if this is problematic, try it out.
            String nsCID = nsClient.getCIDFromGet(
                "/tasks/admin/project/workflow/WorkflowTasks/editWorkflow",
                sendProps
            );

            // Push stuff
            sendProps.clear();
            sendProps.put("cid", nsCID);

            if(process.getDescription() != null) {
                sendProps.put("description", process.getDescription());
            } else {
                sendProps.put("description", "");
            }
    
            sendProps.put("name", process.getName());
            sendProps.put("notificationSchemeId", nsId);
            sendProps.put("saveWorkflow", "Save");
            sendProps.put("skipQuietPeriod", String.valueOf(
                                                context.getWorkflow()
                                                       .isSkippingQuietPeriod()
                )
            );

            if(context.getWorkflow().getWorkflowPriorityScript() != null) {
                sendProps.put("priority", String.valueOf(
                                            context.getWorkflow()
                                                   .getWorkflowPriorityScript()
                                                   .getId()
                    )
                );
            } else {
                sendProps.put("priority", "-2");
            }

            // Add defaults
            for(Map.Entry<String, String> def : defaults.entrySet()) {
                sendProps.put("property:" + def.getKey(), def.getValue());
                sendProps.put("default:property:" + def.getKey(), def.getValue());
            }

            // Push it
            nsClient.sendRawPost(sendProps);

            // Attach source configs and repo's
            for(AbstractSourceConfig.TemplateRepoPair trp :
                                    ucbSourceConfig.getRepoTemplatePairs()) {
                SourceConfigBuilder sb = new SourceConfigBuilder();
                sb.name(trp.getTemplate().getName())
                  .templateId(trp.getTemplate().getId())
                  .projectId(proj.getId())
                  .processId(process.getId())
                  .repository(trp.getRepository().getName());

                // Try to create it.
                Factories.getSourceConfigFactory()
                         .createSourceConfig(sb);
            }

            // Theoretically, we're done!  Holy crap!
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) { // UCB
            throw new MigrateException("Exception from UCB", e);
        }
    }

    /**
     * Create a process template and associated things.  This is basically
     * the construction of the workflow.
     *
     * @param context       The context of the migration.
     * @throws MigrateException on any failure, or JSONException if we
     *         get jibberish from UCB.
     */
    private void migrateWorkflow(UcbContext context)
            throws MigrateException, JSONException
    {
        // UCB's client has very poor support for process template properties
        // and no support for artifacts.  Therefore, we have to build
        // this object by hand.
        JSONObject pt = new JSONObject();

        pt.put("name", workflowName);

        if(context.getWorkflow().getDescription() == null) {
            pt.put("description", "");
        } else {
            pt.put("description", context.getWorkflow().getDescription());
        }

        // type is Build or Secondary.  We're just working with Build for now.
        pt.put("type", "Build");

        // We're always active
        pt.put("active", true);

        // Put the teams in
        RESTTeamParser teamParse = new RESTTeamParser();
        pt.put("teams", teamParse.teamsToJSON(context.teamMappings));

        // Migrate properties
        JSONArray propArray = new JSONArray();

        for(WorkflowProperty wp : context.getWorkflow().getPropertyArray()) {
            /* AM-15: support job execution properties?  I've never seen
             * this being used and the REST interface doesn't support it.
             * I can support it, but it requires a considerable bit more
             * work.
             */
            if(wp.isJobExecutionValue()) {
                throw new MigrateException(
                    "Workflow property uses job execution values, which is " +
                    "not presently supported by the migration tool.  The " +
                    "property in question is: " + wp.getName()
                );
            }

            // UCB doesn't support this
            if((wp.getInputProperties() != null) &&
               (wp.getInputProperties().size() > 0)) {
                throw new MigrateException(
                    "Workflow property uses input properties, which are " +
                    "not supported by UrbanCode Build.  The property in " +
                    "question is: " + wp.getName()
                );
            }

            JSONObject newProp = new JSONObject();

            newProp.put("name", wp.getName());

            // Default this to empty string
            if(wp.getDescription() == null) {
                newProp.put("description", "");
            } else {
                newProp.put("description", wp.getDescription());
            }

            // This defaults to empty string as well
            if(wp.getLabel() == null) {
                newProp.put("label", "");
            } else {
                newProp.put("label", wp.getLabel());
            }

            // Process type
            if(wp.getType().isCheckbox()) {
                newProp.put("display-type", "Checkbox");
            } else if(wp.getType().isMultiSelect()) {
                newProp.put("display-type", "Multi-Select");
            } else if(wp.getType().isSelect()) {
                newProp.put("display-type", "Select");
            } else if(wp.getType().isText()) {
                newProp.put("display-type", "Text");
            } else if(wp.getType().isTextArea()) {
                newProp.put("display-type", "Text Area");
            } else if(wp.getType().isTextSecure()) {
                newProp.put("display-type", "Text (secure)");
            } else {
                throw new MigrateException(
                    "Unknown property display type encountered for " +
                    "property: " + wp.getName() + " - type: " +
                    wp.getType().toString()
                );
            }

            // Scripted boolean
            newProp.put("scripted", wp.isScriptedValue());

            // "runtime" is use may override
            newProp.put("runtime", wp.isUserMayOverride());

            // UCB has a further thing called "save to build config"
            // that is available if isUserMayOverride is set to true.
            // Near as I can tell, AHP doesn't have an equivalent.
            // AM-16 : make sure this is actually hte case.
            newProp.put("saveToBuildConfig", false);

            newProp.put("required", wp.isRequired());

            // Move allowed values and default value value
            if(wp.isScriptedValue()) {
                if((wp.getAllowedValuesScript() != null) &&
                   (wp.getAllowedValuesScript().length() > 0)) {
                    newProp.put("allowed-values", wp.getAllowedValuesScript());
                }

                if((wp.getValueScript() != null) &&
                   (wp.getValueScript().length() > 0)) {
                    newProp.put("default-value", wp.getValueScript());
                } else {
                    newProp.put("default-value", "");
                }
            } else {
                if(wp.getAllowedValues().length > 0) {
                    newProp.put("allowed-values",
                                StringUtils.join(wp.getAllowedValues(), "\n")
                    );
                }


                if(wp.getPropertyValue() != null) {
                    newProp.put("default-value", wp.getPropertyValue());
                }
            }

            // Add the property to our array
            propArray.put(newProp);
        }

        // Add props to our new object
        pt.put("properties", propArray);

        // Migrate artifact sets
        JSONArray artifactSets = new JSONArray();

        // Iterate and add
        for(ArtifactDeliverPatterns adp : context.getWorkflow()
                                                 .getBuildProfile()
                                                 .getArtifactConfigArray()) {
            // These are comparatively straight forward
            JSONObject a = new JSONObject();

            a.put("artifact-set", adp.getArtifactSet().getName());

            if(adp.getBaseDirectory() == null) {
                a.put("base-dir", "");
            } else {
                a.put("base-dir", adp.getBaseDirectory());
            }

            if(adp.getArtifactPatternsString() == null) {
                a.put("includes", "");
            } else {
                a.put("includes", adp.getArtifactPatternsString());
            }

            if(adp.getArtifactExcludePatternsString() == null) {
                a.put("excludes", "");
            } else {
                a.put("excludes", adp.getArtifactExcludePatternsString());
            }

            artifactSets.put(a);
        }

        // Add it!
        pt.put("artifact-set-configuration", artifactSets);

        // Push this stuff to UCB
        UcbClient client = new UcbClient(Urls.TEMPLATE_PROCESS);

        // we need the return value
        JSONObject newPt = client.sendPostReturnObject(pt.toString());

        // We should have an ID
        Long ptId = newPt.getLong("id");

        /* We need to migrate the resource locks, if applicable.
         * AM-17: Lockaable resources are relatively easy to move (though
         * unsupported by UCB client).  Should I merge them by name and
         * re-use existing UCB resources, or should I make new ones with
         * ID suffixes like I do for jobs, etc.?  I'm not sure the use
         * case for these so its hard to know.  Revisit, throw error
         * for now.
         */
        if((context.getWorkflow().getStaticLockableResources() != null) &&
           (context.getWorkflow().getStaticLockableResources().size() > 0)) {
            throw new MigrateException(
                "This workflow uses lockable resources, which are not " +
                "presently supported by the migration tool."
            );
        }

        /* And lastly, the job workflow definition.
         * The UCB client falls somewhat short, as it can handle
         * most aspects of the job workflow steps but it cannot
         * handle iterations.
         *
         * So, we will do one pass where we will more or less
         * directly migrate the WorkflowDefinitionJobConfig
         * objects and then a second pass to add in iteration
         * info.
         */

        try {
            processTemplate = Factories.getProcessTemplateFactory()
                                       .getTemplateById(String.valueOf(ptId));
        } catch(Exception e) {
            throw new MigrateException("Cannot communicate with UCB.", e);
        }

        RESTWorkflow ucbWf = (RESTWorkflow)processTemplate.getWorkflow();

        // Extract the table graph, if we have it.
        if(context.getWorkflow().getWorkflowDefinition() == null) {
            return;
        }

        TableDisplayableGraph<WorkflowDefinitionJobConfig> g =
                                context.getWorkflow()
                                       .getWorkflowDefinition()
                                       .getWorkflowJobConfigGraph();

        // We can straight port our vertex list and our arc list
        // into UCB.  Map AHP ID's to UCB objects so that we
        // can make our arcs.
        HashMap<Long, WorkflowNode> vMap = new HashMap<>(g.getVertexCount());

        for(Vertex<WorkflowDefinitionJobConfig> v : g.getVertexArray()) {
            WorkflowNode newNode = createWorkflowNode(ucbWf, v.getData());
            vMap.put(v.getData().getId(), newNode);
        }

        // Create arcs
        for(com.urbancode.commons.graph.Graph.Arc<WorkflowDefinitionJobConfig>
            arc : g.getArcSet()) {
            ucbWf.getGraph().addArc(
                    vMap.get(arc.getFrom().getData().getId()),
                    vMap.get(arc.getTo().getData().getId())
            );
        }

        // Save it
        try {
            ucbWf.update();
        } catch(Exception e) {
            throw new MigrateException("Cannot communicate with UCB.", e);
        }

        // Reload our workflow.
        try {
            processTemplate = Factories.getProcessTemplateFactory()
                                       .getTemplateById(String.valueOf(ptId));
        } catch(Exception e) {
            throw new MigrateException("Cannot communicate with UCB.", e);
        }

        /*
         * Logic ... Okay, so this is obnoxious.  When I create a graph
         * in the Workflow, I can't set iterations.
         *
         * The vertixes should (theoretically) be in sync, so if we
         * iterate over them both we should figure out who has
         * iterations.
         *
         * AM-18: Fix this.  Isn't working how I expect it to, something
         * is screwing up the order.  I wish UCB's client would just
         * take iterations :P
         *
         * FIGURED IT OUT : I think our hasIteration list should match
         * the graph map that comes back from UCB
         *
         * NOPE, DIDN'T FIGURE IT OUT : It looks like UCB is scrambling
         * ID's around ... this may not be possible without a lot of
         * additional haggling. :/  I may be migrating the graph over
         * wrong and its hard to tell with my test cases since I'm
         * using a copy / paste of the same job.
         *
         * The UCB client throws an exception trying to load an
         * existing graph, so we've got to do this manually.
         */
        UcbClient graphClient = new UcbClient(
            Urls.TEMPLATE_PROCESS_WORKFLOW_GRAPH
                .replace("{}", String.valueOf(ptId))
        );

        // Grab our graph
        JSONObject graph = graphClient.sendGetReturnObject(null);
        JSONArray nodes = graph.getJSONArray("nodes");

        // Make a sorted list of ID's
        ArrayList<Long> nodeIds = new ArrayList<>(nodes.length());

        for(int i = 0; i < nodes.length(); i++) {
            nodeIds.add(nodes.getJSONObject(i).getLong("id"));
        }

        // sort it
        Collections.sort(nodeIds);

        // AM-9: Make this more dynamic
        UcbClient itClient = new UcbClient(
            "/tasks/admin/library/workflow/WorkflowDefinitionTasks/saveJobIterationPlan"
        );

        // Now set up iterations
        for(int i = 0; i < hasIteration.size(); i++) {
            if(hasIteration.get(i) == null) {
                // skip
                continue;
            }

            // Get our iteration
            JobIterationPlan plan = hasIteration.get(i).getJobIterationPlan();

            // Set up iteration.
            Map<String, String> it = new HashMap(9);

            it.put("iterationType", "Fixed");
            it.put("isParallel", String.valueOf(plan.isParallelIteration()));
            it.put("iterateJob", "Set Iteration");
            it.put("iterations", plan.getIterations());

            if(plan.isParallelIteration()) {
                it.put("maxParallelJobs",
                       String.valueOf(plan.getMaxParallelJobs()));
            } else {
                it.put("maxParallelJobs", "");
            }

            it.put("propertyName", "");
            it.put("requireAllAgents", "false");
            it.put("runningOnUniqueAgents",
                   String.valueOf(plan.isRunningOnUniqueAgents()));
            it.put("selectedWorkflowDefinitionJobConfigId",
                   String.valueOf(nodeIds.get(i))
            );
            it.put("workflowTemplateId", String.valueOf(ptId));

            itClient.sendRawPost(it);
        }
    }

    /**
     * Create a WorkflowNode from a WorkflowDefinitionJobConfiguration
     * object, which is the AHP equivalent.  This has the 'side effect'
     * of creating any necessary sub-objects along the way.
     *
     * @param ucbWf     The UCB Client workflow object
     * @param wdjc      The workflowDefinitionJobConfig from AHP
     * @returns a WorkflowNode object ready for use.
     * @throws Migrate exception on any error.
     */
    private WorkflowNode createWorkflowNode(RESTWorkflow ucbWf,
                                            WorkflowDefinitionJobConfig wdjc)
            throws MigrateException
    {
        // So we need a job, agent config, Agent lock duration,
        // work dir config, and job precondition script.
        // We also need to flag iterations.
        try {
            // JOB
            Job job;

            if(knownJobs.containsKey(wdjc.getJobConfig().getId())) {
                job = knownJobs.get(wdjc.getJobConfig().getId());
            } else {
                String jobName = wdjc.getJobConfig().getName() + " (" +
                                 String.valueOf(wdjc.getJobConfig().getId()) +
                                 ")";

                job = Factories.getJobFactory().getJobByName(jobName);

                // This is a paddlin' -- shouldn't happen
                if(job == null) {
                    throw new MigrateException(
                        "The impossible has occured and job " + jobName +
                        "is null.  Maybe it got deleted during the migration?"
                    );
                }

                knownJobs.putIfAbsent(wdjc.getJobConfig().getId(), job);
            }

            // AGENT CONFIG
            AgentConfig agentConfig;

            if(wdjc.isAgentless()) {
                agentConfig = new NoAgentAgentConfig();
            } else if(wdjc.isJobUsingParentAgent()) {
                // we can support this
                agentConfig = new UseParentAgentConfig();
            } else {
                synchronized(agentMutex) {
                    if(allAgentsPool == null) {
                        allAgentsPool = Factories.getAgentPoolFactory()
                                                 .getAgentPoolByName(
                                                    "All Build Agents"
                        );
                    }
                }

                if(allAgentsPool == null) {
                    throw new MigrateException(
                        "Could not load \"All Build Agents\" pool for " +
                        "migration."
                    );
                }

                // Warn 'em
                LOG.warn("uBuild handles agent selection entirely different " +
                         "from Anthill.  We default to all agents as a " +
                         "result unless use parent agent or no agent is " +
                         "used -- we can honor those."
                );

                agentConfig = new AgentPoolAgentConfig(allAgentsPool);
            }

            // AGENT LOCK DURACTION
            AgentLockDuration duration;

            if(wdjc.isJobLockForWorkflow()) {
                duration = AgentLockDuration.WORKFLOW;
            } else {
                duration = AgentLockDuration.JOB;
            }

            // WORKING DIRECTORY
            WorkDirConfig workDir;

            if(wdjc.isJobUsingParentWorkDir()) {
                workDir = new UseParentWorkDirConfig();
            } else if(wdjc.isJobUsingSourceWorkDirScript()) {
                workDir = new SourceConfigWorkDirConfig();
            } else {
                // Try to match work dir scripts by name.
                workDir = new WorkDirScriptWorkDirConfig(
                                    WorkDirScript.get(
                                        wdjc.getJobWorkDirScript()
                                    )
                );
            }

            // PRECOND SCRIPT
            JobPreConditionScript jobScript =
                PreCondScript.getJobPreconditionScript(
                    wdjc.getJobPreConditionScript()
            );

            // Make the node
            WorkflowNode node = ucbWf.createWorkflowNode(
                                            job, agentConfig, duration,
                                            workDir, jobScript
            );

            // Is it going to need iteration?
            if(wdjc.getJobIterationPlan() != null) {
                hasIteration.add(wdjc);
            } else {
                hasIteration.add(null);
            }

            return node;
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            throw new MigrateException("Failure to communicate with UCB", e);
        }
    }

    /**
     * (@inheritdoc)
     */
    @Override
    public void postRun(AbstractContext context)
           throws MigrateException
    {
        postRun((UcbContext)context);
    }
}
