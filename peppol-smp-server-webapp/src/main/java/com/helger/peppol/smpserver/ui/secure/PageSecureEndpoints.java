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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.LocalDate;
import org.joda.time.LocalDateTime;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.serialize.MicroReader;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.SMap;
import com.helger.commons.url.URLHelper;
import com.helger.datetime.CPDT;
import com.helger.datetime.PDTFactory;
import com.helger.datetime.format.PDTFromString;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.html.HC_Target;
import com.helger.html.hc.html.forms.HCCheckBox;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCStrong;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.identifier.CIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifierParts;
import com.helger.peppol.identifier.doctype.SimpleDocumentTypeIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.identifier.process.SimpleProcessIdentifier;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smpserver.data.xml.MetaManager;
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
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageForm;
import com.helger.peppol.smpserver.ui.AppCommonUI;
import com.helger.peppol.utils.CertificateHelper;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.bootstrap3.uictrls.datetimepicker.BootstrapDateTimePicker;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldDate;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.autosize.HCTextAreaAutosize;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.famfam.EFamFamIcon;
import com.helger.validation.error.FormErrors;

@WorkInProgress
public final class PageSecureEndpoints extends AbstractSMPWebPageForm <ISMPServiceInformation>
{
  private final static String FIELD_SERVICE_GROUP_ID = "sgid";
  private final static String FIELD_DOCTYPE_ID = "doctypeid";
  private final static String FIELD_PROCESS_ID = "processid";
  private final static String FIELD_TRANSPORT_PROFILE = "transportprofile";
  private final static String FIELD_ENDPOINT_REFERENCE = "endpointreference";
  private final static String FIELD_REQUIRES_BUSINESS_LEVEL_SIGNATURE = "requiresbusinesslevelsignature";
  private final static String FIELD_MINIMUM_AUTHENTICATION_LEVEL = "minimumauthenticationlevel";
  private final static String FIELD_NOT_BEFORE = "notbefore";
  private final static String FIELD_NOT_AFTER = "notafter";
  private final static String FIELD_CERTIFICATE = "certificate";
  private final static String FIELD_SERVICE_DESCRIPTION = "servicedescription";
  private final static String FIELD_TECHNICAL_CONTACT = "technicalcontact";
  private final static String FIELD_TECHNICAL_INFORMATION = "technicalinformation";
  private final static String FIELD_EXTENSION = "extension";

  private static final String ATTR_PROCESS = "$process";
  private static final String ATTR_ENDPOINT = "$endpoint";

  public PageSecureEndpoints (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Endpoints");
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupManager = MetaManager.getServiceGroupMgr ();
    if (aServiceGroupManager.getSMPServiceGroupCount () == 0)
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
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPServiceInformation aSelectedObject)
  {
    if (eFormAction == EWebPageFormAction.VIEW ||
        eFormAction == EWebPageFormAction.EDIT ||
        eFormAction == EWebPageFormAction.DELETE)
    {
      final String sProcessID = aWPEC.getAttributeAsString (FIELD_PROCESS_ID);
      final SimpleProcessIdentifier aProcessID = sProcessID == null ? null
                                                                    : SimpleProcessIdentifier.createFromURIPartOrNull (sProcessID);
      final String sTransportProfile = aWPEC.getAttributeAsString (FIELD_TRANSPORT_PROFILE);

      final ISMPProcess aProcess = aSelectedObject.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        final ISMPEndpoint aEndpoint = aProcess.getEndpointOfTransportProfile (sTransportProfile);
        if (aEndpoint != null)
        {
          aWPEC.getRequestScope ().setAttribute (ATTR_PROCESS, aProcess);
          aWPEC.getRequestScope ().setAttribute (ATTR_ENDPOINT, aEndpoint);
          return true;
        }
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
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().getCastedAttribute (ATTR_PROCESS);
    final ISMPEndpoint aSelectedEndpoint = aWPEC.getRequestScope ().getCastedAttribute (ATTR_ENDPOINT);
    final LocalDateTime aNowLDT = PDTFactory.getCurrentLocalDateTime ();

    aNodeList.addChild (createActionHeader ("Show details of endpoint"));

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

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Transport profile")
                                                 .setCtrl (aSelectedEndpoint.getTransportProfile ()));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Endpoint reference")
                                                 .setCtrl (HCA.createLinkedWebsite (aSelectedEndpoint.getEndpointReference (),
                                                                                    HC_Target.BLANK)));

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
                                                 .setCtrl (aSelectedEndpoint.getTechnicalContactUrl ()));

    if (StringHelper.hasText (aSelectedEndpoint.getTechnicalInformationUrl ()))
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Technical information")
                                                   .setCtrl (aSelectedEndpoint.getTechnicalInformationUrl ()));

    aNodeList.addChild (aForm);

  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPServiceInformation aSelectedObject,
                                                 @Nonnull final FormErrors aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().getCastedAttribute (ATTR_PROCESS);
    final ISMPEndpoint aSelectedEndpoint = aWPEC.getRequestScope ().getCastedAttribute (ATTR_ENDPOINT);
    final ISMPServiceGroupManager aServiceGroupManager = MetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = MetaManager.getServiceInformationMgr ();
    final ISMPRedirectManager aRedirectMgr = MetaManager.getRedirectMgr ();

    final String sServiceGroupID = bEdit ? aSelectedObject.getServiceGroupID ()
                                         : aWPEC.getAttributeAsString (FIELD_SERVICE_GROUP_ID);
    ISMPServiceGroup aServiceGroup = null;

    final String sDocTypeID = bEdit ? aSelectedObject.getDocumentTypeIdentifier ().getURIEncoded ()
                                    : aWPEC.getAttributeAsString (FIELD_DOCTYPE_ID);
    IPeppolDocumentTypeIdentifier aDocTypeID = null;
    final String sProcessID = bEdit ? aSelectedProcess.getProcessIdentifier ().getURIEncoded ()
                                    : aWPEC.getAttributeAsString (FIELD_PROCESS_ID);
    IPeppolProcessIdentifier aProcessID = null;

    final String sTransportProfile = bEdit ? aSelectedEndpoint.getTransportProfile ()
                                           : aWPEC.getAttributeAsString (FIELD_TRANSPORT_PROFILE);
    final ESMPTransportProfile eTransportProfile = ESMPTransportProfile.getFromIDOrNull (sTransportProfile);
    final String sEndpointReference = aWPEC.getAttributeAsString (FIELD_ENDPOINT_REFERENCE);
    final boolean bRequireBusinessLevelSignature = aWPEC.getAttributeAsBoolean (FIELD_REQUIRES_BUSINESS_LEVEL_SIGNATURE);
    final String sMinimumAuthenticationLevel = aWPEC.getAttributeAsString (FIELD_MINIMUM_AUTHENTICATION_LEVEL);
    final String sNotBefore = aWPEC.getAttributeAsString (FIELD_NOT_BEFORE);
    final LocalDate aNotBeforeDate = PDTFromString.getLocalDateFromString (sNotBefore, aDisplayLocale);
    final String sNotAfter = aWPEC.getAttributeAsString (FIELD_NOT_AFTER);
    final LocalDate aNotAfterDate = PDTFromString.getLocalDateFromString (sNotAfter, aDisplayLocale);
    final String sCertificate = aWPEC.getAttributeAsString (FIELD_CERTIFICATE);
    final String sServiceDescription = aWPEC.getAttributeAsString (FIELD_SERVICE_DESCRIPTION);
    final String sTechnicalContact = aWPEC.getAttributeAsString (FIELD_TECHNICAL_CONTACT);
    final String sTechnicalInformation = aWPEC.getAttributeAsString (FIELD_TECHNICAL_INFORMATION);
    final String sExtension = aWPEC.getAttributeAsString (FIELD_EXTENSION);

    // validations
    if (StringHelper.isEmpty (sServiceGroupID))
      aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "A service group must be selected!");
    else
    {
      aServiceGroup = aServiceGroupManager.getSMPServiceGroupOfID (sServiceGroupID);
      if (aServiceGroup == null)
        aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "The provided service group does not exist!");
    }

    if (StringHelper.isEmpty (sDocTypeID))
      aFormErrors.addFieldError (FIELD_DOCTYPE_ID, "Document type ID must not be empty!");
    else
    {
      aDocTypeID = SimpleDocumentTypeIdentifier.createFromURIPartOrNull (sDocTypeID);
      if (aDocTypeID == null)
        aFormErrors.addFieldError (FIELD_DOCTYPE_ID, "The provided document type ID has an invalid syntax!");
      else
      {
        if (aServiceGroup != null)
          if (aRedirectMgr.getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup.getID (), aDocTypeID) != null)
            aFormErrors.addFieldError (FIELD_DOCTYPE_ID,
                                       "At least one redirect is registered for this document type. Delete the redirect before you can create an endpoint.");
      }
    }

    if (StringHelper.isEmpty (sProcessID))
      aFormErrors.addFieldError (FIELD_PROCESS_ID, "Process ID must not be empty!");
    else
    {
      aProcessID = SimpleProcessIdentifier.createFromURIPartOrNull (sProcessID);
      if (aProcessID == null)
        aFormErrors.addFieldError (FIELD_PROCESS_ID, "The provided process ID has an invalid syntax!");
    }

    if (StringHelper.isEmpty (sTransportProfile))
      aFormErrors.addFieldError (FIELD_TRANSPORT_PROFILE, "Transport Profile must not be empty!");
    else
      if (eTransportProfile == null)
        aFormErrors.addFieldError (FIELD_TRANSPORT_PROFILE,
                                   "Transport Profile of type '" + sTransportProfile + "' does not exist!");

    if (StringHelper.isEmpty (sEndpointReference))
      aFormErrors.addFieldError (FIELD_ENDPOINT_REFERENCE, "Endpoint Reference must not be empty!");
    else
      if (URLHelper.getAsURL (sEndpointReference) == null)
        aFormErrors.addFieldError (FIELD_ENDPOINT_REFERENCE, "The Endpoint Reference is not a valid URL!");

    if (aNotBeforeDate != null && aNotAfterDate != null)
      if (aNotBeforeDate.isAfter (aNotAfterDate))
        aFormErrors.addFieldError (FIELD_NOT_BEFORE, "Not Before Date must not be after Not After Date!");

    if (StringHelper.isEmpty (sCertificate))
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

    if (StringHelper.isEmpty (sServiceDescription))
      aFormErrors.addFieldError (FIELD_SERVICE_DESCRIPTION, "Service Description must not be empty!");

    if (StringHelper.isEmpty (sTechnicalContact))
      aFormErrors.addFieldError (FIELD_TECHNICAL_CONTACT, "Technical Contact must not be empty!");

    if (StringHelper.hasText (sExtension))
    {
      final IMicroDocument aDoc = MicroReader.readMicroXML (sExtension);
      if (aDoc == null)
        aFormErrors.addFieldError (FIELD_EXTENSION, "The extension must be XML content.");
    }

    if (aFormErrors.isEmpty ())
    {
      if (bEdit)
      {
        final SMPEndpoint aEndpoint = (SMPEndpoint) aSelectedEndpoint;
        aEndpoint.setEndpointReference (sEndpointReference);
        aEndpoint.setRequireBusinessLevelSignature (bRequireBusinessLevelSignature);
        aEndpoint.setMinimumAuthenticationLevel (sMinimumAuthenticationLevel);
        aEndpoint.setServiceActivationDateTime (aNotBeforeDate == null ? null
                                                                       : aNotBeforeDate.toLocalDateTime (CPDT.NULL_LOCAL_TIME));
        aEndpoint.setServiceExpirationDateTime (aNotAfterDate == null ? null
                                                                      : aNotAfterDate.toLocalDateTime (CPDT.NULL_LOCAL_TIME));
        aEndpoint.setCertificate (sCertificate);
        aEndpoint.setServiceDescription (sServiceDescription);
        aEndpoint.setTechnicalContactUrl (sTechnicalContact);
        aEndpoint.setTechnicalInformationUrl (sTechnicalInformation);
        aEndpoint.setExtension (sExtension);

        aServiceInfoMgr.updateSMPServiceInformation (aSelectedObject.getID ());

        aNodeList.addChild (new BootstrapSuccessBox ().addChild ("Successfully edited the endpoint"));
      }
      else
      {
        final SMPEndpoint aEndpoint = new SMPEndpoint (sTransportProfile,
                                                       sEndpointReference,
                                                       bRequireBusinessLevelSignature,
                                                       sMinimumAuthenticationLevel,
                                                       aNotBeforeDate == null ? null
                                                                              : aNotBeforeDate.toLocalDateTime (CPDT.NULL_LOCAL_TIME),
                                                       aNotAfterDate == null ? null
                                                                             : aNotAfterDate.toLocalDateTime (CPDT.NULL_LOCAL_TIME),
                                                       sCertificate,
                                                       sServiceDescription,
                                                       sTechnicalContact,
                                                       sTechnicalInformation,
                                                       sExtension);
        final SMPProcess aProcess = new SMPProcess (aProcessID, CollectionHelper.newList (aEndpoint), null);
        final SMPServiceInformation aServiceInfo = new SMPServiceInformation (aServiceGroup,
                                                                              aDocTypeID,
                                                                              CollectionHelper.newList (aProcess),
                                                                              null);
        aServiceInfoMgr.createSMPServiceInformation (aServiceInfo);

        aNodeList.addChild (new BootstrapSuccessBox ().addChild ("Successfully created the new endpoint"));
      }
    }
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPServiceInformation aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrors aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPProcess aSelectedProcess = aWPEC.getRequestScope ().getCastedAttribute (ATTR_PROCESS);
    final ISMPEndpoint aSelectedEndpoint = aWPEC.getRequestScope ().getCastedAttribute (ATTR_ENDPOINT);

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service group")
                                                 .setCtrl (new HCServiceGroupSelect (new RequestField (FIELD_SERVICE_GROUP_ID,
                                                                                                       aSelectedObject != null ? aSelectedObject.getServiceGroupID ()
                                                                                                                               : null),
                                                                                     aDisplayLocale).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_GROUP_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Document type ID")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_DOCTYPE_ID,
                                                                                         aSelectedObject != null ? aSelectedObject.getDocumentTypeIdentifier ()
                                                                                                                                  .getURIEncoded ()
                                                                                                                 : CIdentifier.DEFAULT_DOCUMENT_TYPE_IDENTIFIER_SCHEME +
                                                                                                                   CIdentifier.URL_SCHEME_VALUE_SEPARATOR)).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_DOCTYPE_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Process ID")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_PROCESS_ID,
                                                                                         aSelectedProcess != null ? aSelectedProcess.getProcessIdentifier ()
                                                                                                                                    .getURIEncoded ()
                                                                                                                  : CIdentifier.DEFAULT_PROCESS_IDENTIFIER_SCHEME +
                                                                                                                    CIdentifier.URL_SCHEME_VALUE_SEPARATOR)).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_PROCESS_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Transport Profile")
                                                 .setCtrl (new SMPTransportProfileSelect (new RequestField (FIELD_TRANSPORT_PROFILE,
                                                                                                            aSelectedEndpoint != null ? aSelectedEndpoint.getTransportProfile ()
                                                                                                                                      : ESMPTransportProfile.TRANSPORT_PROFILE_AS2.getID ())).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_TRANSPORT_PROFILE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Endpoint Reference")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_ENDPOINT_REFERENCE,
                                                                                         aSelectedEndpoint != null ? aSelectedEndpoint.getEndpointReference ()
                                                                                                                   : null)))
                                                 .setHelpText ("The URL where messsages of this type should be targeted to.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_ENDPOINT_REFERENCE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Requires Business Level Signature")
                                                 .setCtrl (new HCCheckBox (FIELD_REQUIRES_BUSINESS_LEVEL_SIGNATURE,
                                                                           aSelectedEndpoint != null ? aSelectedEndpoint.isRequireBusinessLevelSignature ()
                                                                                                     : false))
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
                                                                                                              aDisplayLocale)))
                                                 .setHelpText ("Activation date of the service. Senders should ignore services that " +
                                                               "are not yet activated.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_NOT_BEFORE)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not After")
                                                 .setCtrl (new BootstrapDateTimePicker (new RequestFieldDate (FIELD_NOT_AFTER,
                                                                                                              aSelectedEndpoint != null ? aSelectedEndpoint.getServiceExpirationDate ()
                                                                                                                                        : null,
                                                                                                              aDisplayLocale)))
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
  }

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

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new Endpoint", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aNodeList.addChild (aToolbar);

    final HCTable aTable = new HCTable (new DTCol ("Service group").setInitialSorting (ESortOrder.ASCENDING)
                                                                   .setDataSort (0, 1),
                                        new DTCol ("Document type ID").setDataSort (1, 0),
                                        new DTCol ("Process ID"),
                                        new DTCol ("Transport"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPServiceInformation aServiceInfo : aServiceInfoMgr.getAllSMPServiceInformations ())
      for (final ISMPProcess aProcess : aServiceInfo.getAllProcesses ())
        for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        {
          final SMap aParams = new SMap ().add (FIELD_DOCTYPE_ID,
                                                aServiceInfo.getDocumentTypeIdentifier ().getURIPercentEncoded ())
                                          .add (FIELD_PROCESS_ID, aProcess.getProcessIdentifier ().getURIEncoded ())
                                          .add (FIELD_TRANSPORT_PROFILE, aEndpoint.getTransportProfile ());

          final HCRow aRow = aTable.addBodyRow ();
          aRow.addCell (new HCA (createViewURL (aWPEC,
                                                aServiceInfo).addAll (aParams)).addChild (aServiceInfo.getServiceGroupID ()));
          aRow.addCell (AppCommonUI.getDocumentTypeID (aServiceInfo.getDocumentTypeIdentifier ()));
          aRow.addCell (AppCommonUI.getProcessID (aProcess.getProcessIdentifier ()));
          aRow.addCell (aEndpoint.getTransportProfile ());
          aRow.addCell (createEditLink (aWPEC, aServiceInfo, "Edit endpoint", aParams),
                        new HCTextNode (" "),
                        new HCA (LinkHelper.getURIWithServerAndContext (aServiceInfo.getServiceGroup ()
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
