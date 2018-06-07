package net.epicforce.migrate.ahp.toucb.ucb.fixed;

/*
 * RESTStepPreConditionScriptParser.java
 *
 * This source is modified from a decompie of the UCB client.  No real attempt
 * was made to clean it up or comment it beyond notating my fixes.
 *
 * @author IBM
 */

import com.urbancode.ubuild.client.rest.PersistentJSONParser;
import com.urbancode.ubuild.client.script.precondition.step.StepPreConditionScript;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;


/*
 * Note: This class was modified out of necessity, since
 * unfortunately I had to fix the object it interacts
 * with.  Didn't have to touch any of the code though.
 */

public class RESTStepPreConditionScriptParser
  extends com.urbancode.ubuild.client.rest.script.precondition.step.RESTStepPreConditionScriptParser
{
    public StepPreConditionScript toObject(JSONObject json)
      throws JSONException
    {
        String id = getJSONString(json, "id");
        String name = getJSONString(json, "name");
        String description = getJSONString(json, "description");
        String script = getJSONString(json, "body");

        RESTStepPreConditionScript result = new RESTStepPreConditionScript(name, description, script, id);

        return result;
    }

    public List<StepPreConditionScript> parseList(JSONArray array) throws JSONException
    {
        List<StepPreConditionScript> result = new ArrayList();

        for (int i = 0; i < array.length(); i++) {
            StepPreConditionScript script = toObject(array.getJSONObject(i));
            result.add(script);
        }

        return result;
    }

    public JSONObject toJSON(StepPreConditionScript script)
      throws JSONException
    {
        JSONObject json = super.baseToJSON(script);
        put(json, "body", script.getBody());

        return json;
    }
}
