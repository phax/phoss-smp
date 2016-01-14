/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
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
package com.helger.peppol.smpserver.data.xml.mgr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ELockType;
import com.helger.commons.annotation.IsLocked;
import com.helger.commons.annotation.MustBeLocked;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.microdom.IMicroDocument;
import com.helger.commons.microdom.IMicroElement;
import com.helger.commons.microdom.MicroDocument;
import com.helger.commons.microdom.convert.MicroTypeConverter;
import com.helger.commons.state.EChange;
import com.helger.commons.string.StringHelper;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirect;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.photon.basic.app.dao.impl.AbstractWALDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.photon.basic.app.dao.impl.EDAOActionType;
import com.helger.photon.basic.audit.AuditHelper;

/**
 * Manager for all {@link SMPRedirect} objects.
 *
 * @author Philip Helger
 */
public final class XMLRedirectManager extends AbstractWALDAO <SMPRedirect> implements ISMPRedirectManager
{
  private static final String ELEMENT_ROOT = "redirects";
  private static final String ELEMENT_ITEM = "redirect";

  private final Map <String, SMPRedirect> m_aMap = new HashMap <String, SMPRedirect> ();

  public XMLRedirectManager (@Nonnull @Nonempty final String sFilename) throws DAOException
  {
    super (SMPRedirect.class, sFilename);
    initialRead ();
  }

  @Override
  protected void onRecoveryCreate (final SMPRedirect aElement)
  {
    _addSMPRedirect (aElement);
  }

  @Override
  protected void onRecoveryUpdate (final SMPRedirect aElement)
  {
    _addSMPRedirect (aElement);
  }

  @Override
  protected void onRecoveryDelete (final SMPRedirect aElement)
  {
    m_aMap.remove (aElement.getID ());
  }

  @Override
  @Nonnull
  protected EChange onRead (@Nonnull final IMicroDocument aDoc)
  {
    for (final IMicroElement eSMPRedirect : aDoc.getDocumentElement ().getAllChildElements (ELEMENT_ITEM))
      _addSMPRedirect (MicroTypeConverter.convertToNative (eSMPRedirect, SMPRedirect.class));
    return EChange.UNCHANGED;
  }

  @Override
  @Nonnull
  protected IMicroDocument createWriteData ()
  {
    final IMicroDocument aDoc = new MicroDocument ();
    final IMicroElement eRoot = aDoc.appendElement (ELEMENT_ROOT);
    for (final ISMPRedirect aSMPRedirect : CollectionHelper.getSortedByKey (m_aMap).values ())
      eRoot.appendChild (MicroTypeConverter.convertToMicroElement (aSMPRedirect, ELEMENT_ITEM));
    return aDoc;
  }

  @MustBeLocked (ELockType.WRITE)
  private void _addSMPRedirect (@Nonnull final SMPRedirect aSMPRedirect)
  {
    ValueEnforcer.notNull (aSMPRedirect, "SMPRedirect");

    final String sSMPRedirectID = aSMPRedirect.getID ();
    if (m_aMap.containsKey (sSMPRedirectID))
      throw new IllegalArgumentException ("SMPRedirect ID '" + sSMPRedirectID + "' is already in use!");
    m_aMap.put (aSMPRedirect.getID (), aSMPRedirect);
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPRedirect _createSMPRedirect (@Nonnull final SMPRedirect aSMPRedirect)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      _addSMPRedirect (aSMPRedirect);
      markAsChanged (aSMPRedirect, EDAOActionType.CREATE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditCreateSuccess (SMPRedirect.OT,
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded (),
                                      aSMPRedirect.getTargetHref (),
                                      aSMPRedirect.getSubjectUniqueIdentifier (),
                                      aSMPRedirect.getExtension ());
    return aSMPRedirect;
  }

  @Nonnull
  @IsLocked (ELockType.WRITE)
  private ISMPRedirect _updateSMPRedirect (@Nonnull final SMPRedirect aSMPRedirect)
  {
    m_aRWLock.writeLock ().lock ();
    try
    {
      m_aMap.put (aSMPRedirect.getID (), aSMPRedirect);
      markAsChanged (aSMPRedirect, EDAOActionType.UPDATE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditModifySuccess (SMPRedirect.OT,
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded (),
                                      aSMPRedirect.getTargetHref (),
                                      aSMPRedirect.getSubjectUniqueIdentifier (),
                                      aSMPRedirect.getExtension ());
    return aSMPRedirect;
  }

  /**
   * Create or update a redirect for a service group.
   *
   * @param aServiceGroup
   *        Service group
   * @param aDocumentTypeIdentifier
   *        Document type identifier affected.
   * @param sTargetHref
   *        Target URL of the new SMP
   * @param sSubjectUniqueIdentifier
   *        The subject unique identifier of the target SMPs certificate used to
   *        sign its resources.
   * @param sExtension
   *        Optional extension element
   * @return The new or updated {@link ISMPRedirect}. Never <code>null</code>.
   */
  @Nonnull
  public ISMPRedirect createOrUpdateSMPRedirect (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                 @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                 @Nonnull @Nonempty final String sTargetHref,
                                                 @Nonnull @Nonempty final String sSubjectUniqueIdentifier,
                                                 @Nullable final String sExtension)
  {
    ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");

    final ISMPRedirect aOldRedirect = getSMPRedirectOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                   aDocumentTypeIdentifier);
    SMPRedirect aNewRedirect;
    if (aOldRedirect != null)
    {
      // Reuse old ID
      aNewRedirect = new SMPRedirect (aServiceGroup,
                                      aDocumentTypeIdentifier,
                                      sTargetHref,
                                      sSubjectUniqueIdentifier,
                                      sExtension);
      _updateSMPRedirect (aNewRedirect);
    }
    else
    {
      // Create new ID
      aNewRedirect = new SMPRedirect (aServiceGroup,
                                      aDocumentTypeIdentifier,
                                      sTargetHref,
                                      sSubjectUniqueIdentifier,
                                      sExtension);
      _createSMPRedirect (aNewRedirect);
    }
    return aNewRedirect;
  }

  @Nonnull
  public EChange deleteSMPRedirect (@Nullable final ISMPRedirect aSMPRedirect)
  {
    if (aSMPRedirect == null)
      return EChange.UNCHANGED;

    m_aRWLock.writeLock ().lock ();
    try
    {
      final SMPRedirect aRealServiceMetadata = m_aMap.remove (aSMPRedirect.getID ());
      if (aRealServiceMetadata == null)
      {
        AuditHelper.onAuditDeleteFailure (SMPRedirect.OT, "no-such-id", aSMPRedirect.getID ());
        return EChange.UNCHANGED;
      }

      markAsChanged (aRealServiceMetadata, EDAOActionType.DELETE);
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    AuditHelper.onAuditDeleteSuccess (SMPRedirect.OT,
                                      aSMPRedirect.getID (),
                                      aSMPRedirect.getServiceGroupID (),
                                      aSMPRedirect.getDocumentTypeIdentifier ().getURIEncoded ());
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    EChange eChange = EChange.UNCHANGED;
    for (final ISMPRedirect aRedirect : getAllSMPRedirectsOfServiceGroup (aServiceGroup.getID ()))
      eChange = eChange.or (deleteSMPRedirect (aRedirect));
    return eChange;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPRedirect> getAllSMPRedirects ()
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
  public Collection <? extends ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    return getAllSMPRedirectsOfServiceGroup (aServiceGroup == null ? null : aServiceGroup.getID ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final String sServiceGroupID)
  {
    final List <ISMPRedirect> ret = new ArrayList <ISMPRedirect> ();
    if (StringHelper.hasText (sServiceGroupID))
    {
      m_aRWLock.readLock ().lock ();
      try
      {
        for (final ISMPRedirect aRedirect : m_aMap.values ())
          if (aRedirect.getServiceGroupID ().equals (sServiceGroupID))
            ret.add (aRedirect);
      }
      finally
      {
        m_aRWLock.readLock ().unlock ();
      }
    }
    return ret;
  }

  @Nonnegative
  public int getSMPRedirectCount ()
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

  public ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                   @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    if (aServiceGroup == null)
      return null;
    if (aDocTypeID == null)
      return null;

    m_aRWLock.readLock ().lock ();
    try
    {
      for (final ISMPRedirect aRedirect : m_aMap.values ())
        if (aRedirect.getServiceGroupID ().equals (aServiceGroup.getID ()) &&
            IdentifierHelper.areDocumentTypeIdentifiersEqual (aDocTypeID, aRedirect.getDocumentTypeIdentifier ()))
          return aRedirect;
    }
    finally
    {
      m_aRWLock.readLock ().unlock ();
    }
    return null;
  }
}
