/**
 * Copyright (C) 2014-2020 Philip Helger and contributors
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

import java.security.cert.CertificateException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

import javax.annotation.Nonnull;
import javax.net.ssl.SSLSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.PDTFromString;
import com.helger.commons.datetime.PDTToString;
import com.helger.commons.state.EValidity;
import com.helger.commons.state.IValidityIndicator;
import com.helger.commons.string.StringHelper;
import com.helger.html.hc.html.forms.HCTextArea;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.peppol.sml.ISMLInfo;
import com.helger.peppol.smlclient.BDMSLClient;
import com.helger.peppol.smlclient.ManageServiceMetadataServiceCaller;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.security.SMPKeyManager;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.phoss.smp.ui.SMPCommonUI;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap4.form.BootstrapForm;
import com.helger.photon.bootstrap4.form.BootstrapFormGroup;
import com.helger.photon.bootstrap4.pages.BootstrapWebPageUIHandler;
import com.helger.photon.bootstrap4.uictrls.datetimepicker.BootstrapDateTimePicker;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.security.certificate.CertificateHelper;

public class PageSecureSMLCertificateUpdate extends AbstractSMPWebPage
{
  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureSMLCertificateUpdate.class);

  private static final String FIELD_PM_MIGRATION_DATE = "pmmigdate";
  private static final String FIELD_PM_PUBLIC_KEY = "pmpubkey";

  private static final String SUBACTION_SMP_UPDATE_CERT = "smpupdatecert";

  public PageSecureSMLCertificateUpdate (@Nonnull @Nonempty final String sID)
  {
    super (sID, "SML certificate update");
  }

  @Override
  protected IValidityIndicator isValidToDisplayPage (@Nonnull final WebPageExecutionContext aWPEC)
  {
    if (SMPMetaManager.getSettings ().getSMLInfo () == null)
    {
      aWPEC.getNodeList ().addChild (warn ("This page cannot be shown because the SML configuration is invalid."));
      return EValidity.INVALID;
    }
    if (!SMPKeyManager.isKeyStoreValid ())
    {
      aWPEC.getNodeList ()
           .addChild (warn ("This page cannot be shown because the overall keystore configuration is invalid."));
      return EValidity.INVALID;
    }
    return super.isValidToDisplayPage (aWPEC);
  }

  @Nonnull
  private static ManageServiceMetadataServiceCaller _create (@Nonnull final ISMLInfo aSML,
                                                             @Nonnull final SSLSocketFactory aSocketFactory)
  {
    final ManageServiceMetadataServiceCaller ret = new ManageServiceMetadataServiceCaller (aSML);
    ret.setSSLSocketFactory (aSocketFactory);
    return ret;
  }

  private void _updateSMPCertAtSML (@Nonnull final WebPageExecutionContext aWPEC,
                                    @Nonnull final FormErrorList aFormErrors)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final LocalDate aNow = PDTFactory.getCurrentLocalDate ();
    final String sMigrationDate = aWPEC.params ().getAsString (FIELD_PM_MIGRATION_DATE);
    final LocalDate aMigrationDate = PDTFromString.getLocalDateFromString (sMigrationDate, aDisplayLocale);
    final String sMigrationPublicKey = aWPEC.params ().getAsString (FIELD_PM_PUBLIC_KEY);
    X509Certificate aMigrationPublicKey = null;
    final ISMLInfo aSMLInfo = SMPMetaManager.getSettings ().getSMLInfo ();

    if (StringHelper.hasText (sMigrationDate))
    {
      if (aMigrationDate == null)
        aFormErrors.addFieldError (FIELD_PM_MIGRATION_DATE,
                                   "The provided certificate migration date '" + sMigrationDate + "' is invalid!");
      else
        if (aMigrationDate.compareTo (aNow) <= 0)
          aFormErrors.addFieldError (FIELD_PM_MIGRATION_DATE, "The certificate migration date must be in the future!");
    }

    if (StringHelper.hasNoText (sMigrationPublicKey))
    {
      aFormErrors.addFieldError (FIELD_PM_PUBLIC_KEY, "A new public key must be provided.");
    }
    else
    {
      try
      {
        aMigrationPublicKey = CertificateHelper.convertStringToCertficate (sMigrationPublicKey);
      }
      catch (final CertificateException ex)
      {
        // Fall through
      }

      if (aMigrationPublicKey == null)
        aFormErrors.addFieldError (FIELD_PM_PUBLIC_KEY,
                                   "The provided public key cannot be parsed as a X.509 certificate.");
      else
      {
        try
        {
          aMigrationPublicKey.checkValidity ();
        }
        catch (final CertificateExpiredException ex)
        {
          aFormErrors.addFieldError (FIELD_PM_PUBLIC_KEY, "The provided public key is already expired!");
          aMigrationPublicKey = null;
        }
        catch (final CertificateNotYetValidException ex)
        {
          // That's okay
        }
      }
    }

    if (aMigrationPublicKey != null)
    {
      // Using the date only is okay here
      final LocalDate aNotBefore = PDTFactory.createLocalDate (aMigrationPublicKey.getNotBefore ());
      final LocalDate aNotAfter = PDTFactory.createLocalDate (aMigrationPublicKey.getNotAfter ());

      if (aMigrationDate != null)
      {
        if (aMigrationDate.isBefore (aNotBefore))
          aFormErrors.addFieldError (FIELD_PM_MIGRATION_DATE,
                                     "The provided certificate migration date " +
                                                              PDTToString.getAsString (aMigrationDate, aDisplayLocale) +
                                                              " must not be before the certificate NotBefore date " +
                                                              PDTToString.getAsString (aNotBefore, aDisplayLocale) +
                                                              "!");

        if (aMigrationDate.isAfter (aNotAfter))
          aFormErrors.addFieldError (FIELD_PM_MIGRATION_DATE,
                                     "The provided certificate migration date " +
                                                              PDTToString.getAsString (aMigrationDate, aDisplayLocale) +
                                                              " must not be after the certificate NotAfter date " +
                                                              PDTToString.getAsString (aNotAfter, aDisplayLocale) +
                                                              "!");
      }
      else
      {
        if (aNotBefore.compareTo (aNow) <= 0)
          aFormErrors.addFieldError (FIELD_PM_PUBLIC_KEY,
                                     "The effective certificate migration date (" +
                                                          PDTToString.getAsString (aNotBefore, aDisplayLocale) +
                                                          " - taken from the new public key) must be in the future!");
      }
    }

    if (aFormErrors.isEmpty ())
    {
      try
      {
        final BDMSLClient aCaller = new BDMSLClient (aSMLInfo);
        aCaller.setSSLSocketFactory (SMPKeyManager.getInstance ().createSSLContext ().getSocketFactory ());
        aCaller.prepareChangeCertificate (sMigrationPublicKey, aMigrationDate);

        final LocalDateTime aNotBefore = PDTFactory.createLocalDateTime (aMigrationPublicKey.getNotBefore ());
        final LocalDateTime aNotAfter = PDTFactory.createLocalDateTime (aMigrationPublicKey.getNotAfter ());

        final LocalDate aEffectiveMigrationDate = aMigrationDate != null ? aMigrationDate : aNotBefore.toLocalDate ();
        final String sMsg = "Successfully prepared migration of SMP certificate at SML '" +
                            aSMLInfo.getManagementServiceURL () +
                            "'" +
                            " to be exchanged at " +
                            PDTToString.getAsString (aEffectiveMigrationDate, aDisplayLocale) +
                            ".";
        LOGGER.info (sMsg);

        aNodeList.addChild (success ().addChild (div (sMsg))
                                      .addChild (div ("Issuer: " +
                                                      aMigrationPublicKey.getIssuerX500Principal ().toString ()))
                                      .addChild (div ("Subject: " +
                                                      aMigrationPublicKey.getSubjectX500Principal ().toString ()))
                                      .addChild (div ("Not before: " +
                                                      PDTToString.getAsString (aNotBefore, aDisplayLocale)))
                                      .addChild (div ("Not after: " +
                                                      PDTToString.getAsString (aNotAfter, aDisplayLocale))));

        AuditHelper.onAuditExecuteSuccess ("smp-sml-update-cert",
                                           aSMLInfo.getManagementServiceURL (),
                                           sMigrationPublicKey,
                                           aMigrationDate);
      }
      catch (final Exception ex)
      {
        final String sMsg = "Error preparing migration of SMP certificate at SML '" +
                            aSMLInfo.getManagementServiceURL () +
                            "'.";
        LOGGER.error (sMsg, ex);
        aNodeList.addChild (error (sMsg).addChild (SMPCommonUI.getTechnicalDetailsUI (ex)));
        AuditHelper.onAuditExecuteFailure ("smp-sml-update-cert",
                                           aSMLInfo.getManagementServiceURL (),
                                           sMigrationPublicKey,
                                           aMigrationDate,
                                           ex.getClass (),
                                           ex.getMessage ());
      }
    }
    else
      aNodeList.addChild (BootstrapWebPageUIHandler.INSTANCE.createIncorrectInputBox (aWPEC));
  }

  @Override
  protected void fillContent (@Nonnull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final FormErrorList aFormErrors = new FormErrorList ();

    aNodeList.addChild (info ().addChildren (div ("Prepare the update of your SMP certificate in the future."),
                                             div ("Note: this is a custom SML extension that only works with the CEF SML instances!")));

    // check for expired certificate
    boolean bShowForm = true;
    {
      final X509Certificate aEntry = SMPKeyManager.getInstance ().getPrivateKeyCertificate ();
      if (aEntry != null)
      {
        try
        {
          aEntry.checkValidity ();
          aNodeList.addChild (info ("Your SMP certificate is still valid until " +
                                    PDTToString.getAsString (PDTFactory.createLocalDateTime (aEntry.getNotAfter ()),
                                                             aDisplayLocale) +
                                    "."));
        }
        catch (final CertificateExpiredException ex)
        {
          aNodeList.addChild (error ("Your SMP certificate is already expired." +
                                     " This functionality works only if your SMP certificate is NOT expired yet." +
                                     " Please contact CEF-EDELIVERY-SUPPORT@ec.europa.eu with your SMP ID, the new certificate and the requested exchange date!"));
          bShowForm = false;
        }
        catch (final CertificateNotYetValidException ex)
        {
          aNodeList.addChild (warn ("Your SMP certificate is not valid yet." +
                                    " For this page to work you need to have your old certificate (the one that will expire soon) configured." +
                                    " Most likely the certificate change will not work."));
        }
      }
    }

    if (aWPEC.hasAction (CPageParam.ACTION_PERFORM))
    {
      if (aWPEC.hasSubAction (SUBACTION_SMP_UPDATE_CERT))
        _updateSMPCertAtSML (aWPEC, aFormErrors);
    }

    // Update SMP certificate in SML
    if (bShowForm)
    {
      final int nLeft = 3;
      final BootstrapForm aForm = getUIHandler ().createFormFileUploadSelf (aWPEC);
      aForm.setLeft (nLeft);
      aForm.addChild (warn ("It is your responsibility to actually perform the update of the certificate in this SMP at the specified time! This does NOT happen automatically."));

      final BootstrapDateTimePicker aDTP = BootstrapDateTimePicker.create (FIELD_PM_MIGRATION_DATE,
                                                                           (LocalDate) null,
                                                                           aDisplayLocale);
      aDTP.setMinDate (PDTFactory.getCurrentLocalDate ().plusDays (1));
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Certificate migration date")
                                                   .setCtrl (aDTP)
                                                   .setHelpText ("The SML will replace the certificate at this date. It must be in the future and within the validity period of the provided new public key. If not provided, the 'valid from' part of the new certificate is used.")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_PM_MIGRATION_DATE)));

      aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("New public key")
                                                   .setCtrl (new HCTextArea (new RequestField (FIELD_PM_PUBLIC_KEY)).setRows (5))
                                                   .setHelpText ("Paste the public part of your new certificate here (using PEM encoding). Do NOT paste your new private key here.")
                                                   .setErrorList (aFormErrors.getListOfField (FIELD_PM_PUBLIC_KEY)));

      final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
      aToolbar.addHiddenField (CPageParam.PARAM_ACTION, CPageParam.ACTION_PERFORM);
      aToolbar.addHiddenField (CPageParam.PARAM_SUBACTION, SUBACTION_SMP_UPDATE_CERT);
      aToolbar.addSubmitButton ("Prepare certificate update");

      aNodeList.addChild (aForm);
    }
  }
}
