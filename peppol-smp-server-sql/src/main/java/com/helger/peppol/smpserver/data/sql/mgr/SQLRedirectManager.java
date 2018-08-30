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
package com.helger.peppol.smpserver.data.sql.mgr;

import java.util.List;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.eclipse.persistence.config.CacheUsage;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.impl.CommonsArrayList;
import com.helger.commons.collection.impl.CommonsHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsMap;
import com.helger.commons.state.EChange;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.peppol.identifier.generic.doctype.IDocumentTypeIdentifier;
import com.helger.peppol.smpserver.data.sql.AbstractSMPJPAEnabledManager;
import com.helger.peppol.smpserver.data.sql.model.DBServiceMetadataRedirection;
import com.helger.peppol.smpserver.data.sql.model.DBServiceMetadataRedirectionID;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirect;
import com.helger.peppol.smpserver.domain.redirect.ISMPRedirectManager;
import com.helger.peppol.smpserver.domain.redirect.SMPRedirect;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;

/**
 * Manager for all {@link SMPRedirect} objects.
 *
 * @author Philip Helger
 */
public final class SQLRedirectManager extends AbstractSMPJPAEnabledManager implements ISMPRedirectManager
{
  public SQLRedirectManager ()
  {}

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
   * @return The new or updated {@link ISMPRedirect}. Only <code>null</code> in
   *         case of unthrown exception.
   */
  @Nullable
  public ISMPRedirect createOrUpdateSMPRedirect (@Nonnull final ISMPServiceGroup aServiceGroup,
                                                 @Nonnull final IDocumentTypeIdentifier aDocumentTypeIdentifier,
                                                 @Nonnull @Nonempty final String sTargetHref,
                                                 @Nonnull @Nonempty final String sSubjectUniqueIdentifier,
                                                 @Nullable final String sExtension)
  {
    ValueEnforcer.notNull (aServiceGroup, "ServiceGroup");
    ValueEnforcer.notNull (aDocumentTypeIdentifier, "DocumentTypeIdentifier");

    JPAExecutionResult <?> ret;
    ret = doInTransaction ( () -> {
      final EntityManager aEM = getEntityManager ();
      final DBServiceMetadataRedirectionID aDBRedirectID = new DBServiceMetadataRedirectionID (aServiceGroup.getParticpantIdentifier (),
                                                                                               aDocumentTypeIdentifier);
      DBServiceMetadataRedirection aDBRedirect = aEM.find (DBServiceMetadataRedirection.class, aDBRedirectID);

      if (aDBRedirect == null)
      {
        // Create a new one
        aDBRedirect = new DBServiceMetadataRedirection (aDBRedirectID,
                                                        sTargetHref,
                                                        sSubjectUniqueIdentifier,
                                                        sExtension);
        aEM.persist (aDBRedirect);
      }
      else
      {
        // Edit the existing one
        aDBRedirect.setRedirectionUrl (sTargetHref);
        aDBRedirect.setCertificateUid (sSubjectUniqueIdentifier);
        aDBRedirect.setExtension (sExtension);
        aEM.merge (aDBRedirect);
      }
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return null;
    }
    return new SMPRedirect (aServiceGroup, aDocumentTypeIdentifier, sTargetHref, sSubjectUniqueIdentifier, sExtension);
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
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return EChange.UNCHANGED;
    }
    return ret.get ();
  }

  @Nonnull
  public EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction ( () -> {
      final int nCnt = getEntityManager ().createQuery ("DELETE FROM DBServiceMetadataRedirection p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value",
                                                        DBServiceMetadataRedirection.class)
                                          .setParameter ("scheme",
                                                         aServiceGroup.getParticpantIdentifier ().getScheme ())
                                          .setParameter ("value", aServiceGroup.getParticpantIdentifier ().getValue ())
                                          .executeUpdate ();
      return EChange.valueOf (nCnt > 0);
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return EChange.UNCHANGED;
    }
    return ret.get ();
  }

  @Nonnull
  private static SMPRedirect _convert (@Nonnull final DBServiceMetadataRedirection aDBRedirect)
  {
    final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
    return new SMPRedirect (aServiceGroupMgr.getSMPServiceGroupOfID (aDBRedirect.getId ().getAsBusinessIdentifier ()),
                            aDBRedirect.getId ().getAsDocumentTypeIdentifier (),
                            aDBRedirect.getRedirectionUrl (),
                            aDBRedirect.getCertificateUid (),
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
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
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
        exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
        return new CommonsArrayList <> ();
      }

      for (final DBServiceMetadataRedirection aDBRedirect : ret.get ())
        aRedirects.add (_convert (aDBRedirect));
    }
    return aRedirects;
  }

  @Nonnegative
  public int getSMPRedirectCount ()
  {
    JPAExecutionResult <Long> ret;
    ret = doSelect ( () -> {
      final long nCount = getSelectCountResult (getEntityManager ().createQuery ("SELECT COUNT(p.id) FROM DBServiceMetadataRedirection p"));
      return Long.valueOf (nCount);
    });
    if (ret.hasException ())
    {
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return 0;
    }
    return ret.get ().intValue ();
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
      exceptionCallbacks ().forEach (x -> x.onException (ret.getException ()));
      return null;
    }
    final DBServiceMetadataRedirection aDBRedirect = ret.get ();
    if (aDBRedirect == null)
      return null;

    return new SMPRedirect (aServiceGroup,
                            aDocTypeID,
                            aDBRedirect.getRedirectionUrl (),
                            aDBRedirect.getCertificateUid (),
                            aDBRedirect.getExtension ());
  }
}
