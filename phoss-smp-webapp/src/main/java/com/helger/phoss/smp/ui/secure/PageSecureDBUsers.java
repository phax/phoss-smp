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

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.user.ISMPUser;
import com.helger.phoss.smp.domain.user.ISMPUserEditable;
import com.helger.phoss.smp.domain.user.ISMPUserManager;
import com.helger.phoss.smp.migration.CSMPServerMigrations;
import com.helger.phoss.smp.ui.AbstractSMPWebPageForm;
import com.helger.photon.bootstrap4.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.pages.BootstrapPagesMenuConfigurator;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.mgr.PhotonBasicManager;
import com.helger.photon.security.util.SecurityHelper;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.column.DTCol;

/**
 * This class manages the uses, but only if the SQL backend is used. If the XML
 * backend is used, the regular ph-oton user management is to be used.
 *
 * @author Philip Helger
 */
public class PageSecureDBUsers extends AbstractSMPWebPageForm <ISMPUserEditable>
{
  private static final String FIELD_USERNAME = "username";
  private static final String FIELD_PASSWORD = "password";

  public PageSecureDBUsers (@Nonnull @Nonempty final String sID)
  {
    super (sID, "DB users");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPUserEditable, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final ISMPUserEditable aSelectedObject)
      {
        aForm.addChild (question ("Are you sure you want to delete user '" + aSelectedObject.getUserName () + "'?"));
      }

      @Override
      protected void performAction (@Nonnull final WebPageExecutionContext aWPEC,
                                    @Nonnull final ISMPUserEditable aSelectedObject)
      {
        final ISMPUserManager aUserManager = SMPMetaManager.getUserMgr ();
        if (aUserManager.deleteUser (aSelectedObject.getID ()).isChanged ())
          aWPEC.postRedirectGetInternal (success ("The user '" +
                                                  aSelectedObject.getUserName () +
                                                  "' was successfully deleted."));
        else
          aWPEC.postRedirectGetInternal (error ("Failed to delete user '" + aSelectedObject.getUserName () + "'."));
      }
    });
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPUserManager aUserManager = SMPMetaManager.getUserMgr ();
    if (!aUserManager.isSpecialUserManagementNeeded ())
    {
      final BootstrapWarnBox aWarnBox = warn (div ("No special user management is needed. The integrated user management must be used."));
      aNodeList.addChild (aWarnBox);
      if (SecurityHelper.isCurrentUserAssignedToUserGroup (CSMP.USERGROUP_ADMINISTRATORS_ID))
      {
        aNodeList.addChild (new BootstrapButton ().addChild ("Open user management")
                                                  .setOnClick (aWPEC.getLinkToMenuItem (BootstrapPagesMenuConfigurator.MENU_ADMIN_SECURITY_USER))
                                                  .setIcon (EDefaultIcon.YES));
      }
      else
        aWarnBox.addChild (div ("You don't have the permissions to manage users. Please contact your administrator."));
      return EValidity.INVALID;
    }

    if (PhotonBasicManager.getSystemMigrationMgr ()
                          .wasMigrationExecutedSuccessfully (CSMPServerMigrations.MIGRATION_ID_SQL_DBUSER_TO_REGULAR_USERS))
    {
      aWPEC.getNodeList ()
           .addChild (warn ().addChild (div ("The special SQL user management was dropped."))
                             .addChild (div ("All SQL users were migrated to regular ").addChild (new HCA (aWPEC.getLinkToMenuItem (BootstrapPagesMenuConfigurator.MENU_ADMIN_SECURITY_USER)).addChild ("users"))
                                                                                       .addChild (" and the ")
                                                                                       .addChild (code ("smp_user"))
                                                                                       .addChild (" table can safely be dropped.")));
      return EValidity.INVALID;
    }

    return super.isValidToDisplayPage (aWPEC);
  }

  @Override
  protected ISMPUserEditable getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                                @Nullable final String sID)
  {
    final ISMPUserManager aUserManager = SMPMetaManager.getUserMgr ();
    final ISMPUser aUser = aUserManager.getUserOfID (sID);
    return (ISMPUserEditable) aUser;
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPUserEditable aSelectedObject)
  {
    if (eFormAction.isDelete ())
      return SMPMetaManager.getServiceGroupMgr ().getSMPServiceGroupCountOfOwner (aSelectedObject.getID ()) == 0;
    return true;
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPUserEditable aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final BootstrapViewForm aTable = aNodeList.addAndReturnChild (new BootstrapViewForm ());
    aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("User name").setCtrl (aSelectedObject.getUserName ()));
    aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("Password").setCtrl (aSelectedObject.getPassword ()));
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPUserEditable aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                final boolean bFormSubmitted,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrorList aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();
    aForm.addChild (getUIHandler ().createActionHeader (bEdit ? "Edit user '" + aSelectedObject.getUserName () + "'"
                                                              : "Create new user"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("User name")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_USERNAME,
                                                                                         aSelectedObject == null ? null
                                                                                                                 : aSelectedObject.getUserName ())).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_USERNAME)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Password")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_PASSWORD,
                                                                                         aSelectedObject == null ? null
                                                                                                                 : aSelectedObject.getPassword ())))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_PASSWORD)));
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPUserEditable aSelectedObject,
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final ISMPUserManager aUserManager = SMPMetaManager.getUserMgr ();
    final boolean bEdit = eFormAction.isEdit ();
    final String sUserName = bEdit ? aSelectedObject.getUserName () : aWPEC.params ().getAsString (FIELD_USERNAME);
    final String sPassword = aWPEC.params ().getAsString (FIELD_PASSWORD);

    if (StringHelper.hasNoText (sUserName))
      aFormErrors.addFieldError (FIELD_USERNAME, "The user name may not be empty!");
    else
      if (!bEdit && aUserManager.getUserOfID (sUserName) != null)
        aFormErrors.addFieldError (FIELD_USERNAME, "Another user with the same user name already exists!");

    if (StringHelper.hasNoText (sPassword))
      aFormErrors.addFieldError (FIELD_PASSWORD, "The password may not be empty!");

    if (aFormErrors.isEmpty ())
    {
      if (bEdit)
      {
        if (aUserManager.updateUser (sUserName, sPassword).isSuccess ())
          aWPEC.postRedirectGetInternal (success ("User '" + sUserName + "' was successfully edited."));
        else
          aWPEC.postRedirectGetInternal (error ("Failed to edit user '" + sUserName + "'."));
      }
      else
      {
        if (aUserManager.createUser (sUserName, sPassword).isSuccess ())
          aWPEC.postRedirectGetInternal (success ("User '" + sUserName + "' was successfully created."));
        else
          aWPEC.postRedirectGetInternal (error ("Failed to create user '" + sUserName + "'."));
      }
    }
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPUserManager aUserManager = SMPMetaManager.getUserMgr ();

    // Toolbar on top
    final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
    aToolbar.addButtonNew ("Create new user", createCreateURL (aWPEC));

    // List existing
    final HCTable aTable = new HCTable (new DTCol ("Name").setInitialSorting (ESortOrder.ASCENDING),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());

    // Show only the bank accounts of the current client
    for (final ISMPUser aCurObject : aUserManager.getAllUsers ())
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (aCurObject.getUserName ()));
      aRow.addCell (createEditLink (aWPEC, aCurObject, "Edit '" + aCurObject.getUserName () + "'"),
                    new HCTextNode (" "),
                    createCopyLink (aWPEC, aCurObject, "Copy '" + aCurObject.getUserName () + "'"),
                    new HCTextNode (" "),
                    isActionAllowed (aWPEC,
                                     EWebPageFormAction.DELETE,
                                     (ISMPUserEditable) aCurObject) ? createDeleteLink (aWPEC, aCurObject, "Delete '" + aCurObject.getUserName () + "'") : createEmptyAction ());
    }

    final BootstrapDataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
