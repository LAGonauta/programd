/*
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License as published by the Free Software
 * Foundation; either version 2 of the License, or (at your option) any later
 * version. You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */

package org.aitools.programd.interfaces;

import java.io.PrintStream;
import java.net.URL;

import org.aitools.programd.Core;
import org.aitools.programd.Core.Status;
import org.aitools.programd.interfaces.shell.Shell;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

/**
 * Creating a Console essentially means that loggers (as configured) will (may)
 * also print output to the console.
 * 
 * @author <a href="mailto:noel@aitools.org">Noel Bush</a>
 * @since 4.2
 */
public class Console
{
    /** The Core to which this console is (may be) attached. */
    private Core core;

    /** The settings for this console. */
    private ConsoleSettings settings;

    /** The stdout handler. */
    private ShellStreamAppender stdOutAppender;

    /** The stderr handler. */
    private ShellStreamAppender stdErrAppender;

    /** The Shell that will (may) be activated for this console. */
    private Shell shell;

    /**
     * Creates a <code>Console</code> with default output streams.
     * 
     * @param settingsPath the path to the settings file for the console
     */
    public Console(URL settingsPath)
    {
        initialize(settingsPath, System.out, System.err);
    }

    /**
     * Creates a <code>Console</code> with specified input, output and prompt
     * streams (this implies that a shell will be enabled).
     * 
     * @param settingsPath the path to the settings file
     * @param out the stream to use for normal messages
     * @param err the stream to use for error messages
     */
    public Console(URL settingsPath, PrintStream out, PrintStream err)
    {
        initialize(settingsPath, out, err);
    }

    /**
     * Performs initialization common to all constructors.
     * 
     * @param settingsPath the path for the settings file to use
     * @param out the stream to use for normal output messages
     * @param err the stream to use for error messages
     */
    private void initialize(URL settingsPath, PrintStream out, PrintStream err)
    {
        this.settings = new ConsoleSettings(settingsPath);

        // Messages to all logs will go up to the parent "programd" log, and out
        // to the console.
        this.stdOutAppender = new ShellStreamAppender(out, null, Level.INFO, this.settings);
        Logger.getLogger("programd").addAppender(this.stdOutAppender);

        // Error messages will be printed to stderr.
        this.stdErrAppender = new ShellStreamAppender(err, Level.WARN, null, this.settings);
        Logger.getLogger("programd").addAppender(this.stdErrAppender);
    }
    
    /**
     * Attaches the console to the given core.
     * 
     * @param coreToUse the core to which to attach
     */
    public void attachTo(Core coreToUse)
    {
        this.core = coreToUse;

        if (this.settings.useShell())
        {
            addShell(new Shell(this.settings), this.core);
        }
        else
        {
            Logger.getLogger("programd").info("Interactive shell disabled.  Awaiting SIGHUP to shut down.");
        }
    }

    /**
     * Adds the given Shell to the Console
     * 
     * @param shellToAdd the Shell to add
     * @param coreToUse the core to which to attach the Shell
     */
    public void addShell(Shell shellToAdd, Core coreToUse)
    {
        this.shell = shellToAdd;
        this.shell.attachTo(coreToUse);
        this.stdOutAppender.watch(this.shell);
        this.stdErrAppender.watch(this.shell);
    }

    /**
     * Starts the attached shell (if one exists and the shell is enabled).
     */
    public void startShell()
    {
        if (this.settings.useShell())
        {
            this.shell.start();
        }

        // Now just run as long as the core status stays at READY.
        while (this.core.getStatus() == Status.READY)
        {
            try
            {
                Thread.sleep(1000);
            }
            catch (InterruptedException e)
            {
                break;
            }
        }
    }

    /**
     * @return the console settings
     */
    public ConsoleSettings getSettings()
    {
        return this.settings;
    }
}
