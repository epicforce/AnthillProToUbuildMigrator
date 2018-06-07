package net.epicforce.migrate.ahp.toucb.ucb;

/**
 * UcbException
 *
 * An exception that comes from UCB; either a connection error or an
 * authentication error, usually
 *
 * @author sconley
 */

import net.epicforce.migrate.ahp.exception.MigrateException;

public class UcbException extends MigrateException
{
    public UcbException(final String msg)
    {
        super(msg);
    }

    public UcbException(final String msg, final Exception e)
    {
        super(msg, e);
    }
}
