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

import java.net.URL;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.string.StringHelper;
import com.helger.commons.url.URLHelper;
import com.helger.commons.url.URLValidator;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.textlevel.HCA;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.html.hc.impl.HCTextNode;
import com.helger.peppol.sml.CSMLDefault;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smpserver.SMPServerConfiguration;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.security.SMPKeyManager;
import com.helger.peppol.smpserver.settings.ISMPSettings;
import com.helger.peppol.smpserver.ui.AbstractSMPWebPageSimpleForm;
import com.helger.photon.bootstrap3.alert.BootstrapSuccessBox;
import com.helger.photon.bootstrap3.form.BootstrapCheckBox;
import com.helger.photon.bootstrap3.form.BootstrapForm;
import com.helger.photon.bootstrap3.form.BootstrapFormGroup;
import com.helger.photon.bootstrap3.form.BootstrapViewForm;
import com.helger.photon.core.EPhotonCoreText;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.core.form.RequestFieldBoolean;
import com.helger.photon.uicore.page.EWebPageSimpleFormAction;
import com.helger.photon.uicore.page.WebPageExecutionContext;

public final class PageSecureSMPSettings extends AbstractSMPWebPageSimpleForm <ISMPSettings>
{
  private static final String FIELD_SMP_REST_WRITABLE_API_DISABLED = "smprwad";
  private static final String FIELD_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED = "smppdie";
  private static final String FIELD_SMP_PEPPOL_DIRECTORY_INTEGRATION_AUTO_UPDATE = "smppdiau";
  private static final String FIELD_SMP_PEPPOL_DIRECTORY_HOSTNAME = "smppdh";
  private static final String FIELD_SML_ACTIVE = "smla";
  private static final String FIELD_SML_NEEDED = "smln";
  private static final String FIELD_SML_URL = "smlu";

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

    final BootstrapViewForm aTable = aNodeList.addAndReturnChild (new BootstrapViewForm ());
    aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("REST writable API disabled?")
                                                  .setCtrl (EPhotonCoreText.getYesOrNo (aObject.isRESTWritableAPIDisabled (),
                                                                                        aDisplayLocale)));
    aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("PEPPOL Directory integration enabled?")
                                                  .setCtrl (EPhotonCoreText.getYesOrNo (aObject.isPEPPOLDirectoryIntegrationEnabled (),
                                                                                        aDisplayLocale)));
    aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("PEPPOL Directory integration automatic update?")
                                                  .setCtrl (EPhotonCoreText.getYesOrNo (aObject.isPEPPOLDirectoryIntegrationAutoUpdate (),
                                                                                        aDisplayLocale)));
    aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("PEPPOL Directory hostname")
                                                  .setCtrl (HCA.createLinkedWebsite (aObject.getPEPPOLDirectoryHostName ())));
    aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("SML connection enabled?")
                                                  .setCtrl (EPhotonCoreText.getYesOrNo (aObject.isSMLActive (),
                                                                                        aDisplayLocale)));
    aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("SML connection needed?")
                                                  .setCtrl (EPhotonCoreText.getYesOrNo (aObject.isSMLNeeded (),
                                                                                        aDisplayLocale)));

    {
      final String sSMLURL = aObject.getSMLURL ();
      final ISMLInfo aAssignedSML = SMPMetaManager.getSMLInfoMgr ()
                                                  .getAllSMLInfos ()
                                                  .findFirst (x -> x.getManageParticipantIdentifierEndpointAddress ()
                                                                    .toExternalForm ()
                                                                    .equals (sSMLURL));
      aTable.addFormGroup (new BootstrapFormGroup ().setLabel ("SML management URL")
                                                    .setCtrl (HCA.createLinkedWebsite (sSMLURL))
                                                    .setHelpText (aAssignedSML == null ? null
                                                                                       : "This belongs to the defined SML '" +
                                                                                         aAssignedSML.getDisplayName () +
                                                                                         "'"));
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
                                                            .isCheckBoxChecked (FIELD_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED,
                                                                                SMPServerConfiguration.DEFAULT_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED);
    final boolean bPEPPOLDirectoryIntegrationAutoUpdate = aWPEC.params ()
                                                               .isCheckBoxChecked (FIELD_SMP_PEPPOL_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                                                                   SMPServerConfiguration.DEFAULT_SMP_PEPPOL_DIRECTORY_INTEGRATION_AUTO_UPDATE);
    final String sPEPPOLDirectoryHostName = aWPEC.params ().getAsString (FIELD_SMP_PEPPOL_DIRECTORY_HOSTNAME);
    final boolean bSMLActive = aWPEC.params ().isCheckBoxChecked (FIELD_SML_ACTIVE,
                                                                  SMPServerConfiguration.DEFAULT_SML_ACTIVE);
    final boolean bSMLNeeded = aWPEC.params ().isCheckBoxChecked (FIELD_SML_NEEDED,
                                                                  SMPServerConfiguration.DEFAULT_SML_NEEDED);
    final String sSMLURL = aWPEC.params ().getAsString (FIELD_SML_URL);

    if (StringHelper.hasNoText (sPEPPOLDirectoryHostName))
      aFormErrors.addFieldError (FIELD_SMP_PEPPOL_DIRECTORY_HOSTNAME, "PEPPOL Directory hostname may not be empty.");
    else
      if (!URLValidator.isValid (sPEPPOLDirectoryHostName))
        aFormErrors.addFieldError (FIELD_SMP_PEPPOL_DIRECTORY_HOSTNAME,
                                   "PEPPOL Directory hostname must be a valid URL.");

    if (bSMLActive && !SMPKeyManager.isCertificateValid ())
      aFormErrors.addFieldError (FIELD_SML_ACTIVE,
                                 "SML connection cannot be activated, because the configured keystore is invalid!");

    if (StringHelper.hasNoText (sSMLURL))
      aFormErrors.addFieldError (FIELD_SML_URL, "SML management URL may not be empty.");
    else
    {
      final URL aURL = URLHelper.getAsURL (sSMLURL);
      if (aURL == null)
        aFormErrors.addFieldError (FIELD_SML_URL, "SML management URL must be a valid URL.");
      else
      {
        if (SMPMetaManager.getSMLInfoMgr ()
                          .getAllSMLInfos ()
                          .containsNone (x -> x.getManageParticipantIdentifierEndpointAddress ()
                                               .toExternalForm ()
                                               .equals (sSMLURL)))
        {
          aFormErrors.addFieldError (FIELD_SML_URL,
                                     "The SML management URL does not belong to any of the configured SMLs.");
        }
      }
    }

    if (aFormErrors.isEmpty ())
    {
      SMPMetaManager.getSettingsMgr ().updateSettings (bRESTWritableAPIDisabled,
                                                       bPEPPOLDirectoryIntegrationEnabled,
                                                       bPEPPOLDirectoryIntegrationAutoUpdate,
                                                       sPEPPOLDirectoryHostName,
                                                       bSMLActive,
                                                       bSMLNeeded,
                                                       sSMLURL);
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
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("REST writable API disabled?")
                                                 .setCtrl (new BootstrapCheckBox (new RequestFieldBoolean (FIELD_SMP_REST_WRITABLE_API_DISABLED,
                                                                                                           aObject.isRESTWritableAPIDisabled ())))
                                                 .setHelpText ("If this checkbox is checked, all non-standard writing REST APIs are disabled.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SMP_REST_WRITABLE_API_DISABLED)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("PEPPOL Directory integration enabled?")
                                                 .setCtrl (new BootstrapCheckBox (new RequestFieldBoolean (FIELD_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED,
                                                                                                           aObject.isPEPPOLDirectoryIntegrationEnabled ())))
                                                 .setHelpText ("If this checkbox is checked, the PEPPOL Directory integration is enabled.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("PEPPOL Directory integration automatic update?")
                                                 .setCtrl (new BootstrapCheckBox (new RequestFieldBoolean (FIELD_SMP_PEPPOL_DIRECTORY_INTEGRATION_AUTO_UPDATE,
                                                                                                           aObject.isPEPPOLDirectoryIntegrationAutoUpdate ())))
                                                 .setHelpText ("If the PEPPOL Directory integration is enabled and this checkbox is checked, all business card creations, modifications and deletions are automatically pushed to the PEPPOL Directory server.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SMP_PEPPOL_DIRECTORY_INTEGRATION_ENABLED)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("PEPPOL Directory hostname")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_SMP_PEPPOL_DIRECTORY_HOSTNAME,
                                                                                         aObject.getPEPPOLDirectoryHostName ())))
                                                 .setHelpText ("The PEPPOL Directory host where the business cards should be published to. This URL is only used if the PEPPOL Directory integration (see above) is enabled.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SMP_PEPPOL_DIRECTORY_HOSTNAME)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("SML connection enabled?")
                                                 .setCtrl (new BootstrapCheckBox (new RequestFieldBoolean (FIELD_SML_ACTIVE,
                                                                                                           aObject.isSMLActive ())))
                                                 .setHelpText ("If this checkbox is checked, service group changes are propagated to the SML.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SML_ACTIVE)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("SML connection needed?")
                                                 .setCtrl (new BootstrapCheckBox (new RequestFieldBoolean (FIELD_SML_NEEDED,
                                                                                                           aObject.isSMLNeeded ())))
                                                 .setHelpText ("If this checkbox is checked, warnings are emitted if the SML connection is not enabled.")
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SML_NEEDED)));
    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("SML management URL")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_SML_URL,
                                                                                         aObject.getSMLURL ())))
                                                 .setHelpText (new HCTextNode ("The SML manage participant endpoint. This URL is only used if the SML connection (see above) is enabled. The URL must end with "),
                                                               new HCCode ().addChild ("/" +
                                                                                       CSMLDefault.MANAGEMENT_SERVICE_PARTICIPANTIDENTIFIER),
                                                               new HCTextNode ("."))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_SML_URL)));
  }
}
