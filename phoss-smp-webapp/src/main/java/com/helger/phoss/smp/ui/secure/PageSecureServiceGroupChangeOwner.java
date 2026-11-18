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
import java.util.List;
import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.ESortOrder;
import com.helger.base.state.EValidity;
import com.helger.base.state.IValidityIndicator;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.CommonsHashMap;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.html.hc.html.forms.HCCheckBox;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.forms.HCLabel;
import com.helger.html.hc.html.grouping.HCDiv;
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
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.rest.SMPBulkChangeLocks;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.secure.hc.HCUserSelect;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.form.BootstrapForm;
import com.helger.photon.bootstrap5.form.BootstrapFormGroup;
import com.helger.photon.bootstrap5.form.DefaultBootstrapFormGroupRenderer;
import com.helger.photon.bootstrap5.traits.IHCBootstrap5Trait;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldBooleanMultiValue;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.photon.mgrs.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.mgrs.longrun.LongRunningJobResult;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
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

/**
 * Bulk change the owner of multiple service groups at once.
 *
 * @author Philip Helger
 * @since 8.8.0
 */
public final class PageSecureServiceGroupChangeOwner extends AbstractSMPWebPage
{
  /**
   * The async logic
   *
   * @author Philip Helger
   */
  private static final class BulkChangeServiceGroupOwner extends AbstractLongRunningJobRunnable implements
                                                         IHCBootstrap5Trait
  {
    private static final Logger LOGGER = LoggerFactory.getLogger (BulkChangeServiceGroupOwner.class);

    private final ICommonsList <IParticipantIdentifier> m_aServiceGroupPIDs;
    private final String m_sOldOwnerID;
    private final String m_sNewOwnerID;

    public BulkChangeServiceGroupOwner (@NonNull final List <IParticipantIdentifier> aServiceGroupPIDs,
                                        @NonNull @Nonempty final String sOldOwnerID,
                                        @NonNull @Nonempty final String sNewOwnerID,
                                        @Nullable final String sUserID)
    {
      super ("BulkChangeServiceGroupOwner",
             new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Bulk change service group owner"),
             () -> sUserID);
      m_aServiceGroupPIDs = new CommonsArrayList <> (aServiceGroupPIDs);
      m_sOldOwnerID = sOldOwnerID;
      m_sNewOwnerID = sNewOwnerID;
    }

    @NonNull
    public LongRunningJobResult createLongRunningJobResult ()
    {
      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();

      int nChanged = 0;
      final ICommonsList <String> aFailedIDs = new CommonsArrayList <> ();
      for (final IParticipantIdentifier aPID : m_aServiceGroupPIDs)
      {
        final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aPID);
        // The service group may have been deleted or changed in the meantime
        if (aServiceGroup != null && aServiceGroup.getOwnerID ().equals (m_sOldOwnerID))
        {
          try
          {
            // Keep the extensions and the custom properties
            if (aServiceGroupMgr.updateSMPServiceGroup (aPID,
                                                        m_sNewOwnerID,
                                                        aServiceGroup.getExtensions ().getExtensionsAsJsonString (),
                                                        aServiceGroup.getCustomProperties ())
                                .isChanged ())
              nChanged++;
          }
          catch (final SMPServerException ex)
          {
            LOGGER.error ("Failed to change the owner of service group '" + aPID.getURIEncoded () + "'", ex);
            aFailedIDs.add (aPID.getURIEncoded ());
          }
        }
      }

      final HCNodeList aRes = new HCNodeList ();
      if (nChanged > 0)
        aRes.addChild (success (div ("The owner of " +
                                     nChanged +
                                     " " +
                                     (nChanged == 1 ? "service group" : "service groups") +
                                     " was changed from '" +
                                     SMPCommonUI.getOwnerName (m_sOldOwnerID) +
                                     "' to '" +
                                     SMPCommonUI.getOwnerName (m_sNewOwnerID) +
                                     "'.")));
      else
        if (aFailedIDs.isEmpty ())
          aRes.addChild (warn ("No service group owned by '" +
                               SMPCommonUI.getOwnerName (m_sOldOwnerID) +
                               "' was changed."));
      if (aFailedIDs.isNotEmpty ())
        aRes.addChild (error ("Failed to change the owner of the following service groups: " +
                              StringImplode.getImploded (", ", aFailedIDs)));

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
        SMPBulkChangeLocks.SERVICE_GROUP_OWNER.release ();
      }
    }
  }

  private static final String FIELD_SERVICE_GROUP = "servicegroup";
  private static final String FIELD_OLD_OWNER = "oldowner";
  private static final String FIELD_NEW_OWNER = "newowner";

  public PageSecureServiceGroupChangeOwner (@NonNull @Nonempty final String sID)
  {
    super (sID, "Bulk change owner");
  }

  @NonNull
  private static ICommonsMap <String, ICommonsList <String>> _getServiceGroupIDsPerOwner ()
  {
    final ICommonsMap <String, ICommonsList <String>> ret = new CommonsHashMap <> ();
    for (final ISMPServiceGroup aServiceGroup : SMPMetaManager.getServiceGroupMgr ().getAllSMPServiceGroups ())
      ret.computeIfAbsent (aServiceGroup.getOwnerID (), k -> new CommonsArrayList <> ()).add (aServiceGroup.getID ());
    return ret;
  }

  @Override
  @NonNull
  protected IValidityIndicator isValidToDisplayPage (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    if (PhotonSecurityManager.getUserMgr ().getActiveUserCount () <= 1)
    {
      aNodeList.addChild (warn ("At least two active users must be present to change the owner of service groups."));
      return EValidity.INVALID;
    }

    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    if (!aServiceGroupMgr.containsAnySMPServiceGroup ())
    {
      aNodeList.addChild (warn ("No service group is present! At least one service group must be present to change the owner."));
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
    boolean bShowList = true;

    final ICommonsMap <String, ICommonsList <String>> aSGIDsPerOwner = _getServiceGroupIDsPerOwner ();

    {
      final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
      aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
      aNodeList.addChild (aToolbar);

      if (SMPBulkChangeLocks.SERVICE_GROUP_OWNER.isRunning ())
        aNodeList.addChild (warn ("A bulk change is currently running in the background"));
    }

    if (aWPEC.hasAction (CPageParam.ACTION_EDIT))
    {
      final String sOldOwnerID = aWPEC.params ().getAsStringTrimmed (FIELD_OLD_OWNER);
      final ICommonsList <String> aOldOwnerSGIDs = sOldOwnerID == null ? null : aSGIDsPerOwner.get (sOldOwnerID);
      if (aOldOwnerSGIDs == null)
      {
        aNodeList.addChild (warn ("The selected owner does not own any service group."));
      }
      else
      {
        bShowList = false;
        final FormErrorList aFormErrors = new FormErrorList ();

        if (aWPEC.hasSubAction (CPageParam.ACTION_SAVE))
        {
          final String sNewOwnerID = aWPEC.params ().getAsStringTrimmed (FIELD_NEW_OWNER);
          final IUser aNewOwner = StringHelper.isEmpty (sNewOwnerID) ? null : PhotonSecurityManager.getUserMgr ()
                                                                                                    .getUserOfID (sNewOwnerID);

          // Only consider the service groups that are really owned by the old owner
          final ICommonsList <IParticipantIdentifier> aSelectedPIDs = new CommonsArrayList <> ();
          final ICommonsList <String> aSelectedSGIDs = aWPEC.params ().getAsStringList (FIELD_SERVICE_GROUP);
          if (aSelectedSGIDs != null)
            for (final String sSGID : aSelectedSGIDs)
              if (aOldOwnerSGIDs.contains (sSGID))
              {
                final IParticipantIdentifier aPID = aIdentifierFactory.parseParticipantIdentifier (sSGID);
                if (aPID != null)
                  aSelectedPIDs.add (aPID);
              }

          if (StringHelper.isEmpty (sNewOwnerID))
            aFormErrors.addFieldError (FIELD_NEW_OWNER, "A new owner must be selected");
          else
            if (aNewOwner == null)
              aFormErrors.addFieldError (FIELD_NEW_OWNER, "The selected new owner does not exist");
            else
              if (sNewOwnerID.equals (sOldOwnerID))
                aFormErrors.addFieldError (FIELD_NEW_OWNER, "The new owner is identical to the old owner");

          if (aSelectedPIDs.isEmpty ())
            aFormErrors.addFieldError (FIELD_SERVICE_GROUP, "At least one service group must be selected");

          if (aFormErrors.isEmpty ())
          {
            // Only a single bulk change may run at a time
            if (!SMPBulkChangeLocks.SERVICE_GROUP_OWNER.tryAcquire (aWPEC.getLoggedInUserID ()))
            {
              aWPEC.postRedirectGetInternal (warn ("Another bulk change is already running in the background. Please wait until it is finished."));
            }
            else
            {
              try
              {
                PhotonWorkerPool.getInstance ()
                                .run ("BulkChangeServiceGroupOwner",
                                      new BulkChangeServiceGroupOwner (aSelectedPIDs,
                                                                       sOldOwnerID,
                                                                       sNewOwnerID,
                                                                       aWPEC.getLoggedInUserID ()));
              }
              catch (final RuntimeException ex)
              {
                // The job was never started, so it can never release the lock
                SMPBulkChangeLocks.SERVICE_GROUP_OWNER.release ();
                throw ex;
              }

              aWPEC.postRedirectGetInternal (success ("The bulk change of the owner of " +
                                                      aSelectedPIDs.size () +
                                                      " " +
                                                      (aSelectedPIDs.size () == 1 ? "service group" : "service groups") +
                                                      " from '" +
                                                      SMPCommonUI.getOwnerName (sOldOwnerID) +
                                                      "' to '" +
                                                      SMPCommonUI.getOwnerName (sNewOwnerID) +
                                                      "' is now running in the background. Please manually refresh the page to see the update."));
            }
          }
        }

        final int nSGCount = aOldOwnerSGIDs.size ();
        aNodeList.addChild (info ("The selected old owner '" +
                                  SMPCommonUI.getOwnerName (sOldOwnerID) +
                                  "' currently owns " +
                                  nSGCount +
                                  " " +
                                  (nSGCount == 1 ? "service group" : "service groups") +
                                  "."));

        // Show edit screen
        final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormSelf (aWPEC));
        // Otherwise a single service group check box is rendered like a stand-alone check box
        aForm.setFormGroupRenderer (new DefaultBootstrapFormGroupRenderer ().setForceNoCheckBoxHandling (true));
        aForm.addChild (new HCHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_EDIT));
        aForm.addChild (new HCHiddenField (CPageParam.PARAM_SUBACTION, CPageParam.ACTION_SAVE));
        aForm.addChild (new HCHiddenField (FIELD_OLD_OWNER, sOldOwnerID));

        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Old owner")
                                                     .setCtrl (SMPCommonUI.getOwnerName (sOldOwnerID)));

        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("New owner")
                                                     .setCtrl (new HCUserSelect (new RequestField (FIELD_NEW_OWNER),
                                                                                 aDisplayLocale))
                                                     .setHelpText ("The user that becomes the new owner of the selected service groups")
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_NEW_OWNER)));

        final HCNodeList aSGCtrl = new HCNodeList ();
        for (final String sSGID : aOldOwnerSGIDs.getSortedInline (Comparator.naturalOrder ()))
          aSGCtrl.addChild (new HCDiv ().addChild (new HCLabel ().addChild (new HCCheckBox (new RequestFieldBooleanMultiValue (FIELD_SERVICE_GROUP,
                                                                                                                                 sSGID,
                                                                                                                                 true)))
                                                                 .addChild (" " + sSGID)));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service groups")
                                                     .setCtrl (aSGCtrl)
                                                     .setHelpText ("Only the owner of the selected service groups is changed")
                                                     .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_GROUP)));

        final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
        aToolbar.addSubmitButton ("Save changes", EDefaultIcon.SAVE);
        aToolbar.addButtonCancel (aDisplayLocale);
      }
    }

    if (bShowList)
    {
      aNodeList.addChild (info ().addChildren (div ("This page lets you change the owner of multiple service groups at once. This is e.g. helpful when a user leaves your organisation."),
                                               div ("Currently " +
                                                    (aSGIDsPerOwner.size () == 1 ? "1 user owns"
                                                                                 : aSGIDsPerOwner.size () +
                                                                                   " users own") +
                                                    " service groups.")));

      final HCTable aTable = new HCTable (new DTCol ("Owner").setInitialSorting (ESortOrder.ASCENDING),
                                          new DTCol ("Service Group Count").setDisplayType (EDTColType.INT,
                                                                                            aDisplayLocale),
                                          new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
      aSGIDsPerOwner.forEach ( (sOwnerID, aSGIDs) -> {
        final String sOwnerName = SMPCommonUI.getOwnerName (sOwnerID);

        final HCRow aRow = aTable.addBodyRow ();
        aRow.addCell (sOwnerName);

        aRow.addCell (Integer.toString (aSGIDs.size ()));

        final ISimpleURL aEditURL = aWPEC.getSelfHref ()
                                         .add (CPageParam.PARAM_ACTION, CPageParam.ACTION_EDIT)
                                         .add (FIELD_OLD_OWNER, sOwnerID);
        aRow.addCell (new HCA (aEditURL).setTitle ("Change the owner of all service groups owned by " + sOwnerName)
                                        .addChild (EDefaultIcon.EDIT.getAsNode ()));
      });

      final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
      aNodeList.addChild (aTable).addChild (aDataTables);
    }
  }
}
