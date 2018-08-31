/**
 * Copyright (C) 2014-2018 Philip Helger (www.helger.com)
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

import java.time.LocalDate;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.compare.CompareHelper;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.datetime.PDTFromString;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.error.IError;
import com.helger.commons.error.list.IErrorList;
import com.helger.commons.id.factory.GlobalIDFactory;
import com.helger.commons.locale.country.CountryCache;
import com.helger.commons.regex.RegExHelper;
import com.helger.commons.state.ESuccess;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.URLValidator;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCA_MailTo;
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
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.app.AppConfiguration;
import com.helger.peppol.smpserver.app.PDClientProvider;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardContact;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardEntity;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardIdentifier;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.settings.ISMPSettingsManager;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageForm;
import com.helger.peppol.smpserver.ui.ajax.AjaxExecutorSecureCreateBusinessCardContactInput;
import com.helger.peppol.smpserver.ui.ajax.AjaxExecutorSecureCreateBusinessCardIdentifierInput;
import com.helger.peppol.smpserver.ui.ajax.CAjax;
import com.helger.peppol.smpserver.ui.secure.hc.HCServiceGroupSelect;
import com.helger.photon.bootstrap3.CBootstrapCSS;
import com.helger.photon.bootstrap3.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap3.alert.BootstrapQuestionBox;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.button.EBootstrapButtonSize;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapHelpBlock;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.bootstrap3.pages.handler.AbstractBootstrapWebPageActionHandler;
import com.helger.photon.bootstrap3.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap3.panel.BootstrapPanel;
import com.helger.photon.bootstrap3.table.BootstrapTable;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap3.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.bootstrap3.uictrls.datetimepicker.BootstrapDateTimePicker;
import com.helger.photon.core.PhotonUnifiedResponse;
import com.helger.photon.core.app.context.ILayoutExecutionContext;
import com.helger.photon.core.app.context.LayoutExecutionContext;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldDate;
import com.helger.photon.core.url.LinkHelper;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.html.select.HCCountrySelect;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.js.JSJQueryHelper;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.autosize.HCTextAreaAutosize;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.famfam.EFamFamIcon;
import com.helger.servlet.request.IRequestParamMap;
import com.helger.servlet.request.RequestParamMap;
import com.helger.smtp.util.EmailAddressValidator;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;

@WorkInProgress
public final class PageSecureBusinessCard extends AbstractSMPWebPageForm <ISMPBusinessCard>
{
  private static final String FIELD_SERVICE_GROUP_ID = "sgid";
  private static final String PREFIX_ENTITY = "entity";
  private static final String SUFFIX_NAME = "name";
  private static final String SUFFIX_COUNTRY_CODE = "country";
  private static final String SUFFIX_GEO_INFO = "geoinfo";
  private static final String PREFIX_IDENTIFIER = "identifier";
  private static final String SUFFIX_SCHEME = "scheme";
  private static final String SUFFIX_VALUE = "value";
  private static final String SUFFIX_WEBSITE_URIS = "website";
  private static final String PREFIX_CONTACT = "contact";
  private static final String SUFFIX_TYPE = "type";
  private static final String SUFFIX_PHONE = "phone";
  private static final String SUFFIX_EMAIL = "email";
  private static final String SUFFIX_ADDITIONAL_INFO = "additional";
  private static final String SUFFIX_REG_DATE = "regdate";
  private static final String TMP_ID_PREFIX = "tmp";
  private static final String ACTION_PUBLISH_TO_INDEXER = "publishtoindexer";

  public PageSecureBusinessCard (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Business Cards");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPBusinessCard, WebPageExecutionContext> ()
    {
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
        {
          aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The selected Business Card was successfully deleted!" +
                                                                              (SMPMetaManager.getSettings ()
                                                                                             .isPEPPOLDirectoryIntegrationAutoUpdate () ? " " + AppConfiguration.getDirectoryName () + " server should have been updated." : "")));
        }
        else
          aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Failed to delete the selected Business Card!"));
      }
    });
    addCustomHandler (ACTION_PUBLISH_TO_INDEXER,
                      new AbstractBootstrapWebPageActionHandler <ISMPBusinessCard, WebPageExecutionContext> (true)
                      {
                        public boolean handleAction (@Nonnull final WebPageExecutionContext aWPEC,
                                                     @Nonnull final ISMPBusinessCard aSelectedObject)
                        {
                          final String sDirectoryName = AppConfiguration.getDirectoryName ();
                          final IParticipantIdentifier aParticipantID = aSelectedObject.getServiceGroup ()
                                                                                       .getParticpantIdentifier ();
                          final ESuccess eSuccess = PDClientProvider.getInstance ()
                                                                    .getPDClient ()
                                                                    .addServiceGroupToIndex (aParticipantID);
                          if (eSuccess.isSuccess ())
                          {
                            aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("Successfully notified the " +
                                                                                                sDirectoryName +
                                                                                                " to index '" +
                                                                                                aParticipantID.getURIEncoded () +
                                                                                                "'"));
                          }
                          else
                          {
                            aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Error notifying the " +
                                                                                              sDirectoryName +
                                                                                              " to index '" +
                                                                                              aParticipantID.getURIEncoded () +
                                                                                              "'"));
                          }
                          return true;
                        }
                      });
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    final ISMPSettingsManager aSettingsMgr = SMPMetaManager.getSettingsMgr ();
    if (!aSettingsMgr.getSettings ().isPEPPOLDirectoryIntegrationEnabled ())
    {
      aNodeList.addChild (new BootstrapWarnBox ().addChild (AppConfiguration.getDirectoryName () +
                                                            " integration is disabled hence no Business Cards can be created."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Change settings")
                                                .setOnClick (createCreateURL (aWPEC, CMenuSecure.MENU_SMP_SETTINGS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }

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
    // TODO add rules
    return super.isActionAllowed (aWPEC, eFormAction, aSelectedObject);
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPBusinessCard aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of Business Card"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service Group")
                                                 .setCtrl (new HCA (createViewURL (aWPEC,
                                                                                   CMenuSecure.MENU_SERVICE_GROUPS,
                                                                                   aSelectedObject.getServiceGroup ())).addChild (aSelectedObject.getServiceGroupID ())));

    int nIndex = 0;
    for (final SMPBusinessCardEntity aEntity : aSelectedObject.getAllEntities ())
    {
      ++nIndex;
      final BootstrapPanel aPanel = aForm.addAndReturnChild (new BootstrapPanel ());
      aPanel.getOrCreateHeader ().addChild ("Business Entity " + nIndex);

      final BootstrapViewForm aForm2 = aPanel.getBody ().addAndReturnChild (new BootstrapViewForm ());

      aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Name").setCtrl (aEntity.getName ()));
      aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Country code")
                                                    .setCtrl (CountryCache.getInstance ()
                                                                          .getCountry (aEntity.getCountryCode ())
                                                                          .getDisplayCountry (aDisplayLocale) +
                                                              " [" +
                                                              aEntity.getCountryCode () +
                                                              "]"));
      if (aEntity.hasGeographicalInformation ())
      {
        aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Geographical information")
                                                      .setCtrl (HCExtHelper.nl2divList (aEntity.getGeographicalInformation ())));
      }
      if (aEntity.hasIdentifiers ())
      {
        final BootstrapTable aTable = new BootstrapTable (HCCol.star (), HCCol.star ());
        aTable.addHeaderRow ().addCells ("Scheme", "Value");
        for (final SMPBusinessCardIdentifier aIdentifier : aEntity.getIdentifiers ())
          aTable.addBodyRow ().addCells (aIdentifier.getScheme (), aIdentifier.getValue ());
        aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Identifiers").setCtrl (aTable));
      }
      if (aEntity.hasWebsiteURIs ())
      {
        final HCNodeList aNL = new HCNodeList ();
        for (final String sWebsiteURI : aEntity.getAllWebsiteURIs ())
          aNL.addChild (new HCDiv ().addChild (HCA.createLinkedWebsite (sWebsiteURI)));
        aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Website URIs").setCtrl (aNL));
      }
      if (aEntity.hasContacts ())
      {
        final BootstrapTable aTable = new BootstrapTable (HCCol.star (), HCCol.star (), HCCol.star (), HCCol.star ());
        aTable.addHeaderRow ().addCells ("Type", "Name", "Phone number", "Email address");
        for (final SMPBusinessCardContact aContact : aEntity.getContacts ())
        {
          final HCRow aBodyRow = aTable.addBodyRow ();
          aBodyRow.addCells (aContact.getType (), aContact.getName (), aContact.getPhoneNumber ());
          aBodyRow.addCell (HCA_MailTo.createLinkedEmail (aContact.getEmail ()));
        }
        aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Contacts").setCtrl (aTable));
      }
      if (aEntity.hasAdditionalInformation ())
      {
        aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Additional information")
                                                      .setCtrl (HCExtHelper.nl2divList (aEntity.getAdditionalInformation ())));
      }
      if (aEntity.hasRegistrationDate ())
      {
        aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Registration date")
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
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final boolean bEdit = eFormAction.isEdit ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

    final String sServiceGroupID = bEdit ? aSelectedObject.getServiceGroupID ()
                                         : aWPEC.params ().getAsString (FIELD_SERVICE_GROUP_ID);
    ISMPServiceGroup aServiceGroup = null;
    final ICommonsList <SMPBusinessCardEntity> aSMPEntities = new CommonsArrayList <> ();

    // validations
    if (StringHelper.hasNoText (sServiceGroupID))
      aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "A Service Group must be selected!");
    else
    {
      aServiceGroup = aServiceGroupManager.getSMPServiceGroupOfID (aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID));
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
        final ICommonsMap <String, String> aEntityRow = aEntities.getValueMap (sEntityRowID);
        final int nErrors = aFormErrors.size ();

        // Entity name
        final String sFieldName = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityRowID, SUFFIX_NAME);
        final String sEntityName = aEntityRow.get (SUFFIX_NAME);
        if (StringHelper.hasNoText (sEntityName))
          aFormErrors.addFieldError (sFieldName, "The Name of the Entity must be provided!");

        // Entity country code
        final String sFieldCountryCode = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                                       sEntityRowID,
                                                                       SUFFIX_COUNTRY_CODE);
        final String sCountryCode = aEntityRow.get (SUFFIX_COUNTRY_CODE);
        if (StringHelper.hasNoText (sCountryCode))
          aFormErrors.addFieldError (sFieldCountryCode, "The Country Code of the Entity must be provided!");

        // Entity Geographical Information
        final String sGeoInfo = aEntityRow.get (SUFFIX_GEO_INFO);

        // Entity Identifiers
        final ICommonsList <SMPBusinessCardIdentifier> aSMPIdentifiers = new CommonsArrayList <> ();
        final IRequestParamMap aIdentifiers = aEntities.getMap (sEntityRowID, PREFIX_IDENTIFIER);
        if (aIdentifiers != null)
          for (final String sIdentifierRowID : aIdentifiers.keySet ())
          {
            final ICommonsMap <String, String> aIdentifierRow = aIdentifiers.getValueMap (sIdentifierRowID);
            final int nErrors2 = aFormErrors.size ();

            // Scheme
            final String sFieldScheme = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                                      sEntityRowID,
                                                                      PREFIX_IDENTIFIER,
                                                                      sIdentifierRowID,
                                                                      SUFFIX_SCHEME);
            final String sScheme = aIdentifierRow.get (SUFFIX_SCHEME);
            if (StringHelper.hasNoText (sScheme))
              aFormErrors.addFieldError (sFieldScheme, "The Scheme of the Identifier must be provided!");

            // Value
            final String sFieldValue = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                                     sEntityRowID,
                                                                     PREFIX_IDENTIFIER,
                                                                     sIdentifierRowID,
                                                                     SUFFIX_VALUE);
            final String sValue = aIdentifierRow.get (SUFFIX_VALUE);
            if (StringHelper.hasNoText (sValue))
              aFormErrors.addFieldError (sFieldValue, "The Value of the Identifier must be provided!");

            if (aFormErrors.size () == nErrors2)
            {
              final boolean bIsNewIdentifier = sIdentifierRowID.startsWith (TMP_ID_PREFIX);
              aSMPIdentifiers.add (bIsNewIdentifier ? new SMPBusinessCardIdentifier (sScheme, sValue)
                                                    : new SMPBusinessCardIdentifier (sIdentifierRowID,
                                                                                     sScheme,
                                                                                     sValue));
            }
          }

        aSMPIdentifiers.sort ( (o1, o2) -> {
          int ret = o1.getScheme ().compareToIgnoreCase (o2.getScheme ());
          if (ret == 0)
            ret = o1.getValue ().compareToIgnoreCase (o2.getValue ());
          return ret;
        });

        // Entity Website URIs
        final String sFieldWebsiteURIs = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                                       sEntityRowID,
                                                                       SUFFIX_WEBSITE_URIS);
        final String sWebsiteURIs = aEntityRow.get (SUFFIX_WEBSITE_URIS);
        final ICommonsList <String> aWebsiteURIs = new CommonsArrayList <> ();
        for (final String sWebsiteURI : RegExHelper.getSplitToArray (sWebsiteURIs, "\\n"))
        {
          final String sRealWebsiteURI = sWebsiteURI.trim ();
          if (sRealWebsiteURI.length () > 0)
            if (URLValidator.isValid (sRealWebsiteURI))
              aWebsiteURIs.add (sRealWebsiteURI);
            else
              aFormErrors.addFieldError (sFieldWebsiteURIs, "The website URI '" + sRealWebsiteURI + "' is invalid!");
        }

        // Entity Contacts
        final ICommonsList <SMPBusinessCardContact> aSMPContacts = new CommonsArrayList <> ();
        final IRequestParamMap aContacts = aEntities.getMap (sEntityRowID, PREFIX_CONTACT);
        if (aContacts != null)
          for (final String sContactRowID : aContacts.keySet ())
          {
            final ICommonsMap <String, String> aContactRow = aContacts.getValueMap (sContactRowID);
            final int nErrors2 = aFormErrors.size ();

            final String sType = aContactRow.get (SUFFIX_TYPE);
            final String sName = aContactRow.get (SUFFIX_NAME);
            final String sPhoneNumber = aContactRow.get (SUFFIX_PHONE);

            final String sFieldEmail = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                                     sEntityRowID,
                                                                     PREFIX_CONTACT,
                                                                     sContactRowID,
                                                                     SUFFIX_EMAIL);
            final String sEmail = aContactRow.get (SUFFIX_EMAIL);
            if (StringHelper.hasText (sEmail))
              if (!EmailAddressValidator.isValid (sEmail))
                aFormErrors.addFieldError (sFieldEmail, "The provided email address is invalid!");

            final boolean bIsAnySet = StringHelper.hasText (sType) ||
                                      StringHelper.hasText (sName) ||
                                      StringHelper.hasText (sPhoneNumber) ||
                                      StringHelper.hasText (sEmail);

            if (aFormErrors.size () == nErrors2 && bIsAnySet)
            {
              final boolean bIsNewContact = sContactRowID.startsWith (TMP_ID_PREFIX);
              aSMPContacts.add (bIsNewContact ? new SMPBusinessCardContact (sType, sName, sPhoneNumber, sEmail)
                                              : new SMPBusinessCardContact (sContactRowID,
                                                                            sType,
                                                                            sName,
                                                                            sPhoneNumber,
                                                                            sEmail));
            }
          }

        aSMPContacts.sort ( (o1, o2) -> {
          int ret = CompareHelper.compareIgnoreCase (o1.getType (), o2.getType ());
          if (ret == 0)
          {
            ret = CompareHelper.compareIgnoreCase (o1.getName (), o2.getName ());
            if (ret == 0)
            {
              ret = CompareHelper.compareIgnoreCase (o1.getPhoneNumber (), o2.getPhoneNumber ());
              if (ret == 0)
                ret = CompareHelper.compareIgnoreCase (o1.getEmail (), o2.getEmail ());
            }
          }
          return ret;
        });

        // Entity Additional Information
        final String sAdditionalInfo = aEntityRow.get (SUFFIX_ADDITIONAL_INFO);

        // Entity Registration Date
        final String sFieldRegDate = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityRowID, SUFFIX_REG_DATE);
        final String sRegDate = aEntityRow.get (SUFFIX_REG_DATE);
        final LocalDate aRegDate = PDTFromString.getLocalDateFromString (sRegDate, aDisplayLocale);
        if (aRegDate == null && StringHelper.hasText (sRegDate))
          aFormErrors.addFieldError (sFieldRegDate, "The entered registration date is invalid!");

        if (aFormErrors.size () == nErrors)
        {
          // Add to list
          final boolean bIsNewEntity = sEntityRowID.startsWith (TMP_ID_PREFIX);
          final SMPBusinessCardEntity aEntity = bIsNewEntity ? new SMPBusinessCardEntity ()
                                                             : new SMPBusinessCardEntity (sEntityRowID);
          aEntity.setName (sEntityName);
          aEntity.setCountryCode (sCountryCode);
          aEntity.setGeographicalInformation (sGeoInfo);
          aEntity.setIdentifiers (aSMPIdentifiers);
          aEntity.setWebsiteURIs (aWebsiteURIs);
          aEntity.setContacts (aSMPContacts);
          aEntity.setAdditionalInformation (sAdditionalInfo);
          aEntity.setRegistrationDate (aRegDate);
          aSMPEntities.add (aEntity);
        }
      }

    if (aFormErrors.isEmpty ())
    {
      // Store in a consistent manner
      aSMPEntities.sort ( (o1, o2) -> o1.getName ().compareToIgnoreCase (o2.getName ()));
      if (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aServiceGroup, aSMPEntities) != null)
        aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The Business Card for Service Group '" +
                                                                            aServiceGroup.getID () +
                                                                            "' was successfully saved." +
                                                                            (SMPMetaManager.getSettings ()
                                                                                           .isPEPPOLDirectoryIntegrationAutoUpdate () ? " " + AppConfiguration.getDirectoryName () + " server should have been updated." : "")));
      else
        aWPEC.postRedirectGetInternal (new BootstrapErrorBox ().addChild ("Error creating the Business Card for Service Group '" +
                                                                          aServiceGroup.getID () +
                                                                          "'"));
    }
  }

  @Nullable
  public static IHCNode createStandaloneError (@Nullable final IErrorList aFormErrors,
                                               @Nonnull final Locale aDisplayLocale)
  {
    if (aFormErrors == null || aFormErrors.isEmpty ())
      return null;

    final HCDiv aDiv = new HCDiv ().addClass (CBootstrapCSS.HAS_ERROR);
    for (final IError aError : aFormErrors)
      aDiv.addChild (new BootstrapHelpBlock ().addChild (aError.getErrorText (aDisplayLocale)));
    return aDiv;
  }

  @Nonnull
  public static HCRow createIdentifierInputForm (@Nonnull final ILayoutExecutionContext aLEC,
                                                 @Nonnull final String sEntityID,
                                                 @Nullable final SMPBusinessCardIdentifier aExistingIdentifier,
                                                 @Nullable final String sExistingID,
                                                 @Nonnull final FormErrorList aFormErrors)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final String sIdentifierID = aExistingIdentifier != null ? aExistingIdentifier.getID ()
                                                             : StringHelper.hasText (sExistingID) ? sExistingID
                                                                                                  : TMP_ID_PREFIX +
                                                                                                    Integer.toString (GlobalIDFactory.getNewIntID ());

    final HCRow aRow = new HCRow ();

    // Identifier scheme
    final String sFieldScheme = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                              sEntityID,
                                                              PREFIX_IDENTIFIER,
                                                              sIdentifierID,
                                                              SUFFIX_SCHEME);
    aRow.addCell (new HCEdit (new RequestField (sFieldScheme,
                                                aExistingIdentifier == null ? null : aExistingIdentifier.getScheme ()))
                                                                                                                       .setPlaceholder ("Identifier scheme"),
                  createStandaloneError (aFormErrors.getListOfField (sFieldScheme), aDisplayLocale));

    // Identifier Value
    final String sFieldValue = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                             sEntityID,
                                                             PREFIX_IDENTIFIER,
                                                             sIdentifierID,
                                                             SUFFIX_VALUE);
    aRow.addCell (new HCEdit (new RequestField (sFieldValue,
                                                aExistingIdentifier == null ? null : aExistingIdentifier.getValue ()))
                                                                                                                      .setPlaceholder ("Identifier value"),
                  createStandaloneError (aFormErrors.getListOfField (sFieldValue), aDisplayLocale));

    aRow.addCell (new BootstrapButton (EBootstrapButtonSize.MINI).setIcon (EDefaultIcon.DELETE)
                                                                 .setOnClick (JQuery.idRef (aRow).remove ()));

    return aRow;
  }

  @Nonnull
  public static HCRow createContactInputForm (@Nonnull final ILayoutExecutionContext aLEC,
                                              @Nonnull final String sEntityID,
                                              @Nullable final SMPBusinessCardContact aExistingContact,
                                              @Nullable final String sExistingID,
                                              @Nonnull final FormErrorList aFormErrors)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final String sContactID = aExistingContact != null ? aExistingContact.getID ()
                                                       : StringHelper.hasText (sExistingID) ? sExistingID
                                                                                            : TMP_ID_PREFIX +
                                                                                              Integer.toString (GlobalIDFactory.getNewIntID ());

    final HCRow aRow = new HCRow ();

    // Type
    {
      final String sFieldType = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                              sEntityID,
                                                              PREFIX_CONTACT,
                                                              sContactID,
                                                              SUFFIX_TYPE);
      aRow.addCell (new HCEdit (new RequestField (sFieldType,
                                                  aExistingContact == null ? null : aExistingContact.getType ()))
                                                                                                                 .setPlaceholder ("Contact type"),
                    createStandaloneError (aFormErrors.getListOfField (sFieldType), aDisplayLocale));
    }

    // Name
    {
      final String sFieldName = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                              sEntityID,
                                                              PREFIX_CONTACT,
                                                              sContactID,
                                                              SUFFIX_NAME);
      aRow.addCell (new HCEdit (new RequestField (sFieldName,
                                                  aExistingContact == null ? null : aExistingContact.getName ()))
                                                                                                                 .setPlaceholder ("Contact name"),
                    createStandaloneError (aFormErrors.getListOfField (sFieldName), aDisplayLocale));
    }

    // Phone number
    {
      final String sFieldPhone = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                               sEntityID,
                                                               PREFIX_CONTACT,
                                                               sContactID,
                                                               SUFFIX_PHONE);
      aRow.addCell (new HCEdit (new RequestField (sFieldPhone,
                                                  aExistingContact == null ? null : aExistingContact.getPhoneNumber ()))
                                                                                                                        .setPlaceholder ("Contact phone number"),
                    createStandaloneError (aFormErrors.getListOfField (sFieldPhone), aDisplayLocale));
    }

    // Email address
    {
      final String sFieldEmail = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                               sEntityID,
                                                               PREFIX_CONTACT,
                                                               sContactID,
                                                               SUFFIX_EMAIL);
      aRow.addCell (new HCEdit (new RequestField (sFieldEmail,
                                                  aExistingContact == null ? null : aExistingContact.getEmail ()))
                                                                                                                  .setPlaceholder ("Contact email address"),
                    createStandaloneError (aFormErrors.getListOfField (sFieldEmail), aDisplayLocale));
    }

    aRow.addCell (new BootstrapButton (EBootstrapButtonSize.MINI).setIcon (EDefaultIcon.DELETE)
                                                                 .setOnClick (JQuery.idRef (aRow).remove ()));

    return aRow;
  }

  @Nonnull
  public static IHCNode createEntityInputForm (@Nonnull final LayoutExecutionContext aLEC,
                                               @Nullable final SMPBusinessCardEntity aExistingEntity,
                                               @Nullable final String sExistingID,
                                               @Nonnull final FormErrorList aFormErrors)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
    final String sEntityID = aExistingEntity != null ? aExistingEntity.getID ()
                                                     : StringHelper.hasText (sExistingID) ? sExistingID
                                                                                          : TMP_ID_PREFIX +
                                                                                            Integer.toString (GlobalIDFactory.getNewIntID ());

    final BootstrapPanel aPanel = new BootstrapPanel ().setID (sEntityID);
    aPanel.getOrCreateHeader ().addChild ("Business Entity");
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

    // Identifiers
    {
      final String sBodyID = sEntityID + PREFIX_IDENTIFIER;
      final HCNodeList aNL = new HCNodeList ();
      final BootstrapTable aTable = aNL.addAndReturnChild (new BootstrapTable (HCCol.star (),
                                                                               HCCol.star (),
                                                                               HCCol.star ()));
      aTable.addHeaderRow ().addCells ("Scheme", "Value", "");
      aTable.setBodyID (sBodyID);

      final IRequestParamMap aIdentifiers = aLEC.getRequestParamMap ()
                                                .getMap (PREFIX_ENTITY, sEntityID, PREFIX_IDENTIFIER);
      if (aIdentifiers != null)
      {
        // Re-show of form
        for (final String sIdentifierRowID : aIdentifiers.keySet ())
          aTable.addBodyRow (createIdentifierInputForm (aLEC, sEntityID, null, sIdentifierRowID, aFormErrors));
      }
      else
      {
        if (aExistingEntity != null)
        {
          // add all existing stored entities
          for (final SMPBusinessCardIdentifier aIdentifier : aExistingEntity.getIdentifiers ())
            aTable.addBodyRow (createIdentifierInputForm (aLEC, sEntityID, aIdentifier, (String) null, aFormErrors));
        }
      }

      {
        final JSAnonymousFunction aJSAppend = new JSAnonymousFunction ();
        final JSVar aJSAppendData = aJSAppend.param ("data");
        aJSAppend.body ()
                 .add (JQuery.idRef (sBodyID)
                             .append (aJSAppendData.ref (PhotonUnifiedResponse.HtmlHelper.PROPERTY_HTML)));

        final JSPackage aOnAdd = new JSPackage ();
        aOnAdd.add (new JQueryAjaxBuilder ().url (CAjax.FUNCTION_CREATE_BUSINESS_CARD_IDENTIFIER_INPUT.getInvocationURL (aRequestScope)
                                                                                                      .add (AjaxExecutorSecureCreateBusinessCardIdentifierInput.PARAM_ENTITY_ID,
                                                                                                            sEntityID))
                                            .data (new JSAssocArray ())
                                            .success (JSJQueryHelper.jqueryAjaxSuccessHandler (aJSAppend, null))
                                            .build ());

        aNL.addChild (new BootstrapButton ().setIcon (EDefaultIcon.PLUS)
                                            .addChild ("Add Identifier")
                                            .setOnClick (aOnAdd));
      }

      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Identifiers").setCtrl (aNL));
    }

    // Website URIs
    final String sFieldWebsiteURIs = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityID, SUFFIX_WEBSITE_URIS);
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Website URIs")
                                                 .setCtrl (new HCTextAreaAutosize (new RequestField (sFieldWebsiteURIs,
                                                                                                     aExistingEntity == null ? null
                                                                                                                             : StringHelper.getImploded ('\n',
                                                                                                                                                         aExistingEntity.getAllWebsiteURIs ()))))
                                                 .setHelpText ("Put each Website URI in a separate line")
                                                 .setErrorList (aFormErrors.getListOfField (sFieldWebsiteURIs)));

    // Contacts
    {
      final String sBodyID = sEntityID + PREFIX_CONTACT;
      final HCNodeList aNL = new HCNodeList ();
      final BootstrapTable aTable = aNL.addAndReturnChild (new BootstrapTable (HCCol.star (),
                                                                               HCCol.star (),
                                                                               HCCol.star (),
                                                                               HCCol.star (),
                                                                               HCCol.star ()));
      aTable.addHeaderRow ().addCells ("Type", "Name", "Phone number", "Email address", "");
      aTable.setBodyID (sBodyID);

      final IRequestParamMap aContacts = aLEC.getRequestParamMap ().getMap (PREFIX_ENTITY, sEntityID, PREFIX_CONTACT);
      if (aContacts != null)
      {
        // Re-show of form
        for (final String sIdentifierRowID : aContacts.keySet ())
          aTable.addBodyRow (createContactInputForm (aLEC, sEntityID, null, sIdentifierRowID, aFormErrors));
      }
      else
      {
        if (aExistingEntity != null)
        {
          // add all existing stored entities
          for (final SMPBusinessCardContact aContact : aExistingEntity.getContacts ())
            aTable.addBodyRow (createContactInputForm (aLEC, sEntityID, aContact, (String) null, aFormErrors));
        }
      }

      {
        final JSAnonymousFunction aJSAppend = new JSAnonymousFunction ();
        final JSVar aJSAppendData = aJSAppend.param ("data");
        aJSAppend.body ()
                 .add (JQuery.idRef (sBodyID)
                             .append (aJSAppendData.ref (PhotonUnifiedResponse.HtmlHelper.PROPERTY_HTML)));

        final JSPackage aOnAdd = new JSPackage ();
        aOnAdd.add (new JQueryAjaxBuilder ().url (CAjax.FUNCTION_CREATE_BUSINESS_CARD_CONTACT_INPUT.getInvocationURL (aRequestScope)
                                                                                                   .add (AjaxExecutorSecureCreateBusinessCardContactInput.PARAM_ENTITY_ID,
                                                                                                         sEntityID))
                                            .data (new JSAssocArray ())
                                            .success (JSJQueryHelper.jqueryAjaxSuccessHandler (aJSAppend, null))
                                            .build ());

        aNL.addChild (new BootstrapButton ().setIcon (EDefaultIcon.PLUS).addChild ("Add Contact").setOnClick (aOnAdd));
      }

      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Contacts").setCtrl (aNL));
    }

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
                                                                                                              aDisplayLocale)).setEndDate (null))
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
                                @Nonnull final FormErrorList aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

    aForm.addChild (getUIHandler ().createActionHeader (bEdit ? "Edit Business Card" : "Create new Business Card"));

    if (bEdit)
    {
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service Group")
                                                   .setCtrl (aSelectedObject.getServiceGroupID ())
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_GROUP_ID)));
    }
    else
    {
      // Show only service groups that don't have a BC already
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service Group")
                                                   .setCtrl (new HCServiceGroupSelect (new RequestField (FIELD_SERVICE_GROUP_ID,
                                                                                                         aSelectedObject != null ? aSelectedObject.getServiceGroupID ()
                                                                                                                                 : null),
                                                                                       aDisplayLocale,
                                                                                       x -> aBusinessCardMgr.getSMPBusinessCardOfServiceGroup (x) == null))
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_GROUP_ID)));
    }

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
               .add (JQuery.idRef (aEntityContainer)
                           .append (aJSAppendData.ref (PhotonUnifiedResponse.HtmlHelper.PROPERTY_HTML)));

      final JSPackage aOnAdd = new JSPackage ();
      aOnAdd.add (new JQueryAjaxBuilder ().url (CAjax.FUNCTION_CREATE_BUSINESS_CARD_ENTITY_INPUT.getInvocationURL (aRequestScope))
                                          .data (new JSAssocArray ())
                                          .success (JSJQueryHelper.jqueryAjaxSuccessHandler (aJSAppend, null))
                                          .build ());

      aForm.addChild (new BootstrapButton ().addChild ("Add Entity").setIcon (EDefaultIcon.PLUS).setOnClick (aOnAdd));
    }
  }

  @Nonnull
  private IHCNode _createActionCell (@Nonnull final WebPageExecutionContext aWPEC,
                                     @Nonnull final ISMPBusinessCard aCurObject)
  {
    final String sDisplayName = aCurObject.getServiceGroupID ();
    final HCNodeList ret = new HCNodeList ();
    if (isActionAllowed (aWPEC, EWebPageFormAction.EDIT, aCurObject))
      ret.addChild (new HCA (createEditURL (aWPEC, aCurObject)).setTitle ("Edit " + sDisplayName)
                                                               .addChild (EDefaultIcon.EDIT.getAsNode ()));
    else
      ret.addChild (createEmptyAction ());
    ret.addChild (new HCTextNode (" "));
    if (isActionAllowed (aWPEC, EWebPageFormAction.EDIT, aCurObject))
      ret.addChild (new HCA (createCopyURL (aWPEC, aCurObject)).setTitle ("Create a copy of " + sDisplayName)
                                                               .addChild (EDefaultIcon.COPY.getAsNode ()));
    else
      ret.addChild (createEmptyAction ());
    ret.addChild (new HCTextNode (" "));
    if (isActionAllowed (aWPEC, EWebPageFormAction.DELETE, aCurObject))
      ret.addChild (new HCA (createDeleteURL (aWPEC, aCurObject)).setTitle ("Delete " + sDisplayName)
                                                                 .addChild (EDefaultIcon.DELETE.getAsNode ()));
    else
      ret.addChild (createEmptyAction ());
    ret.addChild (new HCTextNode (" "));
    ret.addChild (new HCA (LinkHelper.getURLWithServerAndContext ("businesscard/" +
                                                                  aCurObject.getServiceGroup ()
                                                                            .getParticpantIdentifier ()
                                                                            .getURIPercentEncoded ())).setTitle ("Perform SMP query on " +
                                                                                                                 sDisplayName)
                                                                                                      .setTargetBlank ()
                                                                                                      .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()));

    if (!SMPMetaManager.getSettings ().isPEPPOLDirectoryIntegrationAutoUpdate () || GlobalDebug.isDebugMode ())
    {
      // When auto update is enabled, there is no need for a manual update
      ret.addChildren (new HCTextNode (" "),
                       new HCA (aWPEC.getSelfHref ()
                                     .add (CPageParam.PARAM_ACTION, ACTION_PUBLISH_TO_INDEXER)
                                     .add (CPageParam.PARAM_OBJECT, aCurObject.getID ()))
                                                                                         .setTitle ("Update Business Card in " +
                                                                                                    AppConfiguration.getDirectoryName ())
                                                                                         .addChild (EFamFamIcon.ARROW_RIGHT.getAsNode ()));
    }
    return ret;
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
