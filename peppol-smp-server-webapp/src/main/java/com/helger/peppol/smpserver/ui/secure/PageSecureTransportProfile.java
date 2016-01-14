/**
 * Copyright (C) 2014-2016 Philip Helger (www.helger.com)
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
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.errorlist.FormErrors;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.transportprofile.SMPTransportProfileManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageForm;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapQuestionBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;

public class PageSecureTransportProfile extends AbstractSMPWebPageForm <ISMPTransportProfile>
{
  private static final String FIELD_ID = "id";
  private static final String FIELD_NAME = "name";

  public PageSecureTransportProfile (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Transport profiles");
  }

  @Override
  protected ISMPTransportProfile getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC, @Nullable final String sID)
  {
    final SMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();
    return aTransportProfileMgr.getSMPTransportProfileOfID (sID);
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPTransportProfile aSelectedObject)
  {
    if (eFormAction.isDelete ())
    {
      // Can only delete non-standard protocol
      if (ESMPTransportProfile.getFromIDOrNull (aSelectedObject.getID ()) != null)
        return false;

      // If the transport profile is already used, it cannot be deleted
      final ISMPServiceInformationManager aServiceInformationMgr = SMPMetaManager.getServiceInformationMgr ();
      for (final ISMPServiceInformation aServiceInfo : aServiceInformationMgr.getAllSMPServiceInformation ())
        for (final ISMPProcess aProcess : aServiceInfo.getAllProcesses ())
          for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
            if (aEndpoint.getTransportProfile ().equals (aSelectedObject.getID ()))
              return false;
    }

    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC, @Nonnull final ISMPTransportProfile aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    aNodeList.addChild (createActionHeader ("Show details of transport profile '" + aSelectedObject.getID () + "'"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("ID").setCtrl (aSelectedObject.getID ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Name").setCtrl (aSelectedObject.getName ()));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPTransportProfile aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrors aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();

    aForm.addChild (createActionHeader (bEdit ? "Edit transport profile '" + aSelectedObject.getID () + "'" : "Create new transport profile"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("ID")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_ID,
                                                                                         aSelectedObject != null ? aSelectedObject.getID ()
                                                                                                                 : null)).setReadOnly (bEdit))
                                                 .setHelpText ("The ID of the transport profile to be used in SMP endpoints.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Name")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_NAME,
                                                                                         aSelectedObject != null ? aSelectedObject.getName ()
                                                                                                                 : null)))
                                                 .setHelpText ("The name of the transport profile")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_NAME)));
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPTransportProfile aSelectedObject,
                                                 @Nonnull final FormErrors aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final SMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();

    // Never edit ID
    final String sID = bEdit ? aSelectedObject.getID () : aWPEC.getAttributeAsString (FIELD_ID);
    final String sName = aWPEC.getAttributeAsString (FIELD_NAME);

    // validations
    if (StringHelper.hasNoText (sID))
      aFormErrors.addFieldError (FIELD_ID, "Transport profile ID must not be empty!");
    else
      if (!bEdit)
      {
        final ISMPTransportProfile aOther = aTransportProfileMgr.getSMPTransportProfileOfID (sID);
        if (aOther != null)
          aFormErrors.addFieldError (FIELD_ID, "Another transport profile with the same name already exists!");
      }

    if (StringHelper.isEmpty (sName))
      aFormErrors.addFieldError (FIELD_NAME, "The transport profile name must not be empty!");

    if (aFormErrors.isEmpty ())
    {
      if (bEdit)
      {
        aTransportProfileMgr.updateSMPTransportProfile (sID, sName);
        aWPEC.postRedirectGet (new BootstrapSuccessBox ().addChild ("The transport profile '" + sID + "' was successfully edited."));
      }
      else
      {
        aTransportProfileMgr.createSMPTransportProfile (sID, sName);
        aWPEC.postRedirectGet (new BootstrapSuccessBox ().addChild ("The new transport profile '" + sID + "' was successfully created."));
      }
    }
  }

  @Override
  protected void showDeleteQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                  @Nonnull final BootstrapForm aForm,
                                  @Nonnull final ISMPTransportProfile aSelectedObject)
  {
    aForm.addChild (new BootstrapQuestionBox ().addChild (new HCDiv ().addChild ("Are you sure you want to delete the transport profile '" +
                                                                                 aSelectedObject.getID () +
                                                                                 "'?")));
  }

  @Override
  protected void performDelete (@Nonnull final WebPageExecutionContext aWPEC, @Nonnull final ISMPTransportProfile aSelectedObject)
  {
    final SMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();
    aTransportProfileMgr.removeSMPTransportProfile (aSelectedObject.getID ());
    aWPEC.postRedirectGet (new BootstrapSuccessBox ().addChild ("The transport profile '" +
                                                                aSelectedObject.getID () +
                                                                "' was successfully deleted!"));
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final SMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();

    aNodeList.addChild (new BootstrapInfoBox ().addChild ("This page lets you create custom transport profiles that can be used in service information endpoints."));

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new transport profile", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aNodeList.addChild (aToolbar);

    final HCTable aTable = new HCTable (new DTCol ("ID").setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Name"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPTransportProfile aCurObject : aTransportProfileMgr.getAllSMPTransportProfiles ())
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (aCurObject.getID ()));
      aRow.addCell (aCurObject.getName ());

      aRow.addCell (createEditLink (aWPEC, aCurObject, "Edit " + aCurObject.getID ()),
                    new HCTextNode (" "),
                    createCopyLink (aWPEC, aCurObject, "Copy " + aCurObject.getID ()),
                    new HCTextNode (" "),
                    isActionAllowed (aWPEC, EWebPageFormAction.DELETE, aCurObject)
                                                                                   ? createDeleteLink (aWPEC,
                                                                                                       aCurObject,
                                                                                                       "Delete " + aCurObject.getID ())
                                                                                   : createEmptyAction ());
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
