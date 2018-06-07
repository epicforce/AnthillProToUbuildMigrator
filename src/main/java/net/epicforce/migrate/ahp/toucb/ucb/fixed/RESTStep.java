package net.epicforce.migrate.ahp.toucb.ucb.fixed;

/*
 * RESTStep.java
 *
 * So RESTStep has a means to set the pre condition script....
 *
 * ..... But no means to save it :P  It just gets thrown out.
 * This class overrides 'update' so we can fix it.  Its kind
 * of hacky to avoid 'downclassing'.
 */

import com.urbancode.ubuild.client.AbstractPersistent;
import com.urbancode.ubuild.client.rest.HttpHelper;
import com.urbancode.ubuild.client.script.precondition.step.StepPreConditionScript;
import com.urbancode.ubuild.client.step.Step;
import com.urbancode.ubuild.client.workflow.Job;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.codehaus.jettison.json.JSONObject;


public class RESTStep
  extends com.urbancode.ubuild.client.rest.workflow.PublicRESTStep
{
    public RESTStep(String stepPath, String name, String description, Job job, int seq, StepPreConditionScript script, boolean runInPreflight, boolean runInPreflightOnly, boolean ignoreFailures, String id)
    {
        super(stepPath, name, description, job, seq, script, runInPreflight, runInPreflightOnly, ignoreFailures, id);
    }

    // Copy constructor
    public RESTStep(Step src)
    {
        super(src.getStepPath(), src.getName(), src.getDescription(), src.getJob(),
              src.getSeq(), src.getPreConditionScript(), src.isRunInPreflight(),
              src.isRunInPreflightOnly(), src.isIgnoreFailures(), src.getId());

        setActive(src.isActive());
        setWorkDirOffset(src.getWorkDirOffset());
        setTimeout(src.getTimeout());

        // Thiis is dumb.  Le sigh.
        for(Map.Entry<String, String> ent: src.getProperties().entrySet()) {
            super.setProperty(ent.getKey(), ent.getValue());
        }
    }

    public void update()
      throws Exception
    {
        RESTStepParser parser = new RESTStepParser();
        JSONObject json = parser.toJSON(this);
        new HttpHelper().sendPut("/rest2/jobs/" + getJob().getId() + "/steps",
                                 getId(), json.toString());
    }
}
