/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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

import java.util.Comparator;
import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.ESortOrder;
import com.helger.base.state.EValidity;
import com.helger.base.state.IValidityIndicator;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsMap;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.forms.HCSelect;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.render.HCRenderer;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.IEndpointUsageInfo;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.restapi.cache.SMPRestResponseCache;
import com.helger.phoss.smp.rest.SMPBulkChangeLocks;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.form.BootstrapForm;
import com.helger.photon.bootstrap5.form.BootstrapFormGroup;
import com.helger.photon.bootstrap5.traits.IHCBootstrap5Trait;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.AbstractWebPageForm;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.text.ReadOnlyMultilingualText;
import com.helger.url.ISimpleURL;
import com.helger.url.validate.URLValidator;
import com.helger.web.scope.mgr.WebScoped;

import jakarta.annotation.Nullable;

public final class PageSecureEndpointChangeTechnicalContactURL extends AbstractSMPWebPage
{
  /**
   * The async logic
   *
   * @author Philip Helger
   */
  private static final class BulkChangeEndpointTechnicalContactURL extends AbstractLongRunningJobRunnable implements IHCBootstrap5Trait
  {
    private final IParticipantIdentifier m_aServiceGroupPID;
    private final String m_sOldURL;
    private final String m_sNewURL;

    public BulkChangeEndpointTechnicalContactURL (@Nullable final IParticipantIdentifier aServiceGroupPID,
                                  @NonNull @Nonempty final String sOldURL,
                                  @NonNull @Nonempty final String sNewURL,
                                  @Nullable final String sUserID)
    {
      super ("BulkChangeEndpointTechnicalContactURL",
             new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Bulk change endpoint technical contact URL"),
             () -> sUserID);
      m_aServiceGroupPID = aServiceGroupPID;
      m_sOldURL = sOldURL;
      m_sNewURL = sNewURL;
    }

    @NonNull
    public LongRunningJobResult createLongRunningJobResult ()
    {
      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

      final long nEndpointsChanged = aServiceInfoMgr.updateAllEndpointTechnicalContactUrls (m_aServiceGroupPID, m_sOldURL, m_sNewURL);
      if (nEndpointsChanged > 0)
      {
        // The bulk update does not trigger the callbacks and affects an unknown number of participants
        SMPRestResponseCache.invalidateAll ();
      }

      final IHCNode aRes;
      if (nEndpointsChanged > 0)
        aRes = success (div ("The old technical contact URL '" +
                             m_sOldURL +
                             "' was changed to '" +
                             m_sNewURL +
                             "' in " +
                             nEndpointsChanged +
                             " " +
                             (nEndpointsChanged == 1 ? "endpoint" : "endpoints") +
                             "."));
      else
        aRes = warn ("No endpoint was found that contains the old technical contact URL '" + m_sOldURL + "'");

      return LongRunningJobResult.createXML (HCRenderer.getAsNode (aRes));
    }

    @Override
    public void run ()
    {
      try (final WebScoped w = new WebScoped ())
      {
        super.run ();
      }
      finally
      {
        // Always release, even if the job failed
        SMPBulkChangeLocks.ENDPOINT_TECHNICAL_CONTACT_URL.release ();
      }
    }
  }

  private static final String SERVICE_GROUP_ALL = "all";
  private static final String FIELD_SERVICE_GROUP = "servicegroup";
  private static final String FIELD_OLD_URL = "oldurl";
  private static final String FIELD_NEW_URL = "newurl";

  public PageSecureEndpointChangeTechnicalContactURL (@NonNull @Nonempty final String sID)
  {
    super (sID, "Bulk change technical contact URL");
  }

  @Override
  @NonNull
  protected IValidityIndicator isValidToDisplayPage (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    if (!aServiceGroupMgr.containsAnySMPServiceGroup ())
    {
      aNodeList.addChild (warn ("No service group is present! At least one service group must be present to change endpoints."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new service group")
                                                .setOnClick (AbstractWebPageForm.createCreateURL (aWPEC,
                                                                                                  CMenuSecure.MENU_SERVICE_GROUPS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }
    return super.isValidToDisplayPage (aWPEC);
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    boolean bShowList = true;

    // Init lazily
    ICommonsMap <String, IEndpointUsageInfo> aURLUsage = null;

    {
      final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
      aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
      aNodeList.addChild (aToolbar);

      if (SMPBulkChangeLocks.ENDPOINT_TECHNICAL_CONTACT_URL.isRunning ())
        aNodeList.addChild (warn ("A bulk change is currently running in the background"));
    }

    if (aWPEC.hasAction (CPageParam.ACTION_EDIT))
    {
      bShowList = false;
      final FormErrorList aFormErrors = new FormErrorList ();

      final String sOldURL = aWPEC.params ().getAsStringTrimmed (FIELD_OLD_URL);

      if (aWPEC.hasSubAction (CPageParam.ACTION_SAVE))
      {
        // Find selected service group (if any)
        final String sServiceGroupID = aWPEC.params ().getAsStringTrimmed (FIELD_SERVICE_GROUP);
        ISMPServiceGroup aSelectedServiceGroup = null;
        if (StringHelper.isNotEmpty (sServiceGroupID))
        {
          final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
          if (aParticipantID != null)
            aSelectedServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aParticipantID);
        }

        final String sNewURL = aWPEC.params ().getAsStringTrimmed (FIELD_NEW_URL);

        if (StringHelper.isEmpty (sOldURL))
          aFormErrors.addFieldInfo (FIELD_OLD_URL, "An old technical contact URL must be provided");
        else
          if (!URLValidator.isValid (sOldURL))
            aFormErrors.addFieldInfo (FIELD_OLD_URL, "The old technical contact URL is invalid");

        if (StringHelper.isEmpty (sNewURL))
          aFormErrors.addFieldInfo (FIELD_NEW_URL, "A new technical contact URL must be provided");
        else
          if (!URLValidator.isValid (sNewURL))
            aFormErrors.addFieldInfo (FIELD_NEW_URL, "The new technical contact URL is invalid");
          else
            if (sNewURL.equals (sOldURL))
              aFormErrors.addFieldInfo (FIELD_NEW_URL, "The new technical contact URL is identical to the old technical contact URL");

        // Validate parameters
        if (aFormErrors.isEmpty ())
        {
          // Only a single bulk change may run at a time
          if (!SMPBulkChangeLocks.ENDPOINT_TECHNICAL_CONTACT_URL.tryAcquire (aWPEC.getLoggedInUserID ()))
          {
            aWPEC.postRedirectGetInternal (warn ("Another bulk change is already running in the background. Please wait until it is finished."));
          }
          else
          {
            try
            {
              PhotonWorkerPool.getInstance ()
                              .run ("BulkChangeEndpointTechnicalContactURL",
                                    new BulkChangeEndpointTechnicalContactURL (aSelectedServiceGroup == null ? null
                                                                                             : aSelectedServiceGroup.getParticipantIdentifier (),
                                                               sOldURL,
                                                               sNewURL,
                                                               aWPEC.getLoggedInUserID ()));
            }
            catch (final RuntimeException ex)
            {
              // The job was never started, so it can never release the lock
              SMPBulkChangeLocks.ENDPOINT_TECHNICAL_CONTACT_URL.release ();
              throw ex;
            }

            aWPEC.postRedirectGetInternal (success ("The bulk change of the endpoint technical contact URL from '" +
                                                    sOldURL +
                                                    "' to '" +
                                                    sNewURL +
                                                    "' is now running in the background. Please manually refresh the page to see the update."));
          }
        }
      }

      // Lazy load
      aURLUsage = aServiceInfoMgr.getEndpointTechnicalContactUrlUsageMap ();
      final IEndpointUsageInfo aOldURLUsageInfo = aURLUsage.get (sOldURL);
      final int nSGCount = aOldURLUsageInfo != null ? aOldURLUsageInfo.getServiceGroupCount () : 0;
      final int nEPCount = aOldURLUsageInfo != null ? aOldURLUsageInfo.getEndpointCount () : 0;
      aNodeList.addChild (info ("The selected old technical contact URL '" +
                                sOldURL +
                                "' is currently used in " +
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

      if (nSGCount > 1)
      {
        // Select the affected service groups if more than one is available
        final HCSelect aSGSelect = new HCSelect (new RequestField (FIELD_SERVICE_GROUP));
        aSGSelect.addOption (SERVICE_GROUP_ALL, "All affected Service Groups");
        if (aOldURLUsageInfo != null)
          for (final String sSGID : aOldURLUsageInfo.getServiceGroupIDsSorted (Comparator.naturalOrder ()))
            aSGSelect.addOption (sSGID, sSGID);
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service group")
                                                     .setCtrl (aSGSelect)
                                                     .setHelpText ("If a specific service group is selected, the technical contact URL change will only happen in the endpoints of the selected service group. Othwerwise the endpoint is changed in ALL service groups with matching endpoints.")
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_OLD_URL)));
      }
      else
      {
        // If less than 2 service groups are affected, use the 0/1
        // automatically.
        aForm.addChild (new HCHiddenField (FIELD_SERVICE_GROUP, SERVICE_GROUP_ALL));
      }

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Old technical contact URL")
                                                   .setCtrl (new HCEdit (new RequestField (FIELD_OLD_URL, sOldURL)))
                                                   .setHelpText ("The old technical contact URL that is to be changed in all matching endpoints")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_OLD_URL)));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("New technical contact URL")
                                                   .setCtrl (new HCEdit (new RequestField (FIELD_NEW_URL, sOldURL)))
                                                   .setHelpText ("The new technical contact URL that is used instead")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_NEW_URL)));

      final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
      aToolbar.addSubmitButton ("Save changes", EDefaultIcon.SAVE);
      aToolbar.addButtonCancel (aDisplayLocale);
    }

    if (bShowList)
    {
      if (aURLUsage == null)
        aURLUsage = aServiceInfoMgr.getEndpointTechnicalContactUrlUsageMap ();

      final long nTotalEndpointCount = aServiceInfoMgr.getEndpointCount ();
      final long nTotalEndpointCountWithURL = aURLUsage.values ()
                                                       .stream ()
                                                       .mapToInt (IEndpointUsageInfo::getEndpointCount)
                                                       .sum ();

      aNodeList.addChild (info ().addChildren (div ("This page lets you change the technical contact URLs of multiple endpoints at once. This is e.g. helpful when the support contact page of your organization changed."),
                                               div ("Currently " +
                                                    (nTotalEndpointCount == 1 ? "1 endpoint is"
                                                                              : nTotalEndpointCount +
                                                                                " endpoints are") +
                                                    " registered" +
                                                    (nTotalEndpointCountWithURL < nTotalEndpointCount ? " of which " +
                                                                                                        nTotalEndpointCountWithURL +
                                                                                                        " have a technical contact URL"
                                                                                                      : "") +
                                                    ".")));

      final HCTable aTable = new HCTable (new DTCol ("Technical Contact URL").setInitialSorting (ESortOrder.ASCENDING),
                                          new DTCol ("Service Group Count").setDisplayType (EDTColType.INT,
                                                                                            aDisplayLocale),
                                          new DTCol ("Endpoint Count").setDisplayType (EDTColType.INT, aDisplayLocale),
                                          new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
      aURLUsage.forEach ((sURL, aUsageInfo) -> {
        final HCRow aRow = aTable.addBodyRow ();
        aRow.addCell (sURL);

        aRow.addCell (Integer.toString (aUsageInfo.getServiceGroupCount ()));

        aRow.addCell (Integer.toString (aUsageInfo.getEndpointCount ()));

        final ISimpleURL aEditURL = aWPEC.getSelfHref ()
                                         .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_EDIT)
                                         .add (FIELD_OLD_URL, sURL);
        aRow.addCell (new HCA (aEditURL).setTitle ("Change all endpoints with technical contact URL " + sURL)
                                        .addChild (EDefaultIcon.EDIT.getAsNode ()));
      });

      final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
      aNodeList.addChild (aTable).addChild (aDataTables);
    }
  }
}
