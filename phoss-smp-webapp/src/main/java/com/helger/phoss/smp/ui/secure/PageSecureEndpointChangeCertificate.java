/**
 * Copyright (C) 2014-2021 Philip Helger and contributors
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

import java.security.cert.X509Certificate;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.collection.multimap.MultiHashMapArrayListBased;
import com.helger.collection.multimap.MultiHashMapHashSetBased;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.collection.impl.CommonsTreeSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.collection.impl.ICommonsSortedSet;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.text.ReadOnlyMultilingualText;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.forms.HCTextArea;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.photon.app.PhotonWorkerPool;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.traits.IHCBootstrap4Trait;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.core.longrun.LongRunningJobResult;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.AbstractWebPageForm;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.security.certificate.CertificateHelper;
import com.helger.web.scope.mgr.WebScoped;

public final class PageSecureEndpointChangeCertificate extends AbstractSMPWebPage
{
  /**
   * The async logic
   *
   * @author Philip Helger
   */
  private static final class BulkChangeCertificate extends AbstractLongRunningJobRunnable implements IHCBootstrap4Trait
  {
    private static final AtomicInteger s_aRunningJobs = new AtomicInteger (0);

    private final ICommonsList <ISMPServiceInformation> m_aAllSIs;
    private final Locale m_aDisplayLocale;
    private final String m_sOldUnifiedCert;
    private final String m_sNewCert;

    public BulkChangeCertificate (@Nonnull final ICommonsList <ISMPServiceInformation> aAllSIs,
                                  @Nonnull final Locale aDisplayLocale,
                                  @Nonnull final String sOldUnifiedCert,
                                  @Nonnull final String sNewCert)
    {
      super ("BulkChangeCertificate", new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Bulk change certificate"));
      m_aAllSIs = aAllSIs;
      m_aDisplayLocale = aDisplayLocale;
      m_sOldUnifiedCert = sOldUnifiedCert;
      m_sNewCert = sNewCert;
    }

    @Nonnull
    public LongRunningJobResult createLongRunningJobResult ()
    {
      s_aRunningJobs.incrementAndGet ();
      try (final WebScoped w = new WebScoped ())
      {
        final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

        // Modify all endpoints
        int nChangedEndpoints = 0;
        int nSaveErrors = 0;
        final ICommonsSortedSet <String> aChangedServiceGroup = new CommonsTreeSet <> ();
        for (final ISMPServiceInformation aSI : m_aAllSIs)
        {
          boolean bChanged = false;
          for (final ISMPProcess aProcess : aSI.getAllProcesses ())
            for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
              if (m_sOldUnifiedCert.equals (_getUnifiedCert (aEndpoint.getCertificate ())))
              {
                bChanged = true;
                ((SMPEndpoint) aEndpoint).setCertificate (m_sNewCert);
                ++nChangedEndpoints;
              }
          if (bChanged)
          {
            if (aServiceInfoMgr.mergeSMPServiceInformation (aSI).isFailure ())
              nSaveErrors++;
            aChangedServiceGroup.add (aSI.getServiceGroupID ());
          }
        }

        final IHCNode aRes;
        if (nChangedEndpoints > 0)
        {
          final HCUL aUL = new HCUL ();
          for (final String sChangedServiceGroupID : aChangedServiceGroup)
            aUL.addItem (sChangedServiceGroupID);

          final HCNodeList aNodes = new HCNodeList ().addChildren (div ("The old certificate was changed in " +
                                                                        nChangedEndpoints +
                                                                        " endpoints to the new certificate:"),
                                                                   _getCertificateDisplay (m_sNewCert, m_aDisplayLocale),
                                                                   div ("Effected service groups are:"),
                                                                   aUL);
          if (nSaveErrors == 0)
            aRes = success (aNodes);
          else
          {
            aNodes.addChildAt (0, h3 ("Some changes could NOT be saved! Please check the logs!"));
            aRes = error (aNodes);
          }
        }
        else
          aRes = warn ("No endpoint was found that contains the old certificate");

        return LongRunningJobResult.createXML (aRes);
      }
      finally
      {
        s_aRunningJobs.decrementAndGet ();
      }
    }

    @Nonnegative
    public static int getRunningJobCount ()
    {
      return s_aRunningJobs.get ();
    }
  }

  private static final String FIELD_OLD_CERTIFICATE = "oldcert";
  private static final String FIELD_NEW_CERTIFICATE = "newcert";

  public PageSecureEndpointChangeCertificate (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Bulk change certificate");
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    if (aServiceGroupMgr.getSMPServiceGroupCount () == 0)
    {
      aNodeList.addChild (warn ("No service group is present! At least one service group must be present to change certificates."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new service group")
                                                .setOnClick (AbstractWebPageForm.createCreateURL (aWPEC, CMenuSecure.MENU_SERVICE_GROUPS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }
    return super.isValidToDisplayPage (aWPEC);
  }

  @Nullable
  private static String _getCertificateParsingError (@Nonnull final String sCert)
  {
    X509Certificate aEndpointCert = null;
    try
    {
      aEndpointCert = CertificateHelper.convertStringToCertficate (sCert);
    }
    catch (final Exception ex)
    {
      return ex.getMessage ();
    }
    return aEndpointCert != null ? null : "Invalid input string provided";
  }

  @Nonnull
  private static IHCNode _getCertificateDisplay (@Nullable final String sCert, @Nonnull final Locale aDisplayLocale)
  {
    X509Certificate aEndpointCert = null;
    try
    {
      aEndpointCert = CertificateHelper.convertStringToCertficate (sCert);
    }
    catch (final Exception ex)
    {
      // Ignore
    }
    if (aEndpointCert == null)
    {
      final int nDisplayLen = 20;
      final String sCertPart = (sCert.length () > nDisplayLen ? sCert.substring (0, 20) + "..." : sCert);
      return new HCDiv ().addChild ("Invalid certificate" + (sCert.length () > nDisplayLen ? " starting with: " : ": "))
                         .addChild (new HCCode ().addChild (sCertPart));
    }

    final HCNodeList ret = new HCNodeList ();
    ret.addChild (new HCDiv ().addChild ("Issuer: " + aEndpointCert.getIssuerX500Principal ().toString ()));
    ret.addChild (new HCDiv ().addChild ("Subject: " + aEndpointCert.getSubjectX500Principal ().toString ()));

    final LocalDateTime aNotBefore = PDTFactory.createLocalDateTime (aEndpointCert.getNotBefore ());
    ret.addChild (new HCDiv ().addChild ("Not before: " + PDTToString.getAsString (aNotBefore, aDisplayLocale)));

    final LocalDateTime aNotAfter = PDTFactory.createLocalDateTime (aEndpointCert.getNotAfter ());
    ret.addChild (new HCDiv ().addChild ("Not after: " + PDTToString.getAsString (aNotAfter, aDisplayLocale)));

    return ret;
  }

  @Nonnull
  private static String _getUnifiedCert (@Nullable final String s)
  {
    if (StringHelper.hasNoText (s))
      return "";

    // Trims, removes PEM header, removes spaces
    return CertificateHelper.getWithoutPEMHeader (s);
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    boolean bShowList = true;

    final MultiHashMapArrayListBased <String, ISMPEndpoint> aEndpointsGroupedPerURL = new MultiHashMapArrayListBased <> ();
    final MultiHashMapHashSetBased <String, ISMPServiceGroup> aServiceGroupsGroupedPerURL = new MultiHashMapHashSetBased <> ();
    final ICommonsList <ISMPServiceInformation> aAllSIs = aServiceInfoMgr.getAllSMPServiceInformation ();
    int nTotalEndpointCount = 0;
    for (final ISMPServiceInformation aSI : aAllSIs)
    {
      final ISMPServiceGroup aSG = aSI.getServiceGroup ();
      for (final ISMPProcess aProcess : aSI.getAllProcesses ())
        for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        {
          final String sUnifiedCertificate = _getUnifiedCert (aEndpoint.getCertificate ());
          aEndpointsGroupedPerURL.putSingle (sUnifiedCertificate, aEndpoint);
          aServiceGroupsGroupedPerURL.putSingle (sUnifiedCertificate, aSG);
          ++nTotalEndpointCount;
        }
    }

    {
      final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
      aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
      aNodeList.addChild (aToolbar);

      final int nCount = BulkChangeCertificate.getRunningJobCount ();
      if (nCount > 0)
      {
        aNodeList.addChild (warn ((nCount == 1 ? "1 bulk change is" : nCount + " bulk changes are") +
                                  " currently running in the background"));
      }
    }

    if (aWPEC.hasAction (CPageParam.ACTION_EDIT))
    {
      bShowList = false;
      final FormErrorList aFormErrors = new FormErrorList ();

      final String sOldUnifiedCert = _getUnifiedCert (aWPEC.params ().getAsString (FIELD_OLD_CERTIFICATE));

      if (aWPEC.hasSubAction (CPageParam.ACTION_SAVE))
      {
        final String sNewCert = aWPEC.params ().getAsString (FIELD_NEW_CERTIFICATE);
        final String sNewUnifiedCert = _getUnifiedCert (sNewCert);

        if (StringHelper.hasNoText (sOldUnifiedCert))
          aFormErrors.addFieldInfo (FIELD_OLD_CERTIFICATE, "An old certificate must be provided");
        else
        {
          final String sErrorDetails = _getCertificateParsingError (sOldUnifiedCert);
          if (sErrorDetails != null)
            aFormErrors.addFieldInfo (FIELD_OLD_CERTIFICATE, "The old certificate is invalid: " + sErrorDetails);
        }

        if (StringHelper.hasNoText (sNewUnifiedCert))
          aFormErrors.addFieldError (FIELD_NEW_CERTIFICATE, "A new certificate must be provided");
        else
        {
          final String sErrorDetails = _getCertificateParsingError (sNewUnifiedCert);
          if (sErrorDetails != null)
            aFormErrors.addFieldError (FIELD_NEW_CERTIFICATE, "The new certificate is invalid: " + sErrorDetails);
          else
            if (sNewUnifiedCert.equals (sOldUnifiedCert))
              aFormErrors.addFieldError (FIELD_NEW_CERTIFICATE, "The new certificate is identical to the old certificate");
        }

        // Validate parameters
        if (aFormErrors.containsNoError ())
        {
          PhotonWorkerPool.getInstance ()
                          .run ("BulkChangeCertificate", new BulkChangeCertificate (aAllSIs, aDisplayLocale, sOldUnifiedCert, sNewCert));

          aWPEC.postRedirectGetInternal (success ().addChildren (div ("The bulk change of the endpoint certificate to"),
                                                                 _getCertificateDisplay (sNewUnifiedCert, aDisplayLocale),
                                                                 div ("is now running in the background. Please manually refresh the page to see the update.")));
        }
      }

      final ICommonsSet <ISMPServiceGroup> aServiceGroups = aServiceGroupsGroupedPerURL.get (sOldUnifiedCert);
      final int nSGCount = CollectionHelper.getSize (aServiceGroups);
      final int nEPCount = CollectionHelper.getSize (aEndpointsGroupedPerURL.get (sOldUnifiedCert));
      aNodeList.addChild (info ("The selected old certificate is currently used in " +
                                nEPCount +
                                " " +
                                (nEPCount == 1 ? "endpoint" : "endpoints") +
                                " of " +
                                nSGCount +
                                " " +
                                (nSGCount == 1 ? "service group" : "service groups") +
                                "."));

      // Show edit screen
      final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormSelf (aWPEC));
      aForm.addChild (new HCHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_EDIT));
      aForm.addChild (new HCHiddenField (CPageParam.PARAM_SUBACTION, CPageParam.ACTION_SAVE));
      aForm.addChild (new HCHiddenField (FIELD_OLD_CERTIFICATE, sOldUnifiedCert));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Old certificate")
                                                   .setCtrl (_getCertificateDisplay (sOldUnifiedCert, aDisplayLocale))
                                                   .setHelpText ("The old certificate that is to be changed in all matching endpoints")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_OLD_CERTIFICATE)));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("New certificate")
                                                   .setCtrl (new HCTextArea (new RequestField (FIELD_NEW_CERTIFICATE,
                                                                                               sOldUnifiedCert)).setRows (10))
                                                   .setHelpText ("The new certificate that is used instead")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_NEW_CERTIFICATE)));

      final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
      aToolbar.addSubmitButton ("Save changes", EDefaultIcon.SAVE);
      aToolbar.addButtonCancel (aDisplayLocale);
    }

    if (bShowList)
    {
      aNodeList.addChild (info ().addChildren (div ("This page lets you change the certificates of multiple endpoints at once. This is e.g. helpful when the old certificate expired."),
                                               div ("Currently " +
                                                    (nTotalEndpointCount == 1 ? "1 endpoint is" : nTotalEndpointCount + " endpoints are") +
                                                    " registered.")));

      final HCTable aTable = new HCTable (new DTCol ("Certificate").setInitialSorting (ESortOrder.ASCENDING),
                                          new DTCol ("Service Group Count").setDisplayType (EDTColType.INT, aDisplayLocale),
                                          new DTCol ("Endpoint Count").setDisplayType (EDTColType.INT, aDisplayLocale),
                                          new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
      aEndpointsGroupedPerURL.forEach ( (sCert, aEndpoints) -> {
        final HCRow aRow = aTable.addBodyRow ();
        aRow.addCell (_getCertificateDisplay (sCert, aDisplayLocale));

        final int nSGCount = CollectionHelper.getSize (aServiceGroupsGroupedPerURL.get (sCert));
        aRow.addCell (Integer.toString (nSGCount));

        aRow.addCell (Integer.toString (aEndpoints.size ()));

        final ISimpleURL aEditURL = aWPEC.getSelfHref ()
                                         .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_EDIT)
                                         .add (FIELD_OLD_CERTIFICATE, sCert);
        aRow.addCell (new HCA (aEditURL).setTitle ("Change all endpoints using this certificate")
                                        .addChild (EDefaultIcon.EDIT.getAsNode ()));
      });

      final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
      aNodeList.addChild (aTable).addChild (aDataTables);
    }
  }
}
