/*******************************************************************************
 * Copyright (c) 2004, 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.debug.internal.ui.views.memory;


import java.util.Enumeration;
import java.util.Hashtable;

import org.eclipse.debug.core.IMemoryBlockListener;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IMemoryBlockRetrieval;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.contexts.DebugContextManager;
import org.eclipse.debug.internal.ui.contexts.provisional.IDebugContextListener;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.IWorkbenchPart;

public abstract class AbstractMemoryViewPane implements IMemoryBlockListener, ISelectionListener, SelectionListener, IMemoryView, ISelectionChangedListener, IMemoryViewPane, IDebugContextListener{
	
	public static final String BEGINNING_POPUP = "popUpBegin"; //$NON-NLS-1$
	protected static final StructuredSelection EMPTY = new StructuredSelection();
	
	protected Composite fViewPaneCanvas;
	protected StackLayout fStackLayout;
	protected ViewTabEnablementManager fViewTabEnablementManager;
	protected TabFolder fEmptyTabFolder;
	protected Hashtable fTabFolderForDebugView = new Hashtable(); 
	protected boolean fVisible;
	protected Hashtable fRenderingInfoTable;
	protected IMemoryBlockRetrieval fKey;  // store the key for current tab folder
	protected ViewPaneSelectionProvider fSelectionProvider;
	protected IViewPart fParent;
	protected String fPaneId;
	private Composite fCanvas;
	protected String fLabel;
	
	public AbstractMemoryViewPane(IViewPart parent)
	{
		super();
		fParent = parent;
		fSelectionProvider = new ViewPaneSelectionProvider();
	}
	
	/**
	 * Create the content of the view pane
	 * @param parent
	 * @return the control of the view pane
	 */
	public Control createViewPane(Composite parent, String paneId, String label)
	{	
		fPaneId = paneId;
		fLabel = label;
		fCanvas = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout();
		layout.makeColumnsEqualWidth = false;
		layout.numColumns = 1;
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		GridData data = new GridData();
		data.grabExcessHorizontalSpace = true;
		data.grabExcessVerticalSpace = true;
		data.verticalAlignment = SWT.BEGINNING;
		data.horizontalAlignment = SWT.BEGINNING;
		fCanvas.setLayout(layout);
		fCanvas.setLayoutData(data);

		// memory view area
		Composite memoryViewAreaParent = fCanvas;
		Composite subCanvas = new Composite(memoryViewAreaParent, SWT.NONE);	
		fViewPaneCanvas = subCanvas;
		fStackLayout = new StackLayout();
		GridData memoryAreaData = new GridData();
		memoryAreaData.grabExcessHorizontalSpace = true;
		memoryAreaData.grabExcessVerticalSpace = true;
		memoryAreaData.verticalAlignment = SWT.FILL;
		memoryAreaData.horizontalAlignment = SWT.FILL;
		fViewPaneCanvas.setLayout(fStackLayout);
		fViewPaneCanvas.setLayoutData(memoryAreaData);
		
		fViewTabEnablementManager = new ViewTabEnablementManager();
		
		fEmptyTabFolder = new TabFolder(fViewPaneCanvas, SWT.NULL);
		setTabFolder(fEmptyTabFolder);
		
		addListeners();
		
		Object context = DebugUITools.getDebugContext();
		if (context != null)
		{
			IMemoryBlockRetrieval retrieval = MemoryViewUtil.getMemoryBlockRetrieval(context);
			if (retrieval != null)
				createFolder(retrieval);
		}
		
		fVisible = true;
		
		return fCanvas;
	}
	
	protected void addListeners()
	{
		MemoryViewUtil.getMemoryBlockManager().addListener(this);
		fParent.getViewSite().getPage().addSelectionListener(this);
		DebugContextManager.getDefault().addDebugContextListener(this, fParent.getSite().getWorkbenchWindow());
	}
	
	protected void removeListeners()
	{
		MemoryViewUtil.getMemoryBlockManager().removeListener(this);
		fParent.getViewSite().getPage().removeSelectionListener(this);
		DebugContextManager.getDefault().removeDebugContextListener(this, fParent.getSite().getWorkbenchWindow());
		
		if (fStackLayout.topControl != null)
		{
			TabFolder old = (TabFolder)fStackLayout.topControl;
			
			if (!old.isDisposed())
			{	
				old.removeSelectionListener(this);
				old.removeSelectionListener(fViewTabEnablementManager);
			}
		}
	}
	
	protected void setTabFolder(TabFolder folder)
	{
		if (fStackLayout.topControl != null)
		{
			TabFolder old = (TabFolder)fStackLayout.topControl;
			
			if (!old.isDisposed())
			{	
				old.removeSelectionListener(this);
				old.removeSelectionListener(fViewTabEnablementManager);
			}
		}
		
		fStackLayout.topControl = folder;
		
		if (folder.getItemCount() > 0)
		{
			TabItem[] selectedItem = folder.getSelection();
			
			if (selectedItem.length > 0)
			{
				Object selected = getCurrentSelection();
				if (selected != null)
				{
					fSelectionProvider.setSelection(new StructuredSelection(selected));
				}
				else
				{
					fSelectionProvider.setSelection(AbstractMemoryViewPane.EMPTY);
				}
			}
		}
		else
		{
			fSelectionProvider.setSelection(AbstractMemoryViewPane.EMPTY);
		}
		
		folder.addSelectionListener(this);
		folder.addSelectionListener(fViewTabEnablementManager);
	}	
	
	
	private void createFolder(IMemoryBlockRetrieval memRetrieval)
	{	
		//if we've got a tabfolder to go with the IMemoryBlockRetrieval, display it
		if (fTabFolderForDebugView.containsKey(memRetrieval)) {
			if (fStackLayout.topControl != (TabFolder)fTabFolderForDebugView.get(memRetrieval)) {
				setTabFolder((TabFolder)fTabFolderForDebugView.get(memRetrieval));
				fViewPaneCanvas.layout();
			}
		} else {	//otherwise, add a new one
			fTabFolderForDebugView.put(memRetrieval, new TabFolder(fViewPaneCanvas, SWT.NULL));
			setTabFolder((TabFolder)fTabFolderForDebugView.get(memRetrieval));
			fViewPaneCanvas.layout();
		}
	}

	public IMemoryViewTab getTopMemoryTab() {
		
		if (fStackLayout.topControl instanceof TabFolder)
		{
			TabFolder folder = (TabFolder)fStackLayout.topControl;
			if (!folder.isDisposed())
			{
				int index = folder.getSelectionIndex();
				if (index >= 0) {
					TabItem tab = folder.getItem(index);
					return (IMemoryViewTab)tab.getData();
				}
			}
		}
		return null;
	}
	
	protected void disposeTab(TabItem tabItem)
	{
		if (tabItem == null)
			return;
		
		// dispose the tab item in case the view tab has not 
		// cleaned up the tab item
		if (!tabItem.isDisposed())
		{	
			tabItem.dispose();
		}			
	}

	protected void emptyFolder()
	{		
		setTabFolder(fEmptyTabFolder);
		if (!fViewPaneCanvas.isDisposed()) {
			fViewPaneCanvas.layout();
		}
	}
	
	public void addSelectionListener(ISelectionChangedListener listener)
	{
		if (fSelectionProvider == null)
			fSelectionProvider = new ViewPaneSelectionProvider();
		
		fSelectionProvider.addSelectionChangedListener(listener);
	}
	
	public void removeSelctionListener(ISelectionChangedListener listener)
	{
		if (fSelectionProvider == null)
			return;
		
		fSelectionProvider.removeSelectionChangedListener(listener);
	}
	
	public ISelectionProvider getSelectionProvider()
	{
		return fSelectionProvider;
	}
	
	public void dispose()
	{
		removeListeners();
		
		// dispose empty folders
		fEmptyTabFolder.dispose();
		
		// dispose all other folders
		try {
			
			if (fTabFolderForDebugView != null) {
				Enumeration enumeration = fTabFolderForDebugView.elements();
				
				while (enumeration.hasMoreElements())
				{
					TabFolder tabFolder = (TabFolder)enumeration.nextElement();
					
					if (tabFolder.isDisposed())
						continue;
					
					// if tab folder is not empty, dipose view tabs
					TabItem[] tabs = tabFolder.getItems();
					
					for (int i=0; i<tabs.length; i++)
					{
						disposeTab(tabs[i]);
					}
					
					tabFolder.dispose();
				}
				
				// set to null so that clean up is only done once
				fTabFolderForDebugView.clear();
				fTabFolderForDebugView = null;
			}
		} catch (Exception e) {		
			
			DebugUIPlugin.logErrorMessage("Exception occurred when the Memory View is disposed."); //$NON-NLS-1$
		}		
	}
	
	public void setVisible(boolean visible)
	{
		fVisible = visible;
		
		IMemoryViewTab currentTab = getTopMemoryTab();
		if (currentTab != null)
			currentTab.setEnabled(visible);
	}

	public void selectionChanged(SelectionChangedEvent event) {
		ISelection selection = event.getSelection();
		selectionChanged(fParent,selection);
		
		fSelectionProvider.setSelection(selection);
	}
	
	/**
	 * @return the unique identifier of the view pane
	 */
	public String getPaneId()
	{
		return fPaneId;
	}
	
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.ui.views.memory.IMemoryViewPane#getControl()
	 */
	public Control getControl() {
		return fCanvas;
	}
	
	public boolean isVisible()
	{
		return fVisible;
	}
	
	public String getLabel()
	{
		return fLabel;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.memory.IMemoryBlockListener#MemoryBlockAdded(org.eclipse.debug.core.model.IMemoryBlock)
	 */
	abstract public void  memoryBlocksAdded(IMemoryBlock[] memoryBlocks);
	
	/* (non-Javadoc)
	 * @see org.eclipse.debug.internal.core.memory.IMemoryBlockListener#MemoryBlockRemoved(org.eclipse.debug.core.model.IMemoryBlock)
	 */
	abstract public void memoryBlocksRemoved(final IMemoryBlock[] memoryBlocks);

	/* (non-Javadoc)
	 * @see org.eclipse.ui.ISelectionListener#selectionChanged(org.eclipse.ui.IWorkbenchPart, org.eclipse.jface.viewers.ISelection)
	 */
	abstract public void selectionChanged(IWorkbenchPart part, ISelection selection);
	
	/**
	 * @return current selection from the view pane
	 */
	abstract public Object getCurrentSelection();
	
	/**
	 * retore the view pane based on current selection from the debug view
	 * and the memory blocks and renderings currently exist 
	 */
	abstract public void restoreViewPane();
	
	/**
	 * @return actions to be contributed to the view pane's toolbar
	 */
	abstract public IAction[] getActions();

}