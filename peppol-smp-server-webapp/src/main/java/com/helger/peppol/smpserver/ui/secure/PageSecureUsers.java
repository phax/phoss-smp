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

import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.errorlist.FormErrors;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.smpserver.app.CApp;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserEditable;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageForm;
import com.helger.photon.bootstrap3.alert.BootstrapQuestionBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.pages.BootstrapPagesMenuConfigurator;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.RequestField;
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
public class PageSecureUsers extends AbstractSMPWebPageForm <ISMPUserEditable>
{
  private static final String FIELD_USERNAME = "username";
  private static final String FIELD_PASSWORD = "password";

  public PageSecureUsers (@Nonnull @Nonempty final String sID)
  {
    super (sID, "DB users");
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPUserManager aUserManager = SMPMetaManager.getUserMgr ();
    if (!aUserManager.isSpecialUserManagementNeeded ())
    {
      final BootstrapWarnBox aWarnBox = new BootstrapWarnBox ().addChild (new HCDiv ().addChild ("No special user management is needed. The integrated user management must be used."));
      aNodeList.addChild (aWarnBox);
      if (SecurityHelper.isCurrentUserAssignedToUserGroup (CApp.USERGROUP_ADMINISTRATORS_ID))
      {
        aNodeList.addChild (new BootstrapButton ().addChild ("Open user management")
                                                  .setOnClick (aWPEC.getLinkToMenuItem (BootstrapPagesMenuConfigurator.MENU_ADMIN_SECURITY_USER))
                                                  .setIcon (EDefaultIcon.YES));
      }
      else
        aWarnBox.addChild (new HCDiv ().addChild ("You don't have the permissions to manage users. Please contact your administrator."));
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
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrors aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();
    aForm.addChild (createActionHeader (bEdit ? "Edit user '" + aSelectedObject.getUserName () + "'"
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
                                                 @Nonnull final FormErrors aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final ISMPUserManager aUserManager = SMPMetaManager.getUserMgr ();
    final boolean bEdit = eFormAction.isEdit ();
    final String sUserName = bEdit ? aSelectedObject.getUserName () : aWPEC.getAttributeAsString (FIELD_USERNAME);
    final String sPassword = aWPEC.getAttributeAsString (FIELD_PASSWORD);

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
        aUserManager.updateUser (sUserName, sPassword);
        aWPEC.postRedirectGet (new BootstrapSuccessBox ().addChild ("User '" +
                                                                    sUserName +
                                                                    "' was successfully edited."));
      }
      else
      {
        aUserManager.createUser (sUserName, sPassword);
        aWPEC.postRedirectGet (new BootstrapSuccessBox ().addChild ("User '" +
                                                                    sUserName +
                                                                    "' was successfully created."));
      }
    }
  }

  @Override
  protected void showDeleteQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                  @Nonnull final BootstrapForm aForm,
                                  @Nonnull final ISMPUserEditable aSelectedObject)
  {
    aForm.addChild (new BootstrapQuestionBox ().addChild ("Are you sure you want to delete user '" +
                                                          aSelectedObject.getUserName () +
                                                          "'?"));
  }

  @Override
  protected void performDelete (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final ISMPUserEditable aSelectedObject)
  {
    final ISMPUserManager aUserManager = SMPMetaManager.getUserMgr ();
    aUserManager.deleteUser (aSelectedObject.getID ());
    aWPEC.postRedirectGet (new BootstrapSuccessBox ().addChild ("The user '" +
                                                                aSelectedObject.getUserName () +
                                                                "' was successfully deleted."));
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
                                     (ISMPUserEditable) aCurObject) ? createDeleteLink (aWPEC,
                                                                                        aCurObject,
                                                                                        "Delete '" +
                                                                                                    aCurObject.getUserName () +
                                                                                                    "'")
                                                                    : createEmptyAction ());
    }

    final BootstrapDataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
