/**
 * Copyright (C) 2015-2019 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.backend.sql.mgr;

import java.security.cert.X509Certificate;
import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.eclipse.persistence.config.CacheUsage;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.ReturnsMutableObject;
import com.helger.commons.callback.CallbackList;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.mutable.MutableBoolean;
import com.helger.commons.state.EChange;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.peppolid.IDocumentTypeIdentifier;
import com.helger.phoss.smp.backend.sql.AbstractSMPJPAEnabledManager;
import com.helger.phoss.smp.backend.sql.model.DBServiceMetadataRedirection;
import com.helger.phoss.smp.backend.sql.model.DBServiceMetadataRedirectionID;
import com.helger.phoss.smp.domain.redirect.ISMPRedirect;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectCallback;
import com.helger.phoss.smp.domain.redirect.ISMPRedirectManager;
import com.helger.phoss.smp.domain.redirect.SMPRedirect;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.security.certificate.CertificateHelper;

/**
 * Manager for all {@link SMPRedirect} objects.
 *
 * @author Philip Helger
 */
public final class SMPRedirectManagerSQL extends AbstractSMPJPAEnabledManager implements ISMPRedirectManager
{
  private final ISMPServiceGroupManager m_aServiceGroupMgr;
  private final CallbackList <ISMPRedirectCallback> m_aCallbacks = new CallbackList <> ();

  public SMPRedirectManagerSQL (@Nonnull final ISMPServiceGroupManager aServiceGroupMgr)
  {
    m_aServiceGroupMgr = aServiceGroupMgr;
  }

  @Nonnull
  @ReturnsMutableObject
  public CallbackList <ISMPRedirectCallback> redirectCallbacks ()
  {
    return m_aCallbacks;
  }

  @Nullable
  public ISMPRedirect createOrUpdateSMPRedirect (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                 @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                 @Nonnull @Nonempty final String sTargetHref,
                                                 @Nonnull @Nonempty final String sSubjectUniqueIdentifier,
                                                 @Nullable final X509Certificate aCertificate,
                                                 @Nullable final String sExtension)
  {
    ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    ValueEnforcer.notNull (aDocumentTypeIdentifier, "DocumentTypeIdentifier");

    final MutableBoolean aCreatedNew = new MutableBoolean (true);
    JPAExecutionResult <?> ret;
    ret = doInTransaction ( () -> {
      final EntityManager aEM = getEntityManager ();
      final DBServiceMetadataRedirectionID aDBRedirectID = new DBServiceMetadataRedirectionID (aServiceGroup.getParticpantIdentifier (),
                                                                                               aDocumentTypeIdentifier);
      DBServiceMetadataRedirection aDBRedirect = aEM.find (DBServiceMetadataRedirection.class, aDBRedirectID);

      final String sCertificate = aCertificate == null ? null
                                                       : CertificateHelper.getPEMEncodedCertificate (aCertificate);
      if (aDBRedirect == null)
      {
        // Create a new one
        aDBRedirect = new DBServiceMetadataRedirection (aDBRedirectID,
                                                        sTargetHref,
                                                        sSubjectUniqueIdentifier,
                                                        sCertificate,
                                                        sExtension);
        aEM.persist (aDBRedirect);
        aCreatedNew.set (true);
      }
      else
      {
        // Edit the existing one
        aDBRedirect.setRedirectionUrl (sTargetHref);
        aDBRedirect.setCertificateUid (sSubjectUniqueIdentifier);
        aDBRedirect.setCertificate (sCertificate);
        aDBRedirect.setExtension (sExtension);
        aEM.merge (aDBRedirect);
        aCreatedNew.set (false);
      }
    });
    if (ret.hasException ())
    {
      return null;
    }
    final SMPRedirect aRedirect = new SMPRedirect (aServiceGroup,
                                                   aDocumentTypeIdentifier,
                                                   sTargetHref,
                                                   sSubjectUniqueIdentifier,
                                                   aCertificate,
                                                   sExtension);

    if (aCreatedNew.booleanValue ())
      m_aCallbacks.forEach (x -> x.onSMPRedirectCreated (aRedirect));
    else
      m_aCallbacks.forEach (x -> x.onSMPRedirectUpdated (aRedirect));

    return aRedirect;
  }

  @Nonnull
  public EChange deleteSMPRedirect (@Nullable final ISMPRedirect aSMPRedirect)
  {
    if (aSMPRedirect == null)
      return EChange.UNCHANGED;

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction ( () -> {
      final EntityManager aEM = getEntityManager ();
      final DBServiceMetadataRedirectionID aDBRedirectID = new DBServiceMetadataRedirectionID (aSMPRedirect.getServiceGroup ()
                                                                                                           .getParticpantIdentifier (),
                                                                                               aSMPRedirect.getDocumentTypeIdentifier ());
      final DBServiceMetadataRedirection aDBRedirect = aEM.find (DBServiceMetadataRedirection.class, aDBRedirectID);
      if (aDBRedirect == null)
        return EChange.UNCHANGED;

      aEM.remove (aDBRedirect);
      return EChange.CHANGED;
    });
    if (ret.hasException ())
      return EChange.UNCHANGED;
    if (ret.get ().isUnchanged ())
      return EChange.UNCHANGED;

    m_aCallbacks.forEach (x -> x.onSMPRedirectDeleted (aSMPRedirect));
    return EChange.CHANGED;
  }

  @Nonnull
  public EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    // Remember all existing
    final ICommonsList <ISMPRedirect> aDeletedRedirects = getAllSMPRedirectsOfServiceGroup (aServiceGroup);

    JPAExecutionResult <Integer> ret;
    ret = doInTransaction ( () -> {
      final int nCnt = getEntityManager ().createQuery ("DELETE FROM DBServiceMetadataRedirection p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value",
                                                        DBServiceMetadataRedirection.class)
                                          .setParameter ("scheme",
                                                         aServiceGroup.getParticpantIdentifier ().getScheme ())
                                          .setParameter ("value", aServiceGroup.getParticpantIdentifier ().getValue ())
                                          .executeUpdate ();
      return Integer.valueOf (nCnt);
    });
    if (ret.hasException ())
      return EChange.UNCHANGED;

    final int nDeleted = ret.get ().intValue ();
    if (nDeleted == 0)
      return EChange.UNCHANGED;

    // Callback only, if all were deleted
    if (nDeleted == aDeletedRedirects.size ())
    {
      for (final ISMPRedirect aRedirect : aDeletedRedirects)
        m_aCallbacks.forEach (x -> x.onSMPRedirectDeleted (aRedirect));
    }
    else
      LOGGER.warn (nDeleted +
                   " SMP redirects were deleted, but " +
                   aDeletedRedirects.size () +
                   " were found previously. Because of this inconsistency, no callbacks are triggered");

    return EChange.CHANGED;
  }

  @Nonnull
  private SMPRedirect _convert (@Nonnull final DBServiceMetadataRedirection aDBRedirect)
  {
    final X509Certificate aCertificate = CertificateHelper.convertStringToCertficateOrNull (aDBRedirect.getCertificate ());
    return new SMPRedirect (m_aServiceGroupMgr.getSMPServiceGroupOfID (aDBRedirect.getId ().getAsBusinessIdentifier ()),
                            aDBRedirect.getId ().getAsDocumentTypeIdentifier (),
                            aDBRedirect.getRedirectionUrl (),
                            aDBRedirect.getCertificateUid (),
                            aCertificate,
                            aDBRedirect.getExtension ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirects ()
  {
    JPAExecutionResult <List <DBServiceMetadataRedirection>> ret;
    ret = doInTransaction ( () -> getEntityManager ().createQuery ("SELECT p FROM DBServiceMetadataRedirection p",
                                                                   DBServiceMetadataRedirection.class)
                                                     .getResultList ());
    if (ret.hasException ())
    {
      return new CommonsArrayList <> ();
    }

    final ICommonsList <ISMPRedirect> aRedirects = new CommonsArrayList <> ();
    for (final DBServiceMetadataRedirection aDBRedirect : ret.get ())
      aRedirects.add (_convert (aDBRedirect));
    return aRedirects;
  }

  @Nonnull
  @ReturnsMutableCopy
  public ICommonsList <ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final ICommonsList <ISMPRedirect> aRedirects = new CommonsArrayList <> ();
    if (aServiceGroup != null)
    {
      JPAExecutionResult <List <DBServiceMetadataRedirection>> ret;
      ret = doInTransaction ( () -> getEntityManager ().createQuery ("SELECT p FROM DBServiceMetadataRedirection p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value",
                                                                     DBServiceMetadataRedirection.class)
                                                       .setParameter ("scheme",
                                                                      aServiceGroup.getParticpantIdentifier ()
                                                                                   .getScheme ())
                                                       .setParameter ("value",
                                                                      aServiceGroup.getParticpantIdentifier ()
                                                                                   .getValue ())
                                                       .getResultList ());
      if (ret.hasException ())
      {
        return new CommonsArrayList <> ();
      }

      for (final DBServiceMetadataRedirection aDBRedirect : ret.get ())
        aRedirects.add (_convert (aDBRedirect));
    }
    return aRedirects;
  }

  @Nonnegative
  public long getSMPRedirectCount ()
  {
    JPAExecutionResult <Long> ret;
    ret = doSelect ( () -> {
      final long nCount = getSelectCountResult (getEntityManager ().createQuery ("SELECT COUNT(p.id) FROM DBServiceMetadataRedirection p"));
      return Long.valueOf (nCount);
    });
    if (ret.hasException ())
    {
      return 0;
    }
    return ret.get ().longValue ();
  }

  @Nullable
  public ISMPRedirect getSMPRedirectOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                   @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    if (aServiceGroup == null)
      return null;
    if (aDocTypeID == null)
      return null;

    JPAExecutionResult <DBServiceMetadataRedirection> ret;
    ret = doInTransaction ( () -> {
      // Disable caching here
      final ICommonsMap <String, Object> aProps = new CommonsHashMap <> ();
      aProps.put ("eclipselink.cache-usage", CacheUsage.DoNotCheckCache);
      final DBServiceMetadataRedirectionID aDBRedirectID = new DBServiceMetadataRedirectionID (aServiceGroup.getParticpantIdentifier (),
                                                                                               aDocTypeID);
      return getEntityManager ().find (DBServiceMetadataRedirection.class, aDBRedirectID, aProps);
    });
    if (ret.hasException ())
    {
      return null;
    }
    final DBServiceMetadataRedirection aDBRedirect = ret.get ();
    if (aDBRedirect == null)
      return null;

    final X509Certificate aCertificate = CertificateHelper.convertStringToCertficateOrNull (aDBRedirect.getCertificate ());
    return new SMPRedirect (aServiceGroup,
                            aDocTypeID,
                            aDBRedirect.getRedirectionUrl (),
                            aDBRedirect.getCertificateUid (),
                            aCertificate,
                            aDBRedirect.getExtension ());
  }
}
