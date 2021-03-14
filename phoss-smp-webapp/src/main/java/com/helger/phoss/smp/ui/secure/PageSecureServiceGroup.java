/**
 * Copyright (C) 2014-2021 Philip Helger and contributors
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

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.collection.attr.StringMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.compare.ESortOrder;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.ISimpleURL;
import com.helger.commons.url.SimpleURL;
import com.helger.dns.ip.IPV4Addr;
import com.helger.html.hc.IHCNode;
import com.helger.html.hc.ext.HCExtHelper;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCTextArea;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.html.tabular.HCTable;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.html.textlevel.HCEM;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.network.port.NetworkOnlineStatusDeterminator;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.ESMPRESTType;
import com.helger.phoss.smp.SMPServerConfiguration;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCard;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.businesscard.SMPBusinessCardEntity;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformation;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.smlhook.IRegistrationHook;
import com.helger.phoss.smp.smlhook.RegistrationHookException;
import com.helger.phoss.smp.smlhook.RegistrationHookFactory;
import com.helger.phoss.smp.ui.AbstractSMPWebPageForm;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.secure.hc.HCUserSelect;
import com.helger.photon.app.url.LinkHelper;
import com.helger.photon.bootstrap4.alert.BootstrapQuestionBox;
import com.helger.photon.bootstrap4.badge.BootstrapBadge;
import com.helger.photon.bootstrap4.badge.EBootstrapBadgeType;
import com.helger.photon.bootstrap4.button.BootstrapButton;
import com.helger.photon.bootstrap4.button.EBootstrapButtonSize;
import com.helger.photon.bootstrap4.button.EBootstrapButtonType;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.bootstrap4.grid.BootstrapRow;
import com.helger.photon.bootstrap4.pages.BootstrapPagesMenuConfigurator;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandler;
import com.helger.photon.bootstrap4.pages.handler.AbstractBootstrapWebPageActionHandlerDelete;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDTColAction;
import com.helger.photon.bootstrap4.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.EShowList;
import com.helger.photon.uicore.page.EWebPageFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.DataTables;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.photon.uictrls.datatables.column.EDTColType;
import com.helger.photon.uictrls.famfam.EFamFamIcon;
import com.helger.smpclient.url.IBDXLURLProvider;
import com.helger.smpclient.url.IPeppolURLProvider;
import com.helger.smpclient.url.ISMPURLProvider;
import com.helger.smpclient.url.SMPDNSResolutionException;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;

@WorkInProgress
public final class PageSecureServiceGroup extends AbstractSMPWebPageForm <ISMPServiceGroup>
{
  private static final String FIELD_PARTICIPANT_ID_SCHEME = "participantidscheme";
  private static final String FIELD_PARTICIPANT_ID_VALUE = "participantidvalue";
  private static final String FIELD_OWNING_USER_ID = "owninguser";
  private static final String FIELD_EXTENSION = "extension";

  private static final String ACTION_CHECK_DNS = "checkdns";
  private static final String ACTION_REGISTER_TO_SML = "register-to-sml";
  private static final String ACTION_UNREGISTER_FROM_SML = "unregister-from-sml";

  public PageSecureServiceGroup (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Service groups");
    setDeleteHandler (new AbstractBootstrapWebPageActionHandlerDelete <ISMPServiceGroup, WebPageExecutionContext> ()
    {
      @Override
      protected void showQuery (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final ISMPServiceGroup aSelectedObject)
      {
        final BootstrapQuestionBox aQB = question (div ("Are you sure you want to delete the complete service group '" +
                                                        aSelectedObject.getParticipantIdentifier ().getURIEncoded () +
                                                        "'?")).addChild (div ("This means that all endpoints and all redirects are deleted as well."));
        if (SMPMetaManager.hasBusinessCardMgr ())
          aQB.addChild (div ("If a Business Card for this service group exists, it will also be deleted."));
        if (SMPMetaManager.getSettings ().isSMLEnabled ())
          aQB.addChild (div ("Since the connection to the SML is active this service group will also be deleted from the SML!"));

        aForm.addChild (aQB);
      }

      @Override
      protected void performAction (@Nonnull final WebPageExecutionContext aWPEC, @Nonnull final ISMPServiceGroup aSelectedObject)
      {
        final HCNodeList aNL = new HCNodeList ();
        final IParticipantIdentifier aParticipantID = aSelectedObject.getParticipantIdentifier ();

        try
        {
          // Delete the service group both locally and on the SML (if active)!
          final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
          if (aServiceGroupMgr.deleteSMPServiceGroup (aParticipantID, true).isChanged ())
          {
            aNL.addChild (success ("The SMP ServiceGroup for participant '" +
                                   aParticipantID.getURIEncoded () +
                                   "' was successfully deleted!"));
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
        aWPEC.postRedirectGetInternal (aNL);
      }
    });
    addCustomHandler (ACTION_CHECK_DNS, new AbstractBootstrapWebPageActionHandler <ISMPServiceGroup, WebPageExecutionContext> (false)
    {
      @Nonnull
      public EShowList handleAction (@Nonnull final WebPageExecutionContext aWPEC, @Nullable final ISMPServiceGroup aSelectedObject)
      {
        final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
        final HCNodeList aNodeList = aWPEC.getNodeList ();
        final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
        final ISMPSettings aSettings = SMPMetaManager.getSettings ();

        aNodeList.addChild (getUIHandler ().createActionHeader ("Check DNS state of participants"));

        {
          final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
          aToolbar.addButton ("Refresh", aWPEC.getSelfHref ().add (CPageParam.PARAM_ACTION, ACTION_CHECK_DNS), EDefaultIcon.REFRESH);
          aNodeList.addChild (aToolbar);
        }

        // Simple check if we're online or not
        final boolean bOffline = NetworkOnlineStatusDeterminator.getNetworkStatus ().isOffline ();
        if (bOffline)
        {
          aNodeList.addChild (warn ("It seems like you are offline! So please interpret the results on this page with care!"));
        }
        else
        {
          aNodeList.addChild (info ("Please note that some DNS changes need some time to propagate! All changes should usually be visible within 1 hour!"));
        }

        final String sSMLZoneName = aSettings.getSMLDNSZone ();
        final ISMPURLProvider aURLProvider = SMPMetaManager.getSMPURLProvider ();

        final HCTable aTable = new HCTable (new DTCol ("Service group").setInitialSorting (ESortOrder.ASCENDING),
                                            new DTCol ("DNS name"),
                                            new DTCol ("IP address").setDataSort (2, 0),
                                            new DTCol ("Nice name"),
                                            new DTCol ("Action")).setID (getID () + "_checkdns");
        for (final ISMPServiceGroup aServiceGroup : aServiceGroupMgr.getAllSMPServiceGroups ())
        {
          String sDNSName = null;
          try
          {
            if (aURLProvider instanceof IPeppolURLProvider)
              sDNSName = ((IPeppolURLProvider) aURLProvider).getDNSNameOfParticipant (aServiceGroup.getParticipantIdentifier (),
                                                                                      sSMLZoneName);
            else
              if (aURLProvider instanceof IBDXLURLProvider)
              {
                // Fallback by not resolving the NAPTR
                sDNSName = ((IBDXLURLProvider) aURLProvider).getDNSNameOfParticipant (aServiceGroup.getParticipantIdentifier (),
                                                                                      sSMLZoneName);
              }
          }
          catch (final SMPDNSResolutionException ex)
          {
            // Ignore
          }

          InetAddress aInetAddress = null;
          // Avoid that the loopback interface is returned
          if (sDNSName != null)
            try
            {
              aInetAddress = InetAddress.getByName (sDNSName);
            }
            catch (final UnknownHostException ex)
            {
              // Ignore
            }

          InetAddress aNice = null;
          if (aInetAddress != null)
            try
            {
              aNice = InetAddress.getByAddress (aInetAddress.getAddress ());
            }
            catch (final UnknownHostException ex)
            {
              // Ignore
            }

          final HCRow aRow = aTable.addBodyRow ();
          aRow.addCell (aServiceGroup.getParticipantIdentifier ().getURIEncoded ());
          if (sDNSName != null)
            aRow.addCell (new HCA (new SimpleURL ("http://" + sDNSName)).setTargetBlank ().addChild (sDNSName));
          else
            aRow.addCell (new HCEM ().addChild ("DNS resolve failed"));
          if (aInetAddress != null)
          {
            aRow.addCell (new IPV4Addr (aInetAddress).getAsString ());
            aRow.addCell (aNice == null ? null : aNice.getCanonicalHostName ());
            aRow.addCell (new BootstrapButton (EBootstrapButtonType.DANGER,
                                               EBootstrapButtonSize.SMALL).addChild ("Unregister from SML")
                                                                          .setOnClick (aWPEC.getSelfHref ()
                                                                                            .add (CPageParam.PARAM_ACTION,
                                                                                                  ACTION_UNREGISTER_FROM_SML)
                                                                                            .add (CPageParam.PARAM_OBJECT,
                                                                                                  aServiceGroup.getID ()))
                                                                          .setDisabled (bOffline || !aSettings.isSMLEnabled ()));
          }
          else
          {
            aRow.addCell (new BootstrapBadge (EBootstrapBadgeType.DANGER).addChild ("is not registered in SML"));
            aRow.addCell ();
            aRow.addCell (new BootstrapButton (EBootstrapButtonSize.SMALL).addChild ("Register in SML")
                                                                          .setOnClick (aWPEC.getSelfHref ()
                                                                                            .add (CPageParam.PARAM_ACTION,
                                                                                                  ACTION_REGISTER_TO_SML)
                                                                                            .add (CPageParam.PARAM_OBJECT,
                                                                                                  aServiceGroup.getID ()))
                                                                          .setDisabled (bOffline || !aSettings.isSMLEnabled ()));
          }
        }

        final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
        aNodeList.addChild (aTable).addChild (aDataTables);

        {
          final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
          aToolbar.addButtonBack (aDisplayLocale);
          aNodeList.addChild (aToolbar);
        }

        return EShowList.DONT_SHOW_LIST;
      }
    });
    addCustomHandler (ACTION_REGISTER_TO_SML, new AbstractBootstrapWebPageActionHandler <ISMPServiceGroup, WebPageExecutionContext> (true)
    {
      @Nonnull
      public EShowList handleAction (@Nonnull final WebPageExecutionContext aWPEC, @Nonnull final ISMPServiceGroup aSelectedObject)
      {
        final StringMap aTargetParams = new StringMap ();
        aTargetParams.putIn (CPageParam.PARAM_ACTION, ACTION_CHECK_DNS);
        final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
        final IParticipantIdentifier aParticipantID = aSelectedObject.getParticipantIdentifier ();
        try
        {
          aHook.createServiceGroup (aParticipantID);
          aWPEC.postRedirectGetInternal (success ("The Service group '" +
                                                  aParticipantID.getURIEncoded () +
                                                  "' was successfully registered at the configured SML!"),
                                         aTargetParams);
        }
        catch (final RegistrationHookException ex)
        {
          aWPEC.postRedirectGetInternal (error ("Error registering the Service group '" +
                                                aParticipantID.getURIEncoded () +
                                                "' at the configured SML!").addChild (SMPCommonUI.getTechnicalDetailsUI (ex)),
                                         aTargetParams);
        }
        // Never reached
        return EShowList.DONT_SHOW_LIST;
      }
    });
    addCustomHandler (ACTION_UNREGISTER_FROM_SML,
                      new AbstractBootstrapWebPageActionHandler <ISMPServiceGroup, WebPageExecutionContext> (true)
                      {
                        @Nonnull
                        public EShowList handleAction (@Nonnull final WebPageExecutionContext aWPEC,
                                                       @Nonnull final ISMPServiceGroup aSelectedObject)
                        {
                          final StringMap aTargetParams = new StringMap ();
                          aTargetParams.putIn (CPageParam.PARAM_ACTION, ACTION_CHECK_DNS);
                          final IRegistrationHook aHook = RegistrationHookFactory.getInstance ();
                          try
                          {
                            aHook.deleteServiceGroup (aSelectedObject.getParticipantIdentifier ());
                            aWPEC.postRedirectGetInternal (success ("The Service group '" +
                                                                    aSelectedObject.getParticipantIdentifier ().getURIEncoded () +
                                                                    "' was successfully unregistered from the configured SML!"),
                                                           aTargetParams);
                          }
                          catch (final RegistrationHookException ex)
                          {
                            aWPEC.postRedirectGetInternal (error ("Error unregistering the Service group '" +
                                                                  aSelectedObject.getParticipantIdentifier ().getURIEncoded () +
                                                                  "' from the configured SML!").addChild (SMPCommonUI.getTechnicalDetailsUI (ex)),
                                                           aTargetParams);
                          }
                          // Never reached
                          return EShowList.DONT_SHOW_LIST;
                        }
                      });
  }

  @Override
  @Nonnull
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    if (aUserMgr.getActiveUserCount () == 0)
    {
      aNodeList.addChild (warn ("No user is present! At least one user must be present to create a service group."));
      aNodeList.addChild (new BootstrapButton ().addChild ("Create new user")
                                                .setOnClick (createCreateURL (aWPEC,
                                                                              BootstrapPagesMenuConfigurator.MENU_ADMIN_SECURITY_USER))
                                                .setIcon (EDefaultIcon.YES));
      return EValidity.INVALID;
    }
    return super.isValidToDisplayPage (aWPEC);
  }

  @Override
  @Nullable
  protected ISMPServiceGroup getSelectedObject (@Nonnull final WebPageExecutionContext aWPEC, @Nullable final String sID)
  {
    if (sID == null)
      return null;

    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
    return aServiceGroupMgr.getSMPServiceGroupOfID (aIdentifierFactory.parseParticipantIdentifier (sID));
  }

  @Override
  protected void showSelectedObject (@Nonnull final WebPageExecutionContext aWPEC, @Nonnull final ISMPServiceGroup aSelectedObject)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final boolean bShowBusinessCard = CSMP.ENABLE_ISSUE_56 && aSettings.isDirectoryIntegrationEnabled ();

    aNodeList.addChild (getUIHandler ().createActionHeader ("Show details of service group '" + aSelectedObject.getID () + "'"));

    final BootstrapViewForm aForm = new BootstrapViewForm ();
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Participant ID")
                                                 .setCtrl (aSelectedObject.getParticipantIdentifier ().getURIEncoded ()));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Owning user")
                                                 .setCtrl (SMPCommonUI.getOwnerName (aSelectedObject.getOwnerID ())));
    if (aSelectedObject.extensions ().isNotEmpty ())
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Extension").setCtrl (SMPCommonUI.getExtensionDisplay (aSelectedObject)));

    if (bShowBusinessCard)
    {
      aForm.addChild (getUIHandler ().createDataGroupHeader ("Business Card Details"));

      final ISMPBusinessCardManager aBCMgr = SMPMetaManager.getBusinessCardMgr ();
      final ISMPBusinessCard aBC = aBCMgr.getSMPBusinessCardOfServiceGroup (aSelectedObject);
      if (aBC != null)
      {
        int nIndex = 0;
        for (final SMPBusinessCardEntity aEntity : aBC.getAllEntities ())
        {
          ++nIndex;
          aForm.addChild (PageSecureBusinessCard.showBusinessCardEntity (aEntity, nIndex, aDisplayLocale));
        }
      }
    }

    aNodeList.addChild (aForm);
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nullable final ISMPServiceGroup aSelectedObject,
                                @Nonnull final BootstrapForm aForm,
                                final boolean bFormSubmitted,
                                @Nonnull final EWebPageFormAction eFormAction,
                                @Nonnull final FormErrorList aFormErrors)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final boolean bEdit = eFormAction.isEdit ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();

    aForm.setLeft (2);
    aForm.addChild (getUIHandler ().createActionHeader (bEdit ? "Edit service group '" + aSelectedObject.getID () + "'"
                                                              : "Create new service group"));

    {
      final String sDefaultScheme = aIdentifierFactory.getDefaultParticipantIdentifierScheme ();
      final BootstrapRow aRow = new BootstrapRow ();
      aRow.createColumn (GS_IDENTIFIER_SCHEME)
          .addChild (new HCEdit (new RequestField (FIELD_PARTICIPANT_ID_SCHEME,
                                                   aSelectedObject != null ? aSelectedObject.getParticipantIdentifier ().getScheme ()
                                                                           : sDefaultScheme)).setPlaceholder ("Identifier scheme")
                                                                                             .setReadOnly (bEdit));
      aRow.createColumn (GS_IDENTIFIER_VALUE)
          .addChild (new HCEdit (new RequestField (FIELD_PARTICIPANT_ID_VALUE,
                                                   aSelectedObject != null ? aSelectedObject.getParticipantIdentifier ().getValue ()
                                                                           : null)).setPlaceholder ("Identifier value")
                                                                                   .setReadOnly (bEdit));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Participant ID")
                                                   .setCtrl (aRow)
                                                   .setHelpText ("The participant identifier for which the service group should be created. The left part is the identifier scheme" +
                                                                 (sDefaultScheme == null ? "" : " (default: " + sDefaultScheme + ")") +
                                                                 ", the right part is the identifier value (e.g. 9915:test)")
                                                   .setErrorList (aFormErrors.getListOfFields (FIELD_PARTICIPANT_ID_SCHEME,
                                                                                               FIELD_PARTICIPANT_ID_VALUE)));
    }

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Owning User")
                                                 .setCtrl (new HCUserSelect (new RequestField (FIELD_OWNING_USER_ID,
                                                                                               aSelectedObject != null ? aSelectedObject.getOwnerID ()
                                                                                                                       : LoggedInUserManager.getInstance ()
                                                                                                                                            .getCurrentUserID ()),
                                                                             aDisplayLocale))
                                                 .setHelpText ("The user who owns this entry. Only this user can make changes via the REST API.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_OWNING_USER_ID)));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Extension")
                                                 .setCtrl (new HCTextArea (new RequestField (FIELD_EXTENSION,
                                                                                             aSelectedObject != null ? aSelectedObject.getFirstExtensionXML ()
                                                                                                                     : null)))
                                                 .setHelpText ("Optional extension to the service group. If present it must be valid XML content!")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_EXTENSION)));
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nullable final ISMPServiceGroup aSelectedObject,
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageFormAction eFormAction)
  {
    final boolean bEdit = eFormAction.isEdit ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();

    final String sParticipantIDScheme = aWPEC.params ().getAsString (FIELD_PARTICIPANT_ID_SCHEME);
    final String sParticipantIDValue = aWPEC.params ().getAsString (FIELD_PARTICIPANT_ID_VALUE);
    IParticipantIdentifier aParticipantID = null;
    final String sOwningUserID = aWPEC.params ().getAsString (FIELD_OWNING_USER_ID);
    final IUser aOwningUser = PhotonSecurityManager.getUserMgr ().getUserOfID (sOwningUserID);
    final String sExtension = aWPEC.params ().getAsString (FIELD_EXTENSION);

    // validations
    if (aIdentifierFactory.isParticipantIdentifierSchemeMandatory () && StringHelper.hasNoText (sParticipantIDScheme))
      aFormErrors.addFieldError (FIELD_PARTICIPANT_ID_SCHEME, "Participant ID scheme must not be empty!");
    else
      if (StringHelper.hasNoText (sParticipantIDValue))
        aFormErrors.addFieldError (FIELD_PARTICIPANT_ID_VALUE, "Participant ID value must not be empty!");
      else
      {
        aParticipantID = aIdentifierFactory.createParticipantIdentifier (sParticipantIDScheme, sParticipantIDValue);
        if (aParticipantID == null)
          aFormErrors.addFieldError (FIELD_PARTICIPANT_ID_VALUE, "The provided participant ID has an invalid syntax!");
        else
          if (!bEdit && aServiceGroupMgr.getSMPServiceGroupOfID (aParticipantID) != null)
            aFormErrors.addFieldError (FIELD_PARTICIPANT_ID_VALUE,
                                       "Another service group for the same participant ID is already present (may be case insensitive)!");
      }

    if (StringHelper.hasNoText (sOwningUserID))
      aFormErrors.addFieldError (FIELD_OWNING_USER_ID, "Owning User must not be empty!");
    else
      if (aOwningUser == null)
        aFormErrors.addFieldError (FIELD_OWNING_USER_ID, "Provided owning user does not exist!");

    if (StringHelper.hasText (sExtension))
    {
      final IMicroDocument aDoc = MicroReader.readMicroXML (sExtension);
      if (aDoc == null)
        aFormErrors.addFieldError (FIELD_EXTENSION, "The extension must be XML content.");
    }

    if (aFormErrors.isEmpty ())
    {
      if (bEdit)
      {
        // Edit only the internal data objects because no change to the SML is
        // necessary. Only the owner and the extension can be edited!
        try
        {
          aServiceGroupMgr.updateSMPServiceGroup (aParticipantID, aOwningUser.getID (), sExtension);
          aWPEC.postRedirectGetInternal (success ("The SMP ServiceGroup for participant '" +
                                                  aParticipantID.getURIEncoded () +
                                                  "' was successfully updated."));
        }
        catch (final SMPServerException ex)
        {
          aWPEC.postRedirectGetInternal (error ("Error updating the SMP ServiceGroup for participant '" +
                                                aParticipantID.getURIEncoded () +
                                                "'.").addChild (SMPCommonUI.getTechnicalDetailsUI (ex)));
        }
      }
      else
      {
        // Create the service group both locally and on the SML (if active)!
        ISMPServiceGroup aSG = null;
        Exception aCaughtEx = null;
        try
        {
          aSG = aServiceGroupMgr.createSMPServiceGroup (aOwningUser.getID (), aParticipantID, sExtension, true);
        }
        catch (final Exception ex)
        {
          aCaughtEx = ex;
        }
        if (aSG != null)
        {
          aWPEC.postRedirectGetInternal (success ("The new SMP ServiceGroup for participant '" +
                                                  aParticipantID.getURIEncoded () +
                                                  "' was successfully created."));
        }
        else
        {
          aWPEC.postRedirectGetInternal (error ("Error creating the new SMP ServiceGroup for participant '" +
                                                aParticipantID.getURIEncoded () +
                                                "'.").addChild (SMPCommonUI.getTechnicalDetailsUI (aCaughtEx)));
        }
      }
    }
  }

  @Override
  protected void showListOfExistingObjects (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
    final ISMPBusinessCardManager aBCMgr = SMPMetaManager.getBusinessCardMgr ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final ESMPRESTType eRESTType = SMPServerConfiguration.getRESTType ();
    final boolean bShowExtensionDetails = SMPWebAppConfiguration.isServiceGroupsExtensionsShow ();
    final boolean bShowBusinessCardName = CSMP.ENABLE_ISSUE_56 && aSettings.isDirectoryIntegrationEnabled ();

    final ICommonsList <ISMPServiceGroup> aAllServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();

    final BootstrapButtonToolbar aToolbar = new BootstrapButtonToolbar (aWPEC);
    aToolbar.addButton ("Create new Service group", createCreateURL (aWPEC), EDefaultIcon.NEW);
    aToolbar.addButton ("Refresh", aWPEC.getSelfHref (), EDefaultIcon.REFRESH);
    if (aSettings.isSMLRequired ())
    {
      // Disable button if no SML URL is configured
      // Disable button if no service group is present
      aToolbar.addAndReturnButton ("Check DNS state",
                                   aWPEC.getSelfHref ().add (CPageParam.PARAM_ACTION, ACTION_CHECK_DNS),
                                   EDefaultIcon.MAGNIFIER)
              .setDisabled (aSettings.getSMLDNSZone () == null || aAllServiceGroups.isEmpty () || !aSettings.isSMLEnabled ());
    }
    aNodeList.addChild (aToolbar);

    final boolean bShowDetails = aAllServiceGroups.size () <= 1000;

    final HCTable aTable = new HCTable (new DTCol ("Participant ID").setInitialSorting (ESortOrder.ASCENDING),
                                        new DTCol ("Owner"),
                                        bShowBusinessCardName ? new DTCol ("Business Card Name") : null,
                                        new DTCol (span (bShowExtensionDetails ? "Ext" : "Ext?").setTitle ("Is an Extension present?")),
                                        bShowDetails ? new DTCol (span ("Docs").setTitle ("Number of assigned document types")).setDisplayType (EDTColType.INT,
                                                                                                                                                aDisplayLocale)
                                                     : null,
                                        bShowDetails ? new DTCol (span ("Procs").setTitle ("Number of assigned processes")).setDisplayType (EDTColType.INT,
                                                                                                                                            aDisplayLocale)
                                                     : null,
                                        bShowDetails ? new DTCol (span ("EPs").setTitle ("Number of assigned endpoints")).setDisplayType (EDTColType.INT,
                                                                                                                                          aDisplayLocale)
                                                     : null,
                                        new BootstrapDTColAction (aDisplayLocale)).setID (getID ());
    for (final ISMPServiceGroup aCurObject : aAllServiceGroups)
    {

      final ISimpleURL aViewLink = createViewURL (aWPEC, aCurObject);
      final String sDisplayName = aCurObject.getParticipantIdentifier ().getURIEncoded ();

      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (new HCA (aViewLink).addChild (sDisplayName));
      aRow.addCell (SMPCommonUI.getOwnerName (aCurObject.getOwnerID ()));
      if (bShowBusinessCardName)
      {
        IHCNode aName = null;
        final ISMPBusinessCard aBC = aBCMgr.getSMPBusinessCardOfServiceGroup (aCurObject);
        if (aBC != null)
        {
          final SMPBusinessCardEntity aEntity = aBC.getEntityAtIndex (0);
          if (aEntity != null && aEntity.names ().isNotEmpty ())
            aName = HCTextNode.createOnDemand (aEntity.names ().getFirst ().getName ());
        }
        aRow.addCell (aName);
      }
      if (bShowExtensionDetails)
      {
        if (aCurObject.extensions ().isNotEmpty ())
          aRow.addCell (new HCCode ().addChildren (HCExtHelper.nl2divList (aCurObject.getFirstExtensionXML ())));
        else
          aRow.addCell ();
      }
      else
      {
        aRow.addCell (EPhotonCoreText.getYesOrNo (aCurObject.extensions ().isNotEmpty (), aDisplayLocale));
      }

      if (bShowDetails)
      {
        int nProcesses = 0;
        int nEndpoints = 0;
        final ICommonsList <ISMPServiceInformation> aSIs = aServiceInfoMgr.getAllSMPServiceInformationOfServiceGroup (aCurObject);
        for (final ISMPServiceInformation aSI : aSIs)
        {
          nProcesses += aSI.getProcessCount ();
          nEndpoints += aSI.getTotalEndpointCount ();
        }

        aRow.addCell (Integer.toString (aSIs.size ()));
        aRow.addCell (Integer.toString (nProcesses));
        aRow.addCell (Integer.toString (nEndpoints));
      }

      final HCNodeList aActions = new HCNodeList ();
      aActions.addChildren (createEditLink (aWPEC, aCurObject, "Edit " + sDisplayName),
                            new HCTextNode (" "),
                            createCopyLink (aWPEC, aCurObject, "Copy " + sDisplayName),
                            new HCTextNode (" "),
                            createDeleteLink (aWPEC, aCurObject, "Delete " + sDisplayName),
                            new HCTextNode (" "),
                            new HCA (LinkHelper.getURLWithServerAndContext (aCurObject.getParticipantIdentifier ()
                                                                                      .getURIPercentEncoded ())).setTitle ("Perform SMP query on " +
                                                                                                                           sDisplayName)
                                                                                                                .setTargetBlank ()
                                                                                                                .addChild (EFamFamIcon.SCRIPT_GO.getAsNode ()));
      if (eRESTType.isCompleteServiceGroupSupported ())
      {
        aActions.addChildren (new HCTextNode (" "),
                              new HCA (LinkHelper.getURLWithServerAndContext ("complete/" +
                                                                              aCurObject.getParticipantIdentifier ()
                                                                                        .getURIPercentEncoded ())).setTitle ("Perform complete SMP query on " +
                                                                                                                             sDisplayName)
                                                                                                                  .setTargetBlank ()
                                                                                                                  .addChild (EFamFamIcon.SCRIPT_LINK.getAsNode ()));
      }
      aRow.addCell (aActions);
    }

    final DataTables aDataTables = BootstrapDataTables.createDefaultDataTables (aWPEC, aTable);
    aNodeList.addChild (aTable).addChild (aDataTables);
  }
}
