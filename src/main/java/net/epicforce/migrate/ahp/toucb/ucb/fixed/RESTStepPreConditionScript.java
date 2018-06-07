package net.epicforce.migrate.ahp.toucb.ucb.fixed;

/*
 * RESTStepPreConditionScript.java
 *
 * This source is modified from a decompie of the UCB client.  No real attempt
 * was made to clean it up or comment it beyond notating my fixes.
 *
 * @author IBM
 */

import com.urbancode.ubuild.client.AbstractPersistent;
import com.urbancode.ubuild.client.rest.HttpHelper;
import com.urbancode.ubuild.client.script.precondition.step.StepPreConditionScript;
import org.codehaus.jettison.json.JSONObject;


public class RESTStepPreConditionScript
  extends com.urbancode.ubuild.client.rest.script.precondition.step.PublicRESTStepPreConditionScript
{
    // I really wish you could inherit constructors :P
    public RESTStepPreConditionScript(String name, String description, String body, String id)
    {
        super(name, description, body, id);
    }

    // URL was wrong here too (/rest2/job instead of /rest2/step)
    @Override
    public void update() throws Exception
    {
        RESTStepPreConditionScriptParser parser = new RESTStepPreConditionScriptParser();
        JSONObject object = parser.toJSON(this);
        HttpHelper helper = new HttpHelper();
        helper.sendPut("/rest2/stepPreConditionScripts", getId(), object.toString());
    }

    // RESTStepPReConditionScriptFactory needs this
    public void setId(String id)
    {
        super.setId(id);
    }
}
