/**
 * Copyright (C) 2014-2019 Philip Helger and contributors
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

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.URLValidator;
import com.helger.html.hc.html.forms.HCCheckBox;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCEM;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.app.SMPWebAppConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.helger.peppol.smpserver.settings.ISMPSettings;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageSimpleForm;
import com.helger.peppol.smpserver.ui.secure.hc.HCSMLSelect;
import com.helger.photon.bootstrap4.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap4.card.BootstrapCard;
import com.helger.photon.bootstrap4.card.BootstrapCardBody;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.form.BootstrapViewForm;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldBoolean;
import com.helger.photon.uicore.page.EWebPageSimpleFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PageSecureSMPSettings extends AbstractSMPWebPageSimpleForm <ISMPSettings>
{
  private static final String FIELD_SMP_REST_WRITABLE_API_DISABLED = "smprwad";
  private static final String FIELD_SMP_DIRECTORY_INTEGRATION_ENABLED = "smppdie";
  private static final String FIELD_SML_DIRECTORY_INTEGRATION_REQUIRED = "smppdin";
  private static final String FIELD_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE = "smppdiau";
  private static final String FIELD_SMP_DIRECTORY_HOSTNAME = "smppdh";
  private static final String FIELD_SML_ACTIVE = "smla";
  private static final String FIELD_SML_REQUIRED = "smln";
  private static final String FIELD_SML_INFO = "smlinfo";

  public PageSecureSMPSettings (@Nonnull @Nonempty final String sID)
  {
    super (sID, "SMP Settings");
  }

  @Override
  protected ISMPSettings getObject (@Nonnull final WebPageExecutionContext aWPEC)
  {
    return SMPMetaManager.getSettings ();
  }

  @Override
  protected void showObject (@Nonnull final WebPageExecutionContext aWPEC, @Nonnull final ISMPSettings aObject)
  {
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final String sDirectoryName = SMPWebAppConfiguration.getDirectoryName ();

    {
      final BootstrapCard aCard = aNodeList.addAndReturnChild (new BootstrapCard ());
      aCard.createAndAddHeader ().addChild ("REST API");
      final BootstrapCardBody aCardBody = aCard.createAndAddBody ();

      final BootstrapViewForm aTable = aCardBody.addAndReturnChild (new BootstrapViewForm ());
      aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("REST writable API disabled?")
                                                    .setCtrl (EPhotonCoreText.getYesOrNo (aObject.isRESTWritableAPIDisabled (),
                                                                                          aDisplayLocale)));
    }

    {
      final BootstrapCard aCard = aNodeList.addAndReturnChild (new BootstrapCard ());
      aCard.createAndAddHeader ().addChild (sDirectoryName);
      final BootstrapCardBody aCardBody = aCard.createAndAddBody ();

      final BootstrapViewForm aTable = aCardBody.addAndReturnChild (new BootstrapViewForm ());
      aTable.addFormGroup (new BootstrapFormGroup ().setLabel (sDirectoryName + " integration enabled?")
                                                    .setCtrl (EPhotonCoreText.getYesOrNo (aObject.isPEPPOLDirectoryIntegrationEnabled (),
                                                                                          aDisplayLocale)));
      aTable.addFormGroup (new BootstrapFormGroup ().setLabel (sDirectoryName + " integration required?")
                                                    .setCtrl (EPhotonCoreText.getYesOrNo (aObject.isPEPPOLDirectoryIntegrationRequired (),
                                                                                          aDisplayLocale)));
      aTable.addFormGroup (new BootstrapFormGroup ().setLabel (sDirectoryName + " integration automatic update?")
                                                    .setCtrl (EPhotonCoreText.getYesOrNo (aObject.isPEPPOLDirectoryIntegrationAutoUpdate (),
                                                                                          aDisplayLocale)));
      aTable.addFormGroup (new BootstrapFormGroup ().setLabel (sDirectoryName + " hostname")
                                                    .setCtrl (HCA.createLinkedWebsite (aObject.getPEPPOLDirectoryHostName ())));
    }

    {
      final BootstrapCard aCard = aNodeList.addAndReturnChild (new BootstrapCard ());
      aCard.createAndAddHeader ().addChild ("SMK/SML");
      final BootstrapCardBody aCardBody = aCard.createAndAddBody ();

      final BootstrapViewForm aTable = aCardBody.addAndReturnChild (new BootstrapViewForm ());
      aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("SML connection enabled?")
                                                    .setCtrl (EPhotonCoreText.getYesOrNo (aObject.isSMLActive (),
                                                                                          aDisplayLocale)));
      aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("SML connection required?")
                                                    .setCtrl (EPhotonCoreText.getYesOrNo (aObject.isSMLRequired (),
                                                                                          aDisplayLocale)));

      final ISMLInfo aSMLInfo = aObject.getSMLInfo ();
      aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("SML to be used")
                                                    .setCtrl (aSMLInfo == null ? new HCEM ().addChild ("none")
                                                                               : HCSMLSelect.getDisplayNameNode (aSMLInfo)));
    }
  }

  @Override
  protected void validateAndSaveInputParameters (@Nonnull final WebPageExecutionContext aWPEC,
                                                 @Nonnull final ISMPSettings aObject,
                                                 @Nonnull final FormErrorList aFormErrors,
                                                 @Nonnull final EWebPageSimpleFormAction eSimpleFormAction)
  {
    final boolean bRESTWritableAPIDisabled = aWPEC.params ()
                                                  .isCheckBoxChecked (FIELD_SMP_REST_WRITABLE_API_DISABLED,
                                                                      SMPServerConfiguration.DEFAULT_SMP_REST_WRITABLE_API_DISABLED);
    final boolean bPEPPOLDirectoryIntegrationEnabled = aWPEC.params ()
                                                            .isCheckBoxChecked (FIELD_SMP_DIRECTORY_INTEGRATION_ENABLED,
                                                                                SMPServerConfiguration.DEFAULT_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED);
    final boolean bPEPPOLDirectoryIntegrationRequired = aWPEC.params ()
                                                             .isCheckBoxChecked (FIELD_SML_DIRECTORY_INTEGRATION_REQUIRED,
                                                                                 SMPServerConfiguration.DEFAULT_SMP_PEPPOL_DIRECTORY_INTEGRATION_REQUIRED);
    final boolean bPEPPOLDirectoryIntegrationAutoUpdate = aWPEC.params ()
                                                               .isCheckBoxChecked (FIELD_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                                                                   SMPServerConfiguration.DEFAULT_SMP_PEPPOL_DIRECTORY_INTEGRATION_AUTO_UPDATE);
    final String sPEPPOLDirectoryHostName = aWPEC.params ().getAsString (FIELD_SMP_DIRECTORY_HOSTNAME);
    final boolean bSMLActive = aWPEC.params ()
                                    .isCheckBoxChecked (FIELD_SML_ACTIVE, SMPServerConfiguration.DEFAULT_SML_ACTIVE);
    final boolean bSMLRequired = aWPEC.params ()
                                      .isCheckBoxChecked (FIELD_SML_REQUIRED,
                                                          SMPServerConfiguration.DEFAULT_SML_REQUIRED);
    final String sSMLInfoID = aWPEC.params ().getAsString (FIELD_SML_INFO);
    final ISMLInfo aSMLInfo = SMPMetaManager.getSMLInfoMgr ().getSMLInfoOfID (sSMLInfoID);
    final String sDirectoryName = SMPWebAppConfiguration.getDirectoryName ();

    if (StringHelper.hasNoText (sPEPPOLDirectoryHostName))
      aFormErrors.addFieldError (FIELD_SMP_DIRECTORY_HOSTNAME, sDirectoryName + " hostname may not be empty.");
    else
      if (!URLValidator.isValid (sPEPPOLDirectoryHostName))
        aFormErrors.addFieldError (FIELD_SMP_DIRECTORY_HOSTNAME, sDirectoryName + " hostname must be a valid URL.");

    if (bSMLActive && !SMPKeyManager.isCertificateValid ())
      aFormErrors.addFieldError (FIELD_SML_ACTIVE,
                                 "SML connection cannot be activated, because the configured keystore is invalid!");

    if (aSMLInfo == null)
    {
      if (bSMLActive)
        aFormErrors.addFieldError (FIELD_SML_INFO, "An SML configuration must be selected if SML is active.");
    }

    if (aFormErrors.isEmpty ())
    {
      SMPMetaManager.getSettingsMgr ()
                    .updateSettings (bRESTWritableAPIDisabled,
                                     bPEPPOLDirectoryIntegrationEnabled,
                                     bPEPPOLDirectoryIntegrationRequired,
                                     bPEPPOLDirectoryIntegrationAutoUpdate,
                                     sPEPPOLDirectoryHostName,
                                     bSMLActive,
                                     bSMLRequired,
                                     aSMLInfo);
      aWPEC.postRedirectGetInternal (new BootstrapSuccessBox ().addChild ("The SMP settings were successfully saved."));
    }
  }

  @Override
  protected void showInputForm (@Nonnull final WebPageExecutionContext aWPEC,
                                @Nonnull final ISMPSettings aObject,
                                @Nonnull final BootstrapForm aForm,
                                @Nonnull final EWebPageSimpleFormAction eSimpleFormAction,
                                @Nonnull final FormErrorList aFormErrors)
  {
    final String sDirectoryName = SMPWebAppConfiguration.getDirectoryName ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();

    aForm.addChild (getUIHandler ().createDataGroupHeader ("REST API"));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("REST writable API disabled?")
                                                 .setCtrl (new HCCheckBox (new RequestFieldBoolean (FIELD_SMP_REST_WRITABLE_API_DISABLED,
                                                                                                    aObject.isRESTWritableAPIDisabled ())))
                                                 .setHelpText ("If this checkbox is checked, all non-standard writing REST APIs are disabled.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SMP_REST_WRITABLE_API_DISABLED)));

    aForm.addChild (getUIHandler ().createDataGroupHeader (sDirectoryName));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel (sDirectoryName + " integration enabled?")
                                                 .setCtrl (new HCCheckBox (new RequestFieldBoolean (FIELD_SMP_DIRECTORY_INTEGRATION_ENABLED,
                                                                                                    aObject.isPEPPOLDirectoryIntegrationEnabled ())))
                                                 .setHelpText ("If this checkbox is checked, the " +
                                                               sDirectoryName +
                                                               " integration is enabled.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SMP_DIRECTORY_INTEGRATION_ENABLED)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel (sDirectoryName + " connection required?")
                                                 .setCtrl (new HCCheckBox (new RequestFieldBoolean (FIELD_SML_DIRECTORY_INTEGRATION_REQUIRED,
                                                                                                    aObject.isPEPPOLDirectoryIntegrationRequired ())))
                                                 .setHelpText ("If this checkbox is checked, warnings are emitted if the " +
                                                               sDirectoryName +
                                                               " connection is not enabled.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SML_DIRECTORY_INTEGRATION_REQUIRED)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel (sDirectoryName + " integration automatic update?")
                                                 .setCtrl (new HCCheckBox (new RequestFieldBoolean (FIELD_SMP_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                                                                                    aObject.isPEPPOLDirectoryIntegrationAutoUpdate ())))
                                                 .setHelpText ("If the " +
                                                               sDirectoryName +
                                                               " integration is enabled and this checkbox is checked, all business card creations, modifications and deletions are automatically pushed to the " +
                                                               sDirectoryName +
                                                               " server.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SMP_DIRECTORY_INTEGRATION_ENABLED)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel (sDirectoryName + " hostname")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_SMP_DIRECTORY_HOSTNAME,
                                                                                         aObject.getPEPPOLDirectoryHostName ())))
                                                 .setHelpText ("The " +
                                                               sDirectoryName +
                                                               " host where the business cards should be published to. This URL is only used if the " +
                                                               sDirectoryName +
                                                               " integration (see above) is enabled.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SMP_DIRECTORY_HOSTNAME)));

    aForm.addChild (getUIHandler ().createDataGroupHeader ("SMK/SML"));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("SML connection enabled?")
                                                 .setCtrl (new HCCheckBox (new RequestFieldBoolean (FIELD_SML_ACTIVE,
                                                                                                    aObject.isSMLActive ())))
                                                 .setHelpText ("If this checkbox is checked, service group changes are propagated to the SML.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SML_ACTIVE)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("SML connection required?")
                                                 .setCtrl (new HCCheckBox (new RequestFieldBoolean (FIELD_SML_REQUIRED,
                                                                                                    aObject.isSMLRequired ())))
                                                 .setHelpText ("If this checkbox is checked, warnings are emitted if the SML connection is not enabled.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SML_REQUIRED)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("SML configuration")
                                                 .setCtrl (new HCSMLSelect (new RequestField (FIELD_SML_INFO,
                                                                                              aObject.getSMLInfoID ()),
                                                                            aDisplayLocale,
                                                                            null))
                                                 .setHelpText (new HCTextNode ("Select the SML to operate on. The list of available configurations can be "),
                                                               new HCA (aWPEC.getLinkToMenuItem (CMenuSecure.MENU_SML_CONFIGURATION)).addChild ("customized"),
                                                               new HCTextNode ("."))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SML_INFO)));
  }
}
