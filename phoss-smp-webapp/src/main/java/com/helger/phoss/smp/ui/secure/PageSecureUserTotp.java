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
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.html.embedded.HCImg;
import com.helger.html.hc.html.forms.HCEdit;
import com.helger.html.hc.html.forms.HCHiddenField;
import com.helger.html.hc.html.grouping.HCDiv;
import com.helger.html.hc.html.grouping.HCUL;
import com.helger.html.hc.html.textlevel.HCCode;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.app.CSMP;
import com.helger.phoss.smp.config.SMPServerConfiguration;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.totp.ISMPUserTotp;
import com.helger.phoss.smp.domain.totp.ISMPUserTotpManager;
import com.helger.phoss.smp.domain.totp.SMPTotpHelper;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.phoss.smp.ui.SMPSecondFactorHelper;
import com.helger.photon.app.csrf.CSRFSessionManager;
import com.helger.photon.bootstrap5.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.bootstrap5.form.BootstrapForm;
import com.helger.photon.bootstrap5.form.BootstrapFormGroup;
import com.helger.photon.core.form.FormErrorList;
import com.helger.photon.core.form.RequestField;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageCSRFHandler;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.totp.qr.image.DataUriEncoder;
import com.helger.totp.qr.image.ZxingPngQrCodeImageGenerator;
import com.helger.url.SimpleURL;

/**
 * Page to let the currently logged in user enable or disable two-factor authentication (TOTP) for
 * the <code>/secure</code> application.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
public final class PageSecureUserTotp extends AbstractSMPWebPage
{
  private static final String ACTION_ENROLL = "enroll";
  private static final String ACTION_CONFIRM = "confirm";
  private static final String ACTION_DISABLE = "disable";
  private static final String ACTION_CANCEL = "cancel";
  private static final String ACTION_NEW_RECOVERY_CODES = "newrecoverycodes";

  private static final String FIELD_CODE = "totpcode";

  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureUserTotp.class);

  public PageSecureUserTotp (@NonNull @Nonempty final String sID)
  {
    super (sID, "Two-factor Authentication");
  }

  @NonNull
  private static String _getAccountLabel (@NonNull final IUser aUser)
  {
    final String sLoginName = aUser.getLoginName ();
    return StringHelper.isNotEmpty (sLoginName) ? sLoginName : aUser.getID ();
  }

  /**
   * Create a link to this page, that contains an action and the current CSRF nonce. All actions of
   * this page are state changing, so they must not be triggerable via a plain link from a foreign
   * page.
   */
  @NonNull
  private static SimpleURL _getActionHref (@NonNull final WebPageExecutionContext aWPEC, @NonNull final String sAction)
  {
    return aWPEC.getSelfHref ()
                .add (CPageParam.PARAM_ACTION, sAction)
                .add (CPageParam.FIELD_NONCE, CSRFSessionManager.getInstance ().getNonce ());
  }

  /**
   * Create the new recovery codes of the provided user, store their hashes and show them to the
   * user. This is the only time, the codes are visible.
   */
  private void _createAndShowRecoveryCodes (@NonNull final WebPageExecutionContext aWPEC,
                                            @NonNull final ISMPUserTotpManager aTotpMgr,
                                            @NonNull final String sUserID)
  {
    final ICommonsList <String> aRecoveryCodes = SMPTotpHelper.createNewRecoveryCodes ();
    final ICommonsList <String> aHashes = new CommonsArrayList <> ();
    for (final String sRecoveryCode : aRecoveryCodes)
      aHashes.add (SMPTotpHelper.getRecoveryCodeHash (sRecoveryCode));

    if (aTotpMgr.setRecoveryCodeHashes (sUserID, aHashes).isUnchanged ())
    {
      aWPEC.getNodeList ().addChild (error ("Failed to create new recovery codes."));
      return;
    }

    LOGGER.info ("Created " + aRecoveryCodes.size () + " new TOTP recovery codes for user ID '" + sUserID + "'");

    final HCUL aUL = new HCUL ();
    for (final String sRecoveryCode : aRecoveryCodes)
      aUL.addItem (new HCCode ().addChild (sRecoveryCode));

    aWPEC.getNodeList ()
         .addChild (warn (new HCDiv ().addChild ("These are your recovery codes. Store them in a safe place - each of them can be used exactly once, if you don't have access to your authenticator app.")).addChild (new HCDiv ().addChild ("This is the only time they are shown. All previously created recovery codes are now invalid."))
                                                                                                                                                                                                           .addChild (aUL));
  }

  @Override
  protected void fillContent (@NonNull final WebPageExecutionContext aWPEC)
  {
    final HCNodeList aNodeList = aWPEC.getNodeList ();
    final Locale aDisplayLocale = aWPEC.getDisplayLocale ();
    final ISMPUserTotpManager aTotpMgr = SMPMetaManager.getUserTotpMgr ();
    if (aTotpMgr == null)
    {
      aNodeList.addChild (error ("Two-factor authentication is not supported by the current backend."));
      return;
    }

    final String sUserID = aWPEC.getLoggedInUserID ();
    final IUser aUser = PhotonSecurityManager.getUserMgr ().getUserOfID (sUserID);
    if (aUser == null)
    {
      aNodeList.addChild (error ("Failed to resolve the currently logged in user."));
      return;
    }

    final FormErrorList aFormErrors = new FormErrorList ();

    // All actions of this page are state changing, so all of them require a valid CSRF nonce
    final boolean bIsStateChangingAction = aWPEC.hasAction (ACTION_ENROLL) ||
                                           aWPEC.hasAction (ACTION_CONFIRM) ||
                                           aWPEC.hasAction (ACTION_DISABLE) ||
                                           aWPEC.hasAction (ACTION_CANCEL) ||
                                           aWPEC.hasAction (ACTION_NEW_RECOVERY_CODES);
    if (bIsStateChangingAction && WebPageCSRFHandler.INSTANCE.checkCSRFNonce (aWPEC).isBreak ())
      return;

    // Disable an active or cancel a pending enrollment
    if (aWPEC.hasAction (ACTION_DISABLE) || aWPEC.hasAction (ACTION_CANCEL))
    {
      final boolean bIsCancel = aWPEC.hasAction (ACTION_CANCEL);
      if (aTotpMgr.deleteTotp (sUserID).isChanged ())
      {
        LOGGER.info ((bIsCancel ? "Successfully cancelled the pending TOTP enrollment" : "Successfully disabled TOTP") +
                     " for user ID '" +
                     sUserID +
                     "'");
        aWPEC.postRedirectGetInternal (success (bIsCancel ? "The setup of two-factor authentication was cancelled."
                                                          : "Two-factor authentication was successfully disabled."));
      }
      else
        aWPEC.postRedirectGetInternal (warn ("Two-factor authentication was not enabled."));
      return;
    }

    // Start a new enrollment
    if (aWPEC.hasAction (ACTION_ENROLL))
    {
      aTotpMgr.createOrReplaceTotp (sUserID, SMPTotpHelper.createNewSecret ());
      LOGGER.info ("Created a new pending TOTP enrollment for user ID '" + sUserID + "'");
      aWPEC.postRedirectGetInternal (info ("A new secret was created. Please scan the QR code and confirm it with a code from your authenticator app."));
      return;
    }

    ISMPUserTotp aTotp = aTotpMgr.getTotpOfUserID (sUserID);

    // Create new recovery codes for an already enabled second factor
    if (aWPEC.hasAction (ACTION_NEW_RECOVERY_CODES))
    {
      if (aTotp == null || !aTotp.isEnabled ())
        aWPEC.postRedirectGetInternal (warn ("Two-factor authentication is not enabled."));
      else
        _createAndShowRecoveryCodes (aWPEC, aTotpMgr, sUserID);
      return;
    }

    // Confirm a pending enrollment
    if (aWPEC.hasAction (ACTION_CONFIRM))
    {
      if (aTotp == null)
        aWPEC.postRedirectGetInternal (error ("No pending two-factor authentication setup was found."));
      else
        if (aTotp.isEnabled ())
          aWPEC.postRedirectGetInternal (warn ("Two-factor authentication is already enabled."));
        else
        {
          final String sCode = aWPEC.params ().getAsStringTrimmed (FIELD_CODE);
          if (StringHelper.isEmpty (sCode))
            aFormErrors.addFieldError (FIELD_CODE, "A code from your authenticator app must be provided");
          else
          {
            final Long aMatchingTimeSlot = SMPTotpHelper.getMatchingTimeSlot (aTotp.getSecret (), sCode);
            if (aMatchingTimeSlot != null)
            {
              aTotpMgr.setTotpLastUsedTimeSlot (sUserID, aMatchingTimeSlot.longValue ());
              aTotpMgr.setTotpEnabled (sUserID, true);
              // The user just proved that he owns the second factor
              SMPSecondFactorHelper.markSecondFactorProvided (sUserID);
              LOGGER.info ("Successfully enabled TOTP for user ID '" + sUserID + "'");
              aNodeList.addChild (success ("Two-factor authentication was successfully enabled. It is required the next time you login."));
              // Show the recovery codes exactly once
              _createAndShowRecoveryCodes (aWPEC, aTotpMgr, sUserID);
              return;
            }
            aFormErrors.addFieldError (FIELD_CODE, "The provided code is invalid. Please try again.");
          }
          // Re-read, because the last used time slot may have changed
          aTotp = aTotpMgr.getTotpOfUserID (sUserID);
        }
      if (aFormErrors.isEmpty ())
        return;
    }

    if (aTotp != null && aTotp.isEnabled ())
    {
      // Already enabled
      final int nRecoveryCodes = aTotp.getRecoveryCodeCount ();
      aNodeList.addChild (success ().addChild (div ().addChild ("Two-factor authentication is enabled for user ")
                                                     .addChild (code (_getAccountLabel (aUser)))
                                                     .addChild ("."))
                                    .addChild (div ().addChild ("Enabled since: " +
                                                                PDTToString.getAsString (aTotp.getRegistrationDateTime (),
                                                                                         aDisplayLocale)))
                                    .addChild (div ().addChild (nRecoveryCodes +
                                                                " unused recovery code" +
                                                                (nRecoveryCodes == 1 ? "" : "s") +
                                                                " left")));

      final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
      aToolbar.addButton ("Disable two-factor authentication",
                          _getActionHref (aWPEC, ACTION_DISABLE),
                          EDefaultIcon.DELETE);
      aToolbar.addButton ("Create a new secret", _getActionHref (aWPEC, ACTION_ENROLL), EDefaultIcon.REFRESH);
      aToolbar.addButton ("Create new recovery codes",
                          _getActionHref (aWPEC, ACTION_NEW_RECOVERY_CODES),
                          EDefaultIcon.NEW);
      return;
    }

    if (aTotp == null)
    {
      // Nothing setup yet
      aNodeList.addChild (info ("Two-factor authentication is currently not enabled for user '" +
                                _getAccountLabel (aUser) +
                                "'."));
      aNodeList.addChild (div ("With two-factor authentication enabled, an additional one-time password from an authenticator app (like Authy, Google Authenticator, Microsoft Authenticator, FreeOTP, ...) is required to login."));

      final BootstrapButtonToolbar aToolbar = aNodeList.addAndReturnChild (new BootstrapButtonToolbar (aWPEC));
      aToolbar.addButton ("Enable two-factor authentication", _getActionHref (aWPEC, ACTION_ENROLL), EDefaultIcon.YES);
      return;
    }

    // A pending, not yet confirmed enrollment exists
    aNodeList.addChild (warn ("The setup of two-factor authentication is not yet finished. Please scan the QR code below with your authenticator app and confirm it with a generated code."));

    final String sSecret = aTotp.getSecret ();
    String sDataURI = null;
    try
    {
      final ZxingPngQrCodeImageGenerator aGenerator = new ZxingPngQrCodeImageGenerator ();
      // The issuer identifies this SMP instance in the authenticator app of the user
      final String sConfiguredIssuer = SMPServerConfiguration.getTotpIssuer ();
      final String sIssuer = StringHelper.isNotEmpty (sConfiguredIssuer) ? sConfiguredIssuer
                                                                         : CSMP.getApplicationTitle ();
      final byte [] aImage = aGenerator.generate (SMPTotpHelper.getQrData (sIssuer,
                                                                           sIssuer + ":" + _getAccountLabel (aUser),
                                                                           sSecret));
      sDataURI = DataUriEncoder.getDataUriForImage (aImage, aGenerator.getImageMimeType ());
    }
    catch (final Exception ex)
    {
      LOGGER.error ("Failed to create the TOTP QR code image", ex);
    }

    final BootstrapForm aForm = aNodeList.addAndReturnChild (getUIHandler ().createFormSelf (aWPEC));
    aForm.addChild (new HCHiddenField (CPageParam.PARAM_ACTION, ACTION_CONFIRM));
    aForm.addChild (WebPageCSRFHandler.INSTANCE.createCSRFNonceField ());

    if (sDataURI != null)
      aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("QR code")
                                                   .setCtrl (div (new HCImg ().setSrc (new SimpleURL (sDataURI))
                                                                              .setAlt ("TOTP QR code")))
                                                   .setHelpText ("Scan this QR code with your authenticator app"));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabel ("Secret key")
                                                 .setCtrl (div (code ().addChild (sSecret)))
                                                 .setHelpText ("Alternatively enter this key manually into your authenticator app. Never share it with anybody else."));

    aForm.addFormGroup (new BootstrapFormGroup ().setLabelMandatory ("Code from your authenticator app")
                                                 .setCtrl (new HCEdit (new RequestField (FIELD_CODE)).setPlaceholder ("000000"))
                                                 .setErrorList (aFormErrors.getListOfField (FIELD_CODE)));

    final BootstrapButtonToolbar aToolbar = aForm.addAndReturnChild (getUIHandler ().createToolbar (aWPEC));
    aToolbar.addSubmitButton ("Confirm and enable", EDefaultIcon.SAVE);
    aToolbar.addButton ("Cancel", _getActionHref (aWPEC, ACTION_CANCEL), EDefaultIcon.CANCEL);
  }
}
