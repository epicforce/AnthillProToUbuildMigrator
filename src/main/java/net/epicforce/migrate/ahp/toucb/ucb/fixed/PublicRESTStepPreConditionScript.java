package com.urbancode.ubuild.client.rest.script.precondition.step;

/*
 * This class is a hack to force RESTStepPreConditionScript to have a public constructor.
 *
 * I have no pride left.  Only Zuul.
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


public class PublicRESTStepPreConditionScript extends com.urbancode.ubuild.client.rest.script.precondition.step.RESTStepPreConditionScript
{
    // I really wish you could inherit constructors :P
    public PublicRESTStepPreConditionScript(String name, String description, String body, String id)
    {
        super(name, description, body, id);
    }
}
