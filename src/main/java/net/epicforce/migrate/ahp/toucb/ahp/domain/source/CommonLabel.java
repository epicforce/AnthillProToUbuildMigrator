package net.epicforce.migrate.ahp.toucb.ahp.domain.source;

/*
 * CommonLabel.java
 *
 * AHP uses a different class for every kind of source action.
 * UCB only uses one.  This is common code for all of them.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;

import com.urbancode.anthill3.domain.source.LabelStepConfig;
import com.urbancode.ubuild.client.step.Step;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class CommonLabel extends CommonSource
{
    private final static Logger LOG =
                            LoggerFactory.getLogger(CommonLabel.class);

    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        LabelStepConfig c = (LabelStepConfig)context.getCurrentStep();

        try {
            // The hash map is used to set properties on the
            // step, which are in turn used to build the step.
            HashMap<String, String> props = new HashMap<String, String>(3);

            // These go straight over
            if(c.getMessage() != null) {
                props.put("message", c.getMessage());
            } else {
                props.put("message", "");
            }

            if(c.getLabelString() != null) {
                props.put("label-string", c.getLabelString());
            } else {
                props.put("label-string", "");
            }

            // Create our UCB step
            Step ucbStep = context.getUcbJob()
                                  .createStep(
                                    "Source/Label Source",
                                    c.getName(),
                                    c.getDescription() == null ?
                                        "" : c.getDescription(),
                                    context.nextUcbStep(),
                                    props
            );

            // Add common stuff
            copyCommonBits(context, c, ucbStep);

            // Save it
            ucbUpdate(ucbStep);
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            // Cause UCB
            throw new MigrateException("Error while migrating step", e);
        }
    }
}
