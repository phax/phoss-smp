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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
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
import com.helger.peppol.identifier.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.MetaManager;
import com.helger.peppol.smpserver.domain.SMPHelper;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.servicegroup.SMPServiceGroup;
import com.helger.peppol.smpserver.smlhook.IRegistrationHook;
import com.helger.peppol.smpserver.smlhook.RegistrationHookFactory;

public final class SQLServiceGroupManager implements ISMPServiceGroupManager
{
  private final ReadWriteLock m_aRWLock = new ReentrantReadWriteLock ();
  private final Map <String, SMPServiceGroup> m_aMap = new HashMap <String, SMPServiceGroup> ();
  private final IRegistrationHook m_aHook;

  public SQLServiceGroupManager ()
  {
    m_aHook = RegistrationHookFactory.getOrCreateInstance ();
  }

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

    // It's a new service group - throws exception in case of an error
    m_aHook.createServiceGroup (aParticipantIdentifier);

    m_aRWLock.writeLock ().lock ();
    try
    {
      _addSMPServiceGroup (aSMPServiceGroup);
    }
    catch (final RuntimeException ex)
    {
      // An error occurred - remove from SML again
      m_aHook.undoCreateServiceGroup (aParticipantIdentifier);
      throw ex;
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
    final ISMPServiceGroup aSMPServiceGroup = getSMPServiceGroupOfID (aParticipantIdentifier);
    if (aSMPServiceGroup == null)
      return EChange.UNCHANGED;

    // Delete in SML - throws exception in case of error
    m_aHook.deleteServiceGroup (aSMPServiceGroup.getParticpantIdentifier ());

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPServiceGroup aRealServiceGroup = m_aMap.remove (aSMPServiceGroup.getID ());
      if (aRealServiceGroup == null)
        return EChange.UNCHANGED;

      // Delete all redirects and all service information of this service group
      // as well
      MetaManager.getRedirectMgr ().deleteAllSMPRedirectsOfServiceGroup (aSMPServiceGroup);
      MetaManager.getServiceInformationMgr ().deleteAllSMPServiceInformationOfServiceGroup (aSMPServiceGroup.getID ());
    }
    catch (final RuntimeException ex)
    {
      // An error occurred - remove from SML again
      m_aHook.undoDeleteServiceGroup (aSMPServiceGroup.getParticpantIdentifier ());
      throw ex;
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

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceGroup> getAllSMPServiceGroupsOfOwner (@Nonnull final String sOwnerID)
  {
    final List <ISMPServiceGroup> ret = new ArrayList <> ();
    m_aRWLock.readLock ().lock ();
    try
    {
      for (final ISMPServiceGroup aSG : m_aMap.values ())
        if (aSG.getOwnerID ().equals (sOwnerID))
          ret.add (aSG);
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
    return ret;
  }

  public ISMPServiceGroup getSMPServiceGroupOfID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (aParticipantIdentifier == null)
      return null;

    final String sID = SMPHelper.createSMPServiceGroupID (aParticipantIdentifier);
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

  public boolean containsSMPServiceGroupWithID (@Nullable final IParticipantIdentifier aParticipantIdentifier)
  {
    if (aParticipantIdentifier == null)
      return false;

    m_aRWLock.readLock ().lock ();
    try
    {
      return m_aMap.containsKey (SMPHelper.createSMPServiceGroupID (aParticipantIdentifier));
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
