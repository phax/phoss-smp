/**
 * Copyright (C) 2014-2020 Philip Helger and contributors
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

import java.security.GeneralSecurityException;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppol.smlclient.participant.BadRequestFault;
import com.helger.peppol.smlclient.participant.InternalErrorFault;
import com.helger.peppol.smlclient.participant.NotFoundFault;
import com.helger.peppol.smlclient.participant.UnauthorizedFault;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.AbstractSMPWebPageForm;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.secure.hc.HCServiceGroupSelect;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandler;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EShowList;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;

/**
 * Class to migrate an existing ServiceGroup from this SMP to another SMP.
 *
 * @author Philip Helger
 */
public final class PageSecureServiceGroupMigrationOutbound extends AbstractSMPWebPageForm <ISMPParticipantMigration>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureServiceGroupMigrationOutbound.class);
  private static final String FIELD_PARTICIPANT_ID = "pid";
  private static final String ACTION_FINALIZE_MIGRATION = "finishmig";

  public PageSecureServiceGroupMigrationOutbound (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Migrate to another SMP");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPParticipantMigration, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final ISMPParticipantMigration aSelectedObject)
      {
        aForm.addChild (question ("Are you sure you want to cancel the outbound Participant Migration for '" +
                                  aSelectedObject.getParticipantIdentifier ().getURIEncoded () +
                                  "'? This action will keep the Service Group in this SMP and not migrate it away."));
      }

      @Override
      protected void performAction (@Nonnull final WebPageExecutionContext aWPEC, @Nonnull final ISMPParticipantMigration aSelectedObject)
      {
        final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
        if (aParticipantMigrationMgr.deleteParticipantMigration (aSelectedObject.getID ()).isChanged ())
          aWPEC.postRedirectGetInternal (success ("The outbound Participant Migration for '" +
                                                  aSelectedObject.getParticipantIdentifier ().getURIEncoded () +
                                                  "' was successfully cancelled!"));
        else
          aWPEC.postRedirectGetInternal (error ("Failed to delete outbound Participant Migration for '" +
                                                aSelectedObject.getParticipantIdentifier ().getURIEncoded () +
                                                "'!"));
      }
    });
    addCustomHandler (ACTION_FINALIZE_MIGRATION,
                      new AbstractBootstrapWebPageActionHandler <ISMPParticipantMigration, WebPageExecutionContext> (true)
                      {

                        public EShowList handleAction (@Nonnull final WebPageExecutionContext aWPEC,
                                                       @Nonnull final ISMPParticipantMigration aSelectedObject)
                        {
                          // TODO
                          return null;
                        }
                      });
  }

  @Override
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    if (aSettings.getSMLInfo () == null)
    {
      aNodeList.addChild (warn ("No valid SML Configuration is selected hence no participant can be migrated."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Select SML Configuration in the Settings")
                                                .setOnClick (aWPEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS))
                                                .setIcon (EDefaultIcon.EDIT));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create a new SML Configuration")
                                                .setOnClick (createCreateURL (aWPEC, CMenuSecure.MENU_SML_CONFIGURATION))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }
    if (!aSettings.isSMLEnabled ())
    {
      aNodeList.addChild (warn ("SML Connection is not configured hence no participant can be migrated."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Enable SML in the Settings")
                                                .setOnClick (aWPEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS))
                                                .setIcon (EDefaultIcon.EDIT));
      return EValidity.INVALID;
    }

    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
    if (aServiceGroupManager.getSMPServiceGroupCount () == 0)
    {
      aNodeList.addChild (warn ("No Service Group is present! At least one Service Group must be present to migrate it."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new Service Group")
                                                .setOnClick (createCreateURL (aWPEC, CMenuSecure.MENU_SERVICE_GROUPS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }

    return super.isValidToDisplayPage (aWPEC);
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPParticipantMigration aSelectedObject)
  {
    if (eFormAction.isEdit ())
      return false;

    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  @Nullable
  protected ISMPParticipantMigration getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC, @Nullable final String sID)
  {
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    return aParticipantMigrationMgr.getParticipantMigrationOfID (sID);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC, @Nonnull final ISMPParticipantMigration aSelectedObject)
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
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Initiation datetime")
                                                 .setCtrl (PDTToString.getAsString (aSelectedObject.getInitiationDateTime (),
                                                                                    aDisplayLocale)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Migration Key").setCtrl (code (aSelectedObject.getMigrationKey ())));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPParticipantMigration aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                final boolean bIsFormSubmitted,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrorList aFormErrors)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aForm.addChild (getUIHandler ().createActionHeader ("Start a Participant Migration from this SMP to another SMP"));

    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    final ICommonsList <ISMPParticipantMigration> aExistingOutgoingMigrations = aParticipantMigrationMgr.getAllOutboundParticipantMigrations ();
    final ICommonsList <IParticipantIdentifier> aAffectedPIDs = new CommonsArrayList <> (aExistingOutgoingMigrations,
                                                                                         ISMPParticipantMigration::getParticipantIdentifier);

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service Group")
                                                 .setCtrl (new HCServiceGroupSelect (new RequestField (FIELD_PARTICIPANT_ID),
                                                                                     aDisplayLocale,
                                                                                     x -> aAffectedPIDs.containsNone (y -> x.getParticpantIdentifier ()
                                                                                                                            .hasSameContent (y))))
                                                 .setHelpText ("Select the Service Group to migrate to another SMP. Each Service Group can only be migrated once.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_PARTICIPANT_ID)));
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPParticipantMigration aSelectedObject,
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();

    final String sParticipantID = aWPEC.params ().getAsString (FIELD_PARTICIPANT_ID);
    final IParticipantIdentifier aParticipantID = aIdentifierFactory.parseParticipantIdentifier (sParticipantID);

    if (StringHelper.hasNoText (sParticipantID))
      aFormErrors.addFieldError (FIELD_PARTICIPANT_ID, "A Service Group must be selected.");
    else
      if (aParticipantID == null)
        aFormErrors.addFieldError (FIELD_PARTICIPANT_ID, "The selected Service Group does not exist.");
      else
      {
        if (aParticipantMigrationMgr.containsOutboundMigration (aParticipantID))
          aFormErrors.addFieldError (FIELD_PARTICIPANT_ID, "The migration of the selected Service Group is already in progress.");
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
          sMigrationKey = aCaller.prepareToMigrate (aParticipantID, SMPServerConfiguration.getSMLSMPID ());
        }
        catch (final GeneralSecurityException | BadRequestFault | InternalErrorFault | NotFoundFault | UnauthorizedFault ex)
        {
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
      aParticipantMigrationMgr.createOutboundParticipantMigration (aParticipantID, sMigrationKey);

      aWPEC.postRedirectGetInternal (success ().addChild (div ("The participant migration for '" +
                                                               aParticipantID.getURIEncoded () +
                                                               "' was successfully created."))
                                               .addChild (div ("The created migration key is ").addChild (code (sMigrationKey))));
    }
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();

    {
      final HCOL aOL = new HCOL ();
      aOL.addItem ("The migration is initiated on this SMP, and the SML is informed about the upcoming migration");
      aOL.addItem ("The other SMP, that is taking over the Service Group must acknowledge the migration by providing the same migration code (created by this SMP) to the SML");
      aOL.addItem ("If the migration was successful, the Service Group must be deleted on this SMP, ideally a temporary redirect is created. If the migration was cancelled no action is needed.");
      aNodeList.addChild (info (div ("The process of migrating a Service Group to another SMP consists of multiple steps:")).addChild (aOL)
                                                                                                                            .addChild (div ("Therefore each open Migration must either be finished (deleting the Service Group) or cancelled (no action taken).")));
    }

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addChild (new BootstrapButton ().addChild ("Start Participant Migration")
                                             .setOnClick (createCreateURL (aWPEC))
                                             .setIcon (EDefaultIcon.NEW));
    aNodeList.addChild (aToolbar);

    final HCTable aTable = new HCTable (new DTCol ("ID").setVisible (false),
                                        new DTCol ("Participant ID").setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Initiation").setDisplayType (EDTColType.DATETIME, aDisplayLocale),
                                        new DTCol ("Migration Key"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPParticipantMigration aCurObject : aParticipantMigrationMgr.getAllOutboundParticipantMigrations ())
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);
      final String sParticipantID = aCurObject.getParticipantIdentifier ().getURIEncoded ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (aCurObject.getID ());
      aRow.addCell (a (aViewLink).addChild (sParticipantID));
      aRow.addCell (PDTToString.getAsString (aCurObject.getInitiationDateTime (), aDisplayLocale));
      aRow.addCell (code (aCurObject.getMigrationKey ()));

      aRow.addCell (isActionAllowed (aWPEC, EWebPageFormAction.DELETE, aCurObject)
                                                                                   ? createDeleteLink (aWPEC,
                                                                                                       aCurObject,
                                                                                                       "Cancel Participant Migration of '" +
                                                                                                                   sParticipantID +
                                                                                                                   "'")
                                                                                   : createEmptyAction ());
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
