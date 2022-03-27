/*
 * Copyright (C) 2014-2022 Philip Helger and contributors
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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.attr.StringMap;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTFromString;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.URLHelper;
import com.helger.html.hc.ext.HCA_MailTo;
import com.helger.html.hc.html.HC_Target;
import com.helger.html.hc.html.forms.HCCheckBox;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.forms.HCTextArea;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.IProcessIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.peppolid.peppol.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.peppolid.peppol.doctype.PeppolDocumentTypeIdentifierParts;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.SMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.SMPServiceInformation;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.nicename.NiceNameUI;
import com.helger.phoss.smp.ui.AbstractSMPWebPageForm;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.secure.hc.HCSMPTransportProfileSelect;
import com.helger.phoss.smp.ui.secure.hc.HCServiceGroupSelect;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.grid.BootstrapRow;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap4.uictrls.datetimepicker.BootstrapDateTimePicker;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldBoolean;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EShowList;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uicore.page.handler.IWebPageActionHandler;
import com.helger.security.certificate.CertificateHelper;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;

/**
 * Class to manage endpoints that belong to a service group. To use this page at
 * least one service group must exist.
 *
 * @author Philip Helger
 */
public abstract class AbstractPageSecureEndpoint extends AbstractSMPWebPageForm <ISMPServiceInformation>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (AbstractPageSecureEndpoint.class);

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

  protected static final String ACTION_DELETE_DOCUMENT_TYPE = "del.doctype";
  protected static final String ACTION_DELETE_PROCESS = "del.process";

  public AbstractPageSecureEndpoint (@Nonnull @Nonempty final String sID, @Nonnull final String sName)
  {
    super (sID, sName);
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPServiceInformation, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final BootstrapForm aForm,
                                @Nullable final ISMPServiceInformation aSelectedObject)
      {
        final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_PROCESS);
        final ISMPEndpoint aSelectedEndpoint = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_ENDPOINT);

        aForm.addChild (new HCHiddenField (FIELD_SERVICE_GROUP_ID,
                                           aSelectedObject.getServiceGroup ()
                                                          .getParticipantIdentifier ()
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

        aForm.addChild (question ("Are you sure you want to delete the endpoint for service group '" +
                                  aSelectedObject.getServiceGroupID () +
                                  "' and document type '" +
                                  aSelectedObject.getDocumentTypeIdentifier ().getURIEncoded () +
                                  "'?"));
      }

      @Override
      protected void performAction (@Nonnull final WebPageExecutionContext aWPEC,
                                    @Nullable final ISMPServiceInformation aSelectedObject)
      {
        final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

        final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_PROCESS);
        final ISMPEndpoint aSelectedEndpoint = aWPEC.getRequestScope ().attrs ().getCastedValue (REQUEST_ATTR_ENDPOINT);

        if (aSelectedProcess != null &&
            aSelectedEndpoint != null &&
            aSelectedProcess.deleteEndpoint (aSelectedEndpoint.getTransportProfile ()).isChanged () &&
            aServiceInfoMgr.mergeSMPServiceInformation (aSelectedObject).isSuccess ())
        {
          aWPEC.postRedirectGetInternal (success ("The selected endpoint was successfully deleted!"));
        }
        else
          aWPEC.postRedirectGetInternal (error ("Error deleting the selected endpoint!"));
      }
    });
    addCustomHandler (ACTION_DELETE_DOCUMENT_TYPE,
                      new IWebPageActionHandler <ISMPServiceInformation, WebPageExecutionContext> ()
                      {
                        public boolean isSelectedObjectRequired ()
                        {
                          return true;
                        }

                        @Nonnull
                        public EShowList handleAction (@Nonnull final WebPageExecutionContext aWPEC,
                                                       @Nonnull final ISMPServiceInformation aSelectedObject)
                        {
                          final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
                          if (aServiceInfoMgr.deleteSMPServiceInformation (aSelectedObject).isChanged ())
                          {
                            aWPEC.postRedirectGetInternal (success ("The selected document type '" +
                                                                    aSelectedObject.getDocumentTypeIdentifier ()
                                                                                   .getURIEncoded () +
                                                                    "' was successfully deleted!"));
                          }
                          else
                          {
                            aWPEC.postRedirectGetInternal (error ("Error deleting the selected document type '" +
                                                                  aSelectedObject.getDocumentTypeIdentifier ()
                                                                                 .getURIEncoded () +
                                                                  "'!"));
                          }
                          return EShowList.DONT_SHOW_LIST;
                        }
                      });
    addCustomHandler (ACTION_DELETE_PROCESS,
                      new IWebPageActionHandler <ISMPServiceInformation, WebPageExecutionContext> ()
                      {
                        public boolean isSelectedObjectRequired ()
                        {
                          return true;
                        }

                        @Nonnull
                        public EShowList handleAction (@Nonnull final WebPageExecutionContext aWPEC,
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
                          {
                            if (aServiceInfoMgr.deleteSMPProcess (aSelectedObject, aProcess).isChanged ())
                              aWPEC.postRedirectGetInternal (success ("The selected process '" +
                                                                      aProcess.getProcessIdentifier ()
                                                                              .getURIEncoded () +
                                                                      "' from document type '" +
                                                                      aSelectedObject.getDocumentTypeIdentifier ()
                                                                                     .getURIEncoded () +
                                                                      "' was successfully deleted!"));
                            else
                              aWPEC.postRedirectGetInternal (error ("Error deleting the process '" +
                                                                    aProcess.getProcessIdentifier ().getURIEncoded () +
                                                                    "' from the selected document type '" +
                                                                    aSelectedObject.getDocumentTypeIdentifier ()
                                                                                   .getURIEncoded () +
                                                                    "'!"));
                          }
                          return EShowList.SHOW_LIST;
                        }
                      });
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    if (aServiceGroupMgr.getSMPServiceGroupCount () <= 0)
    {
      aNodeList.addChild (warn ("No service group is present! At least one service group must be present to create an endpoint for it."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new service group")
                                                .setOnClick (createCreateURL (aWPEC, CMenuSecure.MENU_SERVICE_GROUPS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }

    final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();
    if (aTransportProfileMgr.getSMPTransportProfileCount () <= 0)
    {
      aNodeList.addChild (warn ("No transport profile is present! At least one transport profile must be present to create an endpoint for it."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new transport profile")
                                                .setOnClick (createCreateURL (aWPEC,
                                                                              CMenuSecure.MENU_TRANSPORT_PROFILES))
                                                .setIcon (EDefaultIcon.YES));
      aNodeList.addChild (new BootstrapButton ().addChild ("Check default transport profiles")
                                                .setOnClick (aWPEC.getLinkToMenuItem (CMenuSecure.MENU_TRANSPORT_PROFILES))
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
        if (LOGGER.isWarnEnabled ())
          LOGGER.warn ("Action " +
                       eFormAction.getID () +
                       " is not allowed, because endpoint with transport profile is missing ('" +
                       sTransportProfile +
                       "')");
      }
      else
      {
        if (LOGGER.isWarnEnabled ())
          LOGGER.warn ("Action " +
                       eFormAction.getID () +
                       " is not allowed, because process ID fields are missing ('" +
                       sProcessIDScheme +
                       "', '" +
                       sProcessIDValue +
                       "')");
      }

      return false;
    }
    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Nonnull
  protected static StringMap createParamMap (@Nonnull final ISMPServiceInformation aServiceInfo,
                                             @Nullable final ISMPProcess aProcess,
                                             @Nullable final ISMPEndpoint aEndpoint)
  {
    final StringMap ret = new StringMap ();
    ret.putIn (FIELD_SERVICE_GROUP_ID, aServiceInfo.getServiceGroup ().getParticipantIdentifier ().getURIEncoded ());
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
                              createEditURL (aWPEC, aSelectedObject).addAll (createParamMap (aSelectedObject,
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
    final IDocumentTypeIdentifier aDocumentTypeID = aSelectedObject.getDocumentTypeIdentifier ();
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
      final HCNodeList aCtrl = new HCNodeList ();
      aCtrl.addChild (div (NiceNameUI.getDocumentTypeID (aDocumentTypeID, true)));
      try
      {
        final IPeppolDocumentTypeIdentifierParts aParts = PeppolDocumentTypeIdentifierParts.extractFromIdentifier (aDocumentTypeID);
        aCtrl.addChild (SMPCommonUI.getDocumentTypeIDDetails (aParts));
      }
      catch (final IllegalArgumentException ex)
      {
        if (false)
          aCtrl.addChild (error ("Failed to parse document type identifier: " + ex.getMessage ()));
      }
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Document type ID").setCtrl (aCtrl));
    }

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Process ID")
                                                 .setCtrl (NiceNameUI.getProcessID (aSelectedObject.getDocumentTypeIdentifier (),
                                                                                    aSelectedProcess.getProcessIdentifier (),
                                                                                    true)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Transport profile")
                                                 .setCtrl (new HCA (createViewURL (aWPEC,
                                                                                   CMenuSecure.MENU_TRANSPORT_PROFILES,
                                                                                   aSelectedEndpoint.getTransportProfile ())).addChild (NiceNameUI.getTransportProfile (aSelectedEndpoint.getTransportProfile (),
                                                                                                                                                                        true))));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Endpoint reference")
                                                 .setCtrl (StringHelper.hasText (aSelectedEndpoint.getEndpointReference ()) ? HCA.createLinkedWebsite (aSelectedEndpoint.getEndpointReference (),
                                                                                                                                                       HC_Target.BLANK)
                                                                                                                            : em ("none")));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Requires business level signature")
                                                 .setCtrl (EPhotonCoreText.getYesOrNo (aSelectedEndpoint.isRequireBusinessLevelSignature (),
                                                                                       aDisplayLocale)));

    if (aSelectedEndpoint.hasMinimumAuthenticationLevel ())
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Minimum authentication level")
                                                   .setCtrl (aSelectedEndpoint.getMinimumAuthenticationLevel ()));

    if (aSelectedEndpoint.hasServiceActivationDateTime ())
    {
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not before")
                                                   .setCtrl (PDTToString.getAsString (aSelectedEndpoint.getServiceActivationDateTime (),
                                                                                      aDisplayLocale)));
    }
    if (aSelectedEndpoint.hasServiceExpirationDateTime ())
    {
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not after")
                                                   .setCtrl (PDTToString.getAsString (aSelectedEndpoint.getServiceExpirationDateTime (),
                                                                                      aDisplayLocale)));
    }

    if (aSelectedEndpoint.hasCertificate ())
    {
      final X509Certificate aEndpointCert = CertificateHelper.convertStringToCertficateOrNull (aSelectedEndpoint.getCertificate ());
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Certificate")
                                                   .setCtrl (aEndpointCert == null ? strong ("!!!FAILED TO INTERPRETE!!!")
                                                                                   : SMPCommonUI.createCertificateDetailsTable (null,
                                                                                                                                aEndpointCert,
                                                                                                                                aNowLDT,
                                                                                                                                aDisplayLocale)
                                                                                                .setResponsive (true)));
    }

    if (aSelectedEndpoint.hasServiceDescription ())
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service description")
                                                   .setCtrl (aSelectedEndpoint.getServiceDescription ()));

    if (aSelectedEndpoint.hasTechnicalContactUrl ())
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Technical contact")
                                                   .setCtrl (HCA_MailTo.createLinkedEmail (aSelectedEndpoint.getTechnicalContactUrl ())));

    if (aSelectedEndpoint.hasTechnicalInformationUrl ())
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Technical information")
                                                   .setCtrl (HCA.createLinkedWebsite (aSelectedEndpoint.getTechnicalInformationUrl (),
                                                                                      HC_Target.BLANK)));
    if (aSelectedEndpoint.getExtensions ().extensions ().isNotEmpty ())
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Extension")
                                                   .setCtrl (SMPCommonUI.getExtensionDisplay (aSelectedEndpoint)));

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
                                         : aWPEC.params ().getAsStringTrimmed (FIELD_SERVICE_GROUP_ID);
    ISMPServiceGroup aServiceGroup = null;

    final String sDocTypeIDScheme = bEdit ? aSelectedObject.getDocumentTypeIdentifier ().getScheme ()
                                          : aWPEC.params ().getAsStringTrimmed (FIELD_DOCTYPE_ID_SCHEME);
    final String sDocTypeIDValue = bEdit ? aSelectedObject.getDocumentTypeIdentifier ().getValue ()
                                         : aWPEC.params ().getAsStringTrimmed (FIELD_DOCTYPE_ID_VALUE);
    IDocumentTypeIdentifier aDocTypeID = null;
    final String sProcessIDScheme = bEdit ? aSelectedProcess.getProcessIdentifier ().getScheme ()
                                          : aWPEC.params ().getAsStringTrimmed (FIELD_PROCESS_ID_SCHEME);
    final String sProcessIDValue = bEdit ? aSelectedProcess.getProcessIdentifier ().getValue ()
                                         : aWPEC.params ().getAsStringTrimmed (FIELD_PROCESS_ID_VALUE);
    IProcessIdentifier aProcessID = null;

    final String sTransportProfileID = bEdit ? aSelectedEndpoint.getTransportProfile ()
                                             : aWPEC.params ().getAsStringTrimmed (FIELD_TRANSPORT_PROFILE);
    final ISMPTransportProfile aTransportProfile = aTransportProfileMgr.getSMPTransportProfileOfID (sTransportProfileID);
    final String sEndpointReference = aWPEC.params ().getAsStringTrimmed (FIELD_ENDPOINT_REFERENCE);
    final boolean bRequireBusinessLevelSignature = aWPEC.params ()
                                                        .getAsBoolean (FIELD_REQUIRES_BUSINESS_LEVEL_SIGNATURE);
    final String sMinimumAuthenticationLevel = aWPEC.params ().getAsStringTrimmed (FIELD_MINIMUM_AUTHENTICATION_LEVEL);
    final String sNotBefore = aWPEC.params ().getAsStringTrimmed (FIELD_NOT_BEFORE);
    final LocalDate aNotBeforeDate = PDTFromString.getLocalDateFromString (sNotBefore, aDisplayLocale);
    final String sNotAfter = aWPEC.params ().getAsStringTrimmed (FIELD_NOT_AFTER);
    final LocalDate aNotAfterDate = PDTFromString.getLocalDateFromString (sNotAfter, aDisplayLocale);
    final String sCertificate = aWPEC.params ().getAsStringTrimmed (FIELD_CERTIFICATE);
    final String sServiceDescription = aWPEC.params ().getAsStringTrimmed (FIELD_SERVICE_DESCRIPTION);
    final String sTechnicalContact = aWPEC.params ().getAsStringTrimmed (FIELD_TECHNICAL_CONTACT);
    final String sTechnicalInformation = aWPEC.params ().getAsStringTrimmed (FIELD_TECHNICAL_INFORMATION);
    final String sExtension = aWPEC.params ().getAsStringTrimmed (FIELD_EXTENSION);

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
                                             PDTFactory.createXMLOffsetDateTime (aNotBeforeDate),
                                             PDTFactory.createXMLOffsetDateTime (aNotAfterDate),
                                             sCertificate,
                                             sServiceDescription,
                                             sTechnicalContact,
                                             sTechnicalInformation,
                                             sExtension));

      if (aServiceInfoMgr.mergeSMPServiceInformation (aServiceInfo).isSuccess ())
      {
        if (bEdit)
        {
          aWPEC.postRedirectGetInternal (success ("Successfully edited the endpoint for service group '" +
                                                  aServiceGroup.getParticipantIdentifier ().getURIEncoded () +
                                                  "'."));
        }
        else
        {
          aWPEC.postRedirectGetInternal (success ("Successfully created a new endpoint for service group '" +
                                                  aServiceGroup.getParticipantIdentifier ().getURIEncoded () +
                                                  "'."));
        }
      }
      else
      {
        if (bEdit)
        {
          aWPEC.postRedirectGetInternal (error ("Error editing the endpoint for service group '" +
                                                aServiceGroup.getParticipantIdentifier ().getURIEncoded () +
                                                "'."));
        }
        else
        {
          aWPEC.postRedirectGetInternal (error ("Error creating a new endpoint for service group '" +
                                                aServiceGroup.getParticipantIdentifier ().getURIEncoded () +
                                                "'."));
        }
      }
    }
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPServiceInformation aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                final boolean bFormSubmitted,
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
                                                 .setCtrl (new HCCheckBox (new RequestFieldBoolean (FIELD_REQUIRES_BUSINESS_LEVEL_SIGNATURE,
                                                                                                    aSelectedEndpoint != null ? aSelectedEndpoint.isRequireBusinessLevelSignature ()
                                                                                                                              : SMPEndpoint.DEFAULT_REQUIRES_BUSINESS_LEVEL_SIGNATURE)))
                                                 .setHelpText ("Check the box if the recipient requires business-level signatures for " +
                                                               "the message, meaning a signature applied to the business message " +
                                                               "before the message is put on the transport. This is independent of " +
                                                               "the transport-level signatures that a specific transport profile, such " +
                                                               "as the START profile, might mandate. This flag does not indicate " +
                                                               "which type of business-level signature might be required. Setting or " +
                                                               "consuming business-level signatures would typically be the " +
                                                               "responsibility of the final senders and receivers of messages, rather " +
                                                               "than a set of APs.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_REQUIRES_BUSINESS_LEVEL_SIGNATURE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Minimum Authentication Level")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_MINIMUM_AUTHENTICATION_LEVEL,
                                                                                         aSelectedEndpoint != null ? aSelectedEndpoint.getMinimumAuthenticationLevel ()
                                                                                                                   : null)))
                                                 .setHelpText ("Indicates the minimum authentication level that recipient requires. " +
                                                               "The specific semantics of this field is defined in a specific instance " +
                                                               "of the BUSDOX infrastructure. It could for example reflect the " +
                                                               "value of the \"urn:eu:busdox:attribute:assurance-level\" SAML " +
                                                               "attribute defined in the START specification.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_MINIMUM_AUTHENTICATION_LEVEL)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not before")
                                                 .setCtrl (BootstrapDateTimePicker.create (FIELD_NOT_BEFORE,
                                                                                           aSelectedEndpoint != null ? aSelectedEndpoint.getServiceActivationDate ()
                                                                                                                     : null,
                                                                                           aDisplayLocale))
                                                 .setHelpText ("Activation date of the service. Senders should ignore services that " +
                                                               "are not yet activated.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_NOT_BEFORE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not after")
                                                 .setCtrl (BootstrapDateTimePicker.create (FIELD_NOT_AFTER,
                                                                                           aSelectedEndpoint != null ? aSelectedEndpoint.getServiceExpirationDate ()
                                                                                                                     : null,
                                                                                           aDisplayLocale))
                                                 .setHelpText ("Expiration date of the service. Senders should ignore services that " +
                                                               "are expired.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_NOT_AFTER)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Certificate")
                                                 .setCtrl (new HCTextArea (new RequestField (FIELD_CERTIFICATE,
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
                                                 .setCtrl (new HCTextArea (new RequestField (FIELD_EXTENSION,
                                                                                             aSelectedEndpoint != null ? aSelectedEndpoint.getExtensions ()
                                                                                                                                          .getFirstExtensionXMLString ()
                                                                                                                       : null)))
                                                 .setHelpText ("Optional extension to the endpoint. If present it must be valid XML content!")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_EXTENSION)));
  }
}
