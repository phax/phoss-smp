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
import com.helger.collection.commons.ICommonsList;
import com.helger.datetime.format.PDTToString;
import com.helger.html.hc.html.tabular.HCRow;
import com.helger.html.hc.impl.HCNodeList;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.totp.ISMPUserTotp;
import com.helger.phoss.smp.domain.totp.ISMPUserTotpManager;
import com.helger.phoss.smp.ui.AbstractSMPWebPage;
import com.helger.photon.app.csrf.CSRFSessionManager;
import com.helger.photon.bootstrap5.button.BootstrapButton;
import com.helger.photon.bootstrap5.table.BootstrapTable;
import com.helger.photon.bootstrap5.uictrls.datatables.BootstrapDataTables;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.uicore.css.CPageParam;
import com.helger.photon.uicore.icon.EDefaultIcon;
import com.helger.photon.uicore.page.WebPageCSRFHandler;
import com.helger.photon.uicore.page.WebPageExecutionContext;
import com.helger.photon.uictrls.datatables.column.DTCol;
import com.helger.url.SimpleURL;

/**
 * Administrative page that lists the two-factor authentication (TOTP) enrollments of all users and
 * that allows an administrator to reset the enrollment of a single user. This is the recovery path,
 * if a user lost the device with the authenticator app and all recovery codes.
 *
 * @author Philip Helger
 * @since 8.4.3
 */
public final class PageSecureUserTotpAdmin extends AbstractSMPWebPage
{
  private static final String ACTION_RESET = "reset";
  private static final String PARAM_USER_ID = "userid";

  private static final Logger LOGGER = LoggerFactory.getLogger (PageSecureUserTotpAdmin.class);

  public PageSecureUserTotpAdmin (@NonNull @Nonempty final String sID)
  {
    super (sID, "Two-factor authentication of all users");
  }

  @NonNull
  private static String _getUserLabel (@NonNull final String sUserID)
  {
    final IUser aUser = PhotonSecurityManager.getUserMgr ().getUserOfID (sUserID);
    if (aUser == null)
      return sUserID;

    final String sLoginName = aUser.getLoginName ();
    return StringHelper.isNotEmpty (sLoginName) ? sLoginName + " (" + sUserID + ")" : sUserID;
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

    // Reset the enrollment of a single user
    if (aWPEC.hasAction (ACTION_RESET))
    {
      // State changing action - a plain link from a foreign page must not be sufficient
      if (WebPageCSRFHandler.INSTANCE.checkCSRFNonce (aWPEC).isBreak ())
        return;

      final String sUserID = aWPEC.params ().getAsStringTrimmed (PARAM_USER_ID);
      if (StringHelper.isEmpty (sUserID))
        aWPEC.postRedirectGetInternal (error ("No user ID was provided."));
      else
        if (aTotpMgr.deleteTotp (sUserID).isChanged ())
        {
          LOGGER.info ("Administrator '" +
                       aWPEC.getLoggedInUserID () +
                       "' reset the two-factor authentication of user ID '" +
                       sUserID +
                       "'");
          aWPEC.postRedirectGetInternal (success ("The two-factor authentication of user '" +
                                                  _getUserLabel (sUserID) +
                                                  "' was successfully reset."));
        }
        else
          aWPEC.postRedirectGetInternal (warn ("The two-factor authentication of user '" +
                                               _getUserLabel (sUserID) +
                                               "' could not be reset."));
      return;
    }

    aNodeList.addChild (info ("This page lists all users that started or finished the setup of two-factor authentication. Resetting the two-factor authentication of a user removes the second factor of that user, so that the user can login with login name and password only."));

    final ICommonsList <ISMPUserTotp> aAllTotps = aTotpMgr.getAllTotps ();

    final BootstrapTable aTable = new BootstrapTable (new DTCol ("User"),
                                                      new DTCol ("Enabled").setWidth (100),
                                                      new DTCol ("Registration date"),
                                                      new DTCol ("Recovery codes").setWidth (140),
                                                      new DTCol ("Action").setWidth (110)).setID (getID ());
    for (final ISMPUserTotp aTotp : aAllTotps)
    {
      final String sUserID = aTotp.getID ();
      final HCRow aRow = aTable.addBodyRow ();
      aRow.addCell (_getUserLabel (sUserID));
      aRow.addCell (aTotp.isEnabled () ? "yes" : "no (setup pending)");
      aRow.addCell (PDTToString.getAsString (aTotp.getRegistrationDateTime (), aDisplayLocale));
      aRow.addCell (Integer.toString (aTotp.getRecoveryCodeCount ()));

      final SimpleURL aResetURL = aWPEC.getSelfHref ()
                                       .add (CPageParam.PARAM_ACTION, ACTION_RESET)
                                       .add (PARAM_USER_ID, sUserID)
                                       .add (CPageParam.FIELD_NONCE, CSRFSessionManager.getInstance ().getNonce ());
      aRow.addCell (new BootstrapButton ().addChild ("Reset").setIcon (EDefaultIcon.DELETE).setOnClick (aResetURL));
    }

    aNodeList.addChild (aTable);
    aNodeList.addChild (BootstrapDataTables.createDefaultDataTables (aWPEC, aTable));
  }
}
