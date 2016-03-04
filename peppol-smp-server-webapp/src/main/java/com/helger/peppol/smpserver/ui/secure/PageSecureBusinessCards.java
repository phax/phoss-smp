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

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.errorlist.FormErrors;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCEM;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.identifier.participant.SimpleParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardContact;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardEntity;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardIdentifier;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageForm;
import com.helger.peppol.smpserver.ui.secure.hc.HCServiceGroupSelect;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapQuestionBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.famfam.EFamFamIcon;

@WorkInProgress
public final class PageSecureBusinessCards extends AbstractSMPWebPageForm <ISMPBusinessCard>
{
  private static final String FIELD_SERVICE_GROUP_ID = "sgid";

  public PageSecureBusinessCards (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Business Cards");
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
    if (aServiceGroupManager.getSMPServiceGroupCount () == 0)
    {
      aNodeList.addChild (new BootstrapWarnBox ().addChild ("No Service Group is present! At least one Service Group must be present to create a Business Card for it."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new Service Group")
                                                .setOnClick (createCreateURL (aWPEC, CMenuSecure.MENU_SERVICE_GROUPS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }
    return super.isValidToDisplayPage (aWPEC);
  }

  @Override
  @Nullable
  protected ISMPBusinessCard getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                                @Nullable final String sID)
  {
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    return aBusinessCardMgr.getSMPBusinessCardOfID (sID);
  }

  @Override
  protected boolean isActionAllowed (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final EWebPageFormAction eFormAction,
                                     @Nullable final ISMPBusinessCard aSelectedObject)
  {
    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPBusinessCard aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aNodeList.addChild (createActionHeader ("Show details of Business Card"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service Group")
                                                 .setCtrl (new HCA (createViewURL (aWPEC,
                                                                                   CMenuSecure.MENU_SERVICE_GROUPS,
                                                                                   aSelectedObject.getServiceGroup ())).addChild (aSelectedObject.getServiceGroupID ())));

    int nIndex = 0;
    for (final SMPBusinessCardEntity aEntity : aSelectedObject.getAllEntities ())
    {
      ++nIndex;
      aForm.addChild (createDataGroupHeader ("Entity " + nIndex));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Name").setCtrl (aEntity.getName ()));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Country code").setCtrl (aEntity.getCountryCode ()));
      if (aEntity.hasGeographicalInformation ())
      {
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Geographical information")
                                                     .setCtrl (HCExtHelper.nl2divList (aEntity.getGeographicalInformation ())));
      }
      if (aEntity.hasIdentifiers ())
      {
        final BootstrapTable aTable = new BootstrapTable (HCCol.star (), HCCol.star ());
        aTable.addHeaderRow ().addCells ("Scheme", "Value");
        for (final SMPBusinessCardIdentifier aIdentifier : aEntity.getIdentifiers ())
          aTable.addBodyRow ().addCell (aIdentifier.getScheme (), aIdentifier.getValue ());
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Identifiers").setCtrl (aTable));
      }
      if (aEntity.hasWebsiteURIs ())
      {
        final HCNodeList aNL = new HCNodeList ();
        for (final String sWebsiteURI : aEntity.getWebsiteURIs ())
          aNL.addChild (new HCDiv ().addChild (HCA.createLinkedWebsite (sWebsiteURI)));
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Website URIs").setCtrl (aNL));
      }
      if (aEntity.hasContacts ())
      {
        final BootstrapTable aTable = new BootstrapTable (HCCol.star (), HCCol.star (), HCCol.star (), HCCol.star ());
        aTable.addHeaderRow ().addCells ("Type", "Name", "Phone number", "Email address");
        for (final SMPBusinessCardContact aContact : aEntity.getContacts ())
          aTable.addBodyRow ().addCell (aContact.getType (),
                                        aContact.getName (),
                                        aContact.getPhoneNumber (),
                                        aContact.getEmail ());
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Contacts").setCtrl (aTable));
      }
      if (aEntity.hasAdditionalInformation ())
      {
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Additional information")
                                                     .setCtrl (HCExtHelper.nl2divList (aEntity.getAdditionalInformation ())));
      }
      if (aEntity.hasRegistrationDate ())
      {
        aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Registration date")
                                                     .setCtrl (PDTToString.getAsString (aEntity.getRegistrationDate (),
                                                                                        aDisplayLocale)));
      }
    }

    if (nIndex == 0)
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Entity")
                                                   .setCtrl (new HCEM ().addChild ("none defined")));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPBusinessCard aSelectedObject,
                                                 @Nonnull final FormErrors aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

    final String sServiceGroupID = bEdit ? aSelectedObject.getServiceGroupID ()
                                         : aWPEC.getAttributeAsString (FIELD_SERVICE_GROUP_ID);
    ISMPServiceGroup aServiceGroup = null;

    // validations
    if (StringHelper.isEmpty (sServiceGroupID))
      aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "A Service Group must be selected!");
    else
    {
      aServiceGroup = aServiceGroupManager.getSMPServiceGroupOfID (SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID));
      if (aServiceGroup == null)
        aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "The provided Service Group does not exist!");
    }

    // TODO

    if (aFormErrors.isEmpty ())
    {
      final List <SMPBusinessCardEntity> aEntities = new ArrayList <> ();
      // TODO
      aBusinessCardMgr.createOrUpdateSMPBusinessCard (aServiceGroup, aEntities);
      aWPEC.postRedirectGet (new BootstrapSuccessBox ().addChild ("The Business Card for service group '" +
                                                                  aServiceGroup.getID () +
                                                                  "' was successfully saved."));
    }
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPBusinessCard aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrors aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aForm.addChild (createActionHeader (bEdit ? "Edit Business Card" : "Create new Business Card"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service Group")
                                                 .setCtrl (new HCServiceGroupSelect (new RequestField (FIELD_SERVICE_GROUP_ID,
                                                                                                       aSelectedObject != null ? aSelectedObject.getServiceGroupID ()
                                                                                                                               : null),
                                                                                     aDisplayLocale).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_GROUP_ID)));

    // TODO
  }

  @Override
  protected void showDeleteQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                  @Nonnull final BootstrapForm aForm,
                                  @Nonnull final ISMPBusinessCard aSelectedObject)
  {
    aForm.addChild (new BootstrapQuestionBox ().addChild ("Are you sure you want to delete the Business Card for service group '" +
                                                          aSelectedObject.getServiceGroupID () +
                                                          "'?"));
  }

  @Override
  protected void performDelete (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final ISMPBusinessCard aSelectedObject)
  {
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    if (aBusinessCardMgr.deleteSMPBusinessCard (aSelectedObject).isChanged ())
      aWPEC.postRedirectGet (new BootstrapSuccessBox ().addChild ("The selected Business Card was successfully deleted!"));
    else
      aWPEC.postRedirectGet (new BootstrapErrorBox ().addChild ("Failed to delete the selected Business Card!"));
  }

  @Nonnull
  private IHCNode _createActionCell (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPBusinessCard aCurObject)
  {
    final String sDisplayName = aCurObject.getServiceGroupID ();
    return new HCNodeList ().addChildren (new HCA (createEditURL (aWPEC, aCurObject)).setTitle ("Edit " + sDisplayName)
                                                                                     .addChild (EDefaultIcon.EDIT.getAsNode ()),
                                          new HCTextNode (" "),
                                          new HCA (createCopyURL (aWPEC,
                                                                  aCurObject)).setTitle ("Create a copy of " +
                                                                                         sDisplayName)
                                                                              .addChild (EDefaultIcon.COPY.getAsNode ()),
                                          new HCTextNode (" "),
                                          new HCA (createDeleteURL (aWPEC,
                                                                    aCurObject)).setTitle ("Delete " + sDisplayName)
                                                                                .addChild (EDefaultIcon.DELETE.getAsNode ()),
                                          new HCTextNode (" "),
                                          new HCA (LinkHelper.getURLWithServerAndContext ("businesscard/" +
                                                                                          aCurObject.getServiceGroup ()
                                                                                                    .getParticpantIdentifier ()
                                                                                                    .getURIPercentEncoded ())).setTitle ("Perform SMP query on " +
                                                                                                                                         sDisplayName)
                                                                                                                              .setTargetBlank ()
                                                                                                                              .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()));
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new Business Card", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aNodeList.addChild (aToolbar);

    final HCTable aTable = new HCTable (new DTCol ("Service Group").setDataSort (0, 1)
                                                                   .setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Name"),
                                        new DTCol ("Country"),
                                        new DTCol ("GeoInfo"),
                                        new DTCol ("Identifiers"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPBusinessCard aCurObject : aBusinessCardMgr.getAllSMPBusinessCards ())
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);
      final String sDisplayName = aCurObject.getServiceGroupID ();

      if (aCurObject.getEntityCount () == 0)
      {
        final HCRow aRow = aTable.addBodyRow ();
        aRow.addCell (new HCA (aViewLink).addChild (sDisplayName));
        for (int i = 1; i < aTable.getColumnCount () - 1; ++i)
          aRow.addCell ();
        aRow.addCell (_createActionCell (aWPEC, aCurObject));
      }
      else
      {
        for (final SMPBusinessCardEntity aEntity : aCurObject.getAllEntities ())
        {
          final HCRow aRow = aTable.addBodyRow ();
          aRow.addCell (new HCA (aViewLink).addChild (sDisplayName));
          aRow.addCell (aEntity.getName ());
          aRow.addCell (aEntity.getCountryCode ());
          aRow.addCell (aEntity.getGeographicalInformation ());
          {
            final HCNodeList aIdentifiers = new HCNodeList ();
            for (final SMPBusinessCardIdentifier aIdentifier : aEntity.getIdentifiers ())
              aIdentifiers.addChild (new HCDiv ().addChild (aIdentifier.getScheme ())
                                                 .addChild (" - ")
                                                 .addChild (aIdentifier.getValue ()));
          }
          aRow.addCell (_createActionCell (aWPEC, aCurObject));
        }
      }
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);

    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
