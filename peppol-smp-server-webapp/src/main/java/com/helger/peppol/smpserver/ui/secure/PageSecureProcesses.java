/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.helger.peppol.smpserver.ui.secure;

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.type.EBaseType;
import com.helger.commons.url.SMap;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.peppol.identifier.process.SimpleProcessIdentifier;
import com.helger.peppol.smpserver.domain.MetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageForm;
import com.helger.peppol.smpserver.ui.AppCommonUI;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.validation.error.FormErrors;

@WorkInProgress
public final class PageSecureProcesses extends AbstractSMPWebPageForm <ISMPServiceInformation>
{
  private final static String FIELD_DOCTYPE_ID = "doctypeid";
  private final static String FIELD_PROCESS_ID = "processid";

  private static final String ATTR_PROCESS = "$process";

  public PageSecureProcesses (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Processes");
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupManager = MetaManager.getServiceGroupMgr ();
    if (aServiceGroupManager.getSMPServiceGroupCount () == 0)
    {
      aNodeList.addChild (new BootstrapWarnBox ().addChild ("No service group is present! At least one service group must be present to create a process for it."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new service group")
                                                .setOnClick (createCreateURL (aWPEC, CMenuSecure.MENU_SERVICE_GROUPS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }
    return super.isValidToDisplayPage (aWPEC);
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPServiceInformation aSelectedObject)
  {
    if (eFormAction == EWebPageFormAction.EDIT)
      return false;

    if (eFormAction == EWebPageFormAction.VIEW)
    {
      final String sProcessID = aWPEC.getAttributeAsString (FIELD_PROCESS_ID);
      final SimpleProcessIdentifier aProcessID = sProcessID == null ? null
                                                                    : SimpleProcessIdentifier.createFromURIPartOrNull (sProcessID);

      final ISMPProcess aProcess = aSelectedObject.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        aWPEC.getRequestScope ().setAttribute (ATTR_PROCESS, aProcess);
        return true;
      }
      return false;
    }

    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  @Nullable
  protected ISMPServiceInformation getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                                      @Nullable final String sID)
  {
    final ISMPServiceInformationManager aServiceInfoMgr = MetaManager.getServiceInformationMgr ();
    return aServiceInfoMgr.getSMPServiceInformationOfID (sID);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPServiceInformation aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().getCastedAttribute (ATTR_PROCESS);

    aNodeList.addChild (createActionHeader ("Show details of process"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service group")
                                                 .setCtrl (aSelectedObject.getServiceGroupID ()));

    // Document type identifier
    {
      final HCNodeList aCtrl = new HCNodeList ();
      aCtrl.addChild (new HCDiv ().addChild (AppCommonUI.getDocumentTypeID (aSelectedObject.getDocumentTypeIdentifier ())));
      try
      {
        final IPeppolDocumentTypeIdentifierParts aParts = aSelectedObject.getDocumentTypeIdentifier ().getParts ();
        aCtrl.addChild (AppCommonUI.getDocumentTypeIDDetails (aParts));
      }
      catch (final IllegalArgumentException ex)
      {
        aCtrl.addChild (new BootstrapErrorBox ().addChild ("Failed to parse document type identifier: " +
                                                           ex.getMessage ()));
      }
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Document type ID").setCtrl (aCtrl));
    }

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Process ID")
                                                 .setCtrl (AppCommonUI.getProcessID (aSelectedProcess.getProcessIdentifier ())));

    aNodeList.addChild (aForm);

  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPServiceInformation aSelectedObject,
                                                 @Nonnull final FormErrors aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {}

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPServiceInformation aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrors aFormErrors)
  {}

  @Override
  protected void showDeleteQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                  @Nonnull final BootstrapForm aForm,
                                  @Nonnull final ISMPServiceInformation aSelectedObject)
  {}

  @Override
  protected void performDelete (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final ISMPServiceInformation aSelectedObject)
  {}

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceInformationManager aServiceInfoMgr = MetaManager.getServiceInformationMgr ();

    aNodeList.addChild (new BootstrapInfoBox ().addChild ("This page is informational only. You cannot do anything in here."));

    final HCTable aTable = new HCTable (new DTCol ("Service group").setInitialSorting (ESortOrder.ASCENDING)
                                                                   .setDataSort (0, 1, 2),
                                        new DTCol ("Document type ID").setDataSort (1, 0),
                                        new DTCol ("Process ID").setDataSort (2, 1, 0),
                                        new DTCol ("Endpoints").setDisplayType (EBaseType.INT, aDisplayLocale),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPServiceInformation aServiceInfo : aServiceInfoMgr.getAllSMPServiceInformations ())
      for (final ISMPProcess aProcess : aServiceInfo.getAllProcesses ())
      {
        final SMap aParams = new SMap ().add (FIELD_DOCTYPE_ID,
                                              aServiceInfo.getDocumentTypeIdentifier ().getURIPercentEncoded ())
                                        .add (FIELD_PROCESS_ID, aProcess.getProcessIdentifier ().getURIEncoded ());

        final HCRow aRow = aTable.addBodyRow ();
        aRow.addCell (new HCA (createViewURL (aWPEC,
                                              aServiceInfo).addAll (aParams)).addChild (aServiceInfo.getServiceGroupID ()));
        aRow.addCell (AppCommonUI.getDocumentTypeID (aServiceInfo.getDocumentTypeIdentifier ()));
        aRow.addCell (AppCommonUI.getProcessID (aProcess.getProcessIdentifier ()));
        aRow.addCell (Integer.toString (aServiceInfo.getTotalEndpointCount ()));
        aRow.addCell ();
      }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);

    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
