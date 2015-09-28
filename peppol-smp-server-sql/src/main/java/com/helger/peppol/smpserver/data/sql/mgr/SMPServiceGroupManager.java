/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.sql.mgr;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPHelper;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;

public final class SMPServiceGroupManager implements ISMPServiceGroupManager
{
  private final ReadWriteLock m_aRWLock = new ReentrantReadWriteLock ();
  private final Map <String, SMPServiceGroup> m_aMap = new HashMap <String, SMPServiceGroup> ();

  public SMPServiceGroupManager ()
  {}

  private void _addSMPServiceGroup (@Nonnull final SMPServiceGroup aSMPServiceGroup)
  {
    ValueEnforcer.notNull (aSMPServiceGroup, "SMPServiceGroup");

    final String sSMPServiceGroupID = aSMPServiceGroup.getID ();
    if (m_aMap.containsKey (sSMPServiceGroupID))
      throw new IllegalArgumentException ("SMPServiceGroup ID '" + sSMPServiceGroupID + "' is already in use!");
    m_aMap.put (aSMPServiceGroup.getID (), aSMPServiceGroup);
  }

  @Nonnull
  public SMPServiceGroup createSMPServiceGroup (@Nonnull @Nonempty final String sOwnerID,
                                                @Nullable @Nonnull final IParticipantIdentifier aParticipantIdentifier,
                                                final String sExtension)
  {
    final SMPServiceGroup aSMPServiceGroup = new SMPServiceGroup (sOwnerID, aParticipantIdentifier, sExtension);

    m_aRWLock.writeLock ().lock ();
    try
    {
      _addSMPServiceGroup (aSMPServiceGroup);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    return aSMPServiceGroup;
  }

  @Nonnull
  public EChange updateSMPServiceGroup (@Nullable final String sSMPServiceGroupID,
                                        @Nonnull @Nonempty final String sOwnerID,
                                        @Nullable final String sExtension)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPServiceGroup aSMPServiceGroup = m_aMap.get (sSMPServiceGroupID);
      if (aSMPServiceGroup == null)
        return EChange.UNCHANGED;

      EChange eChange = EChange.UNCHANGED;
      eChange = eChange.or (aSMPServiceGroup.setOwnerID (sOwnerID));
      eChange = eChange.or (aSMPServiceGroup.setExtension (sExtension));
      if (eChange.isUnchanged ())
        return EChange.UNCHANGED;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    return deleteSMPServiceGroup (getSMPServiceGroupOfID (aParticipantIdentifier));
  }

  @Nonnull
  public EChange deleteSMPServiceGroup (@Nullable final ISMPServiceGroup aSMPServiceGroup)
  {
    if (aSMPServiceGroup == null)
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPServiceGroup aRealServiceGroup = m_aMap.remove (aSMPServiceGroup.getID ());
      if (aRealServiceGroup == null)
        return EChange.UNCHANGED;
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    return EChange.CHANGED;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceGroup> getAllSMPServiceGroups ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return CollectionHelper.newList (m_aMap.values ());
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (aParticipantIdentifier == null)
      return null;

    return getSMPServiceGroupOfID (SMPHelper.createSMPServiceGroupID (aParticipantIdentifier));
  }

  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return null;

    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aMap.get (sID);
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  public boolean containsSMPServiceGroupWithID (@Nullable final String sID)
  {
    if (StringHelper.hasNoText (sID))
      return false;

    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aMap.containsKey (sID);
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }

  @Nonnegative
  public int getSMPServiceGroupCount ()
  {
    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aMap.size ();
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
  }
}
