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

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.StringParser;
import com.helger.commons.url.URLHelper;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.textlevel.HCEM;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.network.dns.IPV4Addr;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.ManageServiceMetadataServiceCaller;
import com.helger.peppol.smlclient.smp.BadRequestFault;
import com.helger.peppol.smlclient.smp.InternalErrorFault;
import com.helger.peppol.smlclient.smp.NotFoundFault;
import com.helger.peppol.smlclient.smp.UnauthorizedFault;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.helger.peppol.smpserver.security.SMPTrustManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPage;
import com.helger.peppol.smpserver.ui.secure.hc.HCSMLSelect;
import com.helger.photon.basic.audit.AuditHelper;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.nav.BootstrapTabBox;
import com.helger.photon.bootstrap3.pages.BootstrapWebPageUIHandler;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.sun.xml.ws.client.ClientTransportException;

public class PageSecureSMLRegistration extends AbstractSMPWebPage
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureSMLRegistration.class);

  private static final String FIELD_SML_ID = "sml";
  private static final String FIELD_PHYSICAL_ADDRESS = "physicaladdr";
  private static final String FIELD_LOGICAL_ADDRESS = "logicaladdr";

  private static final String HELPTEXT_SMP_ID = "This is the unique ID your SMP will have inside the SML. All continuing operations must use this ID. This ID is taken from the configuration file. All uppercase names are appreciated!";
  private static final String HELPTEXT_PHYSICAL_ADDRESS = "This must be the public IPv4 address of your SMP. IPv6 addresses are not yet supported! By default the IP address of localhost is used.";
  private static final String HELPTEXT_LOGICAL_ADDRESS = "This must be the public fully qualified domain name of your SMP. This can be either a domain name like 'http://smp.example.org' or an IP address like 'http://1.1.1.1'! By default the hostname of localhost is used.";

  private static final String SUBACTION_SMP_REGISTER = "smpregister";
  private static final String SUBACTION_SMP_UPDATE = "smpupdate";
  private static final String SUBACTION_SMP_DELETE = "smpdelete";

  public PageSecureSMLRegistration (@Nonnull @Nonempty final String sID)
  {
    super (sID, "SML registration");
  }

  @Nonnull
  @Nonempty
  private static String _getTechnicalDetails (@Nonnull final Throwable t)
  {
    return " Technical details: " + ClassHelper.getClassLocalName (t) + " " + StringHelper.getNotNull (t.getMessage ());
  }

  private static boolean _canShowPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    // No truststore is okay - that can be handled
    if (false)
      if (!SMPTrustManager.isCertificateValid ())
      {
        aNodeList.addChild (new BootstrapErrorBox ().addChild ("No valid truststore is provided, so no connection with the SML can be established!"));
        return false;
      }
    if (!SMPKeyManager.isCertificateValid ())
    {
      aNodeList.addChild (new BootstrapErrorBox ().addChild ("No valid keystore/certificate is provided, so no connection with the SML can be established!"));
      return false;
    }
    return true;
  }

  @Nonnull
  private static ManageServiceMetadataServiceCaller _create (@Nonnull final ISMLInfo aSML,
                                                             @Nonnull final SSLSocketFactory aSocketFactory)
  {
    final ManageServiceMetadataServiceCaller ret = new ManageServiceMetadataServiceCaller (aSML);
    ret.setSSLSocketFactory (aSocketFactory);
    return ret;
  }

  private static void _registerSMPtoSML (@Nonnull final WebPageExecutionContext aWPEC,
                                         @Nonnull final FormErrorList aFormErrors)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final String sSMLID = aWPEC.params ().getAsString (FIELD_SML_ID);
    final ISMLInfo aSMLInfo = SMPMetaManager.getSMLInfoMgr ().getSMLInfoOfID (sSMLID);
    final String sPhysicalAddress = aWPEC.params ().getAsString (FIELD_PHYSICAL_ADDRESS);
    final String sLogicalAddress = aWPEC.params ().getAsString (FIELD_LOGICAL_ADDRESS);

    if (aSMLInfo == null)
      aFormErrors.addFieldError (FIELD_SML_ID, "A valid SML must be selected!");

    if (StringHelper.hasNoText (sPhysicalAddress))
      aFormErrors.addFieldError (FIELD_PHYSICAL_ADDRESS, "A physical address must be provided!");
    else
      if (!RegExHelper.stringMatchesPattern (IPV4Addr.PATTERN_IPV4, sPhysicalAddress))
        aFormErrors.addFieldError (FIELD_PHYSICAL_ADDRESS,
                                   "The provided physical address does not seem to be an IPv4 address!");
      else
      {
        final String [] aParts = StringHelper.getExplodedArray ('.', sPhysicalAddress, 4);
        final byte [] aBytes = new byte [] { (byte) StringParser.parseInt (aParts[0], -1),
                                             (byte) StringParser.parseInt (aParts[1], -1),
                                             (byte) StringParser.parseInt (aParts[2], -1),
                                             (byte) StringParser.parseInt (aParts[3], -1) };
        try
        {
          InetAddress.getByAddress (aBytes);
        }
        catch (final UnknownHostException ex)
        {
          aFormErrors.addFieldError (FIELD_PHYSICAL_ADDRESS,
                                     "The provided IP address does not resolve to a valid host." +
                                                             _getTechnicalDetails (ex));
        }
      }

    if (StringHelper.hasNoText (sLogicalAddress))
      aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                 "A logical address must be provided in the form 'http://smp.example.org'!");
    else
    {
      final URL aURL = URLHelper.getAsURL (sLogicalAddress);
      if (aURL == null)
        aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                   "The provided logical address seems not be a URL! Please use the form 'http://smp.example.org'");
      else
      {
        if (!"http".equals (aURL.getProtocol ()))
          aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                     "The provided logical address must use the 'http' protocol and may not use the '" +
                                                            aURL.getProtocol () +
                                                            "' protocol. According to the SMP specification, no other protocols than 'http' are allowed!");
        // -1 means default port
        if (aURL.getPort () != 80 && aURL.getPort () != -1)
          aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                     "The provided logical address must use the default http port 80 and not port " +
                                                            aURL.getPort () +
                                                            ". According to the SMP specification, no other ports are allowed!");
        if (StringHelper.hasText (aURL.getPath ()) && !"/".equals (aURL.getPath ()))
          aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                     "The provided logical address may not contain a path (" +
                                                            aURL.getPath () +
                                                            ") because according to the SMP specifications it must run in the root (/) path!");
      }
    }

    if (aFormErrors.isEmpty ())
    {
      final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
      try
      {
        final SSLSocketFactory aSocketFactory = SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ();
        final ManageServiceMetadataServiceCaller aCaller = _create (aSMLInfo, aSocketFactory);
        aCaller.create (sSMPID, sPhysicalAddress, sLogicalAddress);

        final String sMsg = "Successfully registered SMP '" +
                            sSMPID +
                            "' with physical address '" +
                            sPhysicalAddress +
                            "' and logical address '" +
                            sLogicalAddress +
                            "' to the SML '" +
                            aSMLInfo.getManagementServiceURL () +
                            "'.";
        LOGGER.info (sMsg);
        aNodeList.addChild (new BootstrapSuccessBox ().addChild (sMsg));
        AuditHelper.onAuditExecuteSuccess ("smp-sml-create",
                                           sSMPID,
                                           sPhysicalAddress,
                                           sLogicalAddress,
                                           aSMLInfo.getManagementServiceURL ());
      }
      catch (final GeneralSecurityException | BadRequestFault | InternalErrorFault | UnauthorizedFault
          | ClientTransportException ex)
      {
        final String sMsg = "Error registering SMP '" +
                            sSMPID +
                            "' with physical address '" +
                            sPhysicalAddress +
                            "' and logical address '" +
                            sLogicalAddress +
                            "' to the SML '" +
                            aSMLInfo.getManagementServiceURL () +
                            "'.";
        LOGGER.error (sMsg, ex);
        aNodeList.addChild (new BootstrapErrorBox ().addChild (sMsg + _getTechnicalDetails (ex)));
        AuditHelper.onAuditExecuteFailure ("smp-sml-create",
                                           sSMPID,
                                           sPhysicalAddress,
                                           sLogicalAddress,
                                           aSMLInfo.getManagementServiceURL (),
                                           ex.getClass (),
                                           ex.getMessage ());
      }
    }
    else
      aNodeList.addChild (BootstrapWebPageUIHandler.INSTANCE.createIncorrectInputBox (aWPEC));
  }

  private static void _updateSMPatSML (@Nonnull final WebPageExecutionContext aWPEC,
                                       @Nonnull final FormErrorList aFormErrors)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final String sSMLID = aWPEC.params ().getAsString (FIELD_SML_ID);
    final ISMLInfo aSMLInfo = SMPMetaManager.getSMLInfoMgr ().getSMLInfoOfID (sSMLID);
    final String sPhysicalAddress = aWPEC.params ().getAsString (FIELD_PHYSICAL_ADDRESS);
    final String sLogicalAddress = aWPEC.params ().getAsString (FIELD_LOGICAL_ADDRESS);

    if (aSMLInfo == null)
      aFormErrors.addFieldError (FIELD_SML_ID, "A valid SML must be selected!");

    if (StringHelper.hasNoText (sPhysicalAddress))
      aFormErrors.addFieldError (FIELD_PHYSICAL_ADDRESS, "A physical address must be provided!");
    else
      if (!RegExHelper.stringMatchesPattern (IPV4Addr.PATTERN_IPV4, sPhysicalAddress))
        aFormErrors.addFieldError (FIELD_PHYSICAL_ADDRESS,
                                   "The provided physical address does not seem to be an IPv4 address!");
      else
      {
        final String [] aParts = StringHelper.getExplodedArray ('.', sPhysicalAddress, 4);
        final byte [] aBytes = new byte [] { (byte) StringParser.parseInt (aParts[0], -1),
                                             (byte) StringParser.parseInt (aParts[1], -1),
                                             (byte) StringParser.parseInt (aParts[2], -1),
                                             (byte) StringParser.parseInt (aParts[3], -1) };
        try
        {
          InetAddress.getByAddress (aBytes);
        }
        catch (final UnknownHostException ex)
        {
          aFormErrors.addFieldError (FIELD_PHYSICAL_ADDRESS,
                                     "The provided IP address does not resolve to a valid host." +
                                                             _getTechnicalDetails (ex));
        }
      }

    if (StringHelper.hasNoText (sLogicalAddress))
      aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                 "A logical address must be provided in the form 'http://smp.example.org'!");
    else
    {
      final URL aURL = URLHelper.getAsURL (sLogicalAddress);
      if (aURL == null)
        aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                   "The provided logical address seems not be a URL! Please use the form 'http://smp.example.org'");
      else
      {
        if (!"http".equals (aURL.getProtocol ()))
          aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                     "The provided logical address must use the 'http' protocol and may not use the '" +
                                                            aURL.getProtocol () +
                                                            "' protocol. According to the SMP specification, no other protocols than 'http' are allowed!");
        // -1 means default port
        if (aURL.getPort () != 80 && aURL.getPort () != -1)
          aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                     "The provided logical address must use the default http port 80 and not port " +
                                                            aURL.getPort () +
                                                            ". According to the SMP specification, no other ports are allowed!");
        if (StringHelper.hasText (aURL.getPath ()) && !"/".equals (aURL.getPath ()))
          aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                     "The provided logical address may not contain a path (" +
                                                            aURL.getPath () +
                                                            ") because according to the SMP specifications it must run in the root (/) path!");
      }
    }

    if (aFormErrors.isEmpty ())
    {
      final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
      try
      {
        final SSLSocketFactory aSocketFactory = SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ();
        final ManageServiceMetadataServiceCaller aCaller = _create (aSMLInfo, aSocketFactory);
        aCaller.update (sSMPID, sPhysicalAddress, sLogicalAddress);

        final String sMsg = "Successfully updated SMP '" +
                            sSMPID +
                            "' with physical address '" +
                            sPhysicalAddress +
                            "' and logical address '" +
                            sLogicalAddress +
                            "' at the SML '" +
                            aSMLInfo.getManagementServiceURL () +
                            "'.";
        LOGGER.info (sMsg);
        aNodeList.addChild (new BootstrapSuccessBox ().addChild (sMsg));
        AuditHelper.onAuditExecuteSuccess ("smp-sml-update",
                                           sSMPID,
                                           sPhysicalAddress,
                                           sLogicalAddress,
                                           aSMLInfo.getManagementServiceURL ());
      }
      catch (final GeneralSecurityException | BadRequestFault | InternalErrorFault | UnauthorizedFault | NotFoundFault
          | ClientTransportException ex)
      {
        final String sMsg = "Error updating SMP '" +
                            sSMPID +
                            "' with physical address '" +
                            sPhysicalAddress +
                            "' and logical address '" +
                            sLogicalAddress +
                            "' to the SML '" +
                            aSMLInfo.getManagementServiceURL () +
                            "'.";
        LOGGER.error (sMsg, ex);
        aNodeList.addChild (new BootstrapErrorBox ().addChild (sMsg + _getTechnicalDetails (ex)));
        AuditHelper.onAuditExecuteFailure ("smp-sml-update",
                                           sSMPID,
                                           sPhysicalAddress,
                                           sLogicalAddress,
                                           aSMLInfo.getManagementServiceURL (),
                                           ex.getClass (),
                                           ex.getMessage ());
      }
    }
    else
      aNodeList.addChild (BootstrapWebPageUIHandler.INSTANCE.createIncorrectInputBox (aWPEC));
  }

  private static void _deleteSMPfromSML (@Nonnull final WebPageExecutionContext aWPEC,
                                         @Nonnull final FormErrorList aFormErrors)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final String sSMLID = aWPEC.params ().getAsString (FIELD_SML_ID);
    final ISMLInfo aSMLInfo = SMPMetaManager.getSMLInfoMgr ().getSMLInfoOfID (sSMLID);

    if (aSMLInfo == null)
      aFormErrors.addFieldError (FIELD_SML_ID, "A valid SML must be selected!");

    if (aFormErrors.isEmpty ())
    {
      final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
      try
      {
        final SSLSocketFactory aSocketFactory = SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ();
        final ManageServiceMetadataServiceCaller aCaller = _create (aSMLInfo, aSocketFactory);
        aCaller.delete (sSMPID);

        final String sMsg = "Successfully deleted SMP '" +
                            sSMPID +
                            "' from the SML '" +
                            aSMLInfo.getManagementServiceURL () +
                            "'.";
        LOGGER.info (sMsg);
        aNodeList.addChild (new BootstrapSuccessBox ().addChild (sMsg));
        AuditHelper.onAuditExecuteSuccess ("smp-sml-delete", sSMPID, aSMLInfo.getManagementServiceURL ());
      }
      catch (final GeneralSecurityException | BadRequestFault | InternalErrorFault | UnauthorizedFault | NotFoundFault
          | ClientTransportException ex)
      {
        final String sMsg = "Error deleting SMP '" +
                            sSMPID +
                            "' from the SML '" +
                            aSMLInfo.getManagementServiceURL () +
                            "'.";
        LOGGER.error (sMsg, ex);
        aNodeList.addChild (new BootstrapErrorBox ().addChild (sMsg + _getTechnicalDetails (ex)));
        AuditHelper.onAuditExecuteFailure ("smp-sml-delete",
                                           sSMPID,
                                           aSMLInfo.getManagementServiceURL (),
                                           ex.getClass (),
                                           ex.getMessage ());
      }
    }
    else
      aNodeList.addChild (BootstrapWebPageUIHandler.INSTANCE.createIncorrectInputBox (aWPEC));
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    if (!_canShowPage (aWPEC))
      return;

    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final FormErrorList aFormErrors = new FormErrorList ();
    final boolean bShowInput = true;
    final ISMLInfo aDefaultSML = SMPMetaManager.getSettings ().getSMLInfo ();
    final String sSMPID = SMPServerConfiguration.getSMLSMPID ();

    if (aDefaultSML != null)
    {
      // Check if this SMP is already registered
      final String sPublisherDNSName = sSMPID + "." + aDefaultSML.getPublisherDNSZone ();
      try
      {
        final InetAddress aIA = InetAddress.getByName (sPublisherDNSName);
        aNodeList.addChild (new BootstrapInfoBox ().addChild (new HCDiv ().addChild ("An SMP is already registered at the configured SML using the DNS name '" +
                                                                                     sPublisherDNSName +
                                                                                     "'. The determined IP address is " +
                                                                                     aIA.getHostAddress ()))
                                                   .addChild (new HCDiv ().addChild ("Note: this can be a different machine than this one, if another SMP uses the same ID as this one (" +
                                                                                     sSMPID +
                                                                                     ")")));
      }
      catch (final UnknownHostException ex)
      {
        // continue
      }
    }

    if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
    {
      if (aWPEC.hasSubAction (SUBACTION_SMP_REGISTER))
        _registerSMPtoSML (aWPEC, aFormErrors);
      else
        if (aWPEC.hasSubAction (SUBACTION_SMP_UPDATE))
          _updateSMPatSML (aWPEC, aFormErrors);
        else
          if (aWPEC.hasSubAction (SUBACTION_SMP_DELETE))
            _deleteSMPfromSML (aWPEC, aFormErrors);
    }

    if (bShowInput)
    {
      // Get default from configuration
      String sPhysicalAddress = SMPServerConfiguration.getSMLSMPIP ();
      String sLogicalAddress = SMPServerConfiguration.getSMLSMPHostname ();

      try
      {
        final InetAddress aLocalHost = InetAddress.getLocalHost ();
        if (StringHelper.hasNoText (sPhysicalAddress))
          sPhysicalAddress = aLocalHost.getHostAddress ();
        if (StringHelper.hasNoText (sLogicalAddress))
          sLogicalAddress = "http://" + aLocalHost.getCanonicalHostName ();
      }
      catch (final UnknownHostException ex)
      {
        LOGGER.error ("Error determining localhost address", ex);
      }

      final BootstrapTabBox aTabBox = new BootstrapTabBox ();
      final Predicate <ISMLInfo> aSMLFilter = ISMLInfo::isClientCertificateRequired;

      // Register SMP at SML
      {
        final BootstrapForm aForm = getUIHandler ().createFormSelf (aWPEC);
        aForm.setEncTypeFileUpload ().setLeft (3);
        aForm.addChild (new BootstrapInfoBox ().addChild ("Register this SMP to the SML. This must only be done once per SMP!"));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("SML")
                                                     .setCtrl (new HCSMLSelect (new RequestField (FIELD_SML_ID,
                                                                                                  aDefaultSML != null ? aDefaultSML.getID ()
                                                                                                                      : null),
                                                                                aDisplayLocale,
                                                                                aSMLFilter))
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_SML_ID)));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("SMP ID")
                                                     .setCtrl (new HCEM ().addChild (sSMPID))
                                                     .setHelpText (HELPTEXT_SMP_ID));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Physical address")
                                                     .setCtrl (new HCEdit (new RequestField (FIELD_PHYSICAL_ADDRESS,
                                                                                             sPhysicalAddress)).setPlaceholder ("The IPv4 address of your SMP. E.g. 1.2.3.4"))
                                                     .setHelpText (HELPTEXT_PHYSICAL_ADDRESS)
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_PHYSICAL_ADDRESS)));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Logical address")
                                                     .setCtrl (new HCEdit (new RequestField (FIELD_LOGICAL_ADDRESS,
                                                                                             sLogicalAddress)).setPlaceholder ("The domain name of your SMP server. E.g. http://smp.example.org"))
                                                     .setHelpText (HELPTEXT_LOGICAL_ADDRESS)
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_LOGICAL_ADDRESS)));

        final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
        aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
        aToolbar.addHiddenField (CPageParam.PARAM_SUBACTION, SUBACTION_SMP_REGISTER);
        aToolbar.addSubmitButton ("Register SMP at SML");

        aTabBox.addTab ("register", "Register SMP to SML", aForm, aWPEC.hasSubAction (SUBACTION_SMP_REGISTER));
      }

      // Update SMP at SML
      {
        final BootstrapForm aForm = getUIHandler ().createFormSelf (aWPEC);
        aForm.setEncTypeFileUpload ().setLeft (3);
        aForm.addChild (new BootstrapInfoBox ().addChild ("Update this SMP at the SML. This must only be done when either the IP address or the host name of the SMP changed!"));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("SML")
                                                     .setCtrl (new HCSMLSelect (new RequestField (FIELD_SML_ID,
                                                                                                  aDefaultSML == null ? null
                                                                                                                      : aDefaultSML.getID ()),
                                                                                aDisplayLocale,
                                                                                aSMLFilter))
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_SML_ID)));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("SMP ID")
                                                     .setCtrl (new HCEM ().addChild (sSMPID))
                                                     .setHelpText (HELPTEXT_SMP_ID));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Physical address")
                                                     .setCtrl (new HCEdit (new RequestField (FIELD_PHYSICAL_ADDRESS,
                                                                                             sPhysicalAddress)).setPlaceholder ("The IPv4 address of your SMP. E.g. 1.2.3.4"))
                                                     .setHelpText (HELPTEXT_PHYSICAL_ADDRESS)
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_PHYSICAL_ADDRESS)));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Logical address")
                                                     .setCtrl (new HCEdit (new RequestField (FIELD_LOGICAL_ADDRESS,
                                                                                             sLogicalAddress)).setPlaceholder ("The domain name of your SMP server. E.g. http://smp.example.org"))
                                                     .setHelpText (HELPTEXT_LOGICAL_ADDRESS)
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_LOGICAL_ADDRESS)));

        final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
        aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
        aToolbar.addHiddenField (CPageParam.PARAM_SUBACTION, SUBACTION_SMP_UPDATE);
        aToolbar.addSubmitButton ("Update SMP at SML");

        aTabBox.addTab ("update", "Update SMP at SML", aForm, aWPEC.hasSubAction (SUBACTION_SMP_UPDATE));
      }

      // Delete SMP from SML
      {
        final BootstrapForm aForm = getUIHandler ().createFormSelf (aWPEC);
        aForm.setEncTypeFileUpload ().setLeft (3);
        aForm.addChild (new BootstrapInfoBox ().addChild ("Delete this SMP from the SML."));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("SML")
                                                     .setCtrl (new HCSMLSelect (new RequestField (FIELD_SML_ID,
                                                                                                  aDefaultSML == null ? null
                                                                                                                      : aDefaultSML.getID ()),
                                                                                aDisplayLocale,
                                                                                aSMLFilter))
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_SML_ID)));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("SMP ID")
                                                     .setCtrl (new HCEM ().addChild (sSMPID))
                                                     .setHelpText (HELPTEXT_SMP_ID));

        final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
        aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
        aToolbar.addHiddenField (CPageParam.PARAM_SUBACTION, SUBACTION_SMP_DELETE);
        aToolbar.addSubmitButton ("Delete SMP from SML");

        aTabBox.addTab ("delete", "Delete SMP from SML", aForm, aWPEC.hasSubAction (SUBACTION_SMP_DELETE));
      }

      aNodeList.addChild (aTabBox);
    }
  }
}
