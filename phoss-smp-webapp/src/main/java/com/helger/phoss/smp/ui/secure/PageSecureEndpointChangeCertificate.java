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

import java.security.cert.X509Certificate;
import java.time.OffsetDateTime;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.base.compare.ESortOrder;
import com.helger.base.numeric.mutable.MutableInt;
import com.helger.base.state.EValidity;
import com.helger.base.state.IValidityIndicator;
import com.helger.base.string.StringHelper;
import com.helger.collection.CollectionHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.CommonsTreeSet;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.collection.commons.ICommonsSet;
import com.helger.collection.commons.ICommonsSortedSet;
import com.helger.datetime.helper.PDTFactory;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.forms.HCTextArea;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCEM;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.ui.CertificateUI;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPEndpoint;
import com.helger.phoss.smp.domain.serviceinfo.ISMPProcess;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.serviceinfo.SMPEndpoint;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
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
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.AbstractWebPageForm;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.security.certificate.CertificateDecodeHelper;
import com.helger.security.certificate.CertificateHelper;
import com.helger.text.ReadOnlyMultilingualText;
import com.helger.url.ISimpleURL;
import com.helger.web.scope.mgr.WebScoped;

import jakarta.annotation.Nullable;

public final class PageSecureEndpointChangeCertificate extends AbstractSMPWebPage
{
  /**
   * The async logic
   *
   * @author Philip Helger
   */
  private static final class BulkChangeCertificate extends AbstractLongRunningJobRunnable implements IHCBootstrap4Trait
  {
    private static final AtomicInteger RUNNING_JOBS = new AtomicInteger (0);

    private final Locale m_aDisplayLocale;
    private final String m_sOldUnifiedCert;
    private final String m_sNewCert;

    public BulkChangeCertificate (@NonNull final Locale aDisplayLocale,
                                  @NonNull final String sOldUnifiedCert,
                                  @NonNull final String sNewCert)
    {
      super ("BulkChangeCertificate",
             new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Bulk change certificate"));
      m_aDisplayLocale = aDisplayLocale;
      m_sOldUnifiedCert = sOldUnifiedCert;
      m_sNewCert = sNewCert;
    }

    @NonNull
    public LongRunningJobResult createLongRunningJobResult ()
    {
      RUNNING_JOBS.incrementAndGet ();
      try (final WebScoped w = new WebScoped ())
      {
        final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

        // Modify all endpoints
        final MutableInt aChangedEndpoints = new MutableInt (0);
        final MutableInt aSaveErrors = new MutableInt (0);
        final ICommonsList <ISMPServiceInformation> aChangedSIs = new CommonsArrayList <> ();
        aServiceInfoMgr.forEachSMPServiceInformation (aSI -> {
          boolean bChanged = false;
          for (final ISMPProcess aProcess : aSI.getAllProcesses ())
            for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
              if (m_sOldUnifiedCert.equals (_getUnifiedCert (aEndpoint.getCertificate ())))
              {
                bChanged = true;
                ((SMPEndpoint) aEndpoint).setCertificate (m_sNewCert);
                aChangedEndpoints.inc ();
              }
          if (bChanged)
          {
            // Remember and do not merge here to avoid deadlock
            aChangedSIs.add (aSI);
          }
        });

        // Write out of read-lock
        final ICommonsSortedSet <String> aChangedServiceGroup = new CommonsTreeSet <> ();
        for (final var aSI : aChangedSIs)
        {
          if (aServiceInfoMgr.mergeSMPServiceInformation (aSI).isFailure ())
            aSaveErrors.inc ();
          aChangedServiceGroup.add (aSI.getServiceGroupID ());
        }

        final IHCNode aRes;
        if (aChangedEndpoints.isGT0 ())
        {
          final HCUL aUL = new HCUL ();
          for (final String sChangedServiceGroupID : aChangedServiceGroup)
            aUL.addItem (sChangedServiceGroupID);

          final HCNodeList aNodes = new HCNodeList ().addChildren (div ("The old certificate was changed in " +
                                                                        aChangedEndpoints.intValue () +
                                                                        " endpoints to the new certificate:"),
                                                                   _getCertificateDisplay (m_sNewCert,
                                                                                           m_aDisplayLocale),
                                                                   div ("Effected service groups are:"),
                                                                   aUL);
          if (aSaveErrors.is0 ())
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
        RUNNING_JOBS.decrementAndGet ();
      }
    }

    @Nonnegative
    public static int getRunningJobCount ()
    {
      return RUNNING_JOBS.get ();
    }
  }

  private static final String FIELD_OLD_CERTIFICATE = "oldcert";
  private static final String FIELD_NEW_CERTIFICATE = "newcert";

  public PageSecureEndpointChangeCertificate (@NonNull @Nonempty final String sID)
  {
    super (sID, "Bulk change certificate");
  }

  @Override
  @NonNull
  protected IValidityIndicator isValidToDisplayPage (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    if (aServiceGroupMgr.getSMPServiceGroupCount () <= 0)
    {
      aNodeList.addChild (warn ("No service group is present! At least one service group must be present to change certificates."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new service group")
                                                .setOnClick (AbstractWebPageForm.createCreateURL (aWPEC,
                                                                                                  CMenuSecure.MENU_SERVICE_GROUPS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }
    return super.isValidToDisplayPage (aWPEC);
  }

  @Nullable
  private static String _getCertificateParsingError (@NonNull final String sCert)
  {
    X509Certificate aEndpointCert = null;
    try
    {
      aEndpointCert = new CertificateDecodeHelper ().source (sCert).pemEncoded (true).getDecodedOrThrow ();
    }
    catch (final Exception ex)
    {
      return ex.getMessage ();
    }
    return aEndpointCert != null ? null : "Invalid input string provided";
  }

  @NonNull
  private static IHCNode _getCertificateDisplay (@Nullable final String sCert, @NonNull final Locale aDisplayLocale)
  {
    final X509Certificate aEndpointCert = new CertificateDecodeHelper ().source (sCert)
                                                                        .pemEncoded (true)
                                                                        .getDecodedOrNull ();
    if (aEndpointCert == null)
    {
      final int nDisplayLen = 20;
      final int nCertLen = StringHelper.getLength (sCert);
      final String sCertPart = nCertLen > nDisplayLen ? sCert.substring (0, 20) + "..." : sCert;
      final HCDiv ret = new HCDiv ().addChild ("Invalid certificate")
                                    .addChild (nCertLen > nDisplayLen ? " starting with: " : ": ");
      if (nCertLen > 0)
        ret.addChild (new HCCode ().addChild (sCertPart));
      else
        ret.addChild (new HCEM ().addChild ("empty"));
      return ret;
    }

    final OffsetDateTime aNowODT = PDTFactory.getCurrentOffsetDateTime ();
    return CertificateUI.createCertificateDetailsTable (null, aEndpointCert, aNowODT, aDisplayLocale);
  }

  @NonNull
  private static String _getUnifiedCert (@Nullable final String s)
  {
    if (StringHelper.isEmpty (s))
      return "";

    // Trims, removes PEM header, removes spaces
    return CertificateHelper.getWithoutPEMHeader (s);
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    boolean bShowList = true;

    final ICommonsMap <String, ICommonsList <ISMPEndpoint>> aEndpointsGroupedPerURL = new CommonsHashMap <> ();
    final ICommonsMap <String, ICommonsSet <ISMPServiceGroup>> aServiceGroupsGroupedPerURL = new CommonsHashMap <> ();
    final MutableInt aTotalEndpointCount = new MutableInt (0);
    aServiceInfoMgr.forEachSMPServiceInformation (aSI -> {
      // Service Group needs to be resolved in here
      final ISMPServiceGroup aSG = aServiceGroupMgr.getSMPServiceGroupOfID (aSI.getServiceGroupParticipantIdentifier ());
      for (final ISMPProcess aProcess : aSI.getAllProcesses ())
        for (final ISMPEndpoint aEndpoint : aProcess.getAllEndpoints ())
        {
          final String sUnifiedCertificate = _getUnifiedCert (aEndpoint.getCertificate ());
          aEndpointsGroupedPerURL.computeIfAbsent (sUnifiedCertificate, k -> new CommonsArrayList <> ())
                                 .add (aEndpoint);
          aServiceGroupsGroupedPerURL.computeIfAbsent (sUnifiedCertificate, k -> new CommonsHashSet <> ()).add (aSG);
          aTotalEndpointCount.inc ();
        }
    });

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

      final String sOldUnifiedCert = _getUnifiedCert (aWPEC.params ().getAsStringTrimmed (FIELD_OLD_CERTIFICATE));

      if (aWPEC.hasSubAction (CPageParam.ACTION_SAVE))
      {
        final String sNewCert = aWPEC.params ().getAsStringTrimmed (FIELD_NEW_CERTIFICATE);
        final String sNewUnifiedCert = _getUnifiedCert (sNewCert);

        if (StringHelper.isEmpty (sOldUnifiedCert))
          aFormErrors.addFieldInfo (FIELD_OLD_CERTIFICATE, "An old certificate must be provided");
        else
        {
          final String sErrorDetails = _getCertificateParsingError (sOldUnifiedCert);
          if (sErrorDetails != null)
            aFormErrors.addFieldInfo (FIELD_OLD_CERTIFICATE, "The old certificate is invalid: " + sErrorDetails);
        }

        if (StringHelper.isEmpty (sNewUnifiedCert))
          aFormErrors.addFieldError (FIELD_NEW_CERTIFICATE, "A new certificate must be provided");
        else
        {
          final String sErrorDetails = _getCertificateParsingError (sNewUnifiedCert);
          if (sErrorDetails != null)
            aFormErrors.addFieldError (FIELD_NEW_CERTIFICATE, "The new certificate is invalid: " + sErrorDetails);
          else
            if (sNewUnifiedCert.equals (sOldUnifiedCert))
              aFormErrors.addFieldError (FIELD_NEW_CERTIFICATE,
                                         "The new certificate is identical to the old certificate");
        }

        // Validate parameters
        if (aFormErrors.containsNoError ())
        {
          PhotonWorkerPool.getInstance ()
                          .run ("BulkChangeCertificate",
                                new BulkChangeCertificate (aDisplayLocale, sOldUnifiedCert, sNewCert));

          aWPEC.postRedirectGetInternal (success ().addChildren (div ("The bulk change of the endpoint certificate to"),
                                                                 _getCertificateDisplay (sNewUnifiedCert,
                                                                                         aDisplayLocale),
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
      aForm.setLeft (-1, -1, 12, -1, 2);
      aForm.addChild (new HCHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_EDIT));
      aForm.addChild (new HCHiddenField (CPageParam.PARAM_SUBACTION, CPageParam.ACTION_SAVE));
      aForm.addChild (new HCHiddenField (FIELD_OLD_CERTIFICATE, sOldUnifiedCert));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Old certificate")
                                                   .setCtrl (_getCertificateDisplay (sOldUnifiedCert, aDisplayLocale))
                                                   .setHelpText ("The old certificate that is to be changed in all matching endpoints")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_OLD_CERTIFICATE)));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("New certificate")
                                                   .setCtrl (new HCTextArea (new RequestField (FIELD_NEW_CERTIFICATE,
                                                                                               sOldUnifiedCert)).setRows (CSMP.TEXT_AREA_CERT_ROWS))
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
                                                    (aTotalEndpointCount.intValue () == 1 ? "1 endpoint is"
                                                                                          : aTotalEndpointCount.intValue () +
                                                                                            " endpoints are") +
                                                    " registered.")));

      final HCTable aTable = new HCTable (new DTCol ("Certificate").setInitialSorting (ESortOrder.ASCENDING),
                                          new DTCol ("Service Group Count").setDisplayType (EDTColType.INT,
                                                                                            aDisplayLocale),
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
