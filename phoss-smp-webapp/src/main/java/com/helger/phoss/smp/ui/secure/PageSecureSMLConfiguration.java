/*
 * Copyright (C) 2014-2025 Philip Helger and contributors
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
package com.helger.phoss.smp.ui.secure;

import java.net.URL;
import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.ESortOrder;
import com.helger.base.string.StringHelper;
import com.helger.base.url.URLHelper;
import com.helger.html.hc.html.forms.HCCheckBox;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.sml.SMLInfo;
import com.helger.phoss.smp.ESMPRESTType;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.sml.ISMLInfoManager;
import com.helger.phoss.smp.ui.AbstractSMPWebPageForm;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldBoolean;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.url.ISimpleURL;

import jakarta.annotation.Nullable;

public class PageSecureSMLConfiguration extends AbstractSMPWebPageForm <ISMLInfo>
{
  private static final String FIELD_DISPLAY_NAME = "displayname";
  private static final String FIELD_DNS_ZONE = "dnszone";
  private static final String FIELD_MANAGEMENT_ADDRESS_URL = "mgmtaddrurl";
  private static final String FIELD_URL_SUFFIX_MANAGE_SMP = "managesmp";
  private static final String FIELD_URL_SUFFIX_MANAGE_PARTICIPANT = "manageparticipant";
  private static final String FIELD_CLIENT_CERTIFICATE_REQUIRED = "clientcert";

  private static final boolean DEFAULT_CLIENT_CERTIFICATE_REQUIRED = true;

  public PageSecureSMLConfiguration (@NonNull @Nonempty final String sID)
  {
    super (sID, "SML configuration");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMLInfo, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@NonNull final WebPageExecutionContext aWPEC,
                                @NonNull final BootstrapForm aForm,
                                @Nullable final ISMLInfo aSelectedObject)
      {
        aForm.addChild (question ("Are you sure you want to delete the SML configuration '" +
                                  aSelectedObject.getDisplayName () +
                                  "'?"));
      }

      @Override
      protected void performAction (@NonNull final WebPageExecutionContext aWPEC,
                                    @Nullable final ISMLInfo aSelectedObject)
      {
        final ISMLInfoManager aSMLInfoMgr = SMPMetaManager.getSMLInfoMgr ();
        if (aSMLInfoMgr.deleteSMLInfo (aSelectedObject.getID ()).isChanged ())
          aWPEC.postRedirectGetInternal (success ("The SML configuration '" +
                                                  aSelectedObject.getDisplayName () +
                                                  "' was successfully deleted!"));
        else
          aWPEC.postRedirectGetInternal (error ("The SML configuration '" +
                                                aSelectedObject.getDisplayName () +
                                                "' could not be deleted!"));
      }
    });
  }

  @Override
  protected ISMLInfo getSelectedObject (@NonNull final WebPageExecutionContext aWPEC, @Nullable final String sID)
  {
    final ISMLInfoManager aSMLInfoMgr = SMPMetaManager.getSMLInfoMgr ();
    return aSMLInfoMgr.getSMLInfoOfID (sID);
  }

  @Override
  protected boolean isActionAllowed (@NonNull final WebPageExecutionContext aWPEC,
                                     @NonNull final EWebPageFormAction eFormAction,
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
  protected void showSelectedObject (@NonNull final WebPageExecutionContext aWPEC,
                                     @NonNull final ISMLInfo aSelectedObject)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ESMPRESTType eRESTType = SMPServerConfiguration.getRESTType ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of SML configuration '" +
                                                            aSelectedObject.getDisplayName () +
                                                            "'"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Name").setCtrl (aSelectedObject.getDisplayName ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("DNS Zone").setCtrl (aSelectedObject.getDNSZone ()));
    if (eRESTType.isPeppol ())
    {
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Publisher DNS Zone")
                                                   .setCtrl (aSelectedObject.getPublisherDNSZone ()));
    }
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Management Service URL")
                                                 .setCtrl (HCA.createLinkedWebsite (aSelectedObject.getManagementServiceURL ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("URL suffix to manage SMPs")
                                                 .setCtrl (code (aSelectedObject.getURLSuffixManageSMP ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Manage Service Metadata Endpoint")
                                                 .setCtrl (HCA.createLinkedWebsite (aSelectedObject.getManageServiceMetaDataEndpointAddress ()
                                                                                                   .toExternalForm ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("URL suffix to manage Participants")
                                                 .setCtrl (code (aSelectedObject.getURLSuffixManageParticipant ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Manage Participant Identifier Endpoint")
                                                 .setCtrl (HCA.createLinkedWebsite (aSelectedObject.getManageParticipantIdentifierEndpointAddress ()
                                                                                                   .toExternalForm ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Client certificate required?")
                                                 .setCtrl (EPhotonCoreText.getYesOrNo (aSelectedObject.isClientCertificateRequired (),
                                                                                       aDisplayLocale)));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void showInputForm (@NonNull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMLInfo aSelectedObject,
                                @NonNull final BootstrapForm aForm,
                                final boolean bFormSubmitted,
                                @NonNull final EWebPageFormAction eFormAction,
                                @NonNull final FormErrorList aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();

    aForm.addChild (getUIHandler ().createActionHeader (bEdit ? "Edit SML configuration '" +
                                                                aSelectedObject.getDisplayName () +
                                                                "'" : "Create new SML configuration"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Name")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_DISPLAY_NAME,
                                                                                         aSelectedObject != null
                                                                                                                 ? aSelectedObject.getDisplayName ()
                                                                                                                 : null)))
                                                 .setHelpText ("The name of the SML configuration. This is for informational purposes only and has no effect on the functionality.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_DISPLAY_NAME)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("DNS Zone")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_DNS_ZONE,
                                                                                         aSelectedObject != null
                                                                                                                 ? aSelectedObject.getDNSZone ()
                                                                                                                 : null)))
                                                 .setHelpText (new HCTextNode ("The name of the DNS Zone that this SML is working upon (e.g. "),
                                                               code ("acc.edelivery.tech.ec.europa.eu"),
                                                               new HCTextNode ("). The value will automatically converted to all-lowercase!"))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_DNS_ZONE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Management Service URL")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_MANAGEMENT_ADDRESS_URL,
                                                                                         aSelectedObject != null
                                                                                                                 ? aSelectedObject.getManagementServiceURL ()
                                                                                                                 : null)))
                                                 .setHelpText ("The service URL where the SML management application is running on including the host name. It must not contain the suffixes to manage SMPs or participants!")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_MANAGEMENT_ADDRESS_URL)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("URL suffix to manage SMPs")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_URL_SUFFIX_MANAGE_SMP,
                                                                                         aSelectedObject != null
                                                                                                                 ? aSelectedObject.getURLSuffixManageSMP ()
                                                                                                                 : SMLInfo.DEFAULT_SUFFIX_MANAGE_SMP)))
                                                 .setHelpText ("This suffix is appended to the Management Service URL. If may be empty or alternatively it must start with a slash ('/').")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_URL_SUFFIX_MANAGE_SMP)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("URL suffix to manage Participants")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_URL_SUFFIX_MANAGE_PARTICIPANT,
                                                                                         aSelectedObject != null
                                                                                                                 ? aSelectedObject.getURLSuffixManageParticipant ()
                                                                                                                 : SMLInfo.DEFAULT_SUFFIX_MANAGE_PARTICIPANT)))
                                                 .setHelpText ("This suffix is appended to the Management Service URL. If may be empty or alternatively it must start with a slash ('/').")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_URL_SUFFIX_MANAGE_PARTICIPANT)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Client Certificate required?")
                                                 .setCtrl (new HCCheckBox (new RequestFieldBoolean (FIELD_CLIENT_CERTIFICATE_REQUIRED,
                                                                                                    aSelectedObject !=
                                                                                                                                       null ? aSelectedObject.isClientCertificateRequired ()
                                                                                                                                            : DEFAULT_CLIENT_CERTIFICATE_REQUIRED)))
                                                 .setHelpText ("Check this if this SML requires a client certificate for access. Both Peppol production SML and SMK require a client certificate. Only a locally running SML software may not require a client certificate.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_CLIENT_CERTIFICATE_REQUIRED)));
  }

  @Override
  protected void validateAndSaveInputParameters (@NonNull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMLInfo aSelectedObject,
                                                 @NonNull final FormErrorList aFormErrors,
                                                 @NonNull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final ISMLInfoManager aSMLInfoMgr = SMPMetaManager.getSMLInfoMgr ();

    final String sDisplayName = aWPEC.params ().getAsStringTrimmed (FIELD_DISPLAY_NAME);
    final String sDNSZone = aWPEC.params ().getAsStringTrimmed (FIELD_DNS_ZONE);
    final String sManagementAddressURL = aWPEC.params ().getAsStringTrimmed (FIELD_MANAGEMENT_ADDRESS_URL);
    final String sURLSuffixManageSMP = aWPEC.params ().getAsStringTrimmed (FIELD_URL_SUFFIX_MANAGE_SMP);
    final String sURLSuffixManageParticipant = aWPEC.params ().getAsStringTrimmed (FIELD_URL_SUFFIX_MANAGE_PARTICIPANT);
    final boolean bClientCertificateRequired = aWPEC.params ()
                                                    .isCheckBoxChecked (FIELD_CLIENT_CERTIFICATE_REQUIRED,
                                                                        DEFAULT_CLIENT_CERTIFICATE_REQUIRED);

    // validations
    if (StringHelper.isEmpty (sDisplayName))
      aFormErrors.addFieldError (FIELD_DISPLAY_NAME, "The SML configuration name must not be empty!");

    if (StringHelper.isEmpty (sDNSZone))
      aFormErrors.addFieldError (FIELD_DNS_ZONE, "The DNS Zone must not be empty!");

    if (StringHelper.isEmpty (sManagementAddressURL))
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

    if (StringHelper.isNotEmpty (sURLSuffixManageSMP))
    {
      if (!SMLInfo.isValidURLSuffix (sURLSuffixManageSMP))
        aFormErrors.addFieldError (FIELD_URL_SUFFIX_MANAGE_SMP,
                                   "The URL suffix must be empty or start with a slash followed by at least one character.");
    }

    if (StringHelper.isNotEmpty (sURLSuffixManageParticipant))
    {
      if (!SMLInfo.isValidURLSuffix (sURLSuffixManageParticipant))
        aFormErrors.addFieldError (FIELD_URL_SUFFIX_MANAGE_PARTICIPANT,
                                   "The URL suffix must be empty or start with a slash followed by at least one character.");
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
                                   sURLSuffixManageSMP,
                                   sURLSuffixManageParticipant,
                                   bClientCertificateRequired);
        aWPEC.postRedirectGetInternal (success ("The SML configuration '" +
                                                sDisplayName +
                                                "' was successfully edited."));
      }
      else
      {
        aSMLInfoMgr.createSMLInfo (sDisplayName,
                                   sDNSZoneLC,
                                   sManagementAddressURL,
                                   sURLSuffixManageSMP,
                                   sURLSuffixManageParticipant,
                                   bClientCertificateRequired);
        aWPEC.postRedirectGetInternal (success ("The new SML configuration '" +
                                                sDisplayName +
                                                "' was successfully created."));
      }
    }
  }

  @Override
  protected void showListOfExistingObjects (@NonNull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMLInfoManager aSMLInfoMgr = SMPMetaManager.getSMLInfoMgr ();

    aNodeList.addChild (info ("This page lets you create custom SML configurations that can be used for registration."));

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
                    isActionAllowed (aWPEC, EWebPageFormAction.DELETE, aCurObject) ? createDeleteLink (aWPEC,
                                                                                                       aCurObject,
                                                                                                       "Delete " +
                                                                                                                   aCurObject.getDisplayName ())
                                                                                   : createEmptyAction ());
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
