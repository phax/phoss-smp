/**
 * Copyright (C) 2015-2018 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.peppol.smpserver.data.sql;

import javax.annotation.Nonnull;
import javax.persistence.EntityManager;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.db.jpa.AbstractPerRequestEntityManager;

/**
 * The per-request singleton, that creates {@link EntityManager} objects from
 * {@link SMPEntityManagerFactory}.
 *
 * @author philip
 */
public final class SMPEntityManagerWrapper extends AbstractPerRequestEntityManager
{
  @Deprecated
  @UsedViaReflection
  public SMPEntityManagerWrapper ()
  {}

  public static SMPEntityManagerWrapper getInstance ()
  {
    return getRequestSingleton (SMPEntityManagerWrapper.class);
  }

  @Override
  @Nonnull
  protected EntityManager createEntityManager ()
  {
    return SMPEntityManagerFactory.getInstance ().createEntityManager ();
  }
}
