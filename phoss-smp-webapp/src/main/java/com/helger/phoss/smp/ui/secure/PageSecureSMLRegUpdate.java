/*
 * Copyright (C) 2014-2024 Philip Helger and contributors
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

import java.net.InetAddress;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.Locale;
import java.util.function.Predicate;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.string.StringHelper;
import com.helger.commons.string.StringParser;
import com.helger.commons.text.util.TextHelper;
import com.helger.commons.url.URLHelper;
import com.helger.dns.ip.IPV4Addr;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.ManageServiceMetadataServiceCaller;
import com.helger.phoss.smp.ESMPRESTType;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.secure.hc.HCSMLSelect;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.pages.BootstrapWebPageUIHandler;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public class PageSecureSMLRegUpdate extends AbstractPageSecureSMLReg
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureSMLRegUpdate.class);

  private static final String FIELD_SML_ID = "sml";
  private static final String FIELD_PHYSICAL_ADDRESS = "physicaladdr";
  private static final String FIELD_LOGICAL_ADDRESS = "logicaladdr";

  public PageSecureSMLRegUpdate (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Update SML registration");
  }

  private void _updateSMPatSML (@Nonnull final WebPageExecutionContext aWPEC, @Nonnull final FormErrorList aFormErrors)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ESMPRESTType eRESTType = SMPServerConfiguration.getRESTType ();
    final boolean bUseHttpConstraints = eRESTType.isHttpConstraint ();
    final boolean bUsePort80Constraints = eRESTType.isPort80Constraint ();
    final boolean bUsePathConstraints = eRESTType.isPathConstraint ();
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
                                     "The provided IP address does not resolve to a valid host. " +
                                                             SMPCommonUI.getTechnicalDetailsString (ex));
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
        {
          if (bUseHttpConstraints || !"https".equals (aURL.getProtocol ()))
            aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                       "The provided logical address must use the 'http'" +
                                                              (bUseHttpConstraints ? "" : " or the 'https'") +
                                                              " protocol and may not use the '" +
                                                              aURL.getProtocol () +
                                                              "' protocol." +
                                                              (bUseHttpConstraints ? " According to the underlying SMP specification, no other protocols than 'http' are allowed!"
                                                                                   : ""));
        }
        if (bUsePort80Constraints)
        {
          // -1 means default port
          if (aURL.getPort () != 80 && aURL.getPort () != -1)
            aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                       "The provided logical address must use the default http port 80 and not port " +
                                                              aURL.getPort () +
                                                              ". According to the underlying SMP specification, no other ports are allowed!");
        }
        if (bUsePathConstraints)
        {
          if (StringHelper.hasText (aURL.getPath ()) && !"/".equals (aURL.getPath ()))
            aFormErrors.addFieldError (FIELD_LOGICAL_ADDRESS,
                                       "The provided logical address may not contain a path (" +
                                                              aURL.getPath () +
                                                              ") because according to the SMP specifications it must run in the root (/) path!");
        }
      }
    }

    if (aFormErrors.isEmpty ())
    {
      final String sSMPID = SMPServerConfiguration.getSMLSMPID ();
      try
      {
        final SSLSocketFactory aSocketFactory = SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ();
        final ManageServiceMetadataServiceCaller aCaller = createSMLCaller (aSMLInfo, aSocketFactory);
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
        aNodeList.addChild (success (sMsg));
        AuditHelper.onAuditExecuteSuccess ("smp-sml-update",
                                           sSMPID,
                                           sPhysicalAddress,
                                           sLogicalAddress,
                                           aSMLInfo.getManagementServiceURL ());
      }
      catch (final Exception ex)
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
        aNodeList.addChild (error (sMsg).addChild (SMPCommonUI.getTechnicalDetailsUI (ex)));
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

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    if (!canShowPage (aWPEC))
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
        aNodeList.addChild (success (div ("An SMP is already registered at the configured SML using the DNS name '" +
                                          sPublisherDNSName +
                                          "'. The determined IP address is " +
                                          aIA.getHostAddress ())).addChild (div ("Note: this can be a different machine than this one, if another SMP uses the same ID as this one (" +
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
      _updateSMPatSML (aWPEC, aFormErrors);
    }

    if (bShowInput)
    {
      // Get default from configuration
      final String sPhysicalAddress = SMPServerConfiguration.getSMLSMPIP ();
      final String sLogicalAddress = SMPServerConfiguration.getSMLSMPHostname ();
      String sDefaultPhysicalAddress = "";
      String sDefaultLogicalAddress = "";

      try
      {
        final InetAddress aLocalHost = InetAddress.getLocalHost ();
        sDefaultPhysicalAddress = aLocalHost.getHostAddress ();
        sDefaultLogicalAddress = "http://" + aLocalHost.getCanonicalHostName ();
      }
      catch (final UnknownHostException ex)
      {
        LOGGER.error ("Error determining localhost address", ex);
      }

      final Predicate <ISMLInfo> aSMLFilter = ISMLInfo::isClientCertificateRequired;

      // Update SMP at SML
      {
        final BootstrapForm aForm = getUIHandler ().createFormSelf (aWPEC).setLeft (3);
        aForm.addChild (info ("Update this SMP at the SML. This must only be done when either the IP address or the host name of the SMP changed!"));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("SML")
                                                     .setCtrl (new HCSMLSelect (new RequestField (FIELD_SML_ID,
                                                                                                  aDefaultSML == null ? null
                                                                                                                      : aDefaultSML.getID ()),
                                                                                aDisplayLocale,
                                                                                aSMLFilter))
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_SML_ID)));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("SMP ID")
                                                     .setCtrl (em (sSMPID))
                                                     .setHelpText (HELPTEXT_SMP_ID));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Physical address")
                                                     .setCtrl (new HCEdit (new RequestField (FIELD_PHYSICAL_ADDRESS,
                                                                                             sPhysicalAddress)).setPlaceholder ("The IPv4 address of your SMP. E.g. 1.2.3.4"))
                                                     .setHelpText (TextHelper.getFormattedText (HELPTEXT_PHYSICAL_ADDRESS,
                                                                                                sDefaultPhysicalAddress))
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_PHYSICAL_ADDRESS)));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Logical address")
                                                     .setCtrl (new HCEdit (new RequestField (FIELD_LOGICAL_ADDRESS,
                                                                                             sLogicalAddress)).setPlaceholder ("The domain name of your SMP server. E.g. http://smp.example.org"))
                                                     .setHelpText (TextHelper.getFormattedText (HELPTEXT_LOGICAL_ADDRESS,
                                                                                                sDefaultLogicalAddress))
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_LOGICAL_ADDRESS)));

        final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
        aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
        aToolbar.addSubmitButton ("Update SMP at SML");

        aNodeList.addChild (aForm);
      }
    }
  }
}
