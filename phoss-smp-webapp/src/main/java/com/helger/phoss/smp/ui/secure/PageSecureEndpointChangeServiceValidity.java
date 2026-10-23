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

import java.time.LocalDate;
import java.util.Comparator;
import java.util.Locale;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.ESortOrder;
import com.helger.base.state.EValidity;
import com.helger.base.state.IValidityIndicator;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsMap;
import com.helger.datetime.format.PDTFromString;
import com.helger.datetime.helper.PDTFactory;
import com.helger.datetime.xml.XMLOffsetDateTime;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.forms.HCSelect;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.render.HCRenderer;
import com.helger.peppol.ui.nicename.NiceNameUI;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.IEndpointUsageInfo;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.rest.SMPBulkChangeLocks;
import com.helger.phoss.smp.restapi.cache.SMPRestResponseCache;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.form.BootstrapForm;
import com.helger.photon.bootstrap5.form.BootstrapFormGroup;
import com.helger.photon.bootstrap5.traits.IHCBootstrap5Trait;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.bootstrap5.uictrls.datetimepicker.BootstrapDateTimePicker;
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
import com.helger.web.scope.mgr.WebScoped;

import jakarta.annotation.Nullable;

public final class PageSecureEndpointChangeServiceValidity extends AbstractSMPWebPage
{
  /**
   * The async logic
   *
   * @author Philip Helger
   */
  private static final class BulkChangeEndpointServiceValidity extends AbstractLongRunningJobRunnable implements
                                                               IHCBootstrap5Trait
  {
    private final IDocumentTypeIdentifier m_aDocTypeID;
    private final IParticipantIdentifier m_aServiceGroupPID;
    private final boolean m_bChangeActivation;
    private final XMLOffsetDateTime m_aNewActivation;
    private final boolean m_bChangeExpiration;
    private final XMLOffsetDateTime m_aNewExpiration;

    public BulkChangeEndpointServiceValidity (@NonNull final IDocumentTypeIdentifier aDocTypeID,
                                              @Nullable final IParticipantIdentifier aServiceGroupPID,
                                              final boolean bChangeActivation,
                                              @Nullable final XMLOffsetDateTime aNewActivation,
                                              final boolean bChangeExpiration,
                                              @Nullable final XMLOffsetDateTime aNewExpiration,
                                              @Nullable final String sUserID)
    {
      super ("BulkChangeEndpointServiceValidity",
             new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Bulk change endpoint service validity"),
             () -> sUserID);
      m_aDocTypeID = aDocTypeID;
      m_aServiceGroupPID = aServiceGroupPID;
      m_bChangeActivation = bChangeActivation;
      m_aNewActivation = aNewActivation;
      m_bChangeExpiration = bChangeExpiration;
      m_aNewExpiration = aNewExpiration;
    }

    @NonNull
    public LongRunningJobResult createLongRunningJobResult ()
    {
      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();

      final long nEndpointsChanged = aServiceInfoMgr.updateAllEndpointServiceValidity (m_aDocTypeID,
                                                                                       m_aServiceGroupPID,
                                                                                       m_bChangeActivation,
                                                                                       m_aNewActivation,
                                                                                       m_bChangeExpiration,
                                                                                       m_aNewExpiration);
      if (nEndpointsChanged > 0)
      {
        // The bulk update does not trigger the callbacks and affects an unknown number of
        // participants
        SMPRestResponseCache.invalidateAll ();
      }

      final IHCNode aRes;
      if (nEndpointsChanged > 0)
        aRes = success (div ("The service validity of the document type '" +
                             m_aDocTypeID.getURIEncoded () +
                             "' was changed in " +
                             nEndpointsChanged +
                             " " +
                             (nEndpointsChanged == 1 ? "endpoint" : "endpoints") +
                             "."));
      else
        aRes = warn ("No endpoint was found for the document type '" + m_aDocTypeID.getURIEncoded () + "'");

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
        SMPBulkChangeLocks.ENDPOINT_SERVICE_VALIDITY.release ();
      }
    }
  }

  private static final String SERVICE_GROUP_ALL = "all";
  private static final String MODE_KEEP = "keep";
  private static final String MODE_SET = "set";
  private static final String MODE_CLEAR = "clear";
  private static final String FIELD_DOCTYPE_ID = "doctypeid";
  private static final String FIELD_SERVICE_GROUP = "servicegroup";
  private static final String FIELD_NOT_BEFORE_MODE = "notbeforemode";
  private static final String FIELD_NOT_BEFORE = "notbefore";
  private static final String FIELD_NOT_AFTER_MODE = "notaftermode";
  private static final String FIELD_NOT_AFTER = "notafter";

  public PageSecureEndpointChangeServiceValidity (@NonNull @Nonempty final String sID)
  {
    super (sID, "Bulk change Service Validity");
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

  @NonNull
  private static HCSelect _createModeSelect (@NonNull final String sFieldName)
  {
    final HCSelect ret = new HCSelect (new RequestField (sFieldName, MODE_KEEP));
    ret.addOption (MODE_KEEP, "Keep unchanged");
    ret.addOption (MODE_SET, "Set to the date below");
    ret.addOption (MODE_CLEAR, "Remove the date");
    return ret;
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
    ICommonsMap <String, IEndpointUsageInfo> aDocTypeUsage = null;

    {
      final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
      aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
      aNodeList.addChild (aToolbar);

      if (SMPBulkChangeLocks.ENDPOINT_SERVICE_VALIDITY.isRunning ())
        aNodeList.addChild (warn ("A bulk change is currently running in the background"));
    }

    if (aWPEC.hasAction (CPageParam.ACTION_EDIT))
    {
      bShowList = false;
      final FormErrorList aFormErrors = new FormErrorList ();

      final String sDocTypeID = aWPEC.params ().getAsStringTrimmed (FIELD_DOCTYPE_ID);
      final IDocumentTypeIdentifier aDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocTypeID);

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

        final String sNotBeforeMode = aWPEC.params ().getAsStringTrimmed (FIELD_NOT_BEFORE_MODE);
        final String sNotAfterMode = aWPEC.params ().getAsStringTrimmed (FIELD_NOT_AFTER_MODE);
        final boolean bChangeNotBefore = MODE_SET.equals (sNotBeforeMode) || MODE_CLEAR.equals (sNotBeforeMode);
        final boolean bChangeNotAfter = MODE_SET.equals (sNotAfterMode) || MODE_CLEAR.equals (sNotAfterMode);
        final LocalDate aNotBeforeDate = MODE_SET.equals (sNotBeforeMode) ? PDTFromString.getLocalDateFromString (aWPEC.params ()
                                                                                                                       .getAsStringTrimmed (FIELD_NOT_BEFORE),
                                                                                                                  aDisplayLocale)
                                                                          : null;
        final LocalDate aNotAfterDate = MODE_SET.equals (sNotAfterMode) ? PDTFromString.getLocalDateFromString (aWPEC.params ()
                                                                                                                     .getAsStringTrimmed (FIELD_NOT_AFTER),
                                                                                                                aDisplayLocale)
                                                                        : null;

        if (aDocTypeID == null)
          aFormErrors.addFieldInfo (FIELD_DOCTYPE_ID, "A valid document type must be provided");

        if (!bChangeNotBefore && !bChangeNotAfter)
          aFormErrors.addFieldInfo (FIELD_NOT_BEFORE_MODE, "At least one of the dates must be changed");

        if (MODE_SET.equals (sNotBeforeMode) && aNotBeforeDate == null)
          aFormErrors.addFieldInfo (FIELD_NOT_BEFORE, "A valid date must be provided");
        if (MODE_SET.equals (sNotAfterMode) && aNotAfterDate == null)
          aFormErrors.addFieldInfo (FIELD_NOT_AFTER, "A valid date must be provided");

        if (aNotBeforeDate != null && aNotAfterDate != null && aNotBeforeDate.isAfter (aNotAfterDate))
          aFormErrors.addFieldInfo (FIELD_NOT_BEFORE, "Not Before Date must not be after Not After Date!");

        // Validate parameters
        if (aFormErrors.isEmpty ())
        {
          // Only a single bulk change may run at a time
          if (!SMPBulkChangeLocks.ENDPOINT_SERVICE_VALIDITY.tryAcquire (aWPEC.getLoggedInUserID ()))
          {
            aWPEC.postRedirectGetInternal (warn ("Another bulk change is already running in the background. Please wait until it is finished."));
          }
          else
          {
            try
            {
              PhotonWorkerPool.getInstance ()
                              .run ("BulkChangeEndpointServiceValidity",
                                    new BulkChangeEndpointServiceValidity (aDocTypeID,
                                                                           aSelectedServiceGroup == null ? null
                                                                                                         : aSelectedServiceGroup.getParticipantIdentifier (),
                                                                           bChangeNotBefore,
                                                                           PDTFactory.createXMLOffsetDateTime (aNotBeforeDate),
                                                                           bChangeNotAfter,
                                                                           PDTFactory.createXMLOffsetDateTime (aNotAfterDate),
                                                                           aWPEC.getLoggedInUserID ()));
            }
            catch (final RuntimeException ex)
            {
              // The job was never started, so it can never release the lock
              SMPBulkChangeLocks.ENDPOINT_SERVICE_VALIDITY.release ();
              throw ex;
            }

            aWPEC.postRedirectGetInternal (success ("The bulk change of the service validity of the document type '" +
                                                    sDocTypeID +
                                                    "' is now running in the background. Please manually refresh the page to see the update."));
          }
        }
      }

      // Lazy load
      aDocTypeUsage = aServiceInfoMgr.getEndpointDocumentTypeUsageMap ();
      final IEndpointUsageInfo aDocTypeUsageInfo = aDocTypeID != null ? aDocTypeUsage.get (aDocTypeID.getURIEncoded ())
                                                                      : null;
      final int nSGCount = aDocTypeUsageInfo != null ? aDocTypeUsageInfo.getServiceGroupCount () : 0;
      final int nEPCount = aDocTypeUsageInfo != null ? aDocTypeUsageInfo.getEndpointCount () : 0;
      final HCDiv aDocTypeDiv = div ("Selected document type: ");
      if (aDocTypeID != null)
        aDocTypeDiv.addChild (NiceNameUI.createDocTypeID (aDocTypeID, true));
      else
        aDocTypeDiv.addChild (sDocTypeID);
      aNodeList.addChild (info ().addChildren (aDocTypeDiv,
                                               div ("It is currently used in " +
                                                    nEPCount +
                                                    " " +
                                                    (nEPCount == 1 ? "endpoint" : "endpoints") +
                                                    " of " +
                                                    nSGCount +
                                                    " " +
                                                    (nSGCount == 1 ? "service group" : "service groups") +
                                                    ".")));

      // Show edit screen
      final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormSelf (aWPEC));
      aForm.addChild (new HCHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_EDIT));
      aForm.addChild (new HCHiddenField (CPageParam.PARAM_SUBACTION, CPageParam.ACTION_SAVE));
      aForm.addChild (new HCHiddenField (FIELD_DOCTYPE_ID, sDocTypeID));

      if (nSGCount > 1)
      {
        // Select the affected service groups if more than one is available
        final HCSelect aSGSelect = new HCSelect (new RequestField (FIELD_SERVICE_GROUP));
        aSGSelect.addOption (SERVICE_GROUP_ALL, "All affected Service Groups");
        if (aDocTypeUsageInfo != null)
          for (final String sSGID : aDocTypeUsageInfo.getServiceGroupIDsSorted (Comparator.naturalOrder ()))
            aSGSelect.addOption (sSGID, sSGID);
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service group")
                                                     .setCtrl (aSGSelect)
                                                     .setHelpText ("If a specific service group is selected, the change will only happen in the endpoints of the selected service group. Otherwise the endpoints of the document type are changed in ALL service groups."));
      }
      else
      {
        // If less than 2 service groups are affected, use the 0/1
        // automatically.
        aForm.addChild (new HCHiddenField (FIELD_SERVICE_GROUP, SERVICE_GROUP_ALL));
      }

      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not before - action")
                                                   .setCtrl (_createModeSelect (FIELD_NOT_BEFORE_MODE))
                                                   .setHelpText ("What to do with the activation date of the service.")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_NOT_BEFORE_MODE)));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not before")
                                                   .setCtrl (BootstrapDateTimePicker.create (FIELD_NOT_BEFORE,
                                                                                             (LocalDate) null,
                                                                                             aDisplayLocale))
                                                   .setHelpText ("Activation date of the service. Senders should ignore services that are not yet activated. Only used if the action is \"Set\".")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_NOT_BEFORE)));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not after - action")
                                                   .setCtrl (_createModeSelect (FIELD_NOT_AFTER_MODE))
                                                   .setHelpText ("What to do with the expiration date of the service.")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_NOT_AFTER_MODE)));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Not after")
                                                   .setCtrl (BootstrapDateTimePicker.create (FIELD_NOT_AFTER,
                                                                                             (LocalDate) null,
                                                                                             aDisplayLocale))
                                                   .setHelpText ("Expiration date of the service. Senders should ignore services that are expired. Only used if the action is \"Set\".")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_NOT_AFTER)));

      final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
      aToolbar.addSubmitButton ("Save changes", EDefaultIcon.SAVE);
      aToolbar.addButtonCancel (aDisplayLocale);
    }

    if (bShowList)
    {
      if (aDocTypeUsage == null)
        aDocTypeUsage = aServiceInfoMgr.getEndpointDocumentTypeUsageMap ();

      aNodeList.addChild (info ().addChildren (div ("This page lets you change the service activation date (\"Not before\") and the service expiration date (\"Not after\") of all endpoints of a document type at once."),
                                               div ("Each date can be kept, set to a new value or removed.")));

      final HCTable aTable = new HCTable (new DTCol ("Document type ID").setInitialSorting (ESortOrder.ASCENDING),
                                          new DTCol ("Service Group Count").setDisplayType (EDTColType.INT,
                                                                                            aDisplayLocale),
                                          new DTCol ("Endpoint Count").setDisplayType (EDTColType.INT, aDisplayLocale),
                                          new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
      aDocTypeUsage.forEach ((sDocTypeID, aUsageInfo) -> {
        final HCRow aRow = aTable.addBodyRow ();
        final IDocumentTypeIdentifier aRowDocTypeID = aIdentifierFactory.parseDocumentTypeIdentifier (sDocTypeID);
        if (aRowDocTypeID != null)
          aRow.addCell (NiceNameUI.createDocTypeID (aRowDocTypeID, false));
        else
          aRow.addCell (sDocTypeID);

        aRow.addCell (Integer.toString (aUsageInfo.getServiceGroupCount ()));

        aRow.addCell (Integer.toString (aUsageInfo.getEndpointCount ()));

        final ISimpleURL aEditURL = aWPEC.getSelfHref ()
                                         .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_EDIT)
                                         .add (FIELD_DOCTYPE_ID, sDocTypeID);
        aRow.addCell (new HCA (aEditURL).setTitle ("Change the service validity of all endpoints of " + sDocTypeID)
                                        .addChild (EDefaultIcon.EDIT.getAsNode ()));
      });

      final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
      aNodeList.addChild (aTable).addChild (aDataTables);
    }
  }
}
