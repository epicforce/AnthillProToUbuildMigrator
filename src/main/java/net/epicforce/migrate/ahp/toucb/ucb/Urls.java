package net.epicforce.migrate.ahp.toucb.ucb;

/*
 * Urls.java
 *
 * This is a class simply to contain all the different URL constants
 * and the notes associated with them.
 *
 * In all cases {} in URLs can be replaced with the ID being loaded,
 * or sometimes the name of the entity (But not always).  Which things
 * names work for and which they don't is kind of random.
 *
 * You'll note that I've laundry-listed things that I have chosen
 * to ignore in the API.  This is for self-reference so if it turns
 * out I need them later, I know they exist but just aren't researched.
 *
 * All the rest APIs are defined in the package
 * com.urbancode.ubuild.web.rest.plugin
 *
 * Which currently resides in ibm-ucb-web.jar
 *
 * NOTE TO SELF: There's also a "restlet" interface that lives under
 * the URL root of /rest.  The "restlet" interface speaks XML instead
 * of JSON and looks like it can do property manipulations which rest2
 * shys away from for some reason.  The list of capabilities is in:
 *
 * com.urbancode.ubuild.web.restlet.UBuildRestletApplication
 *
 * also in ibm-ucb-web.jar
 *
 * @author sconley (sconley@epicforce.net)
 */
public final class Urls
{
    /*****************************************************************
     * CONSTANTS
     *
     * These are the known types in UCB and a mapping to their rest2
     * URL paths.
     ****************************************************************/

    /*****************************************************************
     * AGENT URLS
     ****************************************************************/

    public static final String AGENT = "/rest2/agents";

        /*
         * Note here (and probably everywhere else).  The _PROPERTIES call
         * will get you *all* the properties set.  Then, the other calls will
         * get them broken down by the different property types.
         *
         * These all appear to be read only.
         */
        public static final String AGENT_PROPERTIES_RO =
                                    "/rest2/agents/{}/properties";
        public static final String AGENT_PROPERTIES_AGENT_RO =
                                    "/rest2/agents/{}/properties/agent";
        public static final String AGENT_PROPERTIES_SYSTEM_RO =
                                    "/rest2/agents/{}/properties/system";
        public static final String AGENT_PROPERTIES_ENVIRONMENT_RO =
                                    "/rest2/agents/{}/properties/environment";
        public static final String AGENT_PROPERTIES_LOCKED_RO =
                                    "/rest2/agents/{}/properties/locked";

        /*
         * To write a property, you POST it here.  Parameters are
         * "Save=Save", property=propety name, propertyValue=property value
         *
         * New prop or update prop is the same
         *
         * For a secure prop, pass propertySecure=true and I guess
         * propertyValueConfirm= the same as propertyValue (I haven't tested
         * if that's necessary or not, but its what the UI does).
         */
        public static final String AGENT_SAVE_PROPERTY =
            "/tasks/admin/agent/AgentPropertyTasks/saveAgentProperty?agent_id={}";

        // To delete, GET request here - pass property= and agent_id=
        public static final String AGENT_DELETE_PROPERTY =
                "/tasks/admin/agent/AgentPropertyTasks/deleteAgentProperty";

        /*
         * Just to be vexacious, the POST parameters are 'variable' and
         * 'variableValue' here.  Totally not how I would handle it.  Save=Save
         * is the same.
         *
         * This is for environment vars.
         *
         * For secure props, its variableSecure=true and variableValueConfirm
         */
        public static final String AGENT_SAVE_ENVIRONMENT =
            "/tasks/admin/agent/AgentEnvironmentTasks/saveEnvironmentVariable?agent_id={}";

        // To delete, GET request here - pass property= and agent_id=
        // Yes, it's really property here and variable for saving.
        public static final String AGENT_DELETE_ENVIRONMENT =
            "/tasks/admin/agent/AgentEnvironmentTasks/deleteEnvironmentVariable";

    /*****************************************************************
     * AGENTPOOL URLS
     ****************************************************************/

    // Note: Agent Pools have no properties.

    /*
     * Plain ole 'AGENTPOOL' is a read-only resource.  You have to use
     * one of the _FIXED or _DYNAMIC variants to get read/write ability.
     *
     * You can append a /id to this URL.
     */
    public static final String AGENTPOOL = "/rest2/agentPools";

    /*
     * Fixed agent pool is pretty simple.  You can append a /id to the
     * URL.  Supports DELETE, PUT, and POST.
     */
    public static final String AGENTPOOL_FIXED = "/rest2/fixedAgentPools";

    /*
     * Dynamic agent pools are more comeplex.  Supports /id
     *
     * Only supports GET and PUT.  PUT appears to be exclusively
     * to add a group rule.  AHP doesn't support a dynamic grouping
     * quite like this so I didn't put a lot of effort into figuring
     * out the details of creating these.
     */
    public static final String AGENTPOOL_DYNAMIC = "/rest2/dynamicAgentPools";

        // Produces an enumeration of rule types
        public static final String AGENTPOOL_DYNAMIC_RULE_TYPES =
                            "/rest2/dynamicAgentPools/ruleTypes";

        // Produces an enumeration of rule conditions
        public static final String AGENTPOOL_DYNAMIC_RULE_CONDITIONS =
                            "/rest2/dynamicAgentPools/ruleConditions";

    /*****************************************************************
     * ANNOUNCEMENTS - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * ARTIFACTSET URLS
     ****************************************************************/

    // Note: Artifact Sets have no properties

    /*
     * Supports /id.  GET/POST/PUT/DELETE all work exactly how you
     * would expect.
     */
    public static final String ARTIFACTSET = "/rest2/artifactsets";

    /*****************************************************************
     * AUTHTOKENS - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * BUILDLIFE - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * CODESTATION - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * CURRENTACTIVITY - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * DASHBOARD - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * DEPENDENCY - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * I18N - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * JOB URLS
     ****************************************************************/

    /*
     * Jobs, by themselves, do very little and are basically a name,
     * description, and permissions.  The UCB Client API actually
     * handles jobs quite well so you may rather use that.
     *
     * /id is supported.  GET, POST, DELETE, and PUT all work as
     * expected.
     *
     * Jobs have a couple interesting additional endpoints that
     * apply to the /id version, noted below.
     */

    public final static String JOB = "/rest2/jobs";

        /*
         * This is a GET-only endpoint that fetches a list of
         * process templates (workflows) that use this job.
         *
         * Cute, but probably not useful for what we're doing.
         */
        public final static String JOB_PROCESS_TEMPLATES =
                                        "/rest2/jobs/{}/processTemplates";

        /*
         * Internally, this routes to a StepCollectionResource
         *
         * This is basically its own littel endpoint.  GET will
         * get all the jobs, and it supports a /id on the end.
         *
         * This ending /id is a STEP id, not a job ID, so that gets a little
         * confusing.
         *
         * POST, PUT, and DELETE all work as expected.
         */
        public final static String JOB_STEPS = "/rest2/jobs/{}/steps";

    /*****************************************************************
     * JOBTRACE - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * MYACTIVITY - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * PLUGIN URLS
     ****************************************************************/

    /*
     * Practically everything in UCB is a plugin, unlike AHP which relied
     * more on built-ins.  Therefore, it is of vital importance to figure
     * out what plugins are installed.
     *
     * The top level supports GET to list plugins, /id to get a particular
     * one, and curiously enough PUT to upload a new plugin.  DELETE
     * also works, but there appears to be no POST support.
     *
     * The 'id' used here is 'pluginId' from the plugins list, as opposed
     * to the more usual 'id' used everywhere else.  This ID looks like
     * a java class name.
     */
    public static final String PLUGIN = "/rest2/plugins";

        /*
         * Some plugins can have integrations.  This is further
         * configuration of some kind of connection to another
         * service.  This only applies to plugins of type
         * AutomationPlugin, and only to very few of them (such as
         * the uDeploy plugin).  If a plugin doesn't have an
         * integration support, then this returns a null pointer
         * exception.
         *
         * It looks like the only way to determine if something
         * will have an integration or not is to see if it has
         * a 'propSheetDefs' that isn't an empty list.
         *
         * Anyway, this will return the list of integrations.
         */
        public static final String PLUGIN_INTEGRATIONS =
                                            "/rest2/plugins/{}/integrations";

    /*****************************************************************
     * PREFERENCES - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * PREFLIGHT - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * PROJECT URLS
     ****************************************************************/

    /*
     * In UCB, there's a concept of a project which is separate from a
     * project template.  AHP doesn't really have this segregation.
     *
     * An AHP project is basically a project template and project
     * put together.  Most of the AHP project stuff goes in the
     * project template, but the notification scheme and the workflow
     * linkage are in the project.
     *
     * The UCB client's support for projects seems fairly complete,
     * so it may be better to use that then to use this.  As always,
     * up to you.
     *
     * /id is supported.  GET, POST, PUT, and DELETE all operate as
     * expected.  There are a bunch of additional endpoints under
     * /id which are listed below.
     */
    public static final String PROJECT = "/projects";

        /*
         * This sub-endpoint routes to ProcessCollectionResource
         * It is a way to list processes associated with
         * a project.
         *
         * GET and /id are supported, but there's no PUT etc.
         * endpoint.  To create or modify, use /buildProcesses
         * or /secondaryProcesses instead.
         *
         * Note that when you use /id, you will get back a
         * build process or a secondary process which does
         * support GET/POST/etc. the same as if you went to
         * /buildProcesses/id interestingly enough.
         *
         * The sub-sub endpoints are differnt for buildProcesses and
         * secondaryProcesses, so they will be covered under those
         * sections to avoid redundancy but you can structure the
         * URLs in several ways here.
         */
        public static final String PROJECT_PROCESSES =
                                                    "/projects/{}/processes";

        /*
         * This sub-endpoint is for originating workflows.  You will
         * need to use this one, not the generic processes one, to
         * create a new originating workflow.
         *
         * It supports /id and GET, along with POST to create a new one.
         * /id supports GET and PUT.  There are additional sub-endpoints
         * listed below as well.
         */
        public static final String PROJECT_BUILD_PROCESSES = 
                                                "/projects/{}/buildProcesses";

            // /buildLives - returns build lives (we don't care)
            // /activity/{build life ID} - return build activity (we don't care)
            // /activity/{pageSize}/{page} - paginated activity (don't care)
            // /activity - all activity (don't care)
            // /dependenciesXml - generate XML of dependencies (don't care)
            // /dependencies - list of dependencies (don't care)
            // /successRateReport - don't care
            // /dashboardActivity - don't care
            // /statusSummaries - don't care
            // /dependencySummaries - don't care

            /*
             * This gets the runtime properties of a build process.
             *
             * This is read-only (GET) and does not support /id
             */
            public static final String PROJECT_BUILD_PROCESS_RUNTIME_PROPERTIES
                = "/projects/{}/buildProcesses/{processId}/runtimeProperties";

            /* So how does one set a property, you might ask?
             *
             * With a tremendous amount of pain it looks like.  I'll get
             * to this later.
             */

        /*
         * This is how you access source configs and is a pretty
         * big endpoint of its own, which goes to
         * SourceConfigCollectionResource
         *
         * GET, /id, POST, PUT, and DELETE all work as expected
         *
         * Note the 'processes' part of this URL can be replaced
         * with buildProcesses or secondaryProcesses and all 3 work
         * the same.  This is the most generic of this shared point.
         *
         * Note this URL's 2 variable substitution.
         */
        public static final String PROJECT_SOURCE_CONFIGS =
                            "/projects/{}/processes/{processId}/sourceConfigs";


        /*
         * This is how you access triggers.  Its is own endpoint handler
         * named TriggerCollectionResource
         *
         * As with source configs, 'processes' can be any of the 3.
         *
         * GET, /id, POST, PUT, and DELETE all work as expected.
         *
         * Note this URL's 2 variable substitution.
         */
        public static final String PROJECT_TRIGGERS =
                                "/projects/{}/processes/{processId}/triggers";

        /*
         * secondaryProcesses work similar to buildProcesses, but
         * they have fewer available endpoints.  If you wish to
         * create or modify a secondary process, you must use this
         * endpoint.
         *
         * GET, /id, PUT, POSRT, and DELETE all work as expected.
         */
        public static final String PROJECT_SECONDARY_PROCESS =
                                "/projects/{}/secondaryProcesses";

            // /dashboardActivity -- don't care

        // Ignored: notificationSubscriptions.  This is for users to
        // configure what notifications they get.  There is no AHP
        // analogue

        // ignored: /recentProcessActivity
        // ignored: /latestStatus
        // ignored: /latestBuilds

    /*****************************************************************
     * RADIATOR - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/
    
    /*****************************************************************
     * REPORTING - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * REPORTS - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * REPOSITORIES URL
     ****************************************************************/

    /*
     * Supports GET, /id, POST, PUT, and DELETE as you would expect.
     *
     * It also has a /trigger which supports GET, POST, and DELETE
     * for creating repository triggers.
     */
    public final static String REPOSITORY = "/rest2/repositories";

        public final static String REPOSITORY_TRIGGER =
                                        "/rest2/repositories/{}/trigger";

    /*****************************************************************
     * REQUEST - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * SCRIPT URL
     ****************************************************************/

    /*
     * This is for handling certain system level scripts such as
     * working directory and preconditions.
     *
     * While grouped under the same package in UCB, the URLs are not
     * really very related.
     */

    // Supports GET, /id, POST, PUT, and DELETE as expected
    public final static String SCRIPT_WORKDIR = "/rest2/workDirScripts";

    // Supports GET, /id, POST, PUT, and DELETE as expected
    public final static String SCRIPT_JOB_PRECOND =
                                        "/rest2/jobPreConditionScripts";

    // Supports GET, /id, POST, PUT, and DELETE as expected
    public final static String SCRIPT_STEP_PRECOND =
                                        "/rest2/stepPreConditionScripts";

    /*****************************************************************
     * SECURITY URL
     ****************************************************************/

    /*
     * This is some kind of endpoint for loading the logged in user.
     * It's read-only and seems of minimal utility for what
     * we're doing, but it's simple enough.  GET is supported, no /id
     */
    public final static String SECURITY = "/rest2/security/user";

    /*****************************************************************
     * STATUS URL
     ****************************************************************/

    /*
     * These are for working with different statuses.  They're something
     * that used to be part of the life cycle model in AHP, but now are
     * a more global thing.
     *
     * Supports GET, /id, PUT, POSRT, and DELETE as you would expect.
     *
     * This also has a "/nonArchived" endpoint which throws a
     * Null Pointer Exception when I try to use it.  Not sure what
     * its for, as the UI doesn't seem to use it either.  I haven't
     * included it in the URL list as it doesn't seem to function
     * and would be irrelevant even if it did.
     */
    public final static String STATUS = "/rest2/statuses";

    /*****************************************************************
     * SUBSCRIPTION - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * TEMPLATE URL
     ****************************************************************/

    /*
     * Sort of like SCRIPT, template isn't really an endpoint itself
     * but is rather a collection of child endpoints.  We will treat
     * each like a top-level endpoint.
     */

    /*
     * Build and secondary process templates are handled a lot like
     * they are on the project level, where there are 3 endpoints
     * with a lot of weird overlap.
     *
     * The /processTemplates endpoint is the main one, which lets
     * you do GET, /id, POST, and PUT.  You can use the
     * /buildProcessTemplates or the /secondaryProcessTemplates
     * endpoint if you want to filter things out, but it appears
     * there are no additional endpoints you can get just from
     * using those paths.
     *
     * All these process templates are basically AHP workflows.
     */

    public static final String TEMPLATE_PROCESS = "/rest2/processTemplates";
    public static final String TEMPLATE_PROCESS_BUILD =
                                                "/rest2/buildProcessTemplates";
    public static final String TEMPLATE_PROCESS_SECONDARY =
                                            "/rest2/secondaryProcessTemplates";

        // /processes -- lists processes that use a given template, don't care

        /*
         * So these two endpoints are for interacting with workflows
         * which are an intrinsic part of a process.
         *
         * The /workflow endpoint appears to be read-only
         * However, the /workflow/graph endpoint supports
         * GET/POST/PUT.  POST and PUT do the exact same thing
         * under the hood, and there's no /id
         *
         * UCB's client provides a decent interface to this, so
         * I would recommend that over trying to decypher this
         * jungle yourself.
         */
        public static final String TEMPLATE_PROCESS_WORKFLOW =
                                    "/rest2/processTemplates/{}/workflow";

        public static final String TEMPLATE_PROCESS_WORKFLOW_GRAPH =
                                    "/rest2/processTemplates/{}/workflow/graph";

    /*
     * Project templates define mostly the pre-build stuff.
     * This is kind of like a lifecycle model from AHP but not really.
     *
     * Supports GET, /id, POST, PUT, and DELETE.  There's also some
     * additional endpoints.
     */
    public static final String TEMPLATE_PROJECT = "/rest2/templates";

        // /projects - lists projects that use this template, don't care

        /*
         * This endpoint GET's all the properties.  You can also
         * PUT to this endpoint to update (and presumably create)
         * properties.  It looks like they all have to go as
         * a group, rather than one at a time.
         */
        public static final String TEMPLATE_PROJECT_PROPERTIES =
                                        "/rest2/templates/{}/properties";

        // This is just for DELETE, note it requires a second
        // parameter -- the property name to delete
        public static final String TEMPLATE_PROJECT_PROPERTY_DELETE =
                                    "/rest2/tempaltes/{}/property/{name}";

    /*
     * Source templates are are akin to the source configuration
     * of a project in AHP but abstracted/shared.
     *
     * This supports GET, /id, POST, PUT, and DELETE as you would
     * expect
     */
    public static final String TEMPLATE_SOURCE = "/rest2/sourceTemplates";

        // This also supports a /sourceConfigs endpoint that we don't
        // care about (it lists what uses a given template)

    
    /*****************************************************************
     * TRENDS - Ignored.  This API is irrelevant for our
     * purpose.
     ****************************************************************/

    /*****************************************************************
     * USERS URL
     ****************************************************************/

    /*
     * This gives some extremely limited support to manipulating users.
     * You can GET to list users, or GET the /id to get a specific
     * user.  For a given user ID, you can further PUT to
     * /activate or /inactivate to do the given action.  However, you
     * cannot create users or modify users with this interface.
     */
    public static final String USER_SOURCE = "/rest2/users";

        // /inactivate -- don't care (PUT only)
        // /activate -- don't care. (PUT only)

    /*****************************************************************
     * UTIL URL
     ****************************************************************/

    /*
     * This is called the util package but the endpoint itself is
     * an importer.  This looks like it can import whatever XML you
     * want into the system.  Only PUT is supported.
     *
     * This strikes me as extremely powerful and may be a gateway
     * to importing things that aren't otherwise supported by the
     * interface.
     */
    public static final String UTIL_IMPORT = "/rest2/import";
}
