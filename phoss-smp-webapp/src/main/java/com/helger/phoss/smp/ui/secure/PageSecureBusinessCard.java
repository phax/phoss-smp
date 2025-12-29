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

import java.time.LocalDate;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.Nonempty;
import com.helger.annotation.Nonnegative;
import com.helger.annotation.misc.WorkInProgress;
import com.helger.base.compare.CompareHelper;
import com.helger.base.compare.ESortOrder;
import com.helger.base.id.factory.GlobalIDFactory;
import com.helger.base.state.ESuccess;
import com.helger.base.state.EValidity;
import com.helger.base.state.IValidityIndicator;
import com.helger.base.string.StringHelper;
import com.helger.base.string.StringImplode;
import com.helger.cache.regex.RegExHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsMap;
import com.helger.datetime.format.PDTFromString;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCA_MailTo;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCTextArea;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.tabular.HCCol;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.tabular.IHCCell;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.html.jquery.JQuery;
import com.helger.html.jquery.JQueryAjaxBuilder;
import com.helger.html.jscode.JSAnonymousFunction;
import com.helger.html.jscode.JSAssocArray;
import com.helger.html.jscode.JSPackage;
import com.helger.html.jscode.JSParam;
import com.helger.pd.client.IPDClientExceptionCallback;
import com.helger.pd.client.PDClient;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.app.PDClientProvider;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardContact;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardIdentifier;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardName;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.AbstractSMPWebPageForm;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.ajax.CAjax;
import com.helger.phoss.smp.ui.secure.hc.HCServiceGroupSelect;
import com.helger.photon.ajax.decl.IAjaxFunctionDeclaration;
import com.helger.photon.app.PhotonUnifiedResponse;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.alert.BootstrapErrorBox;
import com.helger.photon.bootstrap4.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.button.EBootstrapButtonSize;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.card.BootstrapCard;
import com.helger.photon.bootstrap4.card.BootstrapCardBody;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapFormHelper;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandler;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap4.table.BootstrapTable;
import com.helger.photon.bootstrap4.traits.IHCBootstrap4Trait;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.bootstrap4.uictrls.datetimepicker.BootstrapDateTimePicker;
import com.helger.photon.core.execcontext.ILayoutExecutionContext;
import com.helger.photon.core.execcontext.LayoutExecutionContext;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.longrun.AbstractLongRunningJobRunnable;
import com.helger.photon.core.longrun.LongRunningJobResult;
import com.helger.photon.io.PhotonWorkerPool;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.html.select.HCCountrySelect;
import com.helger.photon.uicore.html.select.HCCountrySelect.EWithDeprecated;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.js.JSJQueryHelper;
import com.helger.photon.uicore.page.EShowList;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.famfam.EFamFamFlagIcon;
import com.helger.photon.uictrls.famfam.EFamFamIcon;
import com.helger.servlet.request.IRequestParamMap;
import com.helger.servlet.request.RequestParamMap;
import com.helger.smtp.util.EmailAddressValidator;
import com.helger.text.ReadOnlyMultilingualText;
import com.helger.text.locale.country.CountryCache;
import com.helger.url.ISimpleURL;
import com.helger.url.validate.URLValidator;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.web.scope.mgr.WebScoped;

import jakarta.annotation.Nullable;

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
  private static final String ACTION_PUBLISH_ALL_TO_INDEXER = "publishalltoindexer";

  private static final String PARAM_ENTITY_ID = "entityid";

  private static final IAjaxFunctionDeclaration AJAX_CREATE_ENTITY;
  private static final IAjaxFunctionDeclaration AJAX_CREATE_CONTACT;
  private static final IAjaxFunctionDeclaration AJAX_CREATE_IDENTIFIER;

  static
  {
    AJAX_CREATE_ENTITY = CAjax.addAjaxWithLogin ( (aRequestScope, aAjaxResponse) -> {
      final LayoutExecutionContext aLEC = LayoutExecutionContext.createForAjaxOrAction (aRequestScope);
      final IHCNode aNode = _createEntityInputForm (aLEC,
                                                    (SMPBusinessCardEntity) null,
                                                    (String) null,
                                                    new FormErrorList (),
                                                    false);

      // Build the HTML response
      aAjaxResponse.html (aNode);
    });
    AJAX_CREATE_CONTACT = CAjax.addAjaxWithLogin ( (aRequestScope, aAjaxResponse) -> {
      final LayoutExecutionContext aLEC = LayoutExecutionContext.createForAjaxOrAction (aRequestScope);
      final String sEntityID = aRequestScope.params ().getAsStringTrimmed (PARAM_ENTITY_ID);

      final IHCNode aNode = _createContactInputForm (aLEC,
                                                     sEntityID,
                                                     (SMPBusinessCardContact) null,
                                                     (String) null,
                                                     new FormErrorList ());

      // Build the HTML response
      aAjaxResponse.html (aNode);
    });
    AJAX_CREATE_IDENTIFIER = CAjax.addAjaxWithLogin ( (aRequestScope, aAjaxResponse) -> {
      final LayoutExecutionContext aLEC = LayoutExecutionContext.createForAjaxOrAction (aRequestScope);
      final String sEntityID = aRequestScope.params ().getAsStringTrimmed (PARAM_ENTITY_ID);

      final IHCNode aNode = _createIdentifierInputForm (aLEC,
                                                        sEntityID,
                                                        (SMPBusinessCardIdentifier) null,
                                                        (String) null,
                                                        new FormErrorList ());

      // Build the HTML response
      aAjaxResponse.html (aNode);
    });
  }

  /**
   * The async logic
   *
   * @author Philip Helger
   */
  private static final class PushAllBusinessCardsToDirectory extends AbstractLongRunningJobRunnable implements
                                                             IHCBootstrap4Trait
  {
    private static final AtomicInteger RUNNING_JOBS = new AtomicInteger (0);

    private final PDClient m_aPDClient;
    private final String m_sUserID;

    public PushAllBusinessCardsToDirectory (@NonNull final PDClient aPDClient, @NonNull final String sUserID)
    {
      super ("PushAllBusinessCardsToDirectory",
             new ReadOnlyMultilingualText (CSMPServer.DEFAULT_LOCALE, "Update all participants in Directory"));
      m_aPDClient = aPDClient;
      m_sUserID = sUserID;
    }

    @Override
    protected String getCurrentUserID ()
    {
      return m_sUserID;
    }

    @NonNull
    public LongRunningJobResult createLongRunningJobResult ()
    {
      RUNNING_JOBS.incrementAndGet ();
      try (final WebScoped w = new WebScoped ())
      {
        final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
        final String sDirectoryName = SMPWebAppConfiguration.getDirectoryName ();

        final ICommonsList <String> aSuccess = new CommonsArrayList <> ();
        final ICommonsList <String> aFailure = new CommonsArrayList <> ();

        for (final ISMPBusinessCard aCurObject : aBusinessCardMgr.getAllSMPBusinessCards ())
        {
          final IParticipantIdentifier aParticipantID = aCurObject.getParticipantIdentifier ();
          final ESuccess eSuccess = m_aPDClient.addServiceGroupToIndex (aParticipantID);
          (eSuccess.isSuccess () ? aSuccess : aFailure).add (aParticipantID.getURIEncoded ());
        }

        final HCNodeList aResultNodes = new HCNodeList ();
        if (aSuccess.isNotEmpty ())
        {
          final BootstrapSuccessBox aBox = success ();
          if (aSuccess.size () <= 20)
          {
            // Otherwise the list would get too long
            for (final String sPI : aSuccess)
              aBox.addChild (div ("Successfully notified the " + sDirectoryName + " to index '" + sPI + "'"));
          }
          else
            aBox.addChild ("Successfully notified the " +
                           sDirectoryName +
                           " to index " +
                           aSuccess.size () +
                           " participants");
          aResultNodes.addChild (aBox);
        }
        if (aFailure.isNotEmpty ())
        {
          final BootstrapErrorBox aBox = error ();
          for (final String sPI : aFailure)
          {
            aBox.addChild (div ("Error notifying the " +
                                sDirectoryName +
                                " to index '" +
                                sPI +
                                "'. See the logs for details."));
          }
          aResultNodes.addChild (aBox);
        }
        if (aResultNodes.hasNoChildren ())
          aResultNodes.addChild (info ("No participants to be indexed to " + sDirectoryName + "."));

        return LongRunningJobResult.createXML (aResultNodes);
      }
      finally
      {
        RUNNING_JOBS.decrementAndGet ();
      }
    }

    @Nonnegative
    public static int getRunningJobCount ()
    {
      return RUNNING_JOBS.get ();
    }
  }

  public PageSecureBusinessCard (@NonNull @Nonempty final String sID)
  {
    super (sID, "Business Cards");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPBusinessCard, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@NonNull final WebPageExecutionContext aWPEC,
                                @NonNull final BootstrapForm aForm,
                                @Nullable final ISMPBusinessCard aSelectedObject)
      {
        aForm.addChild (question ("Are you sure you want to delete the Business Card for service group '" +
                                  aSelectedObject.getID () +
                                  "'?"));
      }

      @Override
      protected void performAction (@NonNull final WebPageExecutionContext aWPEC,
                                    @Nullable final ISMPBusinessCard aSelectedObject)
      {
        final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
        if (aBusinessCardMgr.deleteSMPBusinessCard (aSelectedObject).isChanged ())
        {
          final ISMPSettings aSettings = SMPMetaManager.getSettings ();
          aWPEC.postRedirectGetInternal (success ("The selected Business Card was successfully deleted!" +
                                                  (aSettings.isDirectoryIntegrationEnabled () &&
                                                   aSettings.isDirectoryIntegrationAutoUpdate () ? " " +
                                                                                                   SMPWebAppConfiguration.getDirectoryName () +
                                                                                                   " server should have been updated."
                                                                                                 : "")));
        }
        else
          aWPEC.postRedirectGetInternal (error ("Failed to delete the selected Business Card!"));
      }
    });
    addCustomHandler (ACTION_PUBLISH_TO_INDEXER,
                      new AbstractBootstrapWebPageActionHandler <ISMPBusinessCard, WebPageExecutionContext> (true)
                      {
                        @NonNull
                        public EShowList handleAction (@NonNull final WebPageExecutionContext aWPEC,
                                                       @NonNull final ISMPBusinessCard aSelectedObject)
                        {
                          final String sDirectoryName = SMPWebAppConfiguration.getDirectoryName ();
                          final IParticipantIdentifier aParticipantID = aSelectedObject.getParticipantIdentifier ();
                          PDClient aPDClient = null;
                          Exception aCaughtEx = null;
                          try
                          {
                            aPDClient = PDClientProvider.getInstance ().getPDClient ();
                          }
                          catch (final IllegalStateException ex)
                          {
                            aCaughtEx = ex;
                          }
                          if (aPDClient == null)
                          {
                            aWPEC.postRedirectGetInternal (error ("Failed to create the " +
                                                                  sDirectoryName +
                                                                  " client component. Please check your configuration.").addChild (SMPCommonUI.getTechnicalDetailsUI (aCaughtEx)));
                          }
                          else
                          {
                            final HCNodeList aExceptionDetails = new HCNodeList ();
                            final IPDClientExceptionCallback aOriginalCallback = aPDClient.getExceptionHandler ();
                            final IPDClientExceptionCallback aExCallback = (aExParticipantID, sContext, aException) -> {
                              aOriginalCallback.onException (aExParticipantID, sContext, aException);
                              aExceptionDetails.addChild (SMPCommonUI.getTechnicalDetailsUI (aException));
                            };
                            aPDClient.setExceptionHandler (aExCallback);

                            final ESuccess eSuccess = aPDClient.addServiceGroupToIndex (aParticipantID);
                            if (eSuccess.isSuccess ())
                            {
                              aWPEC.postRedirectGetInternal (success ("Successfully notified the " +
                                                                      sDirectoryName +
                                                                      " to index '" +
                                                                      aParticipantID.getURIEncoded () +
                                                                      "'"));
                            }
                            else
                            {
                              aWPEC.postRedirectGetInternal (error ("Error notifying the " +
                                                                    sDirectoryName +
                                                                    " to index '" +
                                                                    aParticipantID.getURIEncoded () +
                                                                    "'. Please see the log file for details."));
                            }
                          }
                          return EShowList.SHOW_LIST;
                        }
                      });
    addCustomHandler (ACTION_PUBLISH_ALL_TO_INDEXER,
                      new AbstractBootstrapWebPageActionHandler <ISMPBusinessCard, WebPageExecutionContext> (false)
                      {
                        @NonNull
                        public EShowList handleAction (@NonNull final WebPageExecutionContext aWPEC,
                                                       @Nullable final ISMPBusinessCard aSelectedObject)
                        {
                          final String sDirectoryName = SMPWebAppConfiguration.getDirectoryName ();
                          PDClient aPDClient = null;
                          Exception aCaughtEx = null;
                          try
                          {
                            aPDClient = PDClientProvider.getInstance ().getPDClient ();
                          }
                          catch (final IllegalStateException ex)
                          {
                            aCaughtEx = ex;
                          }
                          if (aPDClient == null)
                          {
                            aWPEC.postRedirectGetInternal (error ("Failed to create the " +
                                                                  sDirectoryName +
                                                                  " client component. Please check your configuration.").addChild (SMPCommonUI.getTechnicalDetailsUI (aCaughtEx)));
                          }
                          else
                          {
                            PhotonWorkerPool.getInstance ()
                                            .run ("PushAllBusinessCardsToDirectory",
                                                  new PushAllBusinessCardsToDirectory (aPDClient,
                                                                                       aWPEC.getLoggedInUserID ()));

                            aWPEC.postRedirectGetInternal (success ("The update of the Business Cards in the " +
                                                                    sDirectoryName +
                                                                    " is now running in the background. Please manually refresh the page to see the update."));
                          }
                          return EShowList.SHOW_LIST;
                        }
                      });
  }

  @Override
  @NonNull
  protected IValidityIndicator isValidToDisplayPage (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();

    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    if (!aSettings.isDirectoryIntegrationEnabled ())
    {
      aNodeList.addChild (warn (SMPWebAppConfiguration.getDirectoryName () +
                                " integration is disabled hence no Business Cards can be created."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Change settings")
                                                .setOnClick (createCreateURL (aWPEC, CMenuSecure.MENU_SMP_SETTINGS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }

    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
    if (aServiceGroupManager.getSMPServiceGroupCount () <= 0)
    {
      aNodeList.addChild (warn ("No Service Group is present! At least one Service Group must be present to create a Business Card for it."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new Service Group")
                                                .setOnClick (createCreateURL (aWPEC, CMenuSecure.MENU_SERVICE_GROUPS))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }
    return super.isValidToDisplayPage (aWPEC);
  }

  @Override
  @Nullable
  protected ISMPBusinessCard getSelectedObject (@NonNull final WebPageExecutionContext aWPEC,
                                                @Nullable final String sID)
  {
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    return aBusinessCardMgr.getSMPBusinessCardOfID (aIdentifierFactory.parseParticipantIdentifier (sID));
  }

  @NonNull
  public static IHCNode showBusinessCardEntity (@NonNull final SMPBusinessCardEntity aEntity,
                                                final int nIndex,
                                                @NonNull final Locale aDisplayLocale)
  {
    final BootstrapCard aPanel = new BootstrapCard ();
    aPanel.createAndAddHeader ().addChild ("Business Entity " + nIndex);

    final BootstrapViewForm aForm2 = aPanel.createAndAddBody ().addAndReturnChild (new BootstrapViewForm ());

    aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Name")
                                                  .setCtrl (aEntity.names ().getFirstOrNull ().getName ()));

    {
      final Locale aCountry = CountryCache.getInstance ().getCountry (aEntity.getCountryCode ());
      final HCNodeList aCtrl = new HCNodeList ();
      final EFamFamFlagIcon eIcon = aCountry == null ? null : EFamFamFlagIcon.getFromIDOrNull (aCountry.getCountry ());
      if (eIcon != null)
        aCtrl.addChild (eIcon.getAsNode ()).addChild (" ");
      aCtrl.addChild (aCountry.getDisplayCountry (aDisplayLocale) + " [" + aEntity.getCountryCode () + "]");
      aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Country code").setCtrl (aCtrl));
    }
    if (aEntity.hasGeographicalInformation ())
    {
      aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Geographical information")
                                                    .setCtrl (HCExtHelper.nl2divList (aEntity.getGeographicalInformation ())));
    }
    if (aEntity.identifiers ().isNotEmpty ())
    {
      final BootstrapTable aTable = new BootstrapTable (HCCol.star (), HCCol.star ());
      aTable.addHeaderRow ().addCells ("Scheme", "Value");
      for (final SMPBusinessCardIdentifier aIdentifier : aEntity.identifiers ())
        aTable.addBodyRow ().addCells (aIdentifier.getScheme (), aIdentifier.getValue ());
      aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Identifiers").setCtrl (aTable));
    }
    if (aEntity.websiteURIs ().isNotEmpty ())
    {
      final HCNodeList aNL = new HCNodeList ();
      for (final String sWebsiteURI : aEntity.websiteURIs ())
        aNL.addChild (new HCDiv ().addChild (HCA.createLinkedWebsite (sWebsiteURI)));
      aForm2.addFormGroup (new BootstrapFormGroup ().setLabel ("Website URIs").setCtrl (aNL));
    }
    if (aEntity.contacts ().isNotEmpty ())
    {
      final BootstrapTable aTable = new BootstrapTable (HCCol.star (), HCCol.star (), HCCol.star (), HCCol.star ());
      aTable.addHeaderRow ().addCells ("Type", "Name", "Phone number", "Email address");
      for (final SMPBusinessCardContact aContact : aEntity.contacts ())
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
    return aPanel;
  }

  @Override
  protected void showSelectedObject (@NonNull final WebPageExecutionContext aWPEC,
                                     @NonNull final ISMPBusinessCard aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of Business Card"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Service Group")
                                                 .setCtrl (new HCA (createViewURL (aWPEC,
                                                                                   CMenuSecure.MENU_SERVICE_GROUPS,
                                                                                   aSelectedObject)).addChild (aSelectedObject.getID ())));

    int nIndex = 0;
    for (final SMPBusinessCardEntity aEntity : aSelectedObject.getAllEntities ())
    {
      ++nIndex;
      aForm.addChild (showBusinessCardEntity (aEntity, nIndex, aDisplayLocale));
    }

    if (nIndex == 0)
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Entity").setCtrl (em ("none defined")));

    aNodeList.addChild (aForm);
  }

  @Override
  protected void validateAndSaveInputParameters (@NonNull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPBusinessCard aSelectedObject,
                                                 @NonNull final FormErrorList aFormErrors,
                                                 @NonNull final EWebPageFormAction eFormAction)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final boolean bEdit = eFormAction.isEdit ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    final ISMPServiceGroupManager aServiceGroupManager = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

    final String sServiceGroupID = bEdit ? aSelectedObject.getID () : aWPEC.params ()
                                                                           .getAsStringTrimmed (FIELD_SERVICE_GROUP_ID);
    ISMPServiceGroup aServiceGroup = null;
    final ICommonsList <SMPBusinessCardEntity> aSMPEntities = new CommonsArrayList <> ();

    // validations
    if (StringHelper.isEmpty (sServiceGroupID))
      aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "A Service Group must be selected!");
    else
    {
      aServiceGroup = aServiceGroupManager.getSMPServiceGroupOfID (aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID));
      if (aServiceGroup == null)
        aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "The provided Service Group does not exist!");
      else
        if (!bEdit)
        {
          final ISMPBusinessCard aExistingBusinessCard = aBusinessCardMgr.getSMPBusinessCardOfID (aServiceGroup.getParticipantIdentifier ());
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
        if (StringHelper.isEmpty (sEntityName))
          aFormErrors.addFieldError (sFieldName, "The Name of the Entity must be provided!");

        // Entity country code
        final String sFieldCountryCode = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                                       sEntityRowID,
                                                                       SUFFIX_COUNTRY_CODE);
        final String sCountryCode = aEntityRow.get (SUFFIX_COUNTRY_CODE);
        if (StringHelper.isEmpty (sCountryCode))
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
            if (StringHelper.isEmpty (sScheme))
              aFormErrors.addFieldError (sFieldScheme, "The Scheme of the Identifier must be provided!");

            // Value
            final String sFieldValue = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                                     sEntityRowID,
                                                                     PREFIX_IDENTIFIER,
                                                                     sIdentifierRowID,
                                                                     SUFFIX_VALUE);
            final String sValue = aIdentifierRow.get (SUFFIX_VALUE);
            if (StringHelper.isEmpty (sValue))
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
            if (StringHelper.isNotEmpty (sEmail))
              if (!EmailAddressValidator.isValid (sEmail))
                aFormErrors.addFieldError (sFieldEmail, "The provided email address is invalid!");

            final boolean bIsAnySet = StringHelper.isNotEmpty (sType) ||
                                      StringHelper.isNotEmpty (sName) ||
                                      StringHelper.isNotEmpty (sPhoneNumber) ||
                                      StringHelper.isNotEmpty (sEmail);

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
        if (aRegDate == null && StringHelper.isNotEmpty (sRegDate))
          aFormErrors.addFieldError (sFieldRegDate, "The entered registration date is invalid!");

        if (aFormErrors.size () == nErrors)
        {
          // Add to list
          final boolean bIsNewEntity = sEntityRowID.startsWith (TMP_ID_PREFIX);
          final SMPBusinessCardEntity aEntity = bIsNewEntity ? new SMPBusinessCardEntity ()
                                                             : new SMPBusinessCardEntity (sEntityRowID);
          aEntity.names ().add (new SMPBusinessCardName (sEntityName, null));
          aEntity.setCountryCode (sCountryCode);
          aEntity.setGeographicalInformation (sGeoInfo);
          aEntity.identifiers ().setAll (aSMPIdentifiers);
          aEntity.websiteURIs ().setAll (aWebsiteURIs);
          aEntity.contacts ().setAll (aSMPContacts);
          aEntity.setAdditionalInformation (sAdditionalInfo);
          aEntity.setRegistrationDate (aRegDate);
          aSMPEntities.add (aEntity);
        }
      }

    if (aSMPEntities.isEmpty ())
      if (aFormErrors.isEmpty ())
        aFormErrors.addFieldError (FIELD_SERVICE_GROUP_ID, "At least one entity must be provided.");

    if (aFormErrors.isEmpty ())
    {
      // Store in a consistent manner
      aSMPEntities.sort ( (o1, o2) -> o1.names ()
                                        .getFirstOrNull ()
                                        .getName ()
                                        .compareToIgnoreCase (o2.names ().getFirstOrNull ().getName ()));
      if (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aServiceGroup.getParticipantIdentifier (), aSMPEntities) !=
          null)
      {
        final ISMPSettings aSettings = SMPMetaManager.getSettings ();
        aWPEC.postRedirectGetInternal (success ("The Business Card for Service Group '" +
                                                aServiceGroup.getID () +
                                                "' was successfully saved." +
                                                (aSettings.isDirectoryIntegrationEnabled () &&
                                                 aSettings.isDirectoryIntegrationAutoUpdate () ? " " +
                                                                                                 SMPWebAppConfiguration.getDirectoryName () +
                                                                                                 " server should have been updated."
                                                                                               : "")));
      }
      else
        aWPEC.postRedirectGetInternal (error ("Error creating the Business Card for Service Group '" +
                                              aServiceGroup.getID () +
                                              "'"));
    }
  }

  @NonNull
  private static HCRow _createIdentifierInputForm (@NonNull final ILayoutExecutionContext aLEC,
                                                   @NonNull final String sEntityID,
                                                   @Nullable final SMPBusinessCardIdentifier aExistingIdentifier,
                                                   @Nullable final String sExistingID,
                                                   @NonNull final FormErrorList aFormErrors)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final String sIdentifierID = StringHelper.isNotEmpty (sExistingID) ? sExistingID : TMP_ID_PREFIX +
                                                                                       Integer.toString (GlobalIDFactory.getNewIntID ());

    final HCRow aRow = new HCRow ();

    // Identifier scheme
    {
      final String sFieldScheme = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                                sEntityID,
                                                                PREFIX_IDENTIFIER,
                                                                sIdentifierID,
                                                                SUFFIX_SCHEME);
      final HCEdit aCtrl = new HCEdit (new RequestField (sFieldScheme,
                                                         aExistingIdentifier == null ? null : aExistingIdentifier
                                                                                                                 .getScheme ())).setPlaceholder ("Identifier scheme");
      aCtrl.addClass (CBootstrapCSS.FORM_CONTROL);
      aRow.addCell (aCtrl,
                    BootstrapFormHelper.createDefaultErrorNode (aFormErrors.getListOfField (sFieldScheme),
                                                                aDisplayLocale));
    }

    // Identifier Value
    {
      final String sFieldValue = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                               sEntityID,
                                                               PREFIX_IDENTIFIER,
                                                               sIdentifierID,
                                                               SUFFIX_VALUE);
      final HCEdit aCtrl = new HCEdit (new RequestField (sFieldValue,
                                                         aExistingIdentifier == null ? null : aExistingIdentifier
                                                                                                                 .getValue ())).setPlaceholder ("Identifier value");
      aCtrl.addClass (CBootstrapCSS.FORM_CONTROL);
      aRow.addCell (aCtrl,
                    BootstrapFormHelper.createDefaultErrorNode (aFormErrors.getListOfField (sFieldValue),
                                                                aDisplayLocale));
    }

    aRow.addCell (new BootstrapButton (EBootstrapButtonSize.SMALL).setIcon (EDefaultIcon.DELETE)
                                                                  .setOnClick (JQuery.idRef (aRow).remove ()));

    return aRow;
  }

  @NonNull
  private static HCRow _createContactInputForm (@NonNull final ILayoutExecutionContext aLEC,
                                                @NonNull final String sEntityID,
                                                @Nullable final SMPBusinessCardContact aExistingContact,
                                                @Nullable final String sExistingID,
                                                @NonNull final FormErrorList aFormErrors)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final String sContactID = StringHelper.isNotEmpty (sExistingID) ? sExistingID : TMP_ID_PREFIX +
                                                                                    Integer.toString (GlobalIDFactory.getNewIntID ());

    final HCRow aRow = new HCRow ();

    // Type
    {
      final String sFieldType = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                              sEntityID,
                                                              PREFIX_CONTACT,
                                                              sContactID,
                                                              SUFFIX_TYPE);
      final HCEdit aCtrl = new HCEdit (new RequestField (sFieldType,
                                                         aExistingContact == null ? null : aExistingContact.getType ()))
                                                                                                                        .setPlaceholder ("Contact type");
      aCtrl.addClass (CBootstrapCSS.FORM_CONTROL);
      aRow.addCell (aCtrl,
                    BootstrapFormHelper.createDefaultErrorNode (aFormErrors.getListOfField (sFieldType),
                                                                aDisplayLocale));
    }

    // Name
    {
      final String sFieldName = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                              sEntityID,
                                                              PREFIX_CONTACT,
                                                              sContactID,
                                                              SUFFIX_NAME);
      final HCEdit aCtrl = new HCEdit (new RequestField (sFieldName,
                                                         aExistingContact == null ? null : aExistingContact.getName ()))
                                                                                                                        .setPlaceholder ("Contact name");
      aCtrl.addClass (CBootstrapCSS.FORM_CONTROL);
      aRow.addCell (aCtrl,
                    BootstrapFormHelper.createDefaultErrorNode (aFormErrors.getListOfField (sFieldName),
                                                                aDisplayLocale));
    }

    // Phone number
    {
      final String sFieldPhone = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                               sEntityID,
                                                               PREFIX_CONTACT,
                                                               sContactID,
                                                               SUFFIX_PHONE);
      final HCEdit aCtrl = new HCEdit (new RequestField (sFieldPhone,
                                                         aExistingContact == null ? null : aExistingContact
                                                                                                           .getPhoneNumber ())).setPlaceholder ("Contact phone number");
      aCtrl.addClass (CBootstrapCSS.FORM_CONTROL);
      aRow.addCell (aCtrl,
                    BootstrapFormHelper.createDefaultErrorNode (aFormErrors.getListOfField (sFieldPhone),
                                                                aDisplayLocale));
    }

    // Email address
    {
      final String sFieldEmail = RequestParamMap.getFieldName (PREFIX_ENTITY,
                                                               sEntityID,
                                                               PREFIX_CONTACT,
                                                               sContactID,
                                                               SUFFIX_EMAIL);
      final HCEdit aCtrl = new HCEdit (new RequestField (sFieldEmail,
                                                         aExistingContact == null ? null : aExistingContact
                                                                                                           .getEmail ())).setPlaceholder ("Contact email address");
      aCtrl.addClass (CBootstrapCSS.FORM_CONTROL);
      aRow.addCell (aCtrl,
                    BootstrapFormHelper.createDefaultErrorNode (aFormErrors.getListOfField (sFieldEmail),
                                                                aDisplayLocale));
    }

    aRow.addCell (new BootstrapButton (EBootstrapButtonSize.SMALL).setIcon (EDefaultIcon.DELETE)
                                                                  .setOnClick (JQuery.idRef (aRow).remove ()));

    return aRow;
  }

  @NonNull
  private static IHCNode _createEntityInputForm (@NonNull final LayoutExecutionContext aLEC,
                                                 @Nullable final SMPBusinessCardEntity aExistingEntity,
                                                 @Nullable final String sExistingID,
                                                 @NonNull final FormErrorList aFormErrors,
                                                 final boolean bFormSubmitted)
  {
    final Locale aDisplayLocale = aLEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aLEC.getRequestScope ();
    final String sEntityID = StringHelper.isNotEmpty (sExistingID) ? sExistingID : TMP_ID_PREFIX +
                                                                                   Integer.toString (GlobalIDFactory.getNewIntID ());

    final BootstrapCard aPanel = new BootstrapCard ().setID (sEntityID);
    aPanel.createAndAddHeader ().addChild ("Business Entity");
    final BootstrapCardBody aBody = aPanel.createAndAddBody ();

    final BootstrapViewForm aForm = aBody.addAndReturnChild (new BootstrapViewForm ());

    final String sFieldName = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityID, SUFFIX_NAME);
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Name")
                                                 .setCtrl (new HCEdit (new RequestField (sFieldName,
                                                                                         aExistingEntity == null ? null
                                                                                                                 : aExistingEntity.names ()
                                                                                                                                  .getFirstOrNull ()
                                                                                                                                  .getName ())))
                                                 .setErrorList (aFormErrors.getListOfField (sFieldName)));

    final String sFieldCountryCode = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityID, SUFFIX_COUNTRY_CODE);
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Country")
                                                 .setCtrl (new HCCountrySelect (new RequestField (sFieldCountryCode,
                                                                                                  aExistingEntity ==
                                                                                                                     null ? null
                                                                                                                          : aExistingEntity.getCountryCode ()),
                                                                                aDisplayLocale,
                                                                                HCCountrySelect.getAllCountries (EWithDeprecated.DEFAULT),
                                                                                (aLocale, aContentLocale) -> aLocale
                                                                                                                    .getDisplayCountry (aContentLocale) +
                                                                                                             " (" +
                                                                                                             aLocale.getCountry () +
                                                                                                             ")"))
                                                 .setErrorList (aFormErrors.getListOfField (sFieldCountryCode)));

    final String sFieldGeoInfo = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityID, SUFFIX_GEO_INFO);
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Geographical Information")
                                                 .setCtrl (new HCTextArea (new RequestField (sFieldGeoInfo,
                                                                                             aExistingEntity == null
                                                                                                                     ? null
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
      if (bFormSubmitted)
      {
        // Re-show of form
        if (aIdentifiers != null)
          for (final String sIdentifierRowID : aIdentifiers.keySet ())
            aTable.addBodyRow (_createIdentifierInputForm (aLEC, sEntityID, null, sIdentifierRowID, aFormErrors));
      }
      else
      {
        if (aExistingEntity != null)
        {
          // add all existing stored entities
          for (final SMPBusinessCardIdentifier aIdentifier : aExistingEntity.identifiers ())
            aTable.addBodyRow (_createIdentifierInputForm (aLEC, sEntityID, aIdentifier, (String) null, aFormErrors));
        }
      }

      {
        final JSAnonymousFunction aJSAppend = new JSAnonymousFunction ();
        final JSParam aJSAppendData = aJSAppend.param ("data");
        aJSAppend.body ()
                 .add (JQuery.idRef (sBodyID)
                             .append (aJSAppendData.ref (PhotonUnifiedResponse.HtmlHelper.PROPERTY_HTML)));

        final JSPackage aOnAdd = new JSPackage ();
        aOnAdd.add (new JQueryAjaxBuilder ().url (AJAX_CREATE_IDENTIFIER.getInvocationURL (aRequestScope)
                                                                        .add (PARAM_ENTITY_ID, sEntityID))
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
                                                 .setCtrl (new HCTextArea (new RequestField (sFieldWebsiteURIs,
                                                                                             aExistingEntity == null
                                                                                                                     ? null
                                                                                                                     : StringImplode.imploder ()
                                                                                                                                    .source (aExistingEntity.websiteURIs ())
                                                                                                                                    .separator ('\n')
                                                                                                                                    .build ())))
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
      if (bFormSubmitted)
      {
        // Re-show of form
        if (aContacts != null)
          for (final String sIdentifierRowID : aContacts.keySet ())
            aTable.addBodyRow (_createContactInputForm (aLEC, sEntityID, null, sIdentifierRowID, aFormErrors));
      }
      else
      {
        if (aExistingEntity != null)
        {
          // add all existing stored entities
          for (final SMPBusinessCardContact aContact : aExistingEntity.contacts ())
            aTable.addBodyRow (_createContactInputForm (aLEC, sEntityID, aContact, (String) null, aFormErrors));
        }
      }

      {
        final JSAnonymousFunction aJSAppend = new JSAnonymousFunction ();
        final JSParam aJSAppendData = aJSAppend.param ("data");
        aJSAppend.body ()
                 .add (JQuery.idRef (sBodyID)
                             .append (aJSAppendData.ref (PhotonUnifiedResponse.HtmlHelper.PROPERTY_HTML)));

        final JSPackage aOnAdd = new JSPackage ();
        aOnAdd.add (new JQueryAjaxBuilder ().url (AJAX_CREATE_CONTACT.getInvocationURL (aRequestScope)
                                                                     .add (PARAM_ENTITY_ID, sEntityID))
                                            .data (new JSAssocArray ())
                                            .success (JSJQueryHelper.jqueryAjaxSuccessHandler (aJSAppend, null))
                                            .build ());

        aNL.addChild (new BootstrapButton ().setIcon (EDefaultIcon.PLUS).addChild ("Add Contact").setOnClick (aOnAdd));
      }

      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Contacts").setCtrl (aNL));
    }

    final String sFieldAdditionalInfo = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityID, SUFFIX_ADDITIONAL_INFO);
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Additional Information")
                                                 .setCtrl (new HCTextArea (new RequestField (sFieldAdditionalInfo,
                                                                                             aExistingEntity == null
                                                                                                                     ? null
                                                                                                                     : aExistingEntity.getAdditionalInformation ())))
                                                 .setErrorList (aFormErrors.getListOfField (sFieldAdditionalInfo)));

    final String sFieldRegDate = RequestParamMap.getFieldName (PREFIX_ENTITY, sEntityID, SUFFIX_REG_DATE);
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Registration Date")
                                                 .setCtrl (BootstrapDateTimePicker.create (sFieldRegDate,
                                                                                           aExistingEntity == null
                                                                                                                   ? null
                                                                                                                   : aExistingEntity.getRegistrationDate (),
                                                                                           aDisplayLocale))
                                                 .setErrorList (aFormErrors.getListOfField (sFieldRegDate)));

    final BootstrapButtonToolbar aToolbar = aBody.addAndReturnChild (new BootstrapButtonToolbar (aLEC));
    aToolbar.addButton ("Delete this Entity", JQuery.idRef (aPanel).remove (), EDefaultIcon.DELETE);

    return aPanel;
  }

  @Override
  protected void showInputForm (@NonNull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPBusinessCard aSelectedObject,
                                @NonNull final BootstrapForm aForm,
                                final boolean bFormSubmitted,
                                @NonNull final EWebPageFormAction eFormAction,
                                @NonNull final FormErrorList aFormErrors)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

    aForm.addChild (getUIHandler ().createActionHeader (bEdit ? "Edit Business Card" : "Create new Business Card"));

    if (bEdit)
    {
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service Group")
                                                   .setCtrl (aSelectedObject.getID ())
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_GROUP_ID)));
    }
    else
    {
      // Show only service groups that don't have a BC already
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Service Group")
                                                   .setCtrl (new HCServiceGroupSelect (new RequestField (FIELD_SERVICE_GROUP_ID,
                                                                                                         aSelectedObject !=
                                                                                                                                 null ? aSelectedObject.getID ()
                                                                                                                                      : null),
                                                                                       aDisplayLocale,
                                                                                       x -> aBusinessCardMgr.getSMPBusinessCardOfID (x.getParticipantIdentifier ()) ==
                                                                                            null))
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_SERVICE_GROUP_ID)));
    }

    final HCDiv aEntityContainer = aForm.addAndReturnChild (new HCDiv ().setID ("entitycontainer"));

    final IRequestParamMap aEntities = aWPEC.getRequestParamMap ().getMap (PREFIX_ENTITY);
    if (bFormSubmitted)
    {
      // Re-show of form
      if (aEntities != null)
        for (final String sEntityRowID : aEntities.keySet ())
          aEntityContainer.addChild (_createEntityInputForm (aWPEC, null, sEntityRowID, aFormErrors, bFormSubmitted));
    }
    else
    {
      if (aSelectedObject != null)
      {
        // add all existing stored entities
        for (final SMPBusinessCardEntity aEntity : aSelectedObject.getAllEntities ())
          aEntityContainer.addChild (_createEntityInputForm (aWPEC,
                                                             aEntity,
                                                             (String) null,
                                                             aFormErrors,
                                                             bFormSubmitted));
      }
    }

    {
      final JSAnonymousFunction aJSAppend = new JSAnonymousFunction ();
      final JSParam aJSAppendData = aJSAppend.param ("data");
      aJSAppend.body ()
               .add (JQuery.idRef (aEntityContainer)
                           .append (aJSAppendData.ref (PhotonUnifiedResponse.HtmlHelper.PROPERTY_HTML)));

      final JSPackage aOnAdd = new JSPackage ();
      aOnAdd.add (new JQueryAjaxBuilder ().url (AJAX_CREATE_ENTITY.getInvocationURL (aRequestScope))
                                          .data (new JSAssocArray ())
                                          .success (JSJQueryHelper.jqueryAjaxSuccessHandler (aJSAppend, null))
                                          .build ());

      aForm.addChild (new BootstrapButton ().addChild ("Add Entity").setIcon (EDefaultIcon.PLUS).setOnClick (aOnAdd));
    }
  }

  @NonNull
  private IHCNode _createActionCell (@NonNull final WebPageExecutionContext aWPEC,
                                     @NonNull final ISMPBusinessCard aCurObject)
  {
    final String sDisplayName = aCurObject.getID ();
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
                                                                  aCurObject.getParticipantIdentifier ()
                                                                            .getURIPercentEncoded ())).setTitle ("Perform SMP query on " +
                                                                                                                 sDisplayName)
                                                                                                      .setTargetBlank ()
                                                                                                      .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()));

    // When auto update is enabled, there is no need for a manual update
    // Change: update possibility always, in case document type was changed
    {
      ret.addChildren (new HCTextNode (" "),
                       new HCA (aWPEC.getSelfHref ()
                                     .add (CPageParam.PARAM_ACTION, ACTION_PUBLISH_TO_INDEXER)
                                     .add (CPageParam.PARAM_OBJECT, aCurObject.getID ())).setTitle (
                                                                                                    "Update Business Card in " +
                                                                                                    SMPWebAppConfiguration.getDirectoryName ())
                                                                                         .addChild (EFamFamIcon.ARROW_RIGHT.getAsNode ()));
    }

    return ret;
  }

  @Override
  protected void showListOfExistingObjects (@NonNull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final String sDirectoryName = SMPWebAppConfiguration.getDirectoryName ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    final ICommonsList <ISMPBusinessCard> aAllBusinessCards = aBusinessCardMgr.getAllSMPBusinessCards ();

    {
      final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
      aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
      aToolbar.addButton ("Create new Business Card", createCreateURL (aWPEC), EDefaultIcon.NEW);
      aToolbar.addChild (new BootstrapButton ().setOnClick (aWPEC.getSelfHref ()
                                                                 .add (CPageParam.PARAM_ACTION,
                                                                       ACTION_PUBLISH_ALL_TO_INDEXER))
                                               .setIcon (EFamFamIcon.ARROW_REDO)
                                               .addChild ("Update all Business Cards in " + sDirectoryName)
                                               .setDisabled (aAllBusinessCards.isEmpty ()));

      aNodeList.addChild (aToolbar);

      final int nCount = PushAllBusinessCardsToDirectory.getRunningJobCount ();
      if (nCount > 0)
      {
        aNodeList.addChild (warn ("Currently Business Cards are pushed to the " +
                                  sDirectoryName +
                                  " in the background"));
      }
    }

    final HCTable aTable = new HCTable (new DTCol ("Service Group").setDataSort (0, 1)
                                                                   .setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Name"),
                                        new DTCol ("Country"),
                                        new DTCol ("GeoInfo"),
                                        new DTCol ("Identifiers"),
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPBusinessCard aCurObject : aAllBusinessCards)
    {
      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);
      final String sDisplayName = aCurObject.getID ();

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
          aRow.addCell (aEntity.names ().getFirstOrNull ().getName ());

          final Locale aCountry = CountryCache.getInstance ().getCountry (aEntity.getCountryCode ());
          final IHCCell <?> aCountryCell = aRow.addCell ();
          final EFamFamFlagIcon eIcon = aCountry == null ? null : EFamFamFlagIcon.getFromIDOrNull (aCountry
                                                                                                           .getCountry ());
          if (eIcon != null)
            aCountryCell.addChild (eIcon.getAsNode ()).addChild (" ");
          aCountryCell.addChild (aCountry.getDisplayCountry (aDisplayLocale));

          aRow.addCell (HCExtHelper.nl2divList (aEntity.getGeographicalInformation ()));
          {
            final HCNodeList aIdentifiers = new HCNodeList ();
            for (final SMPBusinessCardIdentifier aIdentifier : aEntity.identifiers ())
              aIdentifiers.addChild (div (aIdentifier.getScheme ()).addChild (" - ")
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
