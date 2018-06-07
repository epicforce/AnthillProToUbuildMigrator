package net.epicforce.migrate.ahp.toucb.notification;

/**
 * NotFoundException.java
 *
 * Exception used by classes in this package to indicate we could
 * not create the notification scheme for non-error related reasons
 * (such as unable to migrate Beanshell to Javascript)
 *
 * @author sconley (sconley@epicforce.net)
 */

public class NotFoundException extends Exception
{
    public NotFoundException(String reason)
    {
        super(reason);
    }
}
