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

import java.util.Locale;
import java.util.Map;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashSet;
import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.CommonsLinkedHashSet;
import com.helger.commons.collection.impl.ICommonsIterable;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.collection.impl.ICommonsOrderedSet;
import com.helger.commons.collection.impl.ICommonsSet;
import com.helger.commons.error.level.EErrorLevel;
import com.helger.commons.error.level.IErrorLevel;
import com.helger.commons.log.InMemoryLogger;
import com.helger.commons.log.LogMessage;
import com.helger.commons.string.StringHelper;
import com.helger.html.hc.html.forms.HCEditFile;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.app.AppConfiguration;
import com.helger.peppol.smpserver.app.CSMPExchange;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.businesscard.SMPBusinessCardMicroTypeConverter;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirectMicroTypeConverter;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupProvider;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroupMicroTypeConverter;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformationMicroTypeConverter;
import com.helger.peppol.smpserver.domain.user.ISMPUser;
import com.helger.peppol.smpserver.domain.user.ISMPUserManager;
import com.helger.peppol.smpserver.settings.ISMPSettings;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPage;
import com.helger.peppol.smpserver.ui.ajax.CAjax;
import com.helger.peppol.smpserver.ui.secure.hc.HCSMPUserSelect;
import com.helger.photon.bootstrap3.alert.BootstrapInfoBox;
import com.helger.photon.bootstrap3.alert.BootstrapWarnBox;
import com.helger.photon.bootstrap3.button.BootstrapButton;
import com.helger.photon.bootstrap3.button.BootstrapButtonToolbar;
import com.helger.photon.bootstrap3.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap3.form.BootstrapCheckBox;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.label.BootstrapLabel;
import com.helger.photon.bootstrap3.label.EBootstrapLabelType;
import com.helger.photon.bootstrap3.nav.BootstrapTabBox;
import com.helger.photon.bootstrap3.panel.BootstrapPanel;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldBoolean;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.fileupload.IFileItem;
import com.helger.web.scope.IRequestWebScopeWithoutResponse;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.IMicroElement;
import com.helger.xml.microdom.serialize.MicroReader;
import com.helger.xml.serialize.read.SAXReaderSettings;

/**
 * Class to import and export service groups with all contents
 *
 * @author Philip Helger
 */
public final class PageSecureServiceGroupExchange extends AbstractSMPWebPage
{
  private static final String FIELD_IMPORT_FILE = "importfile";
  private static final String FIELD_OVERWRITE_EXISTING = "overwriteexisting";
  private static final String FIELD_DEFAULT_OWNER = "defaultowner";
  private static final boolean DEFAULT_OVERWRITE_EXISTING = false;

  private static final class SGImportData
  {
    private final ICommonsList <ISMPServiceInformation> m_aServiceInfos = new CommonsArrayList <> ();
    private final ICommonsList <ISMPRedirect> m_aRedirects = new CommonsArrayList <> ();

    public void addServiceInfo (@Nonnull final ISMPServiceInformation aServiceInfo)
    {
      m_aServiceInfos.add (aServiceInfo);
    }

    @Nonnull
    public ICommonsIterable <ISMPServiceInformation> getServiceInfo ()
    {
      return m_aServiceInfos;
    }

    public void addRedirect (@Nonnull final ISMPRedirect aRedirect)
    {
      m_aRedirects.add (aRedirect);
    }

    @Nonnull
    public ICommonsIterable <ISMPRedirect> getRedirects ()
    {
      return m_aRedirects;
    }
  }

  public PageSecureServiceGroupExchange (@Nonnull @Nonempty final String sID)
  {
    super (sID, "Import/Export");
  }

  public static void importXMLVer10 (@Nonnull final IMicroElement eRoot,
                                     final boolean bOverwriteExisting,
                                     @Nonnull final ISMPUser aDefaultOwner,
                                     @Nonnull final ICommonsList <ISMPServiceGroup> aAllServiceGroups,
                                     @Nonnull final ICommonsList <ISMPBusinessCard> aAllBusinessCards,
                                     @Nonnull final InMemoryLogger aLogger)
  {
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();

    final ICommonsOrderedMap <ISMPServiceGroup, SGImportData> aImportServiceGroups = new CommonsLinkedHashMap <> ();
    final ICommonsList <ISMPServiceGroup> aDeleteServiceGroups = new CommonsArrayList <> ();

    // First read all service groups as they are dependents of the
    // business cards
    int nSGIndex = 0;
    for (final IMicroElement eServiceGroup : eRoot.getAllChildElements (CSMPExchange.ELEMENT_SERVICEGROUP))
    {
      // Read service group and service information
      final ISMPServiceGroup aServiceGroup = SMPServiceGroupMicroTypeConverter.convertToNative (eServiceGroup, x -> {
        ISMPUser ret = SMPMetaManager.getUserMgr ().getUserOfID (x);
        if (ret == null)
        {
          // Select the default owner if an unknown user is contained
          ret = aDefaultOwner;
        }
        return ret;
      });
      if (aServiceGroup == null)
      {
        aLogger.error ("Failed to read service group at index " + nSGIndex);
      }
      else
      {
        final String sServiceGroupID = aServiceGroup.getID ();
        final boolean bIsServiceGroupContained = aAllServiceGroups.containsAny (x -> x.getID ()
                                                                                      .equals (sServiceGroupID));
        if (!bIsServiceGroupContained || bOverwriteExisting)
        {
          if (aImportServiceGroups.containsKey (aServiceGroup))
          {
            aLogger.error ("The service group " +
                           sServiceGroupID +
                           " is already contained in the file. Will overwrite the previous definition.");
          }

          // Remember to create/overwrite the service group
          final SGImportData aSGInfo = new SGImportData ();
          aImportServiceGroups.put (aServiceGroup, aSGInfo);
          if (bIsServiceGroupContained)
            aDeleteServiceGroups.add (aServiceGroup);
          aLogger.success ("Will " +
                           (bIsServiceGroupContained ? "overwrite" : "import") +
                           " service group " +
                           sServiceGroupID);

          // read all contained service information
          {
            int nSIIndex = 0;
            int nSIFound = 0;
            for (final IMicroElement eServiceInfo : eServiceGroup.getAllChildElements (CSMPExchange.ELEMENT_SERVICEINFO))
            {
              final ISMPServiceInformation aServiceInfo = SMPServiceInformationMicroTypeConverter.convertToNative (eServiceInfo,
                                                                                                                   x -> aServiceGroup);
              if (aServiceInfo == null)
              {
                aLogger.error ("Failed to read service group " +
                               sServiceGroupID +
                               " service information at index " +
                               nSIIndex);
              }
              else
              {
                aSGInfo.addServiceInfo (aServiceInfo);
                ++nSIFound;
              }
              ++nSIIndex;
            }
            aLogger.info ("Read " + nSIFound + " service information of service group " + sServiceGroupID);
          }

          // read all contained redirects
          {
            int nRDIndex = 0;
            int nRDFound = 0;
            for (final IMicroElement eRedirect : eServiceGroup.getAllChildElements (CSMPExchange.ELEMENT_REDIRECT))
            {
              final ISMPRedirect aRedirect = SMPRedirectMicroTypeConverter.convertToNative (eRedirect,
                                                                                            x -> aServiceGroup);
              if (aRedirect == null)
              {
                aLogger.error ("Failed to read service group " + sServiceGroupID + " redirect at index " + nRDIndex);
              }
              else
              {
                aSGInfo.addRedirect (aRedirect);
                ++nRDFound;
              }
              ++nRDIndex;
            }
            aLogger.info ("Read " + nRDFound + " redirects of service group " + sServiceGroupID);
          }
        }
        else
        {
          aLogger.warn ("Ignoring already contained service group " + sServiceGroupID);
        }
      }
      ++nSGIndex;
    }

    // Now read the business cards
    final ICommonsOrderedSet <ISMPBusinessCard> aImportBusinessCards = new CommonsLinkedHashSet <> ();
    final ICommonsList <ISMPBusinessCard> aDeleteBusinessCards = new CommonsArrayList <> ();
    if (aSettings.isPEPPOLDirectoryIntegrationEnabled ())
    {
      // Read them only if the PEPPOL Directory integration is enabled
      int nBCIndex = 0;
      for (final IMicroElement eBusinessCard : eRoot.getAllChildElements (CSMPExchange.ELEMENT_BUSINESSCARD))
      {
        // Read business card
        ISMPBusinessCard aBusinessCard = null;
        try
        {
          final ISMPServiceGroupProvider aSGProvider = x -> {
            // First look in service groups to import
            ISMPServiceGroup aSG = aImportServiceGroups.findFirstKey (y -> x.hasSameContent (y.getKey ()
                                                                                              .getParticpantIdentifier ()));
            if (aSG == null)
            {
              // Lookup in all existing service group
              aSG = aAllServiceGroups.findFirst (y -> x.hasSameContent (y.getParticpantIdentifier ()));
            }
            return aSG;
          };
          aBusinessCard = SMPBusinessCardMicroTypeConverter.convertToNative (eBusinessCard, aSGProvider);
        }
        catch (final IllegalStateException ex)
        {
          // Service group not found
          aLogger.error ("Business card at index " + nBCIndex + " contains an invalid/unknown service group!");
        }
        if (aBusinessCard == null)
        {
          aLogger.error ("Failed to read business card at index " + nBCIndex);
        }
        else
        {
          final String sBusinessCardID = aBusinessCard.getID ();
          final boolean bIsBusinessCardContained = aAllBusinessCards.containsAny (x -> x.getID ()
                                                                                        .equals (sBusinessCardID));
          if (!bIsBusinessCardContained || bOverwriteExisting)
          {
            final ISMPServiceGroup aBCSG = aBusinessCard.getServiceGroup ();
            if (aImportBusinessCards.removeIf (x -> x.getServiceGroup ().equals (aBCSG)))
            {
              aLogger.error ("The business card for " +
                             sBusinessCardID +
                             " is already contained in the file. Will overwrite the previous definition.");
            }
            aImportBusinessCards.add (aBusinessCard);
            if (bIsBusinessCardContained)
              aDeleteBusinessCards.add (aBusinessCard);
            aLogger.success ("Will " +
                             (bIsBusinessCardContained ? "overwrite" : "import") +
                             " business card for " +
                             sBusinessCardID);
          }
          else
          {
            aLogger.warn ("Ignoring already contained business card " + sBusinessCardID);
          }
        }
        ++nBCIndex;
      }
    }

    if (aImportServiceGroups.isEmpty () && aImportBusinessCards.isEmpty ())
    {
      if (aSettings.isPEPPOLDirectoryIntegrationEnabled ())
        aLogger.warn ("Found neither a service group nor a business card to import.");
      else
        aLogger.warn ("Found no service group to import.");
    }
    else
      if (aLogger.containsAtLeastOneError ())
      {
        aLogger.error ("Nothing will be imported because of the previous errors!");
      }
      else
      {
        // Start importing
        aLogger.info ("Import is performed!");

        final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
        final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
        final ISMPRedirectManager aRedirectMgr = SMPMetaManager.getRedirectMgr ();
        final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();

        // 1. delete all existing service groups to be imported (if overwrite);
        // this may implicitly delete business cards
        final ICommonsSet <IParticipantIdentifier> aDeletedServiceGroups = new CommonsHashSet <> ();
        for (final ISMPServiceGroup aDeleteServiceGroup : aDeleteServiceGroups)
        {
          final IParticipantIdentifier aPI = aDeleteServiceGroup.getParticpantIdentifier ();
          if (aServiceGroupMgr.deleteSMPServiceGroup (aPI).isChanged ())
          {
            aLogger.success ("Successfully deleted service group " + aDeleteServiceGroup.getID ());
            aDeletedServiceGroups.add (aPI);
          }
          else
            aLogger.error ("Failed to delete service group " + aDeleteServiceGroup.getID ());
        }

        // 2. create all service groups
        for (final Map.Entry <ISMPServiceGroup, SGImportData> aEntry : aImportServiceGroups.entrySet ())
        {
          final ISMPServiceGroup aImportServiceGroup = aEntry.getKey ();
          ISMPServiceGroup aNewServiceGroup = null;
          try
          {
            aNewServiceGroup = aServiceGroupMgr.createSMPServiceGroup (aImportServiceGroup.getOwnerID (),
                                                                       aImportServiceGroup.getParticpantIdentifier (),
                                                                       aImportServiceGroup.getExtensionAsString ());
            if (aNewServiceGroup != null)
              aLogger.success ("Successfully created service group " + aImportServiceGroup.getID ());
          }
          catch (final Exception ex)
          {
            // E.g. if SML connection failed
            aLogger.error ("Error creating the new service group " + aImportServiceGroup.getID (), ex);
          }
          if (aNewServiceGroup != null)
          {
            // 3a. create all endpoints
            for (final ISMPServiceInformation aImportServiceInfo : aEntry.getValue ().getServiceInfo ())
              try
              {
                if (aServiceInfoMgr.mergeSMPServiceInformation (aImportServiceInfo).isSuccess ())
                  aLogger.success ("Successfully created service information for " + aImportServiceGroup.getID ());
              }
              catch (final Exception ex)
              {
                aLogger.error ("Error creating the new service information for " + aImportServiceGroup.getID (), ex);
              }

            // 3b. create all redirects
            for (final ISMPRedirect aImportRedirect : aEntry.getValue ().getRedirects ())
              try
              {
                if (aRedirectMgr.createOrUpdateSMPRedirect (aNewServiceGroup,
                                                            aImportRedirect.getDocumentTypeIdentifier (),
                                                            aImportRedirect.getTargetHref (),
                                                            aImportRedirect.getSubjectUniqueIdentifier (),
                                                            aImportRedirect.getExtensionAsString ()) != null)
                  aLogger.success ("Successfully created redirect for " + aImportServiceGroup.getID ());
              }
              catch (final Exception ex)
              {
                aLogger.error ("Error creating the new redirect for " + aImportServiceGroup.getID (), ex);
              }
          }
        }

        // 4. delete all existing business cards to be imported (if overwrite)
        // Note: if PD integration is disabled, the list is empty
        for (final ISMPBusinessCard aDeleteBusinessCard : aDeleteBusinessCards)
          try
          {
            if (aBusinessCardMgr.deleteSMPBusinessCard (aDeleteBusinessCard).isChanged ())
              aLogger.success ("Successfully deleted business card " + aDeleteBusinessCard.getID ());
            else
            {
              // If the service group to which the business card belongs was
              // already deleted, don't display an error, as the business card
              // was automatically deleted afterwards
              if (!aDeletedServiceGroups.contains (aDeleteBusinessCard.getServiceGroup ().getParticpantIdentifier ()))
                aLogger.error ("Failed to delete business card " + aDeleteBusinessCard.getID ());
            }
          }
          catch (final Exception ex)
          {
            aLogger.error ("Failed to delete business card " +
                           aDeleteBusinessCard.getID () +
                           "; Technical details: " +
                           ex.getMessage (),
                           ex);
          }

        // 5. create all new business cards
        // Note: if PD integration is disabled, the list is empty
        for (final ISMPBusinessCard aImportBusinessCard : aImportBusinessCards)
          try
          {
            if (aBusinessCardMgr.createOrUpdateSMPBusinessCard (aImportBusinessCard.getServiceGroup (),
                                                                aImportBusinessCard.getAllEntities ()) != null)
              aLogger.success ("Successfully created business card " + aImportBusinessCard.getID ());
          }
          catch (final Exception ex)
          {
            aLogger.error ("Failed to create business card " + aImportBusinessCard.getID (), ex);
          }
      }
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final IRequestWebScopeWithoutResponse aRequestScope = aWPEC.getRequestScope ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    final ISMPUserManager aUserMgr = SMPMetaManager.getUserMgr ();
    final ICommonsList <ISMPServiceGroup> aAllServiceGroups = aServiceGroupMgr.getAllSMPServiceGroups ();
    final int nServiceGroupCount = aAllServiceGroups.size ();
    final ICommonsList <ISMPBusinessCard> aAllBusinessCards = aBusinessCardMgr.getAllSMPBusinessCards ();
    final FormErrorList aFormErrors = new FormErrorList ();

    boolean bSelectImportTab = false;
    final HCUL aImportResultUL = new HCUL ();

    if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
    {
      bSelectImportTab = true;

      // Start import
      final IFileItem aImportFile = aWPEC.params ().getAsFileItem (FIELD_IMPORT_FILE);
      final boolean bOverwriteExisting = aWPEC.params ()
                                              .isCheckBoxChecked (FIELD_OVERWRITE_EXISTING, DEFAULT_OVERWRITE_EXISTING);
      final String sDefaultOwnerID = aWPEC.params ().getAsString (FIELD_DEFAULT_OWNER);
      final ISMPUser aDefaultOwner = aUserMgr.getUserOfID (sDefaultOwnerID);

      if (aImportFile == null || aImportFile.getSize () == 0)
        aFormErrors.addFieldError (FIELD_IMPORT_FILE, "A file to import must be selected!");

      if (StringHelper.hasNoText (sDefaultOwnerID))
        aFormErrors.addFieldError (FIELD_DEFAULT_OWNER, "A default owner must be selected!");
      else
        if (aDefaultOwner == null)
          aFormErrors.addFieldError (FIELD_DEFAULT_OWNER, "A valid default owner must be selected!");

      if (aFormErrors.isEmpty ())
      {
        final SAXReaderSettings aSRS = new SAXReaderSettings ();
        final IMicroDocument aDoc = MicroReader.readMicroXML (aImportFile, aSRS);
        if (aDoc == null || aDoc.getDocumentElement () == null)
          aFormErrors.addFieldError (FIELD_IMPORT_FILE, "The provided file is not a valid XML file!");
        else
        {
          // Start interpreting
          final String sVersion = aDoc.getDocumentElement ().getAttributeValue (CSMPExchange.ATTR_VERSION);
          if (CSMPExchange.VERSION_10.equals (sVersion))
          {
            // Version 1.0
            final InMemoryLogger aLogger = new InMemoryLogger ();
            importXMLVer10 (aDoc.getDocumentElement (),
                            bOverwriteExisting,
                            aDefaultOwner,
                            aAllServiceGroups,
                            aAllBusinessCards,
                            aLogger);
            for (final LogMessage aLogMsg : aLogger)
            {
              final IErrorLevel aErrorLevel = aLogMsg.getErrorLevel ();
              EBootstrapLabelType eLabelType;
              if (aErrorLevel.isGE (EErrorLevel.ERROR))
                eLabelType = EBootstrapLabelType.DANGER;
              else
                if (aErrorLevel.isGE (EErrorLevel.WARN))
                  eLabelType = EBootstrapLabelType.WARNING;
                else
                  if (aErrorLevel.isGE (EErrorLevel.INFO))
                    eLabelType = EBootstrapLabelType.INFO;
                  else
                    eLabelType = EBootstrapLabelType.SUCCESS;

              String sMsg = aLogMsg.getMessage ().toString ();
              if (aLogMsg.getThrowable () != null)
                sMsg += " Technical details: " +
                        aLogMsg.getThrowable ().getClass ().getName () +
                        " - " +
                        aLogMsg.getThrowable ().getMessage ();
              aImportResultUL.addItem (new BootstrapLabel (eLabelType).addChild (sMsg));
            }
          }
          else
          {
            // Unsupported or no version present
            if (sVersion == null)
              aFormErrors.addFieldError (FIELD_IMPORT_FILE,
                                         "The provided file cannot be imported because it has the wrong layout.");
            else
              aFormErrors.addFieldError (FIELD_IMPORT_FILE,
                                         "The provided file contains the unsupported version '" + sVersion + "'.");
          }
        }
      }
    }

    final BootstrapTabBox aTabBox = aNodeList.addAndReturnChild (new BootstrapTabBox ());
    final boolean bHandleBusinessCards = aSettings.isPEPPOLDirectoryIntegrationEnabled ();

    // Export tab
    {
      final HCNodeList aExport = new HCNodeList ();
      if (nServiceGroupCount == 0)
        aExport.addChild (new BootstrapWarnBox ().addChild ("Since no service group is present, nothing can be exported!"));
      else
      {
        aExport.addChild (new BootstrapInfoBox ().addChild ("Export " +
                                                            (nServiceGroupCount == 1 ? "service group"
                                                                                     : "all " +
                                                                                       aAllServiceGroups.size () +
                                                                                       " service groups") +
                                                            (bHandleBusinessCards ? " and business card" +
                                                                                    (nServiceGroupCount == 1 ? "" : "s")
                                                                                  : "") +
                                                            " to an XML file."));
      }

      final BootstrapButtonToolbar aToolbar = aExport.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
      aToolbar.addChild (new BootstrapButton ().addChild ("Export all Service Groups")
                                               .setIcon (EDefaultIcon.SAVE_ALL)
                                               .setOnClick (CAjax.FUNCTION_EXPORT_ALL_SERVICE_GROUPS.getInvocationURL (aRequestScope))
                                               .setDisabled (aAllServiceGroups.isEmpty ()));
      aTabBox.addTab ("export", "Export", aExport, !bSelectImportTab);
    }

    // Import tab
    {
      final HCNodeList aImport = new HCNodeList ();

      if (aImportResultUL.hasChildren ())
      {
        final BootstrapPanel aPanel = new BootstrapPanel ();
        aPanel.getOrCreateHeader ().addChild ("Import results");
        aPanel.getBody ().addChild (aImportResultUL);
        aImport.addChild (aPanel);
      }

      aImport.addChild (new BootstrapInfoBox ().addChild ("Import service groups incl. all endpoints" +
                                                          (bHandleBusinessCards ? " and business cards" : "") +
                                                          " from a file."));

      final BootstrapForm aForm = aImport.addAndReturnChild (getUIHandler ().createFormFileUploadSelf (aWPEC));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("File to import")
                                                   .setCtrl (new HCEditFile (FIELD_IMPORT_FILE))
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_IMPORT_FILE)));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Overwrite existing elements")
                                                   .setCtrl (new BootstrapCheckBox (new RequestFieldBoolean (FIELD_OVERWRITE_EXISTING,
                                                                                                             DEFAULT_OVERWRITE_EXISTING)))
                                                   .setHelpText ("If this box is checked, all existing endpoints etc. of a service group are deleted and new endpoints are created! If the " +
                                                                 AppConfiguration.getDirectoryName () +
                                                                 " integration is enabled, existing business cards contained in the import are also overwritten!")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_OVERWRITE_EXISTING)));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Owner of the new service groups")
                                                   .setCtrl (new HCSMPUserSelect (new RequestField (FIELD_DEFAULT_OWNER),
                                                                                  aDisplayLocale))
                                                   .setHelpText ("This owner is only selected, if the owner contained in the import file is unknown.")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_DEFAULT_OWNER)));

      final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
      aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
      aToolbar.addChild (new BootstrapSubmitButton ().addChild ("Import Service Groups").setIcon (EDefaultIcon.ADD));
      aTabBox.addTab ("import", "Import", aImport, bSelectImportTab);
    }
  }
}
