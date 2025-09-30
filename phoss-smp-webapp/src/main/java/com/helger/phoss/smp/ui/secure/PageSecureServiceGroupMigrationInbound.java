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

import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.compare.ESortOrder;
import com.helger.base.state.EValidity;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsIterable;
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.format.PDTToString;
import com.helger.diagnostics.error.SingleError;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCTextArea;
import com.helger.html.hc.html.grouping.HCOL;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.tabular.IHCCell;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.smlclient.ManageParticipantIdentifierServiceCaller;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.pmigration.EParticipantMigrationState;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigration;
import com.helger.phoss.smp.domain.pmigration.ISMPParticipantMigrationManager;
import com.helger.phoss.smp.domain.pmigration.SMPParticipantMigration;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.AbstractSMPWebPageForm;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.secure.hc.HCUserSelect;
import com.helger.photon.bootstrap4.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.grid.BootstrapRow;
import com.helger.photon.bootstrap4.nav.BootstrapTabBox;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapTechnicalUI;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.url.ISimpleURL;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;

import jakarta.annotation.Nonnull;
import jakarta.annotation.Nullable;

/**
 * Class to migrate an existing Service Group from another SMP to this SMP. If a migration is
 * approved by the SMK/SML it means, the service group needs to be created on this SMP.
 *
 * @author Philip Helger
 */
public final class PageSecureServiceGroupMigrationInbound extends AbstractSMPWebPageForm <ISMPParticipantMigration>
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureServiceGroupMigrationInbound.class);
  private static final String FIELD_PARTICIPANT_ID_SCHEME = "participantidscheme";
  private static final String FIELD_PARTICIPANT_ID_VALUE = "participantidvalue";
  private static final String FIELD_OWNING_USER_ID = "owninguser";
  private static final String FIELD_MIGRATION_KEY = "migkey";
  private static final String FIELD_EXTENSION = "extension";

  public PageSecureServiceGroupMigrationInbound (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Migrate to this SMP");

    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPParticipantMigration, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final BootstrapForm aForm,
                                @Nullable final ISMPParticipantMigration aSelectedObject)
      {
        aForm.addChild (question ("Are you sure you want to delete the inbound Participant Migration for participant '" +
                                  aSelectedObject.getParticipantIdentifier ().getURIEncoded () +
                                  "'?"));
      }

      @Override
      protected void performAction (@Nonnull final WebPageExecutionContext aWPEC,
                                    @Nullable final ISMPParticipantMigration aSelectedObject)
      {
        final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
        if (aParticipantMigrationMgr.deleteParticipantMigrationOfID (aSelectedObject.getID ()).isChanged ())
          aWPEC.postRedirectGetInternal (success ("The selected Participant Migration was successfully deleted!"));
        else
          aWPEC.postRedirectGetInternal (error ("Failed to delete the selected Participant Migration!"));
      }
    });
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
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
    }

    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  @Nullable
  protected ISMPParticipantMigration getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                                        @Nullable final String sID)
  {
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    return aParticipantMigrationMgr.getParticipantMigrationOfID (sID);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPParticipantMigration aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of inbound Participant Migration"));

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
  protected String getCreateToolbarSubmitButtonText (@Nonnull final Locale aDisplayLocale)
  {
    return "Migrate";
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
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Migration Key")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_MIGRATION_KEY)))
                                                 .setHelpText ("The migration key received from the other SMP that was acknowledged by the SMK/SML.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_MIGRATION_KEY)));

    {
      final String sDefaultScheme = aIdentifierFactory.getDefaultParticipantIdentifierScheme ();
      final BootstrapRow aRow = new BootstrapRow ();
      aRow.createColumn (GS_IDENTIFIER_SCHEME)
          .addChild (new HCEdit (new RequestField (FIELD_PARTICIPANT_ID_SCHEME,
                                                   aSelectedObject != null ? aSelectedObject.getParticipantIdentifier ()
                                                                                            .getScheme ()
                                                                           : sDefaultScheme)).setPlaceholder ("Identifier scheme"));
      aRow.createColumn (GS_IDENTIFIER_VALUE)
          .addChild (new HCEdit (new RequestField (FIELD_PARTICIPANT_ID_VALUE,
                                                   aSelectedObject != null ? aSelectedObject.getParticipantIdentifier ()
                                                                                            .getValue () : null))
                                                                                                                 .setPlaceholder ("Identifier value"));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Participant ID")
                                                   .setCtrl (aRow)
                                                   .setHelpText ("The participant identifier for which the service group should be created. The left part is the identifier scheme" +
                                                                 (sDefaultScheme == null ? "" : " (default: " +
                                                                                                sDefaultScheme +
                                                                                                ")") +
                                                                 ", the right part is the identifier value (e.g. 9915:test)")
                                                   .setErrorList (aFormErrors.getListOfFields (FIELD_PARTICIPANT_ID_SCHEME,
                                                                                               FIELD_PARTICIPANT_ID_VALUE)));
    }

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Owning User")
                                                 .setCtrl (new HCUserSelect (new RequestField (FIELD_OWNING_USER_ID,
                                                                                               LoggedInUserManager.getInstance ()
                                                                                                                  .getCurrentUserID ()),
                                                                             aDisplayLocale))
                                                 .setHelpText ("The user who owns this entry. Only this user can make changes via the REST API.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_OWNING_USER_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Extension")
                                                 .setCtrl (new HCTextArea (new RequestField (FIELD_EXTENSION)))
                                                 .setHelpText ("Optional extension to the service group. If present it must be valid XML content!")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_EXTENSION)));
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPParticipantMigration aSelectedObject,
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();

    final String sMigrationKey = aWPEC.params ().getAsStringTrimmed (FIELD_MIGRATION_KEY);
    final String sParticipantIDScheme = aWPEC.params ().getAsStringTrimmed (FIELD_PARTICIPANT_ID_SCHEME);
    final String sParticipantIDValue = aWPEC.params ().getAsStringTrimmed (FIELD_PARTICIPANT_ID_VALUE);
    IParticipantIdentifier aParticipantID = null;
    boolean bParticipantSourceIsThisSMP = false;
    final String sOwningUserID = aWPEC.params ().getAsStringTrimmed (FIELD_OWNING_USER_ID);
    final IUser aOwningUser = PhotonSecurityManager.getUserMgr ().getUserOfID (sOwningUserID);
    final String sExtension = aWPEC.params ().getAsStringTrimmed (FIELD_EXTENSION);

    // validations
    if (StringHelper.isEmpty (sMigrationKey))
      aFormErrors.addFieldError (FIELD_MIGRATION_KEY, "The migration key must not be empty!");
    else
      if (!SMPParticipantMigration.isValidMigrationKey (sMigrationKey))
        aFormErrors.addFieldError (FIELD_MIGRATION_KEY,
                                   "The migration key is not valid. Please verify the received code is correct.");

    if (aIdentifierFactory.isParticipantIdentifierSchemeMandatory () && StringHelper.isEmpty (sParticipantIDScheme))
      aFormErrors.addFieldError (FIELD_PARTICIPANT_ID_SCHEME, "Participant ID scheme must not be empty!");
    else
      if (StringHelper.isEmpty (sParticipantIDValue))
        aFormErrors.addFieldError (FIELD_PARTICIPANT_ID_VALUE, "Participant ID value must not be empty!");
      else
      {
        aParticipantID = aIdentifierFactory.createParticipantIdentifier (sParticipantIDScheme, sParticipantIDValue);
        if (aParticipantID == null)
          aFormErrors.addFieldError (FIELD_PARTICIPANT_ID_VALUE, "The provided participant ID has an invalid syntax!");
        else
          if (aServiceGroupMgr.getSMPServiceGroupOfID (aParticipantID) != null)
          {
            // There is an edge case, when a participant is migrated from this SMP to this SMP.
            // So show the error only, if no outbound migration for the participant is present.
            if (aParticipantMigrationMgr.containsOutboundMigrationInProgress (aParticipantID))
            {
              bParticipantSourceIsThisSMP = true;
              LOGGER.info ("The inbound participant migration is for a participant from this SMP!");
            }
            else
              aFormErrors.addFieldError (FIELD_PARTICIPANT_ID_VALUE,
                                         "Another service group for the same participant ID is already present (may be case insensitive)!");
          }
      }

    if (StringHelper.isEmpty (sOwningUserID))
      aFormErrors.addFieldError (FIELD_OWNING_USER_ID, "Owning User must not be empty!");
    else
      if (aOwningUser == null)
        aFormErrors.addFieldError (FIELD_OWNING_USER_ID, "Provided owning user does not exist!");

    if (StringHelper.isNotEmpty (sExtension))
    {
      final IMicroDocument aDoc = MicroReader.readMicroXML (sExtension);
      if (aDoc == null)
        aFormErrors.addFieldError (FIELD_EXTENSION, "The extension must be XML content.");
    }

    final HCNodeList aRedirectNotes = new HCNodeList ();

    if (aFormErrors.isEmpty ())
    {
      // First get the SMK/SML migration result and if that was successful,
      // create the Service Group locally
      try
      {
        final ManageParticipantIdentifierServiceCaller aCaller = new ManageParticipantIdentifierServiceCaller (aSettings.getSMLInfo ());
        aCaller.setSSLSocketFactory (SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ());

        // SML call
        aCaller.migrate (aParticipantID, sMigrationKey, SMPServerConfiguration.getSMLSMPID ());
        LOGGER.info ("Successfully migrated '" +
                     aParticipantID.getURIEncoded () +
                     "' in the SML to this SMP using migration key '" +
                     sMigrationKey +
                     "'");
        aRedirectNotes.addChild (success ("Successfully migrated '" +
                                          aParticipantID.getURIEncoded () +
                                          "' in SML to this SMP using migration key ").addChild (code (sMigrationKey)));
      }
      catch (final Exception ex)
      {
        LOGGER.error ("Error invoking migrate on SML", ex);

        // Use a global field error here, to avoid that users must enter the
        // values over and over in case of error
        aFormErrors.add (SingleError.builderError ()
                                    .errorText ("Failed to confirm the migration for participant '" +
                                                aParticipantID.getURIEncoded () +
                                                "' in SML, hence the migration failed." +
                                                " Please check the participant identifier and the migration key.\n" +
                                                BootstrapTechnicalUI.getTechnicalDetailsString (ex,
                                                                                                CSMPServer.DEFAULT_LOCALE))
                                    .build ());
        if (false)
          aWPEC.postRedirectGetInternal (error ("Failed to confirm the migration for participant '" +
                                                aParticipantID.getURIEncoded () +
                                                "' in SML, hence the migration failed." +
                                                " Please check the participant identifier and the migration key.").addChild (SMPCommonUI.getTechnicalDetailsUI (ex)));
      }
    }

    if (aFormErrors.isEmpty ())
    {
      // Now create the service group locally (it was already checked that the
      // PID is available on this SMP)
      ISMPServiceGroup aSG = null;
      Exception aCaughtEx = null;
      if (bParticipantSourceIsThisSMP)
      {
        // Special case - existing migration from this SMP to this SMP
        aSG = aServiceGroupMgr.getSMPServiceGroupOfID (aParticipantID);
      }
      else
      {
        try
        {
          // Do NOT create in SMK/SML
          aSG = aServiceGroupMgr.createSMPServiceGroup (aOwningUser.getID (), aParticipantID, sExtension, false);
        }
        catch (final Exception ex)
        {
          aCaughtEx = ex;
        }

        if (aSG != null)
        {
          aRedirectNotes.addChild (success ("The new SMP Service Group for participant '" +
                                            aParticipantID.getURIEncoded () +
                                            "' was successfully created."));
        }
        else
        {
          aRedirectNotes.addChild (error ("Error creating the new SMP Service Group for participant '" +
                                          aParticipantID.getURIEncoded () +
                                          "'.").addChild (SMPCommonUI.getTechnicalDetailsUI (aCaughtEx)));
        }
      }

      // Remember internally
      if (aParticipantMigrationMgr.createInboundParticipantMigration (aParticipantID, sMigrationKey) != null)
      {
        aRedirectNotes.addChild (success ().addChild (div ("The participant migration for '" +
                                                           aParticipantID.getURIEncoded () +
                                                           "' with migration key ").addChild (code (sMigrationKey))
                                                                                   .addChild (" was successfully performed."))
                                           .addChild (div ("Please inform the source SMP that the migration was successful.")));
      }
      else
      {
        aRedirectNotes.addChild (error ("Failed to store the participant migration for '" +
                                        aParticipantID.getURIEncoded () +
                                        "'."));
      }

      if (bParticipantSourceIsThisSMP)
      {
        // Special case - existing migration from this SMP to this SMP
        // Also immediately close the outbound migration
        if (aParticipantMigrationMgr.setParticipantMigrationState (sMigrationKey, EParticipantMigrationState.MIGRATED)
                                    .isChanged ())
        {
          aRedirectNotes.addChild (success ("The outbound Participant Migration with ID '" +
                                            sMigrationKey +
                                            "' for '" +
                                            aParticipantID.getURIEncoded () +
                                            "' was successfully finalized!"));
        }
        else
        {
          aRedirectNotes.addChild (error ("Failed to finalize the outbound Participant Migration with ID '" +
                                          sMigrationKey +
                                          "' for '" +
                                          aParticipantID.getURIEncoded () +
                                          "'. Please see the logs for details."));
        }
      }

      aWPEC.postRedirectGetInternal (aRedirectNotes);
    }
  }

  @Nonnull
  private IHCNode _createTable (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final ICommonsIterable <ISMPParticipantMigration> aMigs,
                                @Nonnull final EParticipantMigrationState eState)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    final HCTable aTable = new HCTable (new DTCol ("ID").setVisible (false),
                                        new DTCol ("Participant ID").setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Migration").setDisplayType (EDTColType.DATETIME, aDisplayLocale),
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
      aActionCell.addChild (createDeleteLink (aWPEC,
                                              aCurObject,
                                              "Delete Participant Migration of '" + sParticipantID + "'"));
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    return new HCNodeList ().addChild (aTable).addChild (aDataTables);
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPParticipantMigrationManager aParticipantMigrationMgr = SMPMetaManager.getParticipantMigrationMgr ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();

    {
      final HCOL aOL = new HCOL ();
      aOL.addItem ("The migration was initiated by another SMP, and the SML must have been informed about the upcoming migration");
      aOL.addItem ("This SMP, that is taking over the Service Group, must acknowledge the migration by providing the same migration code (created by the other SMP) to the SML");
      aOL.addItem ("If the migration was successful, the Service Group must be deleted from the other SMP, ideally a temporary redirect to the new SMP is created");
      aNodeList.addChild (info ().addChild (div ("The process of migrating a Service Group to another SMP consists of multiple steps:"))
                                 .addChild (aOL)
                                 .addChild (div ("If a Migration is unsuccessful, it can be retried later.")));
    }

    EValidity eCanMigrate = EValidity.VALID;
    if (aSettings.getSMLInfo () == null)
    {
      final BootstrapWarnBox aWarnBox = aNodeList.addAndReturnChild (warn ().addChild (div ("No valid SML Configuration is selected hence no participant can be migrated."))
                                                                            .addChild (new BootstrapButton ().addChild ("Select SML Configuration in the Settings")
                                                                                                             .setOnClick (aWPEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS))
                                                                                                             .setIcon (EDefaultIcon.EDIT)));
      if (aSettings.isSMLEnabled () || aSettings.isSMLRequired ())
      {
        aWarnBox.addChild (div (new BootstrapButton ().addChild ("Create a new SML Configuration")
                                                      .setOnClick (createCreateURL (aWPEC,
                                                                                    CMenuSecure.MENU_SML_CONFIGURATION))
                                                      .setIcon (EDefaultIcon.YES)));
      }
      eCanMigrate = EValidity.INVALID;
    }
    else
      if (!aSettings.isSMLEnabled ())
      {
        aNodeList.addChild (warn ().addChild (div ("SML Connection is not enabled hence no participant can be migrated."))
                                   .addChild (div (new BootstrapButton ().addChild ("Enable SML in the Settings")
                                                                         .setOnClick (aWPEC.getLinkToMenuItem (CMenuSecure.MENU_SMP_SETTINGS))
                                                                         .setIcon (EDefaultIcon.EDIT))));
        eCanMigrate = EValidity.INVALID;
      }

    {
      final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
      aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
      aToolbar.addChild (new BootstrapButton ().addChild ("Start Participant Migration")
                                               .setOnClick (createCreateURL (aWPEC))
                                               .setDisabled (eCanMigrate.isInvalid ())
                                               .setIcon (EDefaultIcon.NEW));
      aNodeList.addChild (aToolbar);
    }

    final BootstrapTabBox aTabBox = aNodeList.addAndReturnChild (new BootstrapTabBox ());

    final ICommonsList <ISMPParticipantMigration> aAllMigs = aParticipantMigrationMgr.getAllInboundParticipantMigrations (null);
    for (final EParticipantMigrationState eState : EParticipantMigrationState.values ())
      if (eState.isInboundState ())
      {
        final ICommonsList <ISMPParticipantMigration> aMatchingMigs = aAllMigs.getAll (x -> x.getState () == eState);
        aTabBox.addTab (eState.getID (),
                        eState.getDisplayName () + " (" + aMatchingMigs.size () + ")",
                        _createTable (aWPEC, aMatchingMigs, eState));
      }
  }
}
