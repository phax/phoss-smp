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

import java.net.URL;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.URLHelper;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.sml.CSMLDefault;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.sml.ISMLInfoManager;
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

public class PageSecureSMLConfiguration extends AbstractSMPWebPageForm <ISMLInfo>
{
  private static final String FIELD_DISPLAY_NAME = "displayname";
  private static final String FIELD_DNS_ZONE = "dnszone";
  private static final String FIELD_MANAGEMENT_ADDRESS_URL = "mgmtaddrurl";
  private static final String FIELD_CLIENT_CERTIFICATE_REQUIRED = "clientcert";

  public PageSecureSMLConfiguration (@Nonnull @Nonempty final String sID)
  {
    super (sID, "SML configuration");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMLInfo, WebPageExecutionContext> ()
    {
      @Override
      protected void showDeleteQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                      @Nonnull final BootstrapForm aForm,
                                      @Nonnull final ISMLInfo aSelectedObject)
      {
        aForm.addChild (new BootstrapQuestionBox ().addChild (new HCDiv ().addChild ("Are you sure you want to delete the SML configuration '" +
                                                                                     aSelectedObject.getDisplayName () +
                                                                                     "'?")));
      }

      @Override
      protected void performDelete (@Nonnull final WebPageExecutionContext aWPEC,
                                    @Nonnull final ISMLInfo aSelectedObject)
      {
        final ISMLInfoManager aSMLInfoMgr = SMPMetaManager.getSMLInfoMgr ();
        if (aSMLInfoMgr.removeSMLInfo (aSelectedObject.getID ()).isChanged ())
          aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The SML configuration '" +
                                                                              aSelectedObject.getDisplayName () +
                                                                              "' was successfully deleted!"));
        else
          aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("The SML configuration '" +
                                                                            aSelectedObject.getDisplayName () +
                                                                            "' could not be deleted!"));
      }
    });
  }

  @Override
  protected ISMLInfo getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC, @Nullable final String sID)
  {
    final ISMLInfoManager aSMLInfoMgr = SMPMetaManager.getSMLInfoMgr ();
    return aSMLInfoMgr.getSMLInfoOfID (sID);
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMLInfo aSelectedObject)
  {
    if (eFormAction.isDelete ())
    {
      // Cannot delete the selected object!
      if (aSelectedObject == SMPMetaManager.getSettings ().getSMLInfo ())
        return false;
    }
    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMLInfo aSelectedObject)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of SML configuration '" +
                                                            aSelectedObject.getDisplayName () +
                                                            "'"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Name").setCtrl (aSelectedObject.getDisplayName ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("DNS Zone").setCtrl (aSelectedObject.getDNSZone ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Publisher DNS Zone")
                                                 .setCtrl (aSelectedObject.getPublisherDNSZone ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Management Service URL")
                                                 .setCtrl (HCA.createLinkedWebsite (aSelectedObject.getManagementServiceURL ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Manage Service Metadata Endpoint")
                                                 .setCtrl (HCA.createLinkedWebsite (aSelectedObject.getManageServiceMetaDataEndpointAddress ()
                                                                                                   .toExternalForm ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Manage Participant Identifier Endpoint")
                                                 .setCtrl (HCA.createLinkedWebsite (aSelectedObject.getManageParticipantIdentifierEndpointAddress ()
                                                                                                   .toExternalForm ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Client certificate required?")
                                                 .setCtrl (EPhotonCoreText.getYesOrNo (aSelectedObject.isClientCertificateRequired (),
                                                                                       aDisplayLocale)));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMLInfo aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrorList aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();

    aForm.addChild (getUIHandler ().createActionHeader (bEdit ? "Edit SML configuration '" +
                                                                aSelectedObject.getDisplayName () +
                                                                "'"
                                                              : "Create new SML configuration"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Name")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_DISPLAY_NAME,
                                                                                         aSelectedObject != null ? aSelectedObject.getDisplayName ()
                                                                                                                 : null)))
                                                 .setHelpText ("The name of the SML configuration. This is for informational purposes only and has no effect on the functionality.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_DISPLAY_NAME)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("DNS Zone")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_DNS_ZONE,
                                                                                         aSelectedObject != null ? aSelectedObject.getDNSZone ()
                                                                                                                 : null)))
                                                 .setHelpText (new HCTextNode ("The name of the DNS Zone that this SML is working upon (e.g. "),
                                                               new HCCode ().addChild ("sml.peppolcentral.org"),
                                                               new HCTextNode ("). The value will automatically converted to all-lowercase!"))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_DNS_ZONE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Management Service URL")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_MANAGEMENT_ADDRESS_URL,
                                                                                         aSelectedObject != null ? aSelectedObject.getManagementServiceURL ()
                                                                                                                 : null)))
                                                 .setHelpText ("The service URL where the SML management application is running on including the host name. It may not contain the '" +
                                                               CSMLDefault.MANAGEMENT_SERVICE_METADATA +
                                                               "' or '" +
                                                               CSMLDefault.MANAGEMENT_SERVICE_PARTICIPANTIDENTIFIER +
                                                               "' path elements!")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_MANAGEMENT_ADDRESS_URL)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Client Certificate required?")
                                                 .setCtrl (new BootstrapCheckBox (new RequestFieldBoolean (FIELD_CLIENT_CERTIFICATE_REQUIRED,
                                                                                                           aSelectedObject != null ? aSelectedObject.isClientCertificateRequired ()
                                                                                                                                   : true)))
                                                 .setHelpText ("Check this if this SML requires a client certificate for access. Both PEPPOL production SML and SMK require a client certificate. Only a locally running SML software may not require a client certificate.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_CLIENT_CERTIFICATE_REQUIRED)));

  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMLInfo aSelectedObject,
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final ISMLInfoManager aSMLInfoMgr = SMPMetaManager.getSMLInfoMgr ();

    final String sDisplayName = aWPEC.params ().getAsString (FIELD_DISPLAY_NAME);
    final String sDNSZone = aWPEC.params ().getAsString (FIELD_DNS_ZONE);
    final String sManagementAddressURL = aWPEC.params ().getAsString (FIELD_MANAGEMENT_ADDRESS_URL);
    final boolean bClientCertificateRequired = aWPEC.params ().isCheckBoxChecked (FIELD_CLIENT_CERTIFICATE_REQUIRED,
                                                                                  true);

    // validations
    if (StringHelper.hasNoText (sDisplayName))
      aFormErrors.addFieldError (FIELD_DISPLAY_NAME, "The SML configuration name must not be empty!");

    if (StringHelper.hasNoText (sDNSZone))
      aFormErrors.addFieldError (FIELD_DNS_ZONE, "The DNS Zone must not be empty!");

    if (StringHelper.hasNoText (sManagementAddressURL))
      aFormErrors.addFieldError (FIELD_MANAGEMENT_ADDRESS_URL, "The Management Address URL must not be empty!");
    else
    {
      final URL aURL = URLHelper.getAsURL (sManagementAddressURL);
      if (aURL == null)
        aFormErrors.addFieldError (FIELD_MANAGEMENT_ADDRESS_URL, "The Management Address URL is not a valid URL!");
      else
        if (!"https".equals (aURL.getProtocol ()) && !"http".equals (aURL.getProtocol ()))
          aFormErrors.addFieldError (FIELD_MANAGEMENT_ADDRESS_URL,
                                     "The Management Address URL should only be use the 'http' or the 'https' protocol!");
    }

    if (aFormErrors.isEmpty ())
    {
      // Lowercase with the US locale - not display locale specific
      final String sDNSZoneLC = sDNSZone.toLowerCase (Locale.US);

      if (bEdit)
      {
        aSMLInfoMgr.updateSMLInfo (aSelectedObject.getID (),
                                   sDisplayName,
                                   sDNSZoneLC,
                                   sManagementAddressURL,
                                   bClientCertificateRequired);
        aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The SML configuration '" +
                                                                            sDisplayName +
                                                                            "' was successfully edited."));
      }
      else
      {
        aSMLInfoMgr.createSMLInfo (sDisplayName, sDNSZoneLC, sManagementAddressURL, bClientCertificateRequired);
        aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The new SML configuration '" +
                                                                            sDisplayName +
                                                                            "' was successfully created."));
      }
    }
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMLInfoManager aSMLInfoMgr = SMPMetaManager.getSMLInfoMgr ();

    aNodeList.addChild (new BootstrapInfoBox ().addChild ("This page lets you create custom SML configurations that can be used for registration."));

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new SML configuration", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aNodeList.addChild (aToolbar);

    final HCTable aTable = new HCTable (new DTCol ("Name").setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("DNS Zone"),
                                        new DTCol ("Management Service URL"),
                                        new DTCol ("Client Cert?"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMLInfo aCurObject : aSMLInfoMgr.getAllSMLInfos ())
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (aCurObject.getDisplayName ()));
      aRow.addCell (aCurObject.getDNSZone ());
      aRow.addCell (aCurObject.getManagementServiceURL ());
      aRow.addCell (EPhotonCoreText.getYesOrNo (aCurObject.isClientCertificateRequired (), aDisplayLocale));

      aRow.addCell (createEditLink (aWPEC, aCurObject, "Edit " + aCurObject.getID ()),
                    new HCTextNode (" "),
                    createCopyLink (aWPEC, aCurObject, "Copy " + aCurObject.getID ()),
                    new HCTextNode (" "),
                    isActionAllowed (aWPEC,
                                     EWebPageFormAction.DELETE,
                                     aCurObject) ? createDeleteLink (aWPEC, aCurObject, "Delete " + aCurObject.getDisplayName ()) : createEmptyAction ());
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
