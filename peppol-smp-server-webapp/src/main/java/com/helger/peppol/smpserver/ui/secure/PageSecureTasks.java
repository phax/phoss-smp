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

import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Locale;

import javax.annotation.Nonnull;

import org.joda.time.LocalDateTime;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.datetime.PDTFactory;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.servicegroup.ComparatorSMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.smlhook.RegistrationHookFactory;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPage;
import com.helger.peppol.utils.CertificateHelper;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.label.BootstrapLabel;
import com.helger.photon.bootstrap3.label.EBootstrapLabelType;
import com.helger.photon.uicore.css.CUICoreCSS;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public class PageSecureTasks extends AbstractSMPWebPage
{
  public PageSecureTasks (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Tasks/problems");
  }

  @Nonnull
  private static IHCNode _createError (@Nonnull final String sMsg)
  {
    return new BootstrapLabel (EBootstrapLabelType.DANGER).addChild (sMsg);
  }

  @Nonnull
  private static IHCNode _createWarning (@Nonnull final String sMsg)
  {
    return new BootstrapLabel (EBootstrapLabelType.WARNING).addChild (sMsg);
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
    final LocalDateTime aNowDT = PDTFactory.getCurrentLocalDateTime ();
    final LocalDateTime aNowPlusDT = aNowDT.plusMonths (3);

    aNodeList.addChild (new BootstrapInfoBox ().addChild ("This page tries to identify upcoming tasks and potential problems in the SMP configuration. It is meant to highlight immediate and upcoming action items as well as potential misconfiguration."));

    final HCOL aOL = new HCOL ();

    // check certificate configuration
    {
      final KeyLoadingResult aKeyLoadingResult = KeyLoadingResult.loadConfiguredKey ();
      if (aKeyLoadingResult.isFailure ())
        aOL.addItem (_createError ("Problem with the certificate configuration"), new HCDiv ().addChild (aKeyLoadingResult.getErrorMessage ()));
    }

    // Check SML configuration
    {
      if (!RegistrationHookFactory.isSMLConnectionActive ())
        aOL.addItem (_createWarning ("The connection to the SML is not active."),
                     new HCDiv ().addChild ("All creations and deletions of service groups needs to be repeated when the SML connection is active!"));
    }

    // check service groups and redirects
    {
      final Collection <? extends ISMPServiceGroup> aServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();
      final Collection <? extends ISMPRedirect> aRedirects = aRedirectMgr.getAllSMPRedirects ();
      if (aServiceGroups.isEmpty () && aRedirects.isEmpty ())
      {
        aOL.addItem (_createWarning ("Neither a service group nor a redirect is configured. This SMP is currently empty."));
      }
      else
      {
        // For all service groups
        for (final ISMPServiceGroup aServiceGroup : CollectionHelper.getSorted (aServiceGroups, new ComparatorSMPServiceGroup ()))
        {
          final HCUL aULPerSG = new HCUL ();
          final Collection <? extends ISMPServiceInformation> aServiceInfos = aServiceInfoMgr.getAllSMPServiceInformationsOfServiceGroup (aServiceGroup);
          if (aServiceInfos.isEmpty ())
          {
            aULPerSG.addItem (_createWarning ("No endpoint is configured for this service group."));
          }
          else
          {
            for (final ISMPServiceInformation aServiceInfo : aServiceInfos)
            {
              final HCUL aULPerDocType = new HCUL ();
              final Collection <? extends ISMPProcess> aProcesses = aServiceInfo.getAllProcesses ();
              for (final ISMPProcess aProcess : aProcesses)
              {
                final HCUL aULPerProcess = new HCUL ();
                final Collection <? extends ISMPEndpoint> aEndpoints = aProcess.getAllEndpoints ();
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
                                                              PDTToString.getAsString (aEndpoint.getServiceActivationDateTime (), aDisplayLocale) +
                                                              "."));
                  }

                  if (aEndpoint.getServiceExpirationDateTime () != null)
                  {
                    if (aEndpoint.getServiceExpirationDateTime ().isBefore (aNowDT))
                      aULPerEndpoint.addItem (_createError ("The endpoint is not longer active. It was valid until " +
                                                            PDTToString.getAsString (aEndpoint.getServiceExpirationDateTime (), aDisplayLocale) +
                                                            "."));
                    else
                      if (aEndpoint.getServiceExpirationDateTime ().isBefore (aNowPlusDT))
                        aULPerEndpoint.addItem (_createWarning ("The endpoint will be inactive soon. It is only valid until " +
                                                                PDTToString.getAsString (aEndpoint.getServiceExpirationDateTime (), aDisplayLocale) +
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
                                                                             .addChild (aProcess.getProcessIdentifier ().getURIEncoded ())),
                                         aULPerProcess);
              }
              // Show per document type errors
              if (aULPerDocType.hasChildren ())
                aULPerSG.addItem (new HCDiv ().addChild ("Document type ")
                                              .addChild (new HCCode ().addClass (CUICoreCSS.CSS_CLASS_NOWRAP)
                                                                      .addChild (aServiceInfo.getDocumentTypeIdentifier ().getURIEncoded ())),
                                  aULPerDocType);
            }
          }

          // Show per service group errors
          if (aULPerSG.hasChildren ())
            aOL.addItem (new HCDiv ().addChild ("Service group ")
                                     .addChild (new HCCode ().addChild (aServiceGroup.getParticpantIdentifier ().getURIEncoded ())),
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
