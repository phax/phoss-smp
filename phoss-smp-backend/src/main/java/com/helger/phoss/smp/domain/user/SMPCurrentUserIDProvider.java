/*
 * Copyright (C) 2015-2026 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.domain.user;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.string.StringHelper;
import com.helger.photon.security.login.LoggedInUserManager;
import com.helger.security.authentication.subject.user.ICurrentUserIDProvider;
import com.helger.web.scope.IRequestWebScope;
import com.helger.web.scope.mgr.WebScopeManager;

/**
 * Provides the authenticated REST API user from the current request, with the regular UI session
 * user as a fallback.
 *
 * @author vinit-thummar
 */
public final class SMPCurrentUserIDProvider implements ICurrentUserIDProvider
{
  private static final String REQUEST_ATTR_API_USER_ID = SMPCurrentUserIDProvider.class.getName () + ".api-user-id";

  public static final SMPCurrentUserIDProvider INSTANCE = new SMPCurrentUserIDProvider (LoggedInUserManager.getInstance ());

  private final ICurrentUserIDProvider m_aFallbackProvider;

  SMPCurrentUserIDProvider (@NonNull final ICurrentUserIDProvider aFallbackProvider)
  {
    m_aFallbackProvider = ValueEnforcer.notNull (aFallbackProvider, "FallbackProvider");
  }

  /**
   * Remember the authenticated REST API user for the lifetime of the current request. If this is
   * called outside a web request, it intentionally has no effect.
   *
   * @param sUserID
   *        The authenticated user ID. May neither be <code>null</code> nor empty.
   */
  public static void setCurrentAPIUserID (@NonNull final String sUserID)
  {
    ValueEnforcer.notEmpty (sUserID, "UserID");

    final IRequestWebScope aRequestScope = WebScopeManager.getRequestScopeOrNull ();
    if (aRequestScope != null)
      aRequestScope.attrs ().putIn (REQUEST_ATTR_API_USER_ID, sUserID);
  }

  @Nullable
  public String getCurrentUserID ()
  {
    final IRequestWebScope aRequestScope = WebScopeManager.getRequestScopeOrNull ();
    if (aRequestScope != null)
    {
      final String sAPIUserID = aRequestScope.attrs ().getCastedValue (REQUEST_ATTR_API_USER_ID);
      if (StringHelper.isNotEmpty (sAPIUserID))
        return sAPIUserID;
    }

    return m_aFallbackProvider.getCurrentUserID ();
  }
}
