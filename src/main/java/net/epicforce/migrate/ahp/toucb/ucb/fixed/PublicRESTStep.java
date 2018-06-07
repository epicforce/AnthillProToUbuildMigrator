package com.urbancode.ubuild.client.rest.workflow;

/*
 * This class is a hack to force RESTStep to have a public constructor.
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


public class PublicRESTStep extends com.urbancode.ubuild.client.rest.workflow.RESTStep
{
    public PublicRESTStep(String stepPath, String name, String description, Job job, int seq, StepPreConditionScript script, boolean runInPreflight, boolean runInPreflightOnly, boolean ignoreFailures, String id)
    {
        super(stepPath, name, description, job, seq, script, runInPreflight, runInPreflightOnly, ignoreFailures, id);
    }
}
