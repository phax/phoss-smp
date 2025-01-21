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

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.html.hc.html.forms.HCCheckBox;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.smp.ESMPTransportProfile;
import com.helger.peppol.smp.ESMPTransportProfileState;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.transportprofile.ISMPTransportProfileManager;
import com.helger.phoss.smp.ui.AbstractSMPWebPageForm;
import com.helger.photon.bootstrap4.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap4.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandler;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldBoolean;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EShowList;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;

public class PageSecureTransportProfiles extends AbstractSMPWebPageForm <ISMPTransportProfile>
{
  private static final String FIELD_ID = "id";
  private static final String FIELD_NAME = "name";
  private static final String FIELD_DEPRECATED = "deprecated";
  private static final boolean DEFAULT_DEPRECATED = false;
  private static final String ACTION_ENSURE_DEFAULT = "ensure-default";

  private static final ICommonsSet <ESMPTransportProfile> DEFAULT_PROFILES = new CommonsHashSet <> ();
  private static final ICommonsSet <String> DEFAULT_PROFILE_IDS;
  static
  {
    // Use all active ones
    DEFAULT_PROFILES.addAll (ESMPTransportProfile.values (), x -> x.getState () == ESMPTransportProfileState.ACTIVE);
    DEFAULT_PROFILE_IDS = new CommonsHashSet <> (DEFAULT_PROFILES, ESMPTransportProfile::getID);
  }

  public PageSecureTransportProfiles (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Transport Profiles");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPTransportProfile, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final BootstrapForm aForm,
                                @Nullable final ISMPTransportProfile aSelectedObject)
      {
        aForm.addChild (question ("Are you sure you want to delete the transport profile '" +
                                  aSelectedObject.getID () +
                                  "'?"));
      }

      @Override
      protected void performAction (@Nonnull final WebPageExecutionContext aWPEC,
                                    @Nullable final ISMPTransportProfile aSelectedObject)
      {
        final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();
        if (aTransportProfileMgr.deleteSMPTransportProfile (aSelectedObject.getID ()).isChanged ())
          aWPEC.postRedirectGetInternal (success ("The transport profile '" +
                                                  aSelectedObject.getID () +
                                                  "' was successfully deleted!"));
        else
          aWPEC.postRedirectGetInternal (error ("Failed to delete transport profile '" +
                                                aSelectedObject.getID () +
                                                "'!"));
      }
    });
    addCustomHandler (ACTION_ENSURE_DEFAULT,
                      new AbstractBootstrapWebPageActionHandler <ISMPTransportProfile, WebPageExecutionContext> (false)
                      {
                        @Nonnull
                        public EShowList handleAction (@Nonnull final WebPageExecutionContext aWPEC,
                                                       @Nullable final ISMPTransportProfile aSelectedObject)
                        {
                          final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();
                          final BootstrapSuccessBox aSuccessBox = success ();
                          final BootstrapErrorBox aErrorBox = error ();
                          for (final ESMPTransportProfile eTP : DEFAULT_PROFILES)
                            if (!aTransportProfileMgr.containsSMPTransportProfileWithID (eTP.getID ()))
                            {
                              if (aTransportProfileMgr.createSMPTransportProfile (eTP.getID (),
                                                                                  eTP.getName (),
                                                                                  eTP.getState () ==
                                                                                                  ESMPTransportProfileState.DEPRECATED) !=
                                  null)
                              {
                                aSuccessBox.addChild (div ("Successfully created the transport profile '" +
                                                           eTP.getName () +
                                                           "' with ID '" +
                                                           eTP.getID () +
                                                           "'"));
                              }
                              else
                              {
                                aErrorBox.addChild (div ("Failed to create the transport profile '" +
                                                         eTP.getName () +
                                                         "' with ID '" +
                                                         eTP.getID () +
                                                         "'"));
                              }
                            }
                          final HCNodeList aSummary = new HCNodeList ().addChild (aSuccessBox.hasChildren () ? aSuccessBox
                                                                                                             : null)
                                                                       .addChild (aErrorBox.hasChildren () ? aErrorBox
                                                                                                           : null);
                          if (aSummary.hasChildren ())
                            aWPEC.postRedirectGetInternal (aSummary);
                          else
                            aWPEC.getNodeList ()
                                 .addChild (info ("All default transport profiles are already registered."));
                          return EShowList.SHOW_LIST;
                        }
                      });
  }

  @Override
  protected ISMPTransportProfile getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                                    @Nullable final String sID)
  {
    final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();
    return aTransportProfileMgr.getSMPTransportProfileOfID (sID);
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPTransportProfile aSelectedObject)
  {
    if (eFormAction.isDelete ())
    {
      // Can only delete non-standard protocol
      if (DEFAULT_PROFILE_IDS.contains (aSelectedObject.getID ()))
        return false;

      // If the transport profile is already used, it cannot be deleted
      // This might be slow depending on the implementation
      final ISMPServiceInformationManager aServiceInformationMgr = SMPMetaManager.getServiceInformationMgr ();
      if (aServiceInformationMgr.containsAnyEndpointWithTransportProfile (aSelectedObject.getID ()))
        return false;
    }

    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPTransportProfile aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of transport profile '" +
                                                            aSelectedObject.getID () +
                                                            "'"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("ID").setCtrl (aSelectedObject.getID ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Name").setCtrl (aSelectedObject.getName ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Deprecated?")
                                                 .setCtrl (EPhotonCoreText.getYesOrNo (aSelectedObject.getState () ==
                                                                                       ESMPTransportProfileState.DEPRECATED,
                                                                                       aDisplayLocale)));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPTransportProfile aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                final boolean bFormSubmitted,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrorList aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();

    aForm.addChild (getUIHandler ().createActionHeader (bEdit ? "Edit transport profile '" +
                                                                aSelectedObject.getID () +
                                                                "'" : "Create new transport profile"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("ID")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_ID,
                                                                                         aSelectedObject != null
                                                                                                                 ? aSelectedObject.getID ()
                                                                                                                 : null)).setReadOnly (bEdit))
                                                 .setHelpText ("The ID of the transport profile to be used in SMP endpoints.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Name")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_NAME,
                                                                                         aSelectedObject != null
                                                                                                                 ? aSelectedObject.getName ()
                                                                                                                 : null)))
                                                 .setHelpText ("The name of the transport profile")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_NAME)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Deprecated?")
                                                 .setCtrl (new HCCheckBox (new RequestFieldBoolean (FIELD_DEPRECATED,
                                                                                                    aSelectedObject !=
                                                                                                                      null ? aSelectedObject.getState () ==
                                                                                                                             ESMPTransportProfileState.DEPRECATED
                                                                                                                           : DEFAULT_DEPRECATED)))
                                                 .setHelpText ("Is the transport profile deprecated?")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_DEPRECATED)));
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPTransportProfile aSelectedObject,
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();

    // Never edit ID
    final String sID = bEdit ? aSelectedObject.getID () : aWPEC.params ().getAsString (FIELD_ID);
    final String sName = aWPEC.params ().getAsString (FIELD_NAME);
    final boolean bIsDeprecated = aWPEC.params ().getAsBoolean (FIELD_DEPRECATED, DEFAULT_DEPRECATED);

    // validations
    if (StringHelper.hasNoText (sID))
      aFormErrors.addFieldError (FIELD_ID, "Transport profile ID must not be empty!");
    else
      if (!bEdit)
      {
        final ISMPTransportProfile aOther = aTransportProfileMgr.getSMPTransportProfileOfID (sID);
        if (aOther != null)
          aFormErrors.addFieldError (FIELD_ID, "Another transport profile with the same name already exists!");
      }

    if (StringHelper.hasNoText (sName))
      aFormErrors.addFieldError (FIELD_NAME, "The transport profile name must not be empty!");

    if (aFormErrors.isEmpty ())
    {
      if (bEdit)
      {
        if (aTransportProfileMgr.updateSMPTransportProfile (sID, sName, bIsDeprecated).isChanged ())
          aWPEC.postRedirectGetInternal (success ("The transport profile '" + sID + "' was successfully edited."));
        else
          aWPEC.postRedirectGetInternal (info ("No change editing transport profile '" + sID + "'."));
      }
      else
      {
        if (aTransportProfileMgr.createSMPTransportProfile (sID, sName, bIsDeprecated) != null)
          aWPEC.postRedirectGetInternal (success ("The new transport profile '" + sID + "' was successfully created."));
        else
          aWPEC.postRedirectGetInternal (error ("Failed to create transport profile '" + sID + "'."));
      }
    }
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPTransportProfileManager aTransportProfileMgr = SMPMetaManager.getTransportProfileMgr ();

    aNodeList.addChild (info ("This page lets you create custom transport profiles that can be used in service information endpoints."));

    final ICommonsList <ISMPTransportProfile> aList = aTransportProfileMgr.getAllSMPTransportProfiles ();

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addChild (new BootstrapButton ().addChild ("Create new transport profile")
                                             .setOnClick (createCreateURL (aWPEC))
                                             .setIcon (EDefaultIcon.NEW));

    final ICommonsSet <String> aExistingIDs = new CommonsHashSet <> (aList, ISMPTransportProfile::getID);
    if (!aExistingIDs.containsAll (DEFAULT_PROFILE_IDS))
    {
      // Show button only on demand
      aToolbar.addChild (new BootstrapButton ().addChild ("Ensure all default transport profiles")
                                               .setOnClick (aWPEC.getSelfHref ()
                                                                 .add (CPageParam.PARAM_ACTION, ACTION_ENSURE_DEFAULT))
                                               .setIcon (EDefaultIcon.PLUS));
    }
    aNodeList.addChild (aToolbar);

    final HCTable aTable = new HCTable (new DTCol ("ID").setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Name"),
                                        new DTCol ("Deprecated?"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPTransportProfile aCurObject : aList)
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (aCurObject.getID ()));
      aRow.addCell (aCurObject.getName ());
      aRow.addCell (EPhotonCoreText.getYesOrNo (aCurObject.getState () == ESMPTransportProfileState.DEPRECATED,
                                                aDisplayLocale));

      aRow.addCell (createEditLink (aWPEC, aCurObject, "Edit " + aCurObject.getID ()),
                    new HCTextNode (" "),
                    createCopyLink (aWPEC, aCurObject, "Copy " + aCurObject.getID ()),
                    new HCTextNode (" "),
                    isActionAllowed (aWPEC, EWebPageFormAction.DELETE, aCurObject) ? createDeleteLink (aWPEC,
                                                                                                       aCurObject,
                                                                                                       "Delete " +
                                                                                                                   aCurObject.getID ())
                                                                                   : createEmptyAction ());
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
