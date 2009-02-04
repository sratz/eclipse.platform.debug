/*******************************************************************************
 * Copyright (c) 2005, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Bjorn Freeman-Benson - initial API and implementation
 *     Pawel Piech (Wind River) - ported PDA Virtual Machine to Java (Bug 261400)
 *******************************************************************************/
package org.eclipse.debug.examples.core.pda.model;

import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.model.DebugElement;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.examples.core.pda.DebugCorePlugin;
import org.eclipse.debug.examples.core.protocol.PDACommand;
import org.eclipse.debug.examples.core.protocol.PDACommandResult;


/**
 * Common function for PDA debug elements.
 */
public class PDADebugElement extends DebugElement {

	/**
	 * Constructs a new debug element in the given target.
	 * 
	 * @param target debug target
	 */
	public PDADebugElement(IDebugTarget target) {
		super(target);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugElement#getModelIdentifier()
	 */
	public String getModelIdentifier() {
		return DebugCorePlugin.ID_PDA_DEBUG_MODEL;
	}
	
	/**
     * Sends a request to the PDA interpreter, waits for and returns the reply.
     * 
     * @param request command
     * @return reply
     * @throws DebugException if the request fails
     * 
     * @see org.eclipse.debug.examples.core.protocol.PDATerminateCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAVMSuspendCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAVMResumeCommand
     * 
     * @see org.eclipse.debug.examples.core.protocol.PDASuspendCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAResumeCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAStepCommand
     * @see org.eclipse.debug.examples.core.protocol.PDADropFrameCommand
     * 
     * @see org.eclipse.debug.examples.core.protocol.PDASetBreakpointCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAClearBreakpointCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAWatchCommand
     * 
     * @see org.eclipse.debug.examples.core.protocol.PDADataCommand
     * @see org.eclipse.debug.examples.core.protocol.PDASetDataCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAPopDataCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAPushDataCommand
     * 
     * @see org.eclipse.debug.examples.core.protocol.PDAEvalCommand
     * 
     * @see org.eclipse.debug.examples.core.protocol.PDAEventStopCommand
     * 
     * @see org.eclipse.debug.examples.core.protocol.PDAStackCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAStackDepthCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAFrameCommand
     * 
     * @see org.eclipse.debug.examples.core.protocol.PDASetVarCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAVarCommand
     * @see org.eclipse.debug.examples.core.protocol.PDAChildrenCommand
     * 
     * @see org.eclipse.debug.examples.core.protocol.PDAGroupsCommand
     * @see org.eclipse.debug.examples.core.protocol.PDARegistersCommand
     * 
     * @since 3.5
     */ 
	public PDACommandResult sendCommand(PDACommand command) throws DebugException {
        return getPDADebugTarget().sendCommand(command);
    }
	
	/**
	 * Returns the debug target as a PDA target.
	 * 
	 * @return PDA debug target
	 */
	protected PDADebugTarget getPDADebugTarget() {
	    return (PDADebugTarget) getDebugTarget();
	}
	
	/**
	 * Returns the breakpoint manager
	 * 
     * @return the breakpoint manager
     */
    protected IBreakpointManager getBreakpointManager() {
        return DebugPlugin.getDefault().getBreakpointManager();
    }	
}