package net.epicforce.migrate.ahp.toucb.ucb.fixed;

/*
 * RESTStepPreConditionScriptFactory.java
 *
 * This source is modified from a decompie of the UCB client.  No real attempt
 * was made to clean it up or comment it beyond notating my fixes.
 *
 * @author IBM
 */

import com.urbancode.ubuild.client.rest.HttpHelper;
import com.urbancode.ubuild.client.script.precondition.step.StepPreConditionScript;
import com.urbancode.ubuild.client.script.precondition.step.StepPreConditionScriptFactory;
import java.util.ArrayList;
import java.util.List;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

public class RESTStepPreConditionScriptFactory
  implements StepPreConditionScriptFactory
{
    // The original URL was for /rest2/*job* instead of step
    static final String URL = "/rest2/stepPreConditionScripts";

    public StepPreConditionScript getPreConditionScriptById(String id)
      throws Exception
    {
        HttpHelper helper = new HttpHelper();
        JSONObject object = new JSONObject(helper.sendGet(URL, id));

        RESTStepPreConditionScriptParser parser = new RESTStepPreConditionScriptParser();
        StepPreConditionScript result = parser.toObject(object);

        return result;
    }

    public StepPreConditionScript getPreConditionScriptByName(String name)
      throws Exception
    {
        return getPreConditionScriptById(name);
    }

    public List<StepPreConditionScript> getAllPreConditionScripts()
      throws Exception
    {
        List<StepPreConditionScript> result = new ArrayList();

        HttpHelper helper = new HttpHelper();
        JSONArray array = new JSONArray(helper.sendGet(URL, null));

        RESTStepPreConditionScriptParser parser = new RESTStepPreConditionScriptParser();
        result = parser.parseList(array);

        return result;
    }

    public StepPreConditionScript createPreConditionScript(String name, String description, String script)
      throws Exception
    {
        RESTStepPreConditionScript preConditionPreConditionScript = new RESTStepPreConditionScript(name, description, script, null);

        RESTStepPreConditionScriptParser parser = new RESTStepPreConditionScriptParser();
        JSONObject object = parser.toJSON(preConditionPreConditionScript);

        HttpHelper helper = new HttpHelper();
        JSONObject response = new JSONObject(helper.sendPost(URL, object.toString()));
    
        preConditionPreConditionScript.setId(response.getString("id"));

        return preConditionPreConditionScript;
    }

    public void deletePreConditionScript(StepPreConditionScript script)
      throws Exception
    {
        HttpHelper helper = new HttpHelper();
        helper.sendDelete(URL, script.getId());
    }
}
