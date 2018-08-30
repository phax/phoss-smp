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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.collection.multimap.IMultiMapListBased;
import com.helger.collection.multimap.MultiHashMapArrayListBased;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.attr.StringMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTFromString;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.URLHelper;
import com.helger.html.hc.ext.HCA_MailTo;
import com.helger.html.hc.html.HC_Target;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCLI;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCEM;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.identifier.generic.process.IProcessIdentifier;
import com.helger.peppol.identifier.peppol.PeppolIdentifierHelper;
import com.helger.peppol.identifier.peppol.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;
import com.helger.peppol.smpserver.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageForm;
import com.helger.peppol.smpserver.ui.AppCommonUI;
import com.helger.peppol.smpserver.ui.secure.hc.HCSMPTransportProfileSelect;
import com.helger.peppol.smpserver.ui.secure.hc.HCServiceGroupSelect;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapQuestionBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapCheckBox;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.grid.BootstrapRow;
import com.helger.photon.bootstrap3.label.BootstrapLabel;
import com.helger.photon.bootstrap3.label.EBootstrapLabelType;
import com.helger.photon.bootstrap3.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.bootstrap3.uictrls.datetimepicker.BootstrapDateTimePicker;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldBoolean;
import com.helger.photon.core.form.RequestFieldDate;
import com.helger.photon.core.form.SessionBackedRequestField;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uicore.page.handler.IWebPageActionHandler;
import com.helger.photon.uictrls.autosize.HCTextAreaAutosize;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.famfam.EFamFamIcon;
import com.helger.security.certificate.CertificateHelper;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;

/**
 * Class to manage endpoints that belong to a service group. To use this page at
 * least one service group must exist.
 *
 * @author Philip Helger
 */
public final class PageSecureEndpoint extends AbstractSMPWebPageForm <ISMPServiceInformation>
{
  private static final String FIELD_SERVICE_GROUP_ID = "sgid";
  private static final String FIELD_DOCTYPE_ID_SCHEME = "doctypeidscheme";
  private static final String FIELD_DOCTYPE_ID_VALUE = "doctypeidvalue";
  private static final String FIELD_PROCESS_ID_SCHEME = "processidscheme";
  private static final String FIELD_PROCESS_ID_VALUE = "processidvalue";
  private static final String FIELD_TRANSPORT_PROFILE = "transportprofile";
  private static final String FIELD_ENDPOINT_REFERENCE = "endpointreference";
  private static final String FIELD_REQUIRES_BUSINESS_LEVEL_SIGNATURE = "requiresbusinesslevelsignature";
  private static final String FIELD_MINIMUM_AUTHENTICATION_LEVEL = "minimumauthenticationlevel";
  private static final String FIELD_NOT_BEFORE = "notbefore";
  private static final String FIELD_NOT_AFTER = "notafter";
  private static final String FIELD_CERTIFICATE = "certificate";
  private static final String FIELD_SERVICE_DESCRIPTION = "servicedescription";
  private static final String FIELD_TECHNICAL_CONTACT = "technicalcontact";
  private static final String FIELD_TECHNICAL_INFORMATION = "technicalinformation";
  private static final String FIELD_EXTENSION = "extension";

  private static final String REQUEST_ATTR_PROCESS = "$process";
  private static final String REQUEST_ATTR_ENDPOINT = "$endpoint";

  private static final String ACTION_DELETE_DOCUMENT_TYPE = "del.doctype";
  private static final String ACTION_DELETE_PROCESS = "del.process";

  private static final String FIELD_VIEW_TYPE = "smp.endpoint.view";
  private static final String VIEW_FLAT = "flat";
  private static final String VIEW_TREE = "tree";

  public PageSecureEndpoint (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Endpoints");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPServiceInformation, WebPageExecutionContext> ()
    {
      @Override
      protected void showDeleteQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                      @Nonnull final BootstrapForm aForm,
                                      @Nonnull final ISMPServiceInformation aSelectedObject)
      {
        final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_PROCESS);
        final ISMPEndpoint aSelectedEndpoint = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_ENDPOINT);

        aForm.addChild (new HCHiddenField (FIELD_SERVICE_GROUP_ID,
                                           aSelectedObject.getServiceGroup ()
                                                          .getParticpantIdentifier ()
                                                          .getURIEncoded ()));
        aForm.addChild (new HCHiddenField (FIELD_DOCTYPE_ID_SCHEME,
                                           aSelectedObject.getDocumentTypeIdentifier ().getScheme ()));
        aForm.addChild (new HCHiddenField (FIELD_DOCTYPE_ID_VALUE,
                                           aSelectedObject.getDocumentTypeIdentifier ().getValue ()));
        aForm.addChild (new HCHiddenField (FIELD_PROCESS_ID_SCHEME,
                                           aSelectedProcess.getProcessIdentifier ().getScheme ()));
        aForm.addChild (new HCHiddenField (FIELD_PROCESS_ID_VALUE,
                                           aSelectedProcess.getProcessIdentifier ().getValue ()));
        aForm.addChild (new HCHiddenField (FIELD_TRANSPORT_PROFILE, aSelectedEndpoint.getTransportProfile ()));

        aForm.addChild (new BootstrapQuestionBox ().addChild ("Are you sure you want to delete the endpoint for service group '" +
                                                              aSelectedObject.getServiceGroupID () +
                                                              "' and document type '" +
                                                              aSelectedObject.getDocumentTypeIdentifier ()
                                                                             .getURIEncoded () +
                                                              "'?"));
      }

      @Override
      protected void performDelete (@Nonnull final WebPageExecutionContext aWPEC,
                                    @Nonnull final ISMPServiceInformation aSelectedObject)
      {
        final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

        final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_PROCESS);
        final ISMPEndpoint aSelectedEndpoint = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_ENDPOINT);

        if (aSelectedProcess != null &&
            aSelectedEndpoint != null &&
            aSelectedProcess.deleteEndpoint (aSelectedEndpoint.getTransportProfile ()).isChanged () &&
            aServiceInfoMgr.mergeSMPServiceInformation (aSelectedObject).isSuccess ())
        {
          aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The selected endpoint was successfully deleted!"));
        }
        else
          aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Error deleting the selected endpoint!"));
      }
    });
    addCustomHandler (ACTION_DELETE_DOCUMENT_TYPE,
                      new IWebPageActionHandler <ISMPServiceInformation, WebPageExecutionContext> ()
                      {
                        public boolean isSelectedObjectRequired ()
                        {
                          return true;
                        }

                        public boolean handleAction (@Nonnull final WebPageExecutionContext aWPEC,
                                                     @Nonnull final ISMPServiceInformation aSelectedObject)
                        {
                          final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
                          if (aServiceInfoMgr.deleteSMPServiceInformation (aSelectedObject).isChanged ())
                            aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The selected document type '" +
                                                                                                aSelectedObject.getDocumentTypeIdentifier ()
                                                                                                               .getURIEncoded () +
                                                                                                "' was successfully deleted!"));
                          else
                            aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Error deleting the selected document type '" +
                                                                                              aSelectedObject.getDocumentTypeIdentifier ()
                                                                                                             .getURIEncoded () +
                                                                                              "'!"));
                          return false;
                        }
                      });
    addCustomHandler (ACTION_DELETE_PROCESS,
                      new IWebPageActionHandler <ISMPServiceInformation, WebPageExecutionContext> ()
                      {
                        public boolean isSelectedObjectRequired ()
                        {
                          return true;
                        }

                        public boolean handleAction (@Nonnull final WebPageExecutionContext aWPEC,
                                                     @Nonnull final ISMPServiceInformation aSelectedObject)
                        {
                          final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
                          final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

                          final String sProcessIDScheme = aWPEC.params ().getAsString (FIELD_PROCESS_ID_SCHEME);
                          final String sProcessIDValue = aWPEC.params ().getAsString (FIELD_PROCESS_ID_VALUE);
                          final IProcessIdentifier aProcessID = aIdentifierFactory.createProcessIdentifier (sProcessIDScheme,
                                                                                                            sProcessIDValue);
                          final ISMPProcess aProcess = aSelectedObject.getProcessOfID (aProcessID);
                          if (aProcess != null)
                            if (aServiceInfoMgr.deleteSMPProcess (aSelectedObject, aProcess).isChanged ())
                              aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The selected process '" +
                                                                                                  aProcess.getProcessIdentifier ()
                                                                                                          .getURIEncoded () +
                                                                                                  "' from document type '" +
                                                                                                  aSelectedObject.getDocumentTypeIdentifier ()
                                                                                                                 .getURIEncoded () +
                                                                                                  "' was successfully deleted!"));
                            else
                              aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Error deleting the process '" +
                                                                                                aProcess.getProcessIdentifier ()
                                                                                                        .getURIEncoded () +
                                                                                                "' from the selected document type '" +
                                                                                                aSelectedObject.getDocumentTypeIdentifier ()
                                                                                                               .getURIEncoded () +
                                                                                                "'!"));
                          return true;
                        }
                      });
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    if (aServiceGroupMgr.getSMPServiceGroupCount () == 0)
    {
      aNodeList.addChild (new BootstrapWarnBox ().addChild ("No service group is present! At least one service group must be present to create an endpoint for it."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new service group")
                                                .setOnClick (createCreateURL (aWPEC, CMenuSecure.MENU_SERVICE_GROUPS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }
    return super.isValidToDisplayPage (aWPEC);
  }

  @Override
  @Nullable
  protected ISMPServiceInformation getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                                      @Nullable final String sID)
  {
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

    final String sServiceGroupID = aWPEC.params ().getAsString (FIELD_SERVICE_GROUP_ID);
    final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
    final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);

    final String sDocTypeIDScheme = aWPEC.params ().getAsString (FIELD_DOCTYPE_ID_SCHEME);
    final String sDocTypeIDValue = aWPEC.params ().getAsString (FIELD_DOCTYPE_ID_VALUE);
    final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (sDocTypeIDScheme,
                                                                                                sDocTypeIDValue);
    return aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup, aDocTypeID);
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPServiceInformation aSelectedObject)
  {
    if (eFormAction == EWebPageFormAction.VIEW ||
        eFormAction == EWebPageFormAction.COPY ||
        eFormAction == EWebPageFormAction.EDIT ||
        eFormAction == EWebPageFormAction.DELETE)
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final String sProcessIDScheme = aWPEC.params ().getAsString (FIELD_PROCESS_ID_SCHEME);
      final String sProcessIDValue = aWPEC.params ().getAsString (FIELD_PROCESS_ID_VALUE);
      final IProcessIdentifier aProcessID = aIdentifierFactory.createProcessIdentifier (sProcessIDScheme,
                                                                                        sProcessIDValue);
      final ISMPProcess aProcess = aSelectedObject.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        final String sTransportProfile = aWPEC.params ().getAsString (FIELD_TRANSPORT_PROFILE);
        final ISMPEndpoint aEndpoint = aProcess.getEndpointOfTransportProfile (sTransportProfile);
        if (aEndpoint != null)
        {
          aWPEC.getRequestScope ().attrs ().putIn (REQUEST_ATTR_PROCESS, aProcess);
          aWPEC.getRequestScope ().attrs ().putIn (REQUEST_ATTR_ENDPOINT, aEndpoint);
          return true;
        }
      }
      return false;
    }
    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Nonnull
  private static StringMap _createParamMap (@Nonnull final ISMPServiceInformation aServiceInfo,
                                            @Nullable final ISMPProcess aProcess,
                                            @Nullable final ISMPEndpoint aEndpoint)
  {
    final StringMap ret = new StringMap ();
    ret.putIn (FIELD_SERVICE_GROUP_ID, aServiceInfo.getServiceGroup ().getParticpantIdentifier ().getURIEncoded ());
    ret.putIn (FIELD_DOCTYPE_ID_SCHEME, aServiceInfo.getDocumentTypeIdentifier ().getScheme ());
    ret.putIn (FIELD_DOCTYPE_ID_VALUE, aServiceInfo.getDocumentTypeIdentifier ().getValue ());
    if (aProcess != null)
    {
      ret.putIn (FIELD_PROCESS_ID_SCHEME, aProcess.getProcessIdentifier ().getScheme ());
      ret.putIn (FIELD_PROCESS_ID_VALUE, aProcess.getProcessIdentifier ().getValue ());
      if (aEndpoint != null)
      {
        ret.putIn (FIELD_TRANSPORT_PROFILE, aEndpoint.getTransportProfile ());
      }
    }
    return ret;
  }

  @Override
  @Nonnull
  protected BootstrapButtonToolbar createViewToolbar (@Nonnull final WebPageExecutionContext aWPEC,
                                                      final boolean bCanGoBack,
                                                      @Nonnull final ISMPServiceInformation aSelectedObject)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_PROCESS);
    final ISMPEndpoint aSelectedEndpoint = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_ENDPOINT);

    final BootstrapButtonToolbar aToolbar = createNewViewToolbar (aWPEC);
    if (bCanGoBack)
    {
      // Back to list
      aToolbar.addButtonBack (aDisplayLocale);
    }
    if (isActionAllowed (aWPEC, EWebPageFormAction.EDIT, aSelectedObject))
    {
      // Edit object
      aToolbar.addButtonEdit (aDisplayLocale,
                              createEditURL (aWPEC, aSelectedObject).addAll (_createParamMap (aSelectedObject,
                                                                                              aSelectedProcess,
                                                                                              aSelectedEndpoint)));
    }

    // Callback
    modifyViewToolbar (aWPEC, aSelectedObject, aToolbar);
    return aToolbar;
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPServiceInformation aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_PROCESS);
    final ISMPEndpoint aSelectedEndpoint = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_ENDPOINT);
    final LocalDateTime aNowLDT = PDTFactory.getCurrentLocalDateTime ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of endpoint"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service group")
                                                 .setCtrl (new HCA (createViewURL (aWPEC,
                                                                                   CMenuSecure.MENU_SERVICE_GROUPS,
                                                                                   aSelectedObject.getServiceGroup ())).addChild (aSelectedObject.getServiceGroupID ())));

    // Document type identifier
    {
      final IDocumentTypeIdentifier aDocumentTypeID = aSelectedObject.getDocumentTypeIdentifier ();
      final HCNodeList aCtrl = new HCNodeList ();
      aCtrl.addChild (new HCDiv ().addChild (AppCommonUI.getDocumentTypeID (aDocumentTypeID)));
      try
      {
        final IPeppolDocumentTypeIdentifierParts aParts = PeppolIdentifierHelper.getDocumentTypeIdentifierParts (aDocumentTypeID);
        aCtrl.addChild (AppCommonUI.getDocumentTypeIDDetails (aParts));
      }
      catch (final IllegalArgumentException ex)
      {
        if (false)
          aCtrl.addChild (new BootstrapErrorBox ().addChild ("Failed to parse document type identifier: " +
                                                             ex.getMessage ()));
      }
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Document type ID").setCtrl (aCtrl));
    }

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Process ID")
                                                 .setCtrl (AppCommonUI.getProcessID (aSelectedProcess.getProcessIdentifier ())));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Transport profile")
                                                 .setCtrl (new HCA (createViewURL (aWPEC,
                                                                                   CMenuSecure.MENU_TRANSPORT_PROFILES,
                                                                                   aSelectedEndpoint.getTransportProfile ())).addChild (aSelectedEndpoint.getTransportProfile ())));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Endpoint reference")
                                                 .setCtrl (StringHelper.hasText (aSelectedEndpoint.getEndpointReference ()) ? HCA.createLinkedWebsite (aSelectedEndpoint.getEndpointReference (),
                                                                                                                                                       HC_Target.BLANK)
                                                                                                                            : new HCEM ().addChild ("none")));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Requires business level signature")
                                                 .setCtrl (EPhotonCoreText.getYesOrNo (aSelectedEndpoint.isRequireBusinessLevelSignature (),
                                                                                       aDisplayLocale)));

    if (StringHelper.hasText (aSelectedEndpoint.getMinimumAuthenticationLevel ()))
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Minimum authentication level")
                                                   .setCtrl (aSelectedEndpoint.getMinimumAuthenticationLevel ()));

    if (aSelectedEndpoint.getServiceActivationDateTime () != null)
    {
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not before")
                                                   .setCtrl (PDTToString.getAsString (aSelectedEndpoint.getServiceActivationDateTime (),
                                                                                      aDisplayLocale)));
    }
    if (aSelectedEndpoint.getServiceExpirationDateTime () != null)
    {
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not after")
                                                   .setCtrl (PDTToString.getAsString (aSelectedEndpoint.getServiceExpirationDateTime (),
                                                                                      aDisplayLocale)));
    }

    X509Certificate aEndpointCert = null;
    try
    {
      aEndpointCert = CertificateHelper.convertStringToCertficate (aSelectedEndpoint.getCertificate ());
    }
    catch (final CertificateException ex)
    {
      // Ignore
    }
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Certificate")
                                                 .setCtrl (aEndpointCert == null ? new HCStrong ().addChild ("!!!FAILED TO INTERPRETE!!!")
                                                                                 : AppCommonUI.createCertificateDetailsTable (aEndpointCert,
                                                                                                                              aNowLDT,
                                                                                                                              aDisplayLocale)
                                                                                              .getAsResponsiveTable ()));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service description")
                                                 .setCtrl (aSelectedEndpoint.getServiceDescription ()));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Technical contact")
                                                 .setCtrl (HCA_MailTo.createLinkedEmail (aSelectedEndpoint.getTechnicalContactUrl ())));

    if (aSelectedEndpoint.hasTechnicalInformationUrl ())
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Technical information")
                                                   .setCtrl (HCA.createLinkedWebsite (aSelectedEndpoint.getTechnicalInformationUrl (),
                                                                                      HC_Target.BLANK)));
    if (aSelectedEndpoint.hasExtension ())
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Extension")
                                                   .setCtrl (AppCommonUI.getExtensionDisplay (aSelectedEndpoint)));

    aNodeList.addChild (aForm);

  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPServiceInformation aSelectedObject,
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_PROCESS);
    final ISMPEndpoint aSelectedEndpoint = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_ENDPOINT);
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();

    final String sServiceGroupID = bEdit ? aSelectedObject.getServiceGroupID ()
                                         : aWPEC.params ().getAsString (FIELD_SERVICE_GROUP_ID);
    ISMPServiceGroup aServiceGroup = null;

    final String sDocTypeIDScheme = bEdit ? aSelectedObject.getDocumentTypeIdentifier ().getScheme ()
                                          : aWPEC.params ().getAsString (FIELD_DOCTYPE_ID_SCHEME);
    final String sDocTypeIDValue = bEdit ? aSelectedObject.getDocumentTypeIdentifier ().getValue ()
                                         : aWPEC.params ().getAsString (FIELD_DOCTYPE_ID_VALUE);
    IDocumentTypeIdentifier aDocTypeID = null;
    final String sProcessIDScheme = bEdit ? aSelectedProcess.getProcessIdentifier ().getScheme ()
                                          : aWPEC.params ().getAsString (FIELD_PROCESS_ID_SCHEME);
    final String sProcessIDValue = bEdit ? aSelectedProcess.getProcessIdentifier ().getValue ()
                                         : aWPEC.params ().getAsString (FIELD_PROCESS_ID_VALUE);
    IProcessIdentifier aProcessID = null;

    final String sTransportProfileID = bEdit ? aSelectedEndpoint.getTransportProfile ()
                                             : aWPEC.params ().getAsString (FIELD_TRANSPORT_PROFILE);
    final ISMPTransportProfile aTransportProfile = aTransportProfileMgr.getSMPTransportProfileOfID (sTransportProfileID);
    final String sEndpointReference = aWPEC.params ().getAsString (FIELD_ENDPOINT_REFERENCE);
    final boolean bRequireBusinessLevelSignature = aWPEC.params ()
                                                        .getAsBoolean (FIELD_REQUIRES_BUSINESS_LEVEL_SIGNATURE);
    final String sMinimumAuthenticationLevel = aWPEC.params ().getAsString (FIELD_MINIMUM_AUTHENTICATION_LEVEL);
    final String sNotBefore = aWPEC.params ().getAsString (FIELD_NOT_BEFORE);
    final LocalDate aNotBeforeDate = PDTFromString.getLocalDateFromString (sNotBefore, aDisplayLocale);
    final String sNotAfter = aWPEC.params ().getAsString (FIELD_NOT_AFTER);
    final LocalDate aNotAfterDate = PDTFromString.getLocalDateFromString (sNotAfter, aDisplayLocale);
    final String sCertificate = aWPEC.params ().getAsString (FIELD_CERTIFICATE);
    final String sServiceDescription = aWPEC.params ().getAsString (FIELD_SERVICE_DESCRIPTION);
    final String sTechnicalContact = aWPEC.params ().getAsString (FIELD_TECHNICAL_CONTACT);
    final String sTechnicalInformation = aWPEC.params ().getAsString (FIELD_TECHNICAL_INFORMATION);
    final String sExtension = aWPEC.params ().getAsString (FIELD_EXTENSION);

    // validations
    if (StringHelper.hasNoText (sServiceGroupID))
      aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "A service group must be selected!");
    else
    {
      aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID));
      if (aServiceGroup == null)
        aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "The provided service group does not exist!");
    }

    if (aIdentifierFactory.isDocumentTypeIdentifierSchemeMandatory () && StringHelper.hasNoText (sDocTypeIDScheme))
      aFormErrors.addFieldError (FIELD_DOCTYPE_ID_SCHEME, "Document type ID scheme must not be empty!");
    else
      if (StringHelper.hasNoText (sDocTypeIDValue))
        aFormErrors.addFieldError (FIELD_DOCTYPE_ID_VALUE, "Document type ID value must not be empty!");
      else
      {
        aDocTypeID = aIdentifierFactory.createDocumentTypeIdentifier (sDocTypeIDScheme, sDocTypeIDValue);
        if (aDocTypeID == null)
          aFormErrors.addFieldError (FIELD_DOCTYPE_ID_VALUE, "The provided document type ID has an invalid syntax!");
        else
        {
          if (aServiceGroup != null)
            if (aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup, aDocTypeID) != null)
              aFormErrors.addFieldError (FIELD_DOCTYPE_ID_VALUE,
                                         "At least one redirect is registered for this document type. Delete the redirect before you can create an endpoint.");
        }
      }

    if (aIdentifierFactory.isProcessIdentifierSchemeMandatory () && StringHelper.hasNoText (sProcessIDScheme))
      aFormErrors.addFieldError (FIELD_PROCESS_ID_SCHEME, "Process ID scheme must not be empty!");
    else
      if (StringHelper.hasNoText (sProcessIDValue))
        aFormErrors.addFieldError (FIELD_PROCESS_ID_SCHEME, "Process ID value must not be empty!");
      else
      {
        aProcessID = aIdentifierFactory.createProcessIdentifier (sProcessIDScheme, sProcessIDValue);
        if (aProcessID == null)
          aFormErrors.addFieldError (FIELD_PROCESS_ID_VALUE, "The provided process ID has an invalid syntax!");
      }

    if (StringHelper.hasNoText (sTransportProfileID))
      aFormErrors.addFieldError (FIELD_TRANSPORT_PROFILE, "Transport Profile must not be empty!");
    else
      if (aTransportProfile == null)
        aFormErrors.addFieldError (FIELD_TRANSPORT_PROFILE,
                                   "Transport Profile of type '" + sTransportProfileID + "' does not exist!");
    if (!bEdit &&
        aServiceGroup != null &&
        aDocTypeID != null &&
        aProcessID != null &&
        aTransportProfile != null &&
        aServiceInfoMgr.findServiceInformation (aServiceGroup, aDocTypeID, aProcessID, aTransportProfile) != null)
    {
      final String sMsg = "Another endpoint for the provided service group, document type, process and transport profile is already present. Some of the identifiers may be treated case insensitive!";
      aFormErrors.addFieldError (FIELD_DOCTYPE_ID_VALUE, sMsg);
      aFormErrors.addFieldError (FIELD_PROCESS_ID_VALUE, sMsg);
      aFormErrors.addFieldError (FIELD_TRANSPORT_PROFILE, sMsg);
    }

    if (StringHelper.hasNoText (sEndpointReference))
    {
      if (false)
        aFormErrors.addFieldError (FIELD_ENDPOINT_REFERENCE, "Endpoint Reference must not be empty!");
    }
    else
      if (URLHelper.getAsURL (sEndpointReference) == null)
        aFormErrors.addFieldError (FIELD_ENDPOINT_REFERENCE, "The Endpoint Reference is not a valid URL!");

    if (aNotBeforeDate != null && aNotAfterDate != null)
      if (aNotBeforeDate.isAfter (aNotAfterDate))
        aFormErrors.addFieldError (FIELD_NOT_BEFORE, "Not Before Date must not be after Not After Date!");

    if (StringHelper.hasNoText (sCertificate))
      aFormErrors.addFieldError (FIELD_CERTIFICATE, "Certificate must not be empty!");
    else
    {
      X509Certificate aCert = null;
      try
      {
        aCert = CertificateHelper.convertStringToCertficate (sCertificate);
      }
      catch (final CertificateException ex)
      {
        // Fall through
      }
      if (aCert == null)
        aFormErrors.addFieldError (FIELD_CERTIFICATE,
                                   "The provided certificate string is not a valid X509 certificate!");
    }

    if (StringHelper.hasNoText (sServiceDescription))
      aFormErrors.addFieldError (FIELD_SERVICE_DESCRIPTION, "Service Description must not be empty!");

    if (StringHelper.hasNoText (sTechnicalContact))
      aFormErrors.addFieldError (FIELD_TECHNICAL_CONTACT, "Technical Contact must not be empty!");

    if (StringHelper.hasText (sExtension))
    {
      final IMicroDocument aDoc = MicroReader.readMicroXML (sExtension);
      if (aDoc == null)
        aFormErrors.addFieldError (FIELD_EXTENSION, "The extension must be XML content.");
    }

    if (aFormErrors.isEmpty ())
    {
      ISMPServiceInformation aServiceInfo = aServiceInfoMgr.getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                                   aDocTypeID);
      if (aServiceInfo == null)
        aServiceInfo = new SMPServiceInformation (aServiceGroup, aDocTypeID, null, null);

      ISMPProcess aProcess = aServiceInfo.getProcessOfID (aProcessID);
      if (aProcess == null)
      {
        aProcess = new SMPProcess (aProcessID, null, null);
        aServiceInfo.addProcess ((SMPProcess) aProcess);
      }

      aProcess.setEndpoint (new SMPEndpoint (sTransportProfileID,
                                             sEndpointReference,
                                             bRequireBusinessLevelSignature,
                                             sMinimumAuthenticationLevel,
                                             aNotBeforeDate == null ? null : aNotBeforeDate.atStartOfDay (),
                                             aNotAfterDate == null ? null : aNotAfterDate.atStartOfDay (),
                                             sCertificate,
                                             sServiceDescription,
                                             sTechnicalContact,
                                             sTechnicalInformation,
                                             sExtension));

      if (aServiceInfoMgr.mergeSMPServiceInformation (aServiceInfo).isSuccess ())
      {
        if (bEdit)
        {
          aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("Successfully edited the endpoint for service group '" +
                                                                              aServiceGroup.getParticpantIdentifier ()
                                                                                           .getURIEncoded () +
                                                                              "'."));
        }
        else
        {
          aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("Successfully created a new endpoint for service group '" +
                                                                              aServiceGroup.getParticpantIdentifier ()
                                                                                           .getURIEncoded () +
                                                                              "'."));
        }
      }
      else
      {
        if (bEdit)
        {
          aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Error editing the endpoint for service group '" +
                                                                            aServiceGroup.getParticpantIdentifier ()
                                                                                         .getURIEncoded () +
                                                                            "'."));
        }
        else
        {
          aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Error creating a new endpoint for service group '" +
                                                                            aServiceGroup.getParticpantIdentifier ()
                                                                                         .getURIEncoded () +
                                                                            "'."));
        }
      }
    }
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPServiceInformation aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrorList aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_PROCESS);
    final ISMPEndpoint aSelectedEndpoint = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_ENDPOINT);
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();

    aForm.addChild (getUIHandler ().createActionHeader (bEdit ? "Edit endpoint" : "Create new endpoint"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service group")
                                                 .setCtrl (new HCServiceGroupSelect (new RequestField (FIELD_SERVICE_GROUP_ID,
                                                                                                       aSelectedObject != null ? aSelectedObject.getServiceGroupID ()
                                                                                                                               : null),
                                                                                     aDisplayLocale).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_GROUP_ID)));

    {
      final BootstrapRow aRow = new BootstrapRow ();
      aRow.createColumn (GS_IDENTIFIER_SCHEME)
          .addChild (new HCEdit (new RequestField (FIELD_DOCTYPE_ID_SCHEME,
                                                   aSelectedObject != null ? aSelectedObject.getDocumentTypeIdentifier ()
                                                                                            .getScheme ()
                                                                           : aIdentifierFactory.getDefaultDocumentTypeIdentifierScheme ())).setPlaceholder ("Identifier scheme")
                                                                                                                                           .setReadOnly (bEdit));
      aRow.createColumn (GS_IDENTIFIER_VALUE)
          .addChild (new HCEdit (new RequestField (FIELD_DOCTYPE_ID_VALUE,
                                                   aSelectedObject != null ? aSelectedObject.getDocumentTypeIdentifier ()
                                                                                            .getValue ()
                                                                           : null)).setPlaceholder ("Identifier value")
                                                                                   .setReadOnly (bEdit));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Document type ID")
                                                   .setCtrl (aRow)
                                                   .setErrorList (aFormErrors.getListOfFields (FIELD_DOCTYPE_ID_SCHEME,
                                                                                               FIELD_DOCTYPE_ID_VALUE)));
    }

    {
      final BootstrapRow aRow = new BootstrapRow ();
      aRow.createColumn (GS_IDENTIFIER_SCHEME)
          .addChild (new HCEdit (new RequestField (FIELD_PROCESS_ID_SCHEME,
                                                   aSelectedProcess != null ? aSelectedProcess.getProcessIdentifier ()
                                                                                              .getScheme ()
                                                                            : aIdentifierFactory.getDefaultProcessIdentifierScheme ())).setPlaceholder ("Identifier scheme")
                                                                                                                                       .setReadOnly (bEdit));
      aRow.createColumn (GS_IDENTIFIER_VALUE)
          .addChild (new HCEdit (new RequestField (FIELD_PROCESS_ID_VALUE,
                                                   aSelectedProcess != null ? aSelectedProcess.getProcessIdentifier ()
                                                                                              .getValue ()
                                                                            : null)).setPlaceholder ("Identifier value")
                                                                                    .setReadOnly (bEdit));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Process ID")
                                                   .setCtrl (aRow)
                                                   .setErrorList (aFormErrors.getListOfFields (FIELD_PROCESS_ID_SCHEME,
                                                                                               FIELD_PROCESS_ID_VALUE)));
    }

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Transport Profile")
                                                 .setCtrl (new HCSMPTransportProfileSelect (new RequestField (FIELD_TRANSPORT_PROFILE,
                                                                                                              aSelectedEndpoint != null ? aSelectedEndpoint.getTransportProfile ()
                                                                                                                                        : ESMPTransportProfile.TRANSPORT_PROFILE_AS2.getID ())).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_TRANSPORT_PROFILE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Endpoint Reference")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_ENDPOINT_REFERENCE,
                                                                                         aSelectedEndpoint != null ? aSelectedEndpoint.getEndpointReference ()
                                                                                                                   : null)))
                                                 .setHelpText ("The URL where messsages of this type should be targeted to.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_ENDPOINT_REFERENCE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Requires Business Level Signature")
                                                 .setCtrl (new BootstrapCheckBox (new RequestFieldBoolean (FIELD_REQUIRES_BUSINESS_LEVEL_SIGNATURE,
                                                                                                           aSelectedEndpoint != null ? aSelectedEndpoint.isRequireBusinessLevelSignature ()
                                                                                                                                     : false)))
                                                 .setHelpText ("Check the box if the recipient requires business-level signatures for\r\n" +
                                                               "the message, meaning a signature applied to the business message\r\n" +
                                                               "before the message is put on the transport. This is independent of\r\n" +
                                                               "the transport-level signatures that a specific transport profile, such\r\n" +
                                                               "as the START profile, might mandate. This flag does not indicate\r\n" +
                                                               "which type of business-level signature might be required. Setting or\r\n" +
                                                               "consuming business-level signatures would typically be the\r\n" +
                                                               "responsibility of the final senders and receivers of messages, rather\r\n" +
                                                               "than a set of APs.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_REQUIRES_BUSINESS_LEVEL_SIGNATURE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Minimum Authentication Level")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_MINIMUM_AUTHENTICATION_LEVEL,
                                                                                         aSelectedEndpoint != null ? aSelectedEndpoint.getMinimumAuthenticationLevel ()
                                                                                                                   : null)))
                                                 .setHelpText ("Indicates the minimum authentication level that recipient requires.\r\n" +
                                                               "The specific semantics of this field is defined in a specific instance\r\n" +
                                                               "of the BUSDOX infrastructure. It could for example reflect the\r\n" +
                                                               "value of the \"urn:eu:busdox:attribute:assurance-level\" SAML\r\n" +
                                                               "attribute defined in the START specification.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_MINIMUM_AUTHENTICATION_LEVEL)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not before")
                                                 .setCtrl (new BootstrapDateTimePicker (new RequestFieldDate (FIELD_NOT_BEFORE,
                                                                                                              aSelectedEndpoint != null ? aSelectedEndpoint.getServiceActivationDate ()
                                                                                                                                        : null,
                                                                                                              aDisplayLocale)).setEndDate (null))
                                                 .setHelpText ("Activation date of the service. Senders should ignore services that " +
                                                               "are not yet activated.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_NOT_BEFORE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not after")
                                                 .setCtrl (new BootstrapDateTimePicker (new RequestFieldDate (FIELD_NOT_AFTER,
                                                                                                              aSelectedEndpoint != null ? aSelectedEndpoint.getServiceExpirationDate ()
                                                                                                                                        : null,
                                                                                                              aDisplayLocale)).setEndDate (null))
                                                 .setHelpText ("Expiration date of the service. Senders should ignore services that " +
                                                               "are expired.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_NOT_AFTER)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Certificate")
                                                 .setCtrl (new HCTextAreaAutosize (new RequestField (FIELD_CERTIFICATE,
                                                                                                     aSelectedEndpoint != null ? aSelectedEndpoint.getCertificate ()
                                                                                                                               : null)))
                                                 .setHelpText ("Holds the complete signing certificate of the recipient AP, as a " +
                                                               "PEM base 64 encoded X509 DER formatted value.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_CERTIFICATE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service Description")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_SERVICE_DESCRIPTION,
                                                                                         aSelectedEndpoint != null ? aSelectedEndpoint.getServiceDescription ()
                                                                                                                   : null)))
                                                 .setHelpText ("A human readable description of the service.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_DESCRIPTION)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Technical Contact")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_TECHNICAL_CONTACT,
                                                                                         aSelectedEndpoint != null ? aSelectedEndpoint.getTechnicalContactUrl ()
                                                                                                                   : null)))
                                                 .setHelpText ("Represents a link to human readable contact information. This " +
                                                               "might also be an email address.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_TECHNICAL_CONTACT)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Technical Information")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_TECHNICAL_INFORMATION,
                                                                                         aSelectedEndpoint != null ? aSelectedEndpoint.getTechnicalInformationUrl ()
                                                                                                                   : null)))
                                                 .setHelpText ("A URL to human readable documentation of the service format. " +
                                                               "This could for example be a web site containing links to XML " +
                                                               "Schemas, WSDLs, Schematrons and other relevant resources.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_TECHNICAL_INFORMATION)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Extension")
                                                 .setCtrl (new HCTextAreaAutosize (new RequestField (FIELD_EXTENSION,
                                                                                                     aSelectedEndpoint != null ? aSelectedEndpoint.getFirstExtensionXML ()
                                                                                                                               : null)))
                                                 .setHelpText ("Optional extension to the endpoint. If present it must be valid XML content!")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_EXTENSION)));
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

    // Default view is now tree view
    final SessionBackedRequestField aSBRF = new SessionBackedRequestField (FIELD_VIEW_TYPE, VIEW_TREE);
    final boolean bTreeView = VIEW_TREE.equals (aSBRF.getRequestValue ());

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new Endpoint", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
    if (bTreeView)
      aToolbar.addButton ("Flat view", aWPEC.getSelfHref ().add (FIELD_VIEW_TYPE, VIEW_FLAT), EDefaultIcon.MAGNIFIER);
    else
      aToolbar.addButton ("Tree view", aWPEC.getSelfHref ().add (FIELD_VIEW_TYPE, VIEW_TREE), EDefaultIcon.MAGNIFIER);
    aNodeList.addChild (aToolbar);

    if (bTreeView)
    {
      // Create sorted list of service groups
      final IMultiMapListBased <ISMPServiceGroup, ISMPServiceInformation> aMap = new MultiHashMapArrayListBased <> ();
      aServiceInfoMgr.getAllSMPServiceInformation ().forEach (x -> aMap.putSingle (x.getServiceGroup (), x));

      final HCUL aULSG = new HCUL ();
      final ICommonsList <ISMPServiceGroup> aServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ()
                                                                             .getSortedInline (ISMPServiceGroup.comparator ());
      for (final ISMPServiceGroup aServiceGroup : aServiceGroups)
      {
        // Print service group
        final HCLI aLISG = aULSG.addAndReturnItem (new HCA (createViewURL (aWPEC,
                                                                           CMenuSecure.MENU_SERVICE_GROUPS,
                                                                           aServiceGroup)).addChild (aServiceGroup.getParticpantIdentifier ()
                                                                                                                  .getURIEncoded ()));
        final HCUL aULDT = new HCUL ();

        final ICommonsList <ISMPServiceInformation> aServiceInfos = aMap.get (aServiceGroup);
        if (aServiceInfos != null)
          for (final ISMPServiceInformation aServiceInfo : aServiceInfos.getSortedInline (ISMPServiceInformation.comparator ()))
          {
            final HCUL aULP = new HCUL ();
            final ICommonsList <ISMPProcess> aProcesses = aServiceInfo.getAllProcesses ()
                                                                      .getSortedInline (ISMPProcess.comparator ());
            for (final ISMPProcess aProcess : aProcesses)
            {
              final BootstrapTable aEPTable = new BootstrapTable (HCCol.perc (40),
                                                                  HCCol.perc (40),
                                                                  HCCol.perc (20)).setBordered (true);
              final ICommonsList <ISMPEndpoint> aEndpoints = aProcess.getAllEndpoints ()
                                                                     .getSortedInline (ISMPEndpoint.comparator ());
              for (final ISMPEndpoint aEndpoint : aEndpoints)
              {
                final StringMap aParams = _createParamMap (aServiceInfo, aProcess, aEndpoint);

                final HCRow aBodyRow = aEPTable.addBodyRow ();
                aBodyRow.addCell (new HCA (createViewURL (aWPEC,
                                                          aServiceInfo,
                                                          aParams)).addChild (aEndpoint.getTransportProfile ()));

                aBodyRow.addCell (aEndpoint.getEndpointReference ());

                final ISimpleURL aEditURL = createEditURL (aWPEC, aServiceInfo).addAll (aParams);
                final ISimpleURL aCopyURL = createCopyURL (aWPEC, aServiceInfo).addAll (aParams);
                final ISimpleURL aDeleteURL = createDeleteURL (aWPEC, aServiceInfo).addAll (aParams);
                aBodyRow.addCell (new HCTextNode (" "),
                                  new HCA (aEditURL).setTitle ("Edit endpoint")
                                                    .addChild (EDefaultIcon.EDIT.getAsNode ()),
                                  new HCTextNode (" "),
                                  new HCA (aCopyURL).setTitle ("Copy endpoint")
                                                    .addChild (EDefaultIcon.COPY.getAsNode ()),
                                  new HCTextNode (" "),
                                  new HCA (aDeleteURL).setTitle ("Delete endpoint")
                                                      .addChild (EDefaultIcon.DELETE.getAsNode ()));
              }

              // Show process + endpoints
              final HCLI aLI = aULP.addItem ();
              final HCDiv aDiv = new HCDiv ().addChild (AppCommonUI.getProcessID (aProcess.getProcessIdentifier ()));
              aLI.addChild (aDiv);
              if (aEndpoints.isEmpty ())
              {
                aDiv.addChild (" ")
                    .addChild (new HCA (aWPEC.getSelfHref ()
                                             .addAll (_createParamMap (aServiceInfo, aProcess, (ISMPEndpoint) null))
                                             .add (CPageParam.PARAM_ACTION, ACTION_DELETE_PROCESS))
                                                                                                   .setTitle ("Delete process")
                                                                                                   .addChild (EDefaultIcon.DELETE.getAsNode ()));
              }
              else
                aLI.addChild (aEPTable);
            }

            // Show document types + children
            final HCLI aLI = aULDT.addItem ();
            final HCDiv aDiv = new HCDiv ().addChild (AppCommonUI.getDocumentTypeID (aServiceInfo.getDocumentTypeIdentifier ()))
                                           .addChild (" ")
                                           .addChild (new HCA (LinkHelper.getURLWithServerAndContext (aServiceInfo.getServiceGroup ()
                                                                                                                  .getParticpantIdentifier ()
                                                                                                                  .getURIPercentEncoded () +
                                                                                                      "/services/" +
                                                                                                      aServiceInfo.getDocumentTypeIdentifier ()
                                                                                                                  .getURIPercentEncoded ())).setTitle ("Perform SMP query on document type ")
                                                                                                                                            .setTargetBlank ()
                                                                                                                                            .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()));
            aLI.addChild (aDiv);
            if (aProcesses.isEmpty ())
            {
              aDiv.addChild (" ")
                  .addChild (new HCA (aWPEC.getSelfHref ()
                                           .addAll (_createParamMap (aServiceInfo,
                                                                     (ISMPProcess) null,
                                                                     (ISMPEndpoint) null))
                                           .add (CPageParam.PARAM_ACTION, ACTION_DELETE_DOCUMENT_TYPE))
                                                                                                       .setTitle ("Delete document type")
                                                                                                       .addChild (EDefaultIcon.DELETE.getAsNode ()));
            }
            else
              aLI.addChild (aULP);
          }
        if (aServiceInfos == null || aServiceInfos.isEmpty () || aULDT.hasNoChildren ())
          aLISG.addChild (new BootstrapLabel (EBootstrapLabelType.INFO).addChild ("This service group has no assigned endpoints!"));
        else
          aLISG.addChild (aULDT);
      }
      aNodeList.addChild (aULSG);
    }
    else
    {
      final HCTable aTable = new HCTable (new DTCol ("Service group").setInitialSorting (ESortOrder.ASCENDING)
                                                                     .setDataSort (0, 1, 2, 3),
                                          new DTCol ("Document type ID").setDataSort (1, 0, 2, 3),
                                          new DTCol ("Process ID").setDataSort (2, 0, 1, 3),
                                          new DTCol ("Transport profile").setDataSort (3, 0, 1, 2),
                                          new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
      for (final ISMPServiceInformation aServiceInfo : aServiceInfoMgr.getAllSMPServiceInformation ())
        for (final ISMPProcess aProcess : aServiceInfo.getAllProcesses ())
          for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
          {
            final StringMap aParams = _createParamMap (aServiceInfo, aProcess, aEndpoint);

            final HCRow aRow = aTable.addBodyRow ();
            aRow.addCell (new HCA (createViewURL (aWPEC,
                                                  aServiceInfo,
                                                  aParams)).addChild (aServiceInfo.getServiceGroupID ()));
            aRow.addCell (AppCommonUI.getDocumentTypeID (aServiceInfo.getDocumentTypeIdentifier ()));
            aRow.addCell (AppCommonUI.getProcessID (aProcess.getProcessIdentifier ()));
            aRow.addCell (new HCA (createViewURL (aWPEC,
                                                  CMenuSecure.MENU_TRANSPORT_PROFILES,
                                                  aEndpoint.getTransportProfile ())).addChild (aEndpoint.getTransportProfile ()));

            final ISimpleURL aEditURL = createEditURL (aWPEC, aServiceInfo).addAll (aParams);
            final ISimpleURL aCopyURL = createCopyURL (aWPEC, aServiceInfo).addAll (aParams);
            final ISimpleURL aDeleteURL = createDeleteURL (aWPEC, aServiceInfo).addAll (aParams);
            aRow.addCell (new HCA (aEditURL).setTitle ("Edit endpoint").addChild (EDefaultIcon.EDIT.getAsNode ()),
                          new HCTextNode (" "),
                          new HCA (aCopyURL).setTitle ("Copy endpoint").addChild (EDefaultIcon.COPY.getAsNode ()),
                          new HCTextNode (" "),
                          new HCA (aDeleteURL).setTitle ("Delete endpoint").addChild (EDefaultIcon.DELETE.getAsNode ()),
                          new HCTextNode (" "),
                          new HCA (LinkHelper.getURLWithServerAndContext (aServiceInfo.getServiceGroup ()
                                                                                      .getParticpantIdentifier ()
                                                                                      .getURIPercentEncoded () +
                                                                          "/services/" +
                                                                          aServiceInfo.getDocumentTypeIdentifier ()
                                                                                      .getURIPercentEncoded ())).setTitle ("Perform SMP query on endpoint ")
                                                                                                                .setTargetBlank ()
                                                                                                                .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()));
          }

      final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
      aNodeList.addChild (aTable).addChild (aDataTables);
    }
  }
}
