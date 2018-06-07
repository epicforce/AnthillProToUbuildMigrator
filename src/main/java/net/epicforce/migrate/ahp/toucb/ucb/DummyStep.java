package net.epicforce.migrate.ahp.toucb.ucb;

/*
 * DummyStep.java
 *
 * This is a placeholder step just to fill in a blank in the job
 * list, for built-in jobs (like StampStep) that aren't really supported
 * by UCB's REST API.
 *
 * What happens is, if I create jobs without using the API, subsequent
 * jobs created by the API will throw an exception because their sequence
 * numbers are too high.  We need a fake step to pad the sequence number,
 * but we don't want to try to save that step to AHP because it will
 * override the functional step we've created.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.Map;

import com.urbancode.ubuild.client.step.Step;
import com.urbancode.ubuild.client.workflow.Job;
import com.urbancode.ubuild.client.script.precondition.step.StepPreConditionScript;


public class DummyStep implements Step
{
    // Just to support protocol
    private Job job;
    private int seq;
    private StepPreConditionScript pre;

    /**
     * None of these methods do anything.  This class is simply a
     * place-holder
     */
    public DummyStep() {}

    @Override
    public void setDescription(String id) { }

    @Override
    public String getDescription()
    {
        return "";
    }

    @Override
    public String getId()
    {
        return "";
    }

    @Override
    public void setName(String id) { }

    @Override
    public String getName()
    {
        return "";
    }

    @Override
    public void setActive(boolean active) { }

    @Override
    public boolean isActive()
    {
        return true;
    }

    @Override
    public void setJob(Job ignored)
    {
        job = ignored;
    }

    @Override
    public Job getJob()
    {
        return job;
    }

    @Override
    public void setSeq(int x)
    {
        seq = x;
    }

    @Override
    public int getSeq()
    {
        return seq;
    }

    @Override
    public StepPreConditionScript getPreConditionScript()
    {
        return pre;
    }

    @Override
    public void setPreConditionScript(StepPreConditionScript pre)
    {
        this.pre = pre;
    }

    @Override
    public void setTimeout(long paramLong) { }

    @Override
    public long getTimeout()
    {
        return 0;
    }

    @Override
    public String getWorkDirOffset()
    {
        return "";
    }

    @Override
    public void setWorkDirOffset(String whatever) {}

    @Override
    public void setRunInPreflight(boolean param) {}

    @Override
    public boolean isRunInPreflight()
    {
        return true;
    }

    @Override
    public void setRunInPreflightOnly(boolean paramBoolean) { }

    @Override
    public boolean isRunInPreflightOnly()
    {
        return false;
    }

    @Override
    public void setIgnoreFailures(boolean paramBoolean) { }

    @Override
    public boolean isIgnoreFailures()
    {
        return false;
    }

    @Override
    public void setProperty(String paramString1, String paramString2) { }

    @Override
    public Map<String, String> getProperties()
    {
        return null;
    }

    @Override
    public String getStepPath()
    {
        return "";
    }

    @Override
    public void update() { }

    @Override
    public void delete() { }
}
