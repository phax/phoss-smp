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
package com.helger.phoss.smp.backend;

import java.time.LocalDate;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.collection.commons.CommonsArrayList;
import com.helger.collection.commons.ICommonsList;
import com.helger.photon.audit.IAuditItem;
import com.helger.photon.audit.IAuditManager;
import com.helger.photon.audit.IAuditor;
import com.helger.photon.audit.LoggingAuditor;
import com.helger.photon.security.login.GlobalUserIDProvider;

/**
 * An {@link IAuditManager} for the read-only mode. Audit items are only logged and never persisted,
 * so that the backend is not modified.
 *
 * @author Philip Helger
 * @since 8.8.0
 */
public class SMPReadOnlyAuditManager implements IAuditManager
{
  private final LoggingAuditor m_aAuditor;

  public SMPReadOnlyAuditManager ()
  {
    m_aAuditor = new LoggingAuditor (GlobalUserIDProvider::getCurrentUserID);
  }

  public boolean isInMemory ()
  {
    return true;
  }

  @Nullable
  public String getBaseDir ()
  {
    // No file system
    return null;
  }

  @NonNull
  public IAuditor getAuditor ()
  {
    return m_aAuditor;
  }

  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <IAuditItem> getLastAuditItems (@Nonnegative final int nMaxItems)
  {
    // Nothing is persisted
    return new CommonsArrayList <> ();
  }

  public void stop ()
  {
    // Nothing to do
  }

  @Nullable
  public LocalDate getEarliestAuditDate ()
  {
    // Nothing is persisted
    return null;
  }
}
