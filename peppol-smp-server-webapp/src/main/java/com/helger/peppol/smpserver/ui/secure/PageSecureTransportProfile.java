/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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
import com.helger.peppol.smpserver.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageForm;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapQuestionBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapCheckBox;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldBoolean;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;

public class PageSecureTransportProfile extends AbstractSMPWebPageForm <ISMPTransportProfile>
{
  private static final String FIELD_ID = "id";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_DEPRECATED = "deprecated";
  private static final boolean DEFAULT_DEPRECATED = false;

  public PageSecureTransportProfile (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Transport profiles");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPTransportProfile, WebPageExecutionContext> ()
    {
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
      protected void performDelete (@Nonnull final WebPageExecutionContext aWPEC,
                                    @Nonnull final ISMPTransportProfile aSelectedObject)
      {
        final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();
        if (aTransportProfileMgr.removeSMPTransportProfile (aSelectedObject.getID ()).isChanged ())
          aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The transport profile '" +
                                                                              aSelectedObject.getID () +
                                                                              "' was successfully deleted!"));
        else
          aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Failed to delete transport profile '" +
                                                                            aSelectedObject.getID () +
                                                                            "'!"));
      }
    });
  }

  @Override
  protected ISMPTransportProfile getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                                    @Nullable final String sID)
  {
    final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();
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
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPTransportProfile aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of transport profile '" +
                                                            aSelectedObject.getID () +
                                                            "'"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("ID").setCtrl (aSelectedObject.getID ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Name").setCtrl (aSelectedObject.getName ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Deprecated?")
                                                 .setCtrl (EPhotonCoreText.getYesOrNo (aSelectedObject.isDeprecated (),
                                                                                       aDisplayLocale)));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPTransportProfile aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrorList aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();

    aForm.addChild (getUIHandler ().createActionHeader (bEdit ? "Edit transport profile '" +
                                                                aSelectedObject.getID () +
                                                                "'"
                                                              : "Create new transport profile"));

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

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Deprecated?")
                                                 .setCtrl (new BootstrapCheckBox (new RequestFieldBoolean (FIELD_DEPRECATED,
                                                                                                           aSelectedObject != null ? aSelectedObject.isDeprecated ()
                                                                                                                                   : DEFAULT_DEPRECATED)))
                                                 .setHelpText ("Is the transport profile deprecated?")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_DEPRECATED)));
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPTransportProfile aSelectedObject,
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();

    // Never edit ID
    final String sID = bEdit ? aSelectedObject.getID () : aWPEC.params ().getAsString (FIELD_ID);
    final String sName = aWPEC.params ().getAsString (FIELD_NAME);
    final boolean bIsDeprecated = aWPEC.params ().getAsBoolean (FIELD_DEPRECATED, DEFAULT_DEPRECATED);

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

    if (StringHelper.hasNoText (sName))
      aFormErrors.addFieldError (FIELD_NAME, "The transport profile name must not be empty!");

    if (aFormErrors.isEmpty ())
    {
      if (bEdit)
      {
        if (aTransportProfileMgr.updateSMPTransportProfile (sID, sName, bIsDeprecated).isChanged ())
          aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The transport profile '" +
                                                                              sID +
                                                                              "' was successfully edited."));
        else
          aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Failed to edit transport profile '" +
                                                                            sID +
                                                                            "'."));
      }
      else
      {
        if (aTransportProfileMgr.createSMPTransportProfile (sID, sName, bIsDeprecated) != null)
          aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The new transport profile '" +
                                                                              sID +
                                                                              "' was successfully created."));
        else
          aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Failed to create transport profile '" +
                                                                            sID +
                                                                            "'."));
      }
    }
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();

    aNodeList.addChild (new BootstrapInfoBox ().addChild ("This page lets you create custom transport profiles that can be used in service information endpoints."));

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new transport profile", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aNodeList.addChild (aToolbar);

    final HCTable aTable = new HCTable (new DTCol ("ID").setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Name"),
                                        new DTCol ("Deprecated?"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPTransportProfile aCurObject : aTransportProfileMgr.getAllSMPTransportProfiles ())
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (aCurObject.getID ()));
      aRow.addCell (aCurObject.getName ());
      aRow.addCell (EPhotonCoreText.getYesOrNo (aCurObject.isDeprecated (), aDisplayLocale));

      aRow.addCell (createEditLink (aWPEC, aCurObject, "Edit " + aCurObject.getID ()),
                    new HCTextNode (" "),
                    createCopyLink (aWPEC, aCurObject, "Copy " + aCurObject.getID ()),
                    new HCTextNode (" "),
                    isActionAllowed (aWPEC,
                                     EWebPageFormAction.DELETE,
                                     aCurObject) ? createDeleteLink (aWPEC, aCurObject, "Delete " + aCurObject.getID ())
                                                 : createEmptyAction ());
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
