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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import org.joda.time.LocalDate;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.errorlist.FormErrors;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.locale.country.CountryCache;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.datetime.format.PDTFromString;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCEM;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.html.jquery.JQuery;
import com.helger.html.jquery.JQueryAjaxBuilder;
import com.helger.html.jscode.JSAnonymousFunction;
import com.helger.html.jscode.JSAssocArray;
import com.helger.html.jscode.JSPackage;
import com.helger.html.jscode.JSVar;
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
import com.helger.peppol.smpserver.ui.ajax.CAjaxSecure;
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
import com.helger.photon.bootstrap3.panel.BootstrapPanel;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.bootstrap3.uictrls.datetimepicker.BootstrapDateTimePicker;
import com.helger.photon.core.ajax.response.AjaxHtmlResponse;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldDate;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.uicore.html.select.HCCountrySelect;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.js.JSJQueryHelper;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.autosize.HCTextAreaAutosize;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.famfam.EFamFamIcon;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.servlet.request.IRequestParamMap;
import com.helger.web.servlet.request.RequestParamMap;

@WorkInProgress
public final class PageSecureBusinessCards extends AbstractSMPWebPageForm <ISMPBusinessCard>
{
  private static final String FIELD_SERVICE_GROUP_ID = "sgid";
  private static final String PREFIX_ENTITY = "entity";
  private static final String SUFFIX_NAME = "name";
  private static final String SUFFIX_COUNTRY_CODE = "country";
  private static final String SUFFIX_GEO_INFO = "geoinfo";
  private static final String SUFFIX_ADDITIONAL_INFO = "additional";
  private static final String SUFFIX_REG_DATE = "regdate";

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
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Country code")
                                                   .setCtrl (CountryCache.getInstance ()
                                                                         .getCountry (aEntity.getCountryCode ())
                                                                         .getDisplayCountry (aDisplayLocale) +
                                                             " [" +
                                                             aEntity.getCountryCode () +
                                                             "]"));
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
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final boolean bEdit = eFormAction.isEdit ();
    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

    final String sServiceGroupID = bEdit ? aSelectedObject.getServiceGroupID ()
                                         : aWPEC.getAttributeAsString (FIELD_SERVICE_GROUP_ID);
    ISMPServiceGroup aServiceGroup = null;
    final List <SMPBusinessCardEntity> aSMPEntities = new ArrayList <> ();

    // validations
    if (StringHelper.isEmpty (sServiceGroupID))
      aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "A Service Group must be selected!");
    else
    {
      aServiceGroup = aServiceGroupManager.getSMPServiceGroupOfID (SimpleParticipantIdentifier.createFromURIPartOrNull (sServiceGroupID));
      if (aServiceGroup == null)
        aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "The provided Service Group does not exist!");
      else
        if (!bEdit)
        {
          final ISMPBusinessCard aExistingBusinessCard = aBusinessCardMgr.getSMPBusinessCardOfID (sServiceGroupID);
          if (aExistingBusinessCard != null)
            aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID,
                                       "The selected Service Group already has a Business Card assigned!");
        }
    }

    final IRequestParamMap aEntities = aWPEC.getRequestParamMap ().getMap (PREFIX_ENTITY);
    if (aEntities != null)
      for (final String sEntityRowID : aEntities.keySet ())
      {
        final Map <String, String> aEntityRow = aEntities.getValueMap (sEntityRowID);
        final int nErrors = aFormErrors.getFieldItemCount ();

        final String sFieldName = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityRowID, SUFFIX_NAME);
        final String sName = aEntityRow.get (SUFFIX_NAME);
        if (StringHelper.hasNoText (sName))
          aFormErrors.addFieldError (sFieldName, "The Name of the Entity must be provided!");

        final String sFieldCountryCode = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                                       sEntityRowID,
                                                                       SUFFIX_COUNTRY_CODE);
        final String sCountryCode = aEntityRow.get (SUFFIX_COUNTRY_CODE);
        if (StringHelper.hasNoText (sCountryCode))
          aFormErrors.addFieldError (sFieldCountryCode, "The Country Code of the Entity must be provided!");

        final String sGeoInfo = aEntityRow.get (SUFFIX_GEO_INFO);
        final String sAdditionalInfo = aEntityRow.get (SUFFIX_ADDITIONAL_INFO);

        final String sFieldRegDate = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityRowID, SUFFIX_REG_DATE);
        final String sRegDate = aEntityRow.get (SUFFIX_REG_DATE);
        final LocalDate aRegDate = PDTFromString.getLocalDateFromString (sRegDate, aDisplayLocale);
        if (aRegDate == null && StringHelper.hasText (sRegDate))
          aFormErrors.addFieldError (sFieldRegDate, "The entered registration date is invalid!");

        if (aFormErrors.getFieldItemCount () == nErrors)
        {
          final boolean bIsNew = sEntityRowID.startsWith ("tmp");
          final SMPBusinessCardEntity aEntity = bIsNew ? new SMPBusinessCardEntity ()
                                                       : new SMPBusinessCardEntity (sEntityRowID);
          aEntity.setName (sName);
          aEntity.setCountryCode (sCountryCode);
          aEntity.setGeographicalInformation (sGeoInfo);
          aEntity.setAdditionalInformation (sAdditionalInfo);
          aEntity.setRegistrationDate (aRegDate);
          aSMPEntities.add (aEntity);
        }
      }

    if (aFormErrors.isEmpty ())
    {
      // Store in a consistent manner
      Collections.sort (aSMPEntities, new Comparator <SMPBusinessCardEntity> ()
      {
        public int compare (final SMPBusinessCardEntity o1, final SMPBusinessCardEntity o2)
        {
          return o1.getName ().compareToIgnoreCase (o2.getName ());
        }
      });
      aBusinessCardMgr.createOrUpdateSMPBusinessCard (aServiceGroup, aSMPEntities);
      aWPEC.postRedirectGet (new BootstrapSuccessBox ().addChild ("The Business Card for service group '" +
                                                                  aServiceGroup.getID () +
                                                                  "' was successfully saved."));
    }
  }

  @Nonnull
  public static IHCNode createEntityInputForm (@Nonnull final LayoutExecutionContext aLEC,
                                               @Nullable final SMPBusinessCardEntity aExistingEntity,
                                               @Nullable final String sExistingID,
                                               @Nonnull final FormErrors aFormErrors)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final String sEntityID = aExistingEntity != null ? aExistingEntity.getID ()
                                                     : StringHelper.hasText (sExistingID) ? sExistingID
                                                                                          : "tmp" +
                                                                                            Integer.toString (GlobalIDFactory.getNewIntID ());

    final BootstrapPanel aPanel = new BootstrapPanel ().setID (sEntityID);
    aPanel.getOrCreateHeader ().addChild ("Business Card Entity");
    final HCDiv aBody = aPanel.getBody ();

    final BootstrapViewForm aForm = aBody.addAndReturnChild (new BootstrapViewForm ());

    final String sFieldName = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityID, SUFFIX_NAME);
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Name")
                                                 .setCtrl (new HCEdit (new RequestField (sFieldName,
                                                                                         aExistingEntity == null ? null
                                                                                                                 : aExistingEntity.getName ())))
                                                 .setErrorList (aFormErrors.getListOfField (sFieldName)));

    final String sFieldCountryCode = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityID, SUFFIX_COUNTRY_CODE);
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Country")
                                                 .setCtrl (new HCCountrySelect (new RequestField (sFieldCountryCode,
                                                                                                  aExistingEntity == null ? null
                                                                                                                          : aExistingEntity.getCountryCode ()),
                                                                                aDisplayLocale))
                                                 .setErrorList (aFormErrors.getListOfField (sFieldCountryCode)));

    final String sFieldGeoInfo = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityID, SUFFIX_GEO_INFO);
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Geographical Information")
                                                 .setCtrl (new HCTextAreaAutosize (new RequestField (sFieldGeoInfo,
                                                                                                     aExistingEntity == null ? null
                                                                                                                             : aExistingEntity.getGeographicalInformation ())))
                                                 .setErrorList (aFormErrors.getListOfField (sFieldGeoInfo)));

    final String sFieldAdditionalInfo = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityID, SUFFIX_ADDITIONAL_INFO);
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Additional Information")
                                                 .setCtrl (new HCTextAreaAutosize (new RequestField (sFieldAdditionalInfo,
                                                                                                     aExistingEntity == null ? null
                                                                                                                             : aExistingEntity.getAdditionalInformation ())))
                                                 .setErrorList (aFormErrors.getListOfField (sFieldAdditionalInfo)));

    final String sFieldRegDate = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityID, SUFFIX_REG_DATE);
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Registration Date")
                                                 .setCtrl (new BootstrapDateTimePicker (new RequestFieldDate (sFieldRegDate,
                                                                                                              aExistingEntity == null ? null
                                                                                                                                      : aExistingEntity.getRegistrationDate (),
                                                                                                              aDisplayLocale)))
                                                 .setErrorList (aFormErrors.getListOfField (sFieldRegDate)));

    final BootstrapButtonToolbar aToolbar = aBody.addAndReturnChild (new BootstrapButtonToolbar (aLEC));
    aToolbar.addButton ("Delete this Entity", JQuery.idRef (aPanel).remove (), EDefaultIcon.DELETE);

    return aPanel;
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
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();

    aForm.addChild (createActionHeader (bEdit ? "Edit Business Card" : "Create new Business Card"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service Group")
                                                 .setCtrl (new HCServiceGroupSelect (new RequestField (FIELD_SERVICE_GROUP_ID,
                                                                                                       aSelectedObject != null ? aSelectedObject.getServiceGroupID ()
                                                                                                                               : null),
                                                                                     aDisplayLocale).setReadOnly (bEdit))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_GROUP_ID)));

    final HCDiv aEntityContainer = aForm.addAndReturnChild (new HCDiv ().setID ("entitycontainer"));

    final IRequestParamMap aEntities = aWPEC.getRequestParamMap ().getMap (PREFIX_ENTITY);
    if (aEntities != null)
    {
      // Re-show of form
      for (final String sEntityRowID : aEntities.keySet ())
        aEntityContainer.addChild (createEntityInputForm (aWPEC, null, sEntityRowID, aFormErrors));
    }
    else
    {
      if (aSelectedObject != null)
      {
        // add all existing stored entities
        for (final SMPBusinessCardEntity aEntity : aSelectedObject.getAllEntities ())
          aEntityContainer.addChild (createEntityInputForm (aWPEC, aEntity, (String) null, aFormErrors));
      }
    }

    {
      final JSAnonymousFunction aJSAppend = new JSAnonymousFunction ();
      final JSVar aJSAppendData = aJSAppend.param ("data");
      aJSAppend.body ()
               .add (JQuery.idRef (aEntityContainer).append (aJSAppendData.ref (AjaxHtmlResponse.PROPERTY_HTML)));

      final JSPackage aOnAdd = new JSPackage ();
      aOnAdd.add (new JQueryAjaxBuilder ().url (CAjaxSecure.FUNCTION_CREATE_BUSINESS_ENTITY_INPUT.getInvocationURL (aRequestScope))
                                          .data (new JSAssocArray ())
                                          .success (JSJQueryHelper.jqueryAjaxSuccessHandler (aJSAppend, null))
                                          .build ());

      aForm.addChild (new BootstrapButton ().addChild ("Add Entity").setIcon (EDefaultIcon.PLUS).setOnClick (aOnAdd));
    }
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
          aRow.addCell (CountryCache.getInstance ()
                                    .getCountry (aEntity.getCountryCode ())
                                    .getDisplayCountry (aDisplayLocale));
          aRow.addCell (HCExtHelper.nl2divList (aEntity.getGeographicalInformation ()));
          {
            final HCNodeList aIdentifiers = new HCNodeList ();
            for (final SMPBusinessCardIdentifier aIdentifier : aEntity.getIdentifiers ())
              aIdentifiers.addChild (new HCDiv ().addChild (aIdentifier.getScheme ())
                                                 .addChild (" - ")
                                                 .addChild (aIdentifier.getValue ()));
            aRow.addCell (aIdentifiers);
          }
          aRow.addCell (_createActionCell (aWPEC, aCurObject));
        }
      }
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);

    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
