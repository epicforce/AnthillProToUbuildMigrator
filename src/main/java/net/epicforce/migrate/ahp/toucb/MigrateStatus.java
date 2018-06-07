package net.epicforce.migrate.ahp.toucb;

import java.lang.StringBuilder;

/*
 * MigrateStatus.java
 *
 * Class to transport migration status from the MigrateEngine
 * In a separate file because this is shared and its a little
 * easier this way.  Don't judge me!
 *
 * @author sconley (sconley@epicforce.net)
 */

public class MigrateStatus
{
    public Long     workflowId;
    public int      progress = 0;
    public String   errorMessage = null;

    /**
     * Politely create a string version of this.
     */
    public String toString()
    {
        int sbLen = 256;

        if(errorMessage != null) {
            sbLen += errorMessage.length();
        }

        StringBuilder sb = new StringBuilder(sbLen);
        sb.append("MigrateStatus: WorkflowId(");
        sb.append(workflowId);
        sb.append(") progress(");
        sb.append(progress);
        sb.append(") errorMessage(");
        sb.append(errorMessage);
        sb.append(")");
        return sb.toString();
    }
}
