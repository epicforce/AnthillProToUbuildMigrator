package net.epicforce.migrate.ahp.toucb.ucb.fixed;

/*
 * RESTStepParser.java
 *
 * So RESTStep has a means to set the pre condition script....
 *
 * ..... But no means to save it :P  It just gets thrown out.
 * This class adds support for precondition scripts.
 */

import com.urbancode.ubuild.client.rest.PersistentJSONParser;
import com.urbancode.ubuild.client.step.Step;
import com.urbancode.ubuild.client.step.StepProperty;
import com.urbancode.ubuild.client.step.StepPropertyType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

// To actually load our precond. script
import net.epicforce.migrate.ahp.toucb.ucb.UcbFactory;

public class RESTStepParser
  extends com.urbancode.ubuild.client.rest.workflow.RESTStepParser
{
/*
 * This is actually inaccesssible code.
 * And its giving me problems
 * So commenting it out.  However, note to future devs:
 *
 * Loading a UCB step doesn't properly set the precond. id.
 *
    public Step toObject(JSONObject json)
      throws Exception
    {
        String id = optJSONString(json, "id");
        String name = getJSONString(json, "name");
        String description = getJSONString(json, "description");

        String stepPath = optJSONString(json, "step-path");
        int seq = json.getInt("seq");
        String preConditionScriptId = getJSONString(json, "pre-cond-script");

        String workDirOffset = optJSONString(json, "workDirOffset");
        boolean runInPreflight = json.getBoolean("runInPreflight");
        boolean runInPreflightOnly = json.getBoolean("runInPreflightOnly");
        boolean ignoreFailures = json.getBoolean("ignoreFailures");

        // Previously, this lkoaded the ID above but didn't do anything
        // with it.  Freakin' great guys.
        RESTStep step = new RESTStep(stepPath, name, description, null, seq, 
                                    UcbFactory.getStepPreConditionScriptFactory()
                                              .getPreConditionScriptById(preConditionScriptId),
                                    runInPreflight, runInPreflightOnly, ignoreFailures, id);

        step.setWorkDirOffset(workDirOffset);
        return step;
    }
 */

    public JSONObject toJSON(Step step)
      throws JSONException
    {
        JSONObject json = super.baseToJSON(step);
        put(json, "properties", toChildJSON(step.getProperties()));
        put(json, "step-path", step.getStepPath());
        put(json, "seq", Integer.valueOf(step.getSeq()));
        put(json, "workDirOffset", step.getWorkDirOffset());

        put(json, "runInPreflight", Boolean.valueOf(step.isRunInPreflight()));
        put(json, "runInPreflightOnly", Boolean.valueOf(step.isRunInPreflightOnly()));
        put(json, "ignoreFailures", Boolean.valueOf(step.isIgnoreFailures()));

        if(step.getPreConditionScript() != null) {
            put(json, "pre-cond-script", String.valueOf(step.getPreConditionScript().getId()));
        }

        return json;
    }

    // The parent has a private toJSON which I won't be able to use
    // so I made this hack.
    private JSONArray toChildJSON(Map<String, String> properties) throws JSONException
    {
        JSONArray array = new JSONArray();
        for (Map.Entry<String, String> property : properties.entrySet()) {
            JSONObject object = new JSONObject();
            put(object, "key", property.getKey());
            put(object, "value", property.getValue());
            array.put(object);
        }

        return array;
    }
}
