package net.epicforce.migrate.ahp.toucb.ahp.domain.builder.maven;

/*
 * MavenBuildStepConfigMigrate.java
 *
 * Class to migrate Maven build steps from AHP to UCB.
 *
 * @author sconley (sconley@epicforce.net)
 */

import java.util.HashMap;

import net.epicforce.migrate.ahp.exception.MigrateException;
import net.epicforce.migrate.ahp.toucb.context.UcbContext;
import net.epicforce.migrate.ahp.toucb.context.UcbStep;

import com.urbancode.anthill3.domain.builder.NameValuePair;
import com.urbancode.anthill3.domain.builder.maven.MavenBuilder;
import com.urbancode.anthill3.domain.builder.maven.MavenBuildStepConfig;
import com.urbancode.ubuild.client.step.Step;


public class MavenBuildStepConfigMigrate extends UcbStep
{
    /**
     * (@inheritdoc)
     */
    @Override
    public void run(UcbContext context) throws MigrateException
    {
        // Require the maven plugin
        context.ucbHasPlugin("com.urbancode.air.plugin.maven", "Maven Plugin");

        // Get our maven bits.
        MavenBuildStepConfig c = (MavenBuildStepConfig)context.getCurrentStep();
        MavenBuilder mb = c.getBuilder();

        try {
            // Collect our Maven details into a hashmap
            HashMap<String, String> props = new HashMap<>(10);

            // Rack 'em up!
            props.put("workDirOffset", mb.getWorkDirOffset());

            // Pom file name is required by UCB, but may be left to default
            // (blank) to pom.xml in AHP
            if((mb.getBuildFilePath() == null) ||
               (mb.getBuildFilePath().length() ==0)) {
                props.put("pomFileName", "pom.xml");
            } else {
                props.put("pomFileName", mb.getBuildFilePath());
            }
            props.put("goals", mb.getGoal());
            props.put("flags", mb.getMavenParams());
            props.put("mavenHome", mb.getMavenHomeVar());

            // AHP supports Maven 1 and 2.  UCB also supports 3.3
            if(mb.isUsingMavenTwo()) {
                props.put("mavenVersion", "2");
            } else {
                props.put("mavenVersion", "1");
            }

            // This is also required by UCB
            if((mb.getJavaHomeVar() == null) ||
               (mb.getJavaHomeVar().length() == 0)) {
                props.put("javaHome", "${env/JAVA_HOME}");
            } else {
                props.put("javaHome", mb.getJavaHomeVar());
            }

            props.put("jvmProperties", mb.getJvmParams());
            props.put("mavenProps", mb.getAllBuildParams());

            StringBuilder sb = new StringBuilder(1024);

            for(NameValuePair nvp : mb.getEnvironmentVariableArray()) {
                sb.append(nvp.getName());
                sb.append("=");
                sb.append(nvp.getValue());
                sb.append("\n");
            }

            props.put("env-vars", sb.toString());

            // Try to cook it
            Step ucbStep = context.getUcbJob()
                                  .createStep(
                                    "Build/Build Tools/Maven/Maven Build",
                                    c.getName(),
                                    c.getDescription() == null ?
                                        "" : c.getDescription(),
                                    context.nextUcbStep(),
                                    props
            );

            // Add the common stuff
            copyCommonBits(context, c, ucbStep);

            // Save it
            ucbUpdate(ucbStep);
        } catch(MigrateException e) {
            throw e;
        } catch(Exception e) {
            throw new MigrateException("Error while migrating Maven step", e);
        }
    }
}
