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
import java.time.LocalDateTime;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.string.StringHelper;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.pd.client.PDClientConfiguration;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smpserver.app.AppConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.helger.peppol.smpserver.security.SMPTrustManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPage;
import com.helger.peppol.utils.PeppolKeyStoreHelper;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.label.BootstrapLabel;
import com.helger.photon.bootstrap3.label.EBootstrapLabelType;
import com.helger.photon.security.CSecurity;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.uicore.css.CUICoreCSS;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.security.certificate.CertificateHelper;
import com.helger.security.keystore.EKeyStoreLoadError;
import com.helger.security.keystore.KeyStoreHelper;
import com.helger.security.keystore.LoadedKeyStore;

public class PageSecureTasks extends AbstractSMPWebPage
{
  public PageSecureTasks (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Tasks/problems");
  }

  @Nonnull
  private static IHCNode _createInfo (@Nonnull final String sMsg)
  {
    return new BootstrapLabel (EBootstrapLabelType.INFO).addChild ("Information: " + sMsg);
  }

  @Nonnull
  private static IHCNode _createWarning (@Nonnull final String sMsg)
  {
    return new BootstrapLabel (EBootstrapLabelType.WARNING).addChild ("Warning: " + sMsg);
  }

  @Nonnull
  private static IHCNode _createError (@Nonnull final String sMsg)
  {
    return new BootstrapLabel (EBootstrapLabelType.DANGER).addChild ("Error: " + sMsg);
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final LocalDateTime aNowDT = PDTFactory.getCurrentLocalDateTime ();
    final LocalDateTime aNowPlusDT = aNowDT.plusMonths (3);

    aNodeList.addChild (new BootstrapInfoBox ().addChild ("This page tries to identify upcoming tasks and potential problems in the SMP configuration. It is meant to highlight immediate and upcoming action items as well as potential misconfiguration."));

    final HCOL aOL = new HCOL ();

    // Check for default password
    if (PhotonSecurityManager.getUserMgr ().areUserIDAndPasswordValid (CSecurity.USER_ADMINISTRATOR_ID,
                                                                       CSecurity.USER_ADMINISTRATOR_PASSWORD))
    {
      aOL.addItem (_createError ("Please change the password of the default user " +
                                 CSecurity.USER_ADMINISTRATOR_EMAIL +
                                 "!"));
    }

    // check keystore configuration
    {
      if (!SMPKeyManager.isCertificateValid ())
        aOL.addItem (_createError ("Problem with the certificate configuration"),
                     new HCDiv ().addChild (SMPKeyManager.getInitializationError ()));
    }

    // check truststore configuration
    {
      if (!SMPTrustManager.isCertificateValid ())
      {
        // Ignore error if no trust store is configured
        if (SMPTrustManager.getInitializationErrorCode () != EKeyStoreLoadError.KEYSTORE_NO_PATH)
          aOL.addItem (_createWarning ("Problem with the truststore configuration"),
                       new HCDiv ().addChild (SMPTrustManager.getInitializationError ()));
      }
    }

    // Check SML configuration
    {
      if (!SMPMetaManager.getSettings ().isSMLActive ())
      {
        // Warn only if SML is needed
        if (SMPMetaManager.getSettings ().isSMLNeeded ())
          aOL.addItem (_createWarning ("The connection to the SML is not active."),
                       new HCDiv ().addChild ("All creations and deletions of service groups needs to be repeated when the SML connection is active!"));
      }
    }

    if (SMPMetaManager.getSettings ().isPEPPOLDirectoryIntegrationEnabled ())
    {
      final String sDirectoryName = AppConfiguration.getDirectoryName ();
      if (StringHelper.hasNoText (SMPMetaManager.getSettings ().getPEPPOLDirectoryHostName ()))
        aOL.addItem (_createError ("An empty " + sDirectoryName + " hostname is provided"),
                     new HCDiv ().addChild ("A connection to the " + sDirectoryName + " server cannot be establised!"));

      final LoadedKeyStore aLoadedKeyStore = KeyStoreHelper.loadKeyStore (PDClientConfiguration.getKeyStoreType (),
                                                                          PDClientConfiguration.getKeyStorePath (),
                                                                          PDClientConfiguration.getKeyStorePassword ());
      if (aLoadedKeyStore.isFailure ())
        aOL.addItem (_createError ("The " + sDirectoryName + " client certificate configuration is invalid."),
                     new HCDiv ().addChild (PeppolKeyStoreHelper.getLoadError (aLoadedKeyStore)));
    }

    // check service groups and redirects
    {
      final ICommonsList <ISMPServiceGroup> aServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();
      if (aServiceGroups.isEmpty ())
      {
        aOL.addItem (_createWarning ("No service group is configured. This SMP is currently empty."));
      }
      else
      {
        // For all service groups
        for (final ISMPServiceGroup aServiceGroup : CollectionHelper.getSorted (aServiceGroups,
                                                                                ISMPServiceGroup.comparator ()))
        {
          final HCUL aULPerSG = new HCUL ();
          final ICommonsList <ISMPServiceInformation> aServiceInfos = aServiceInfoMgr.getAllSMPServiceInformationOfServiceGroup (aServiceGroup);
          if (aServiceInfos.isEmpty ())
          {
            // This is merely a warning or an error
            aULPerSG.addItem (_createInfo ("No endpoint is configured for this service group."));
          }
          else
          {
            for (final ISMPServiceInformation aServiceInfo : aServiceInfos)
            {
              final HCUL aULPerDocType = new HCUL ();
              final ICommonsList <ISMPProcess> aProcesses = aServiceInfo.getAllProcesses ();
              for (final ISMPProcess aProcess : aProcesses)
              {
                final HCUL aULPerProcess = new HCUL ();
                final ICommonsList <ISMPEndpoint> aEndpoints = aProcess.getAllEndpoints ();
                for (final ISMPEndpoint aEndpoint : aEndpoints)
                {
                  final HCUL aULPerEndpoint = new HCUL ();

                  final ESMPTransportProfile eTransportProfile = ESMPTransportProfile.getFromIDOrNull (aEndpoint.getTransportProfile ());
                  if (eTransportProfile == null)
                    aULPerEndpoint.addItem (_createWarning ("The endpoint uses the non-standard transport profile '" +
                                                            aEndpoint.getTransportProfile () +
                                                            "'."));

                  if (aEndpoint.getServiceActivationDateTime () != null)
                  {
                    if (aEndpoint.getServiceActivationDateTime ().isAfter (aNowDT))
                      aULPerEndpoint.addItem (_createWarning ("The endpoint is not yet active. It will be active from " +
                                                              PDTToString.getAsString (aEndpoint.getServiceActivationDateTime (),
                                                                                       aDisplayLocale) +
                                                              "."));
                  }

                  if (aEndpoint.getServiceExpirationDateTime () != null)
                  {
                    if (aEndpoint.getServiceExpirationDateTime ().isBefore (aNowDT))
                      aULPerEndpoint.addItem (_createError ("The endpoint is not longer active. It was valid until " +
                                                            PDTToString.getAsString (aEndpoint.getServiceExpirationDateTime (),
                                                                                     aDisplayLocale) +
                                                            "."));
                    else
                      if (aEndpoint.getServiceExpirationDateTime ().isBefore (aNowPlusDT))
                        aULPerEndpoint.addItem (_createWarning ("The endpoint will be inactive soon. It is only valid until " +
                                                                PDTToString.getAsString (aEndpoint.getServiceExpirationDateTime (),
                                                                                         aDisplayLocale) +
                                                                "."));
                  }

                  X509Certificate aX509Cert = null;
                  try
                  {
                    aX509Cert = CertificateHelper.convertStringToCertficate (aEndpoint.getCertificate ());
                  }
                  catch (final CertificateException ex)
                  {
                    // Ignore
                  }
                  if (aX509Cert == null)
                    aULPerEndpoint.addItem (_createError ("The X.509 certificate configured at the endpoint is invalid and could not be interpreted as a certificate."));
                  else
                  {
                    final LocalDateTime aNotBefore = PDTFactory.createLocalDateTime (aX509Cert.getNotBefore ());
                    if (aNotBefore.isAfter (aNowDT))
                      aULPerEndpoint.addItem (_createError ("The endpoint certificate is not yet active. It will be active from " +
                                                            PDTToString.getAsString (aNotBefore, aDisplayLocale) +
                                                            "."));

                    final LocalDateTime aNotAfter = PDTFactory.createLocalDateTime (aX509Cert.getNotAfter ());
                    if (aNotAfter.isBefore (aNowDT))
                      aULPerEndpoint.addItem (_createError ("The endpoint certificate is already expired. It was valid until " +
                                                            PDTToString.getAsString (aNotAfter, aDisplayLocale) +
                                                            "."));
                    else
                      if (aNotAfter.isBefore (aNowPlusDT))
                        aULPerEndpoint.addItem (_createWarning ("The endpoint certificate will expire soon. It is only valid until " +
                                                                PDTToString.getAsString (aNotAfter, aDisplayLocale) +
                                                                "."));
                  }

                  // Show per endpoint errors
                  if (aULPerEndpoint.hasChildren ())
                    aULPerProcess.addItem (new HCDiv ().addChild ("Transport profile ")
                                                       .addChild (new HCCode ().addChild (aEndpoint.getTransportProfile ())),
                                           aULPerEndpoint);
                }
                // Show per process errors
                if (aULPerProcess.hasChildren ())
                  aULPerDocType.addItem (new HCDiv ().addChild ("Process ")
                                                     .addChild (new HCCode ().addClass (CUICoreCSS.CSS_CLASS_NOWRAP)
                                                                             .addChild (aProcess.getProcessIdentifier ()
                                                                                                .getURIEncoded ())),
                                         aULPerProcess);
              }
              // Show per document type errors
              if (aULPerDocType.hasChildren ())
                aULPerSG.addItem (new HCDiv ().addChild ("Document type ")
                                              .addChild (new HCCode ().addClass (CUICoreCSS.CSS_CLASS_NOWRAP)
                                                                      .addChild (aServiceInfo.getDocumentTypeIdentifier ()
                                                                                             .getURIEncoded ())),
                                  aULPerDocType);
            }
          }

          // Show per service group errors
          if (aULPerSG.hasChildren ())
            aOL.addItem (new HCDiv ().addChild ("Service group ")
                                     .addChild (new HCCode ().addChild (aServiceGroup.getParticpantIdentifier ()
                                                                                     .getURIEncoded ())),
                         aULPerSG);
        }
      }
    }

    // Show results
    if (aOL.hasChildren ())
    {
      aNodeList.addChild (new BootstrapWarnBox ().addChild ("The following list of tasks and problems were identified:"));
      aNodeList.addChild (aOL);
    }
    else
      aNodeList.addChild (new BootstrapSuccessBox ().addChild ("Great job, no tasks or problems identified!"));
  }
}
