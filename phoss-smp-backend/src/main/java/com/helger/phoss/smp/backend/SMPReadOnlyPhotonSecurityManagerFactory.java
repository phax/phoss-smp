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

import org.jspecify.annotations.NonNull;

import com.helger.base.enforce.ValueEnforcer;
import com.helger.photon.audit.IAuditManager;
import com.helger.photon.security.lock.ILockManager;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.mgr.PhotonSecurityManager.IFactory;
import com.helger.photon.security.role.IRoleManager;
import com.helger.photon.security.token.user.IUserTokenManager;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.usergroup.IUserGroupManager;

/**
 * A {@link IFactory} for the read-only mode. All managers are created by the
 * backend specific delegate factory, except for the audit manager, which is replaced by a
 * {@link SMPReadOnlyAuditManager} that never persists anything.
 *
 * @author Philip Helger
 * @since 8.8.0
 */
public final class SMPReadOnlyPhotonSecurityManagerFactory implements IFactory
{
  private final IFactory m_aDelegate;

  public SMPReadOnlyPhotonSecurityManagerFactory (@NonNull final IFactory aDelegate)
  {
    ValueEnforcer.notNull (aDelegate, "Delegate");
    m_aDelegate = aDelegate;
  }

  @NonNull
  public IAuditManager createAuditMgr ()
  {
    return new SMPReadOnlyAuditManager ();
  }

  @Override
  @NonNull
  public ILockManager <String> createLockMgr () throws Exception
  {
    return m_aDelegate.createLockMgr ();
  }

  @NonNull
  public IUserManager createUserMgr () throws Exception
  {
    return m_aDelegate.createUserMgr ();
  }

  @NonNull
  public IRoleManager createRoleMgr () throws Exception
  {
    return m_aDelegate.createRoleMgr ();
  }

  @NonNull
  public IUserGroupManager createUserGroupMgr (@NonNull final IUserManager aUserMgr,
                                               @NonNull final IRoleManager aRoleMgr) throws Exception
  {
    return m_aDelegate.createUserGroupMgr (aUserMgr, aRoleMgr);
  }

  @NonNull
  public IUserTokenManager createUserTokenMgr (@NonNull final IUserManager aUserMgr) throws Exception
  {
    return m_aDelegate.createUserTokenMgr (aUserMgr);
  }

  /**
   * Install the read-only factory wrapping the provided backend specific factory and instantiate
   * the singleton. Must be called before {@link PhotonSecurityManager#getInstance()} is called
   * anywhere else.
   *
   * @param aDelegate
   *        The backend specific factory to wrap. May not be <code>null</code>.
   */
  public static void install (@NonNull final IFactory aDelegate)
  {
    PhotonSecurityManager.setFactory (new SMPReadOnlyPhotonSecurityManagerFactory (aDelegate));
    PhotonSecurityManager.getInstance ();
  }
}
