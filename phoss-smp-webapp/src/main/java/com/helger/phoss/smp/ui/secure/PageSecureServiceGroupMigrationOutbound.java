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

import java.util.Locale;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.ESortOrder;
import com.helger.base.state.EValidity;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsHashSet;
import com.helger.collection.commons.ICommonsIterable;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.tabular.IHCCell;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationState;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.AbstractSMPWebPageForm;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.secure.hc.HCServiceGroupSelect;
import com.helger.phoss.smp.ui.secure.hc.IHCServiceGroupSelect;
import com.helger.photon.bootstrap4.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.nav.BootstrapTabBox;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerWithQuery;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.url.ISimpleURL;

import jakarta.annotation.Nullable;

/**
 * Class to migrate an existing ServiceGroup of this SMP to another SMP. After "prepare to migrate"
 * on the SMK/SML it is up to the user to decide, if the migration was successful or not. In case of
 * a successful migration, the Service Group is deleted in this SMP.
 *
 * @author Philip Helger
 */
public final class PageSecureServiceGroupMigrationOutbound extends AbstractSMPWebPageForm <ISMPParticipantMigration>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureServiceGroupMigrationOutbound.class);
  private static final String FIELD_PARTICIPANT_ID = "pid";
  private static final String ACTION_CANCEL_MIGRATION = "cancelmig";
  private static final String ACTION_FINALIZE_MIGRATION = "finishmig";

  public PageSecureServiceGroupMigrationOutbound (@NonNull @Nonempty final String sID)
  {
    super (sID, "Migrate to another SMP");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPParticipantMigration, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@NonNull final WebPageExecutionContext aWPEC,
                                @NonNull final BootstrapForm aForm,
                                @Nullable final ISMPParticipantMigration aSelectedObject)
      {
        aForm.addChild (question ("Are you sure you want to delete the outbound Participant Migration for participant '" +
                                  aSelectedObject.getParticipantIdentifier ().getURIEncoded () +
                                  "'?"));
      }

      @Override
      protected void performAction (@NonNull final WebPageExecutionContext aWPEC,
                                    @Nullable final ISMPParticipantMigration aSelectedObject)
      {
        final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
        if (aParticipantMigrationMgr.deleteParticipantMigrationOfID (aSelectedObject.getID ()).isChanged ())
          aWPEC.postRedirectGetInternal (success ("The selected Participant Migration was successfully deleted!"));
        else
          aWPEC.postRedirectGetInternal (error ("Failed to delete the selected Participant Migration!"));
      }
    });
    addCustomHandler (ACTION_CANCEL_MIGRATION,
                      new AbstractBootstrapWebPageActionHandlerWithQuery <ISMPParticipantMigration, WebPageExecutionContext> (true,
                                                                                                                              ACTION_CANCEL_MIGRATION,
                                                                                                                              "cancelmig")
                      {
                        @Override
                        protected void showQuery (@NonNull final WebPageExecutionContext aWPEC,
                                                  @NonNull final BootstrapForm aForm,
                                                  @Nullable final ISMPParticipantMigration aSelectedObject)
                        {
                          aForm.addChild (question ("Are you sure you want to cancel the outbound Participant Migration for '" +
                                                    aSelectedObject.getParticipantIdentifier ().getURIEncoded () +
                                                    "'? This action will keep the Service Group in this SMP and not migrate it away."));
                        }

                        @Override
                        protected void performAction (@NonNull final WebPageExecutionContext aWPEC,
                                                      @Nullable final ISMPParticipantMigration aSelectedObject)
                        {
                          final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
                          if (aParticipantMigrationMgr.setParticipantMigrationState (aSelectedObject.getID (),
                                                                                     EParticipantMigrationState.CANCELLED)
                                                      .isChanged ())
                          {
                            aWPEC.postRedirectGetInternal (success ("The outbound Participant Migration for '" +
                                                                    aSelectedObject.getParticipantIdentifier ()
                                                                                   .getURIEncoded () +
                                                                    "' was successfully cancelled!"));
                          }
                          else
                          {
                            aWPEC.postRedirectGetInternal (error ("Failed to cancel outbound Participant Migration for '" +
                                                                  aSelectedObject.getParticipantIdentifier ()
                                                                                 .getURIEncoded () +
                                                                  "'!"));
                          }
                        }
                      });
    addCustomHandler (ACTION_FINALIZE_MIGRATION,
                      new AbstractBootstrapWebPageActionHandlerWithQuery <ISMPParticipantMigration, WebPageExecutionContext> (true,
                                                                                                                              ACTION_FINALIZE_MIGRATION,
                                                                                                                              "finalizemig")
                      {
                        @Override
                        protected void showQuery (@NonNull final WebPageExecutionContext aWPEC,
                                                  @NonNull final BootstrapForm aForm,
                                                  @Nullable final ISMPParticipantMigration aSelectedObject)
                        {
                          aForm.addChild (question ("Are you sure you want to fianlize the outbound Participant Migration for '" +
                                                    aSelectedObject.getParticipantIdentifier ().getURIEncoded () +
                                                    "'? This action will delete the Service Group in this SMP."));
                        }

                        @Override
                        protected void performAction (@NonNull final WebPageExecutionContext aWPEC,
                                                      @Nullable final ISMPParticipantMigration aSelectedObject)
                        {
                          final HCNodeList aNL = new HCNodeList ();
                          final IParticipantIdentifier aParticipantID = aSelectedObject.getParticipantIdentifier ();
                          final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
                          final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();

                          // Remember the old state
                          final EParticipantMigrationState eOldState = aSelectedObject.getState ();

                          boolean bSetStateToMigrated = false;

                          // First set the state
                          if (aParticipantMigrationMgr.setParticipantMigrationState (aSelectedObject.getID (),
                                                                                     EParticipantMigrationState.MIGRATED)
                                                      .isChanged ())
                          {
                            aNL.addChild (success ("The outbound Participant Migration for '" +
                                                   aParticipantID.getURIEncoded () +
                                                   "' was successfully performed!"));
                            bSetStateToMigrated = true;
                          }
                          else
                          {
                            aNL.addChild (error ("Failed to perform outbound Participant Migration for '" +
                                                 aParticipantID.getURIEncoded () +
                                                 "'!"));
                          }

                          if (bSetStateToMigrated)
                          {
                            boolean bDeletedSG = false;
                            try
                            {
                              // Delete the service group only locally but not
                              // in the SML
                              if (aServiceGroupMgr.deleteSMPServiceGroup (aParticipantID, false).isChanged ())
                              {
                                aNL.addChild (success ("The SMP ServiceGroup for participant '" +
                                                       aParticipantID.getURIEncoded () +
                                                       "' was successfully deleted from this SMP!"));
                                bDeletedSG = true;
                              }
                              else
                              {
                                aNL.addChild (error ("The SMP ServiceGroup for participant '" +
                                                     aParticipantID.getURIEncoded () +
                                                     "' could not be deleted! Please check the logs."));
                              }
                            }
                            catch (final Exception ex)
                            {
                              aNL.addChild (error ("Error deleting the SMP ServiceGroup for participant '" +
                                                   aParticipantID.getURIEncoded () +
                                                   "'.").addChild (SMPCommonUI.getTechnicalDetailsUI (ex)));
                            }

                            if (!bDeletedSG)
                            {
                              // Restore old state in participant migration
                              // manager
                              if (aParticipantMigrationMgr.setParticipantMigrationState (aSelectedObject.getID (),
                                                                                         eOldState).isChanged ())
                              {
                                aNL.addChild (success ("Successfully reverted the state of the outbound Participant Migration for '" +
                                                       aParticipantID.getURIEncoded () +
                                                       "' to " +
                                                       eOldState +
                                                       "!"));
                              }
                              else
                              {
                                aNL.addChild (error ("Failed to revert the state of the outbound Participant Migration for '" +
                                                     aParticipantID.getURIEncoded () +
                                                     "' to " +
                                                     eOldState +
                                                     "!"));
                              }
                            }
                          }
                          aWPEC.postRedirectGetInternal (aNL);
                        }
                      });
  }

  @Override
  protected boolean isActionAllowed (@NonNull final WebPageExecutionContext aWPEC,
                                     @NonNull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPParticipantMigration aSelectedObject)
  {
    if (eFormAction.isEdit ())
      return false;

    if (eFormAction.isCreating ())
    {
      final ISMPSettings aSettings = SMPMetaManager.getSettings ();
      if (aSettings.getSMLInfo () == null)
        return false;
      if (!aSettings.isSMLEnabled ())
        return false;

      final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
      if (aServiceGroupManager.getSMPServiceGroupCount () <= 0)
        return false;
    }

    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  @Nullable
  protected ISMPParticipantMigration getSelectedObject (@NonNull final WebPageExecutionContext aWPEC,
                                                        @Nullable final String sID)
  {
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    return aParticipantMigrationMgr.getParticipantMigrationOfID (sID);
  }

  @Override
  protected void showSelectedObject (@NonNull final WebPageExecutionContext aWPEC,
                                     @NonNull final ISMPParticipantMigration aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of outbound Participant Migration"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service Group")
                                                 .setCtrl (new HCA (createViewURL (aWPEC,
                                                                                   CMenuSecure.MENU_SERVICE_GROUPS,
                                                                                   aSelectedObject.getParticipantIdentifier ()
                                                                                                  .getURIEncoded ())).addChild (aSelectedObject.getParticipantIdentifier ()
                                                                                                                                               .getURIEncoded ())));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Migration state")
                                                 .setCtrl (aSelectedObject.getState ().getDisplayName ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Initiation datetime")
                                                 .setCtrl (PDTToString.getAsString (aSelectedObject.getInitiationDateTime (),
                                                                                    aDisplayLocale)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Migration Key")
                                                 .setCtrl (code (aSelectedObject.getMigrationKey ())));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void showInputForm (@NonNull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPParticipantMigration aSelectedObject,
                                @NonNull final BootstrapForm aForm,
                                final boolean bIsFormSubmitted,
                                @NonNull final EWebPageFormAction eFormAction,
                                @NonNull final FormErrorList aFormErrors)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    // State is filtered below
    final ICommonsList <ISMPParticipantMigration> aExistingOutgoingMigrations = aParticipantMigrationMgr.getAllOutboundParticipantMigrations (null);

    // Get all participant identifiers for which NO new migration can be
    // initiated (because they were already migrated or migration is currently
    // in progress)
    final ICommonsSet <IParticipantIdentifier> aPIDsThatCannotBeUsed = new CommonsHashSet <> ();
    aPIDsThatCannotBeUsed.addAllMapped (aExistingOutgoingMigrations,
                                        x -> x.getState ().preventsNewMigration (),
                                        ISMPParticipantMigration::getParticipantIdentifier);

    // Filter out all for which it makes no sense
    final IHCServiceGroupSelect aSGSelect = HCServiceGroupSelect.create (new RequestField (FIELD_PARTICIPANT_ID),
                                                                         aDisplayLocale,
                                                                         x -> aPIDsThatCannotBeUsed.containsNone (y -> x.getParticipantIdentifier ()
                                                                                                                        .hasSameContent (y)),
                                                                         false);
    if (!aSGSelect.containsAnyServiceGroup ())
    {
      aForm.addChild (warn ("No Service Group on this SMP can currently be migrated."));
    }
    else
    {
      aForm.addChild (getUIHandler ().createActionHeader ("Start a Participant Migration from this SMP to another SMP"));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service Group")
                                                   .setCtrl (aSGSelect)
                                                   .setHelpText ("Select the Service Group to migrate to another SMP. Each Service Group can only be migrated once from this SMP. Only Service Groups registered to the SML can be migrated.")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_PARTICIPANT_ID)));
    }
  }

  @Override
  protected void validateAndSaveInputParameters (@NonNull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPParticipantMigration aSelectedObject,
                                                 @NonNull final FormErrorList aFormErrors,
                                                 @NonNull final EWebPageFormAction eFormAction)
  {
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();

    final String sParticipantID = aWPEC.params ().getAsStringTrimmed (FIELD_PARTICIPANT_ID);
    final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sParticipantID);

    if (StringHelper.isEmpty (sParticipantID))
      aFormErrors.addFieldError (FIELD_PARTICIPANT_ID, "A Service Group must be selected.");
    else
      if (aParticipantID == null)
        aFormErrors.addFieldError (FIELD_PARTICIPANT_ID, "The selected Service Group does not exist.");
      else
      {
        if (aParticipantMigrationMgr.containsOutboundMigrationInProgress (aParticipantID))
          aFormErrors.addFieldError (FIELD_PARTICIPANT_ID,
                                     "The migration of the selected Service Group is already in progress.");
      }

    if (aFormErrors.isEmpty ())
    {
      LOGGER.info ("Starting migration of participant ID '" + aParticipantID.getURIEncoded () + "'");

      // Lets take this to the SML
      String sMigrationKey = null;
      if (true)
      {
        try
        {
          final ManageParticipantIdentifierServiceCaller aCaller = new ManageParticipantIdentifierServiceCaller (aSettings.getSMLInfo ());
          aCaller.setSSLSocketFactory (SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ());

          // Create a random migration key,
          // Than call SML
          sMigrationKey = aCaller.prepareToMigrate (aParticipantID, SMPServerConfiguration.getSMLSMPID ());
          LOGGER.info ("Successfully called prepareToMigrate on SML. Created migration key is '" + sMigrationKey + "'");
        }
        catch (final Exception ex)
        {
          LOGGER.error ("Error invoking prepareToMigrate on SML", ex);
          aWPEC.postRedirectGetInternal (error ("Failed to prepare the migration for participant '" +
                                                aParticipantID.getURIEncoded () +
                                                "' in SML.").addChild (SMPCommonUI.getTechnicalDetailsUI (ex)));
        }
      }
      else
      {
        // Dummy for testing only
        sMigrationKey = ManageParticipantIdentifierServiceCaller.createRandomMigrationKey ();
        LOGGER.warn ("Created migration key '" + sMigrationKey + "' was not send to SML!");
      }

      // Remember internally
      if (aParticipantMigrationMgr.createOutboundParticipantMigration (aParticipantID, sMigrationKey) != null)
      {
        aWPEC.postRedirectGetInternal (success ().addChild (div ("The participant migration for '" +
                                                                 aParticipantID.getURIEncoded () +
                                                                 "' was successfully created."))
                                                 .addChild (div ("The created migration key is ").addChild (code (sMigrationKey))));
      }
      else
      {
        aWPEC.postRedirectGetInternal (error ().addChild (div ("Failed to store the participant migration for '" +
                                                               aParticipantID.getURIEncoded () +
                                                               "'."))
                                               .addChild (div ("The created migration key is ").addChild (code (sMigrationKey)))
                                               .addChild (". Please note it down manually!"));
      }
    }
  }

  @NonNull
  private IHCNode _createTable (@NonNull final WebPageExecutionContext aWPEC,
                                @NonNull final ICommonsIterable <ISMPParticipantMigration> aMigs,
                                @NonNull final EParticipantMigrationState eState)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    final HCTable aTable = new HCTable (new DTCol ("ID").setVisible (false),
                                        new DTCol ("Participant ID").setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Initiation").setDisplayType (EDTColType.DATETIME, aDisplayLocale),
                                        new DTCol ("Migration Key"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID () + eState.getID ());
    for (final ISMPParticipantMigration aCurObject : aMigs)
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);
      final String sParticipantID = aCurObject.getParticipantIdentifier ().getURIEncoded ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (aCurObject.getID ());
      aRow.addCell (a (aViewLink).addChild (sParticipantID));
      aRow.addCell (PDTToString.getAsString (aCurObject.getInitiationDateTime (), aDisplayLocale));
      aRow.addCell (code (aCurObject.getMigrationKey ()));

      final IHCCell <?> aActionCell = aRow.addCell ();
      aActionCell.addChild (eState.isInProgress () ? new HCA (aWPEC.getSelfHref ()
                                                                   .add (CPageParam.PARAM_ACTION,
                                                                         ACTION_FINALIZE_MIGRATION)
                                                                   .add (CPageParam.PARAM_OBJECT, aCurObject.getID ()))
                                                                                                                       .setTitle ("Finalize Participant Migration of '" +
                                                                                                                                  sParticipantID +
                                                                                                                                  "'")
                                                                                                                       .addChild (EDefaultIcon.YES.getAsNode ())
                                                   : createEmptyAction ());
      aActionCell.addChild (" ");
      aActionCell.addChild (eState.isInProgress () ? new HCA (aWPEC.getSelfHref ()
                                                                   .add (CPageParam.PARAM_ACTION,
                                                                         ACTION_CANCEL_MIGRATION)
                                                                   .add (CPageParam.PARAM_OBJECT, aCurObject.getID ()))
                                                                                                                       .setTitle ("Cancel Participant Migration of '" +
                                                                                                                                  sParticipantID +
                                                                                                                                  "'")
                                                                                                                       .addChild (EDefaultIcon.NO.getAsNode ())
                                                   : createEmptyAction ());
      aActionCell.addChild (" ");
      aActionCell.addChild (createDeleteLink (aWPEC,
                                              aCurObject,
                                              "Delete Participant Migration of '" + sParticipantID + "'"));
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    return new HCNodeList ().addChild (aTable).addChild (aDataTables);
  }

  @Override
  protected void showListOfExistingObjects (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();

    if (SMPServerConfiguration.isHREdeliveryExtensionMode ())
    {
      aNodeList.addChild (warn (HR_EXT_WARNING));
    }

    {
      final HCOL aOL = new HCOL ();
      aOL.addItem ("The migration is initiated on this SMP, and the SML is informed about the upcoming migration");
      aOL.addItem ("The other SMP, that is taking over the Service Group, must acknowledge the migration by providing the same migration code (created by this SMP) to the SML");
      aOL.addItem ("If the migration was successful, the Service Group must be deleted from this SMP, ideally a temporary redirect to the new SMP is created. If the migration was cancelled no action is needed.");
      aNodeList.addChild (info ().addChild (div ("The process of migrating a Service Group to another SMP consists of multiple steps:"))
                                 .addChild (aOL)
                                 .addChild (div ("Therefore each open Migration must either be finished (deleting the Service Group) or cancelled (no action taken)." +
                                                 " If a Migration is cancelled, it can be retried later.")));
    }

    EValidity eCanStartMigration = EValidity.VALID;
    if (aSettings.getSMLInfo () == null)
    {
      final BootstrapWarnBox aWarn = aNodeList.addAndReturnChild (warn ().addChild (div ("No valid SML Configuration is selected hence no participant can be migrated."))
                                                                         .addChild (div (new BootstrapButton ().addChild ("Select SML Configuration in the Settings")
                                                                                                               .setOnClick (aWPEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS))
                                                                                                               .setIcon (EDefaultIcon.EDIT))));
      if (aSettings.isSMLEnabled () || aSettings.isSMLRequired ())
      {
        aWarn.addChild (div (new BootstrapButton ().addChild ("Create a new SML Configuration")
                                                   .setOnClick (createCreateURL (aWPEC,
                                                                                 CMenuSecure.MENU_SML_CONFIGURATION))
                                                   .setIcon (EDefaultIcon.YES)));
      }
      eCanStartMigration = EValidity.INVALID;
    }
    else
      if (!aSettings.isSMLEnabled ())
      {
        aNodeList.addChild (warn ().addChild (div ("SML Connection is not enabled hence no participant can be migrated."))
                                   .addChild (div (new BootstrapButton ().addChild ("Enable SML in the Settings")
                                                                         .setOnClick (aWPEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS))
                                                                         .setIcon (EDefaultIcon.EDIT))));
        eCanStartMigration = EValidity.INVALID;
      }
      else
      {
        if (aServiceGroupManager.getSMPServiceGroupCount () <= 0)
        {
          aNodeList.addChild (warn ("No Service Group is present! At least one Service Group must be present to migrate it."));
          // Note: makes no to allow to create a new Service Group here and than
          // directly migrate it away
          eCanStartMigration = EValidity.INVALID;
        }
      }

    {
      final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
      aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
      aToolbar.addChild (new BootstrapButton ().addChild ("Start Participant Migration")
                                               .setOnClick (createCreateURL (aWPEC))
                                               .setDisabled (eCanStartMigration.isInvalid ())
                                               .setIcon (EDefaultIcon.NEW));
      aNodeList.addChild (aToolbar);
    }

    final BootstrapTabBox aTabBox = aNodeList.addAndReturnChild (new BootstrapTabBox ());

    final ICommonsList <ISMPParticipantMigration> aAllMigs = aParticipantMigrationMgr.getAllOutboundParticipantMigrations (null);
    for (final EParticipantMigrationState eState : EParticipantMigrationState.values ())
      if (eState.isOutboundState ())
      {
        final ICommonsList <ISMPParticipantMigration> aMatchingMigs = aAllMigs.getAll (x -> x.getState () == eState);
        aTabBox.addTab (eState.getID (),
                        eState.getDisplayName () + " (" + aMatchingMigs.size () + ")",
                        _createTable (aWPEC, aMatchingMigs, eState));
      }
  }
}
