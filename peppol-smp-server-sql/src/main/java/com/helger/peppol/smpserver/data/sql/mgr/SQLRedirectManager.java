/**
 * Copyright (C) 2015-2016 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.data.sql.mgr;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import org.eclipse.persistence.config.CacheUsage;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
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
    ValueEnforcer.notNull (aDocumentTypeIdentifier, "DocumentTypeIdentifier");

    JPAExecutionResult <?> ret;
    ret = doInTransaction (new Runnable ()
    {
      public void run ()
      {
        final EntityManager aEM = getEntityManager ();
        final DBServiceMetadataRedirectionID aDBRedirectID = new DBServiceMetadataRedirectionID (aServiceGroup.getParticpantIdentifier (),
                                                                                                 aDocumentTypeIdentifier);
        DBServiceMetadataRedirection aDBRedirect = aEM.find (DBServiceMetadataRedirection.class, aDBRedirectID);

        if (aDBRedirect == null)
        {
          // Create a new one
          aDBRedirect = new DBServiceMetadataRedirection (aDBRedirectID, sTargetHref, sSubjectUniqueIdentifier, sExtension);
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
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return new SMPRedirect (aServiceGroup, aDocumentTypeIdentifier, sTargetHref, sSubjectUniqueIdentifier, sExtension);
  }

  @Nonnull
  public EChange deleteSMPRedirect (@Nullable final ISMPRedirect aSMPRedirect)
  {
    if (aSMPRedirect == null)
      return EChange.UNCHANGED;

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction (new Callable <EChange> ()
    {
      @Nonnull
      public EChange call ()
      {
        final EntityManager aEM = getEntityManager ();
        final DBServiceMetadataRedirectionID aDBRedirectID = new DBServiceMetadataRedirectionID (aSMPRedirect.getServiceGroup ()
                                                                                                             .getParticpantIdentifier (),
                                                                                                 aSMPRedirect.getDocumentTypeIdentifier ());
        final DBServiceMetadataRedirection aDBRedirect = aEM.find (DBServiceMetadataRedirection.class, aDBRedirectID);
        if (aDBRedirect == null)
          return EChange.UNCHANGED;

        aEM.remove (aDBRedirect);
        return EChange.CHANGED;
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ();
  }

  @Nonnull
  public EChange deleteAllSMPRedirectsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction (new Callable <EChange> ()
    {
      @Nonnull
      public EChange call ()
      {
        final int nCnt = getEntityManager ().createQuery ("DELETE FROM DBServiceMetadataRedirection p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value",
                                                          DBServiceMetadataRedirection.class)
                                            .setParameter ("scheme", aServiceGroup.getParticpantIdentifier ().getScheme ())
                                            .setParameter ("value", aServiceGroup.getParticpantIdentifier ().getValue ())
                                            .executeUpdate ();
        return EChange.valueOf (nCnt > 0);
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
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
  public Collection <? extends ISMPRedirect> getAllSMPRedirects ()
  {
    JPAExecutionResult <List <DBServiceMetadataRedirection>> ret;
    ret = doInTransaction (new Callable <List <DBServiceMetadataRedirection>> ()
    {
      @Nonnull
      public List <DBServiceMetadataRedirection> call ()
      {
        return getEntityManager ().createQuery ("SELECT p FROM DBServiceMetadataRedirection p", DBServiceMetadataRedirection.class).getResultList ();
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());

    final List <SMPRedirect> aRedirects = new ArrayList <> ();
    for (final DBServiceMetadataRedirection aDBRedirect : ret.get ())
      aRedirects.add (_convert (aDBRedirect));
    return aRedirects;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPRedirect> getAllSMPRedirectsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final List <SMPRedirect> aRedirects = new ArrayList <> ();
    if (aServiceGroup != null)
    {
      JPAExecutionResult <List <DBServiceMetadataRedirection>> ret;
      ret = doInTransaction (new Callable <List <DBServiceMetadataRedirection>> ()
      {
        public List <DBServiceMetadataRedirection> call ()
        {
          return getEntityManager ().createQuery ("SELECT p FROM DBServiceMetadataRedirection p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value",
                                                  DBServiceMetadataRedirection.class)
                                    .setParameter ("scheme", aServiceGroup.getParticpantIdentifier ().getScheme ())
                                    .setParameter ("value", aServiceGroup.getParticpantIdentifier ().getValue ())
                                    .getResultList ();
        }
      });
      if (ret.hasThrowable ())
        throw new RuntimeException (ret.getThrowable ());

      for (final DBServiceMetadataRedirection aDBRedirect : ret.get ())
        aRedirects.add (_convert (aDBRedirect));
    }
    return aRedirects;
  }

  @Nonnegative
  public int getSMPRedirectCount ()
  {
    JPAExecutionResult <Long> ret;
    ret = doSelect (new Callable <Long> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Long call () throws Exception
      {
        final long nCount = getSelectCountResult (getEntityManager ().createQuery ("SELECT COUNT(p.id) FROM DBServiceMetadataRedirection p"));
        return Long.valueOf (nCount);
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
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
    ret = doInTransaction (new Callable <DBServiceMetadataRedirection> ()
    {
      @Nonnull
      public DBServiceMetadataRedirection call ()
      {
        // Disable caching here
        final Map <String, Object> aProps = new HashMap <> ();
        aProps.put ("eclipselink.cache-usage", CacheUsage.DoNotCheckCache);
        final DBServiceMetadataRedirectionID aDBRedirectID = new DBServiceMetadataRedirectionID (aServiceGroup.getParticpantIdentifier (),
                                                                                                 aDocTypeID);
        return getEntityManager ().find (DBServiceMetadataRedirection.class, aDBRedirectID, aProps);
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
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
