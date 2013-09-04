/*
 * ###
 * 
 * Copyright (C) 1999 - 2012 Photon Infotech Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * ###
 */
package com.photon.phresco.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.StringUtils;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.custom.BusyIndicator;
import org.eclipse.swt.widgets.Button;
import org.eclipse.ui.INewWizard;
import org.eclipse.ui.IWorkbench;

import com.photon.phresco.commons.PhrescoDialog;
import com.photon.phresco.commons.model.ApplicationInfo;
import com.photon.phresco.commons.model.ProjectInfo;
import com.photon.phresco.commons.model.TechnologyInfo;
import com.photon.phresco.commons.util.ProjectManager;
import com.photon.phresco.exception.PhrescoException;
import com.photon.phresco.ui.model.BaseAction;
import com.photon.phresco.ui.resource.Messages;
import com.photon.phresco.ui.wizards.componets.LayerComponent;
import com.photon.phresco.ui.wizards.pages.AddProjectPage;
import com.photon.phresco.ui.wizards.pages.TechnologyPage;

/**
 * Phresco project wizard
 * 
 * @author suresh_ma
 *
 */
public class PhrescoProjectWizard extends Wizard implements INewWizard {

	private AddProjectPage appInfoPage;
	private TechnologyPage technologyPage;
	
	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection) {

/*		ImageDescriptor myImage = ImageDescriptor.createFromURL(FileLocator.find(PhrescoPlugin.getDefault().getBundle(),
				new Path("icons/phresco.gif"),null));
		super.setDefaultPageImageDescriptor(myImage);*/
		super.setNeedsProgressMonitor(true);
		super.setWindowTitle(Messages.PHRESCO_IMPORT_WINDOW_TITLE);

	}

	@Override
	public void addPages() {
		super.addPages();
		technologyPage = new TechnologyPage(Messages.IMPORT_TECHNOLOGY_PAGE_NAME);
		appInfoPage = new AddProjectPage(Messages.IMPORT_ADD_PROJECT_PAGE_NAME);
		addPage(appInfoPage);
		addPage(technologyPage);
	}
	
	@Override
	public IWizardPage getNextPage(IWizardPage page) {
		IWizardPage wizardPage = getContainer().getCurrentPage();

		if (wizardPage instanceof TechnologyPage) {
			
			final TechnologyPage technologyPage = (TechnologyPage) wizardPage;
			
			IWizardPage firstPage = getContainer().getCurrentPage().getPreviousPage();

			AddProjectPage addProjectPage = (AddProjectPage) firstPage;
			final List<Button> layersList = addProjectPage.getLayersList();
			BusyIndicator.showWhile(null, new Runnable() {
	            public void run() {
	            	technologyPage.renderLayer(layersList);
	            }
	        });
		}
		return super.getNextPage(page);
	}
	
	@Override
	public boolean canFinish() {
		if(getContainer().getCurrentPage() == appInfoPage) {
			return false;
		}
		return true;
	}
	
	@Override
	public boolean performFinish() {
		IWizardPage[] pages = getPages();
		final ProjectInfo projectInfo = new ProjectInfo();
		List<ApplicationInfo> appInfos = new ArrayList<ApplicationInfo>();
		for (IWizardPage wizardPage : pages) {
			if(wizardPage instanceof AddProjectPage) {
				AddProjectPage addProjectPage = (AddProjectPage) wizardPage;
				String projectName = addProjectPage.projectTxt.getText();
				String projectCode = addProjectPage.codeTxt.getText();
				String description = addProjectPage.descriptionTxt.getText();
				String version = addProjectPage.versionTxt.getText();
				projectInfo.setName(projectName);
				projectInfo.setDescription(description);
				projectInfo.setProjectCode(projectCode);
				projectInfo.setVersion(version);
			}
			if(wizardPage instanceof TechnologyPage) {
				TechnologyPage technologyPage = (TechnologyPage) wizardPage;
				String appVersion = projectInfo.getVersion();
				List<LayerComponent> webLayerComponents = technologyPage.layerComponents;
				if(CollectionUtils.isNotEmpty(webLayerComponents)) {
					for (LayerComponent webLayerComponent : webLayerComponents) {
						ApplicationInfo appInfo = new ApplicationInfo();
						String appCode = webLayerComponent.appCodeText.getText();
						if(StringUtils.isEmpty(appCode)) {
							PhrescoDialog.errorDialog(getShell(), Messages.WARNING, Messages.WARN_APPCODE_EMPTY);
							return false;
						}
						String techGroupName = webLayerComponent.techGroupNameCombo.getText();
						String techName = webLayerComponent.techNameCombo.getText();
						String techId = webLayerComponent.getTechIdMap().get(techGroupName + techName);
						String version = webLayerComponent.techVersionCombo.getText();
						appInfo.setAppDirName(appCode);
						appInfo.setCode(appCode);
						appInfo.setName(appCode);
						appInfo.setVersion(appVersion);
						TechnologyInfo techInfo = new TechnologyInfo();
						techInfo.setAppTypeId(webLayerComponent.getAppTypeId());
						techInfo.setId(techId);
						techInfo.setName(techName);
						techInfo.setVersion(version);
						appInfo.setTechInfo(techInfo);
						appInfos.add(appInfo);
					}
				}
			}
		}
		BaseAction baseAction = new BaseAction();
		String customerId = baseAction.getCustomerId();
		projectInfo.setNoOfApps(appInfos.size());
		projectInfo.setCustomerIds(Arrays.asList(customerId));
		projectInfo.setAppInfos(appInfos);
		try {
			getContainer().run(true, true, new IRunnableWithProgress() {
				
				@Override
				public void run(IProgressMonitor monitor) throws InvocationTargetException,
						InterruptedException {
					try {
						ProjectManager.createProject(projectInfo, monitor);
					} catch (PhrescoException e) {
						PhrescoDialog.exceptionDialog(getShell(), e);
					}
				}
			});
			PhrescoDialog.messageDialog(getShell(), Messages.PROJECT_CREATED_SUCCESS_MSG);
		} catch (InvocationTargetException e) {
			PhrescoDialog.exceptionDialog(getShell(), e);
		} catch (InterruptedException e) {
			PhrescoDialog.exceptionDialog(getShell(), e);
		}
		
		return true;
	}
}