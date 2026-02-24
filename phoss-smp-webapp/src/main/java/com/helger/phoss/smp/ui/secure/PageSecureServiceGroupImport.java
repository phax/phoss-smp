/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonempty;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.collection.commons.ICommonsSet;
import com.helger.diagnostics.error.level.EErrorLevel;
import com.helger.diagnostics.error.level.IErrorLevel;
import com.helger.html.hc.html.forms.HCCheckBox;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.app.PDClientProvider;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.businesscard.ISMPBusinessCardManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.exchange.CSMPExchange;
import com.helger.phoss.smp.exchange.ImportActionItem;
import com.helger.phoss.smp.exchange.ImportSummary;
import com.helger.phoss.smp.exchange.ServiceGroupImport;
import com.helger.phoss.smp.settings.ISMPSettings;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.phoss.smp.ui.secure.hc.HCUserSelect;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.badge.BootstrapBadge;
import com.helger.photon.bootstrap4.badge.EBootstrapBadgeType;
import com.helger.photon.bootstrap4.button.BootstrapSubmitButton;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.card.BootstrapCard;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.uictrls.ext.BootstrapFileUpload;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldBoolean;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.web.fileupload.IFileItem;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.serialize.MicroReader;
import com.helger.xml.serialize.read.SAXReaderSettings;

/**
 * Class to import service groups with all contents
 *
 * @author Philip Helger
 */
public final class PageSecureServiceGroupImport extends AbstractSMPWebPage
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureServiceGroupImport.class);
  private static final String FIELD_IMPORT_FILE = "importfile";
  private static final String FIELD_OVERWRITE_EXISTING = "overwriteexisting";
  private static final String FIELD_DEFAULT_OWNER = "defaultowner";
  private static final boolean DEFAULT_OVERWRITE_EXISTING = false;

  public PageSecureServiceGroupImport (@NonNull @Nonempty final String sID)
  {
    super (sID, "Import");
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPSettings aSettings = SMPMetaManager.getSettings ();
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
    final IUserManager aUserMgr = PhotonSecurityManager.getUserMgr ();
    final ICommonsSet <String> aAllServiceGroupIDs = aServiceGroupMgr.getAllSMPServiceGroupIDs ();
    final ICommonsSet <String> aAllBusinessCardIDs = aBusinessCardMgr.getAllSMPBusinessCardIDs ();
    final FormErrorList aFormErrors = new FormErrorList ();

    final HCUL aImportResultUL = new HCUL ();

    if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
    {
      // Start import
      final IFileItem aImportFile = aWPEC.params ().getAsFileItem (FIELD_IMPORT_FILE);
      final boolean bOverwriteExisting = aWPEC.params ()
                                              .isCheckBoxChecked (FIELD_OVERWRITE_EXISTING, DEFAULT_OVERWRITE_EXISTING);
      final String sDefaultOwnerID = aWPEC.params ().getAsStringTrimmed (FIELD_DEFAULT_OWNER);
      final IUser aDefaultOwner = aUserMgr.getActiveUserOfID (sDefaultOwnerID);

      if (aImportFile == null || aImportFile.getSize () == 0)
        aFormErrors.addFieldError (FIELD_IMPORT_FILE, "A file to import must be selected!");

      if (StringHelper.isEmpty (sDefaultOwnerID))
        aFormErrors.addFieldError (FIELD_DEFAULT_OWNER, "A default owner must be selected!");
      else
        if (aDefaultOwner == null)
          aFormErrors.addFieldError (FIELD_DEFAULT_OWNER, "A valid default owner must be selected!");

      if (aFormErrors.isEmpty ())
      {
        LOGGER.info ("Reading uploaded XML file");

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
            final ICommonsList <ImportActionItem> aActionList = new CommonsArrayList <> ();
            final ImportSummary aImportSummary = new ImportSummary ();
            ServiceGroupImport.importXMLVer10 (aDoc.getDocumentElement (),
                                               bOverwriteExisting,
                                               aDefaultOwner,
                                               aAllServiceGroupIDs,
                                               aAllBusinessCardIDs,
                                               PDClientProvider.getInstance ().getPDClient ()::addServiceGroupToIndex,
                                               aActionList,
                                               aImportSummary);
            for (final ImportActionItem aAction : aActionList)
            {
              final IErrorLevel aErrorLevel = aAction.getErrorLevel ();
              final EBootstrapBadgeType eBadgeType;
              if (aErrorLevel.isGE (EErrorLevel.ERROR))
                eBadgeType = EBootstrapBadgeType.DANGER;
              else
                if (aErrorLevel.isGE (EErrorLevel.WARN))
                  eBadgeType = EBootstrapBadgeType.WARNING;
                else
                  if (aErrorLevel.isGE (EErrorLevel.INFO))
                    eBadgeType = EBootstrapBadgeType.INFO;
                  else
                    eBadgeType = EBootstrapBadgeType.SUCCESS;

              // By default is is centered
              aImportResultUL.addItem (new BootstrapBadge (eBadgeType).addChild ((aAction.hasParticipantID () ? "[" +
                                                                                                                aAction.getParticipantID () +
                                                                                                                "] "
                                                                                                              : "") +
                                                                                 aAction.getMessage ())
                                                                      .addChild (SMPCommonUI.getTechnicalDetailsUI (aAction.getLinkedException ()))
                                                                      .addClass (CBootstrapCSS.TEXT_LEFT));
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

    final boolean bHandleBusinessCards = aSettings.isDirectoryIntegrationEnabled ();

    if (aImportResultUL.hasChildren ())
    {
      final BootstrapCard aPanel = new BootstrapCard ();
      aPanel.createAndAddHeader ().addChild ("Import results");
      aPanel.createAndAddBody ().addChild (aImportResultUL);
      aNodeList.addChild (aPanel);
    }

    aNodeList.addChild (info ("Import service groups incl. all endpoints" +
                              (bHandleBusinessCards ? " and business cards" : "") +
                              " from a file."));

    final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormFileUploadSelf (aWPEC));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("File to import")
                                                 .setCtrl (new BootstrapFileUpload (FIELD_IMPORT_FILE, aDisplayLocale))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_IMPORT_FILE)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Overwrite existing elements")
                                                 .setCtrl (new HCCheckBox (new RequestFieldBoolean (FIELD_OVERWRITE_EXISTING,
                                                                                                    DEFAULT_OVERWRITE_EXISTING)))
                                                 .setHelpText ("If this box is checked, all existing endpoints etc. of a service group are deleted and new endpoints are created! If the " +
                                                               SMPWebAppConfiguration.getDirectoryName () +
                                                               " integration is enabled, existing business cards contained in the import are also overwritten!")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_OVERWRITE_EXISTING)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Owner of the new service groups")
                                                 .setCtrl (new HCUserSelect (new RequestField (FIELD_DEFAULT_OWNER),
                                                                             aDisplayLocale))
                                                 .setHelpText ("This owner is only selected, if the owner contained in the import file is unknown.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_DEFAULT_OWNER)));

    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
    aToolbar.addChild (new BootstrapSubmitButton ().addChild ("Import Service Groups").setIcon (EDefaultIcon.ADD));
  }
}
