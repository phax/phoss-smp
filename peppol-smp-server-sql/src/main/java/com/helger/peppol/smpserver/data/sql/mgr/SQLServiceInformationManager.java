/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
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
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.collection.CollectionHelper;
import com.helger.commons.state.EChange;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.data.sql.AbstractSMPJPAEnabledManager;
import com.helger.peppol.smpserver.data.sql.model.DBEndpoint;
import com.helger.peppol.smpserver.data.sql.model.DBEndpointID;
import com.helger.peppol.smpserver.data.sql.model.DBProcess;
import com.helger.peppol.smpserver.data.sql.model.DBProcessID;
import com.helger.peppol.smpserver.data.sql.model.DBServiceGroup;
import com.helger.peppol.smpserver.data.sql.model.DBServiceGroupID;
import com.helger.peppol.smpserver.data.sql.model.DBServiceMetadata;
import com.helger.peppol.smpserver.data.sql.model.DBServiceMetadataID;
import com.helger.peppol.smpserver.data.sql.model.DBServiceMetadataRedirection;
import com.helger.peppol.smpserver.domain.MetaManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformation;
import com.helger.peppol.smpserver.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPEndpoint;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPProcess;
import com.helger.peppol.smpserver.domain.serviceinfo.SMPServiceInformation;

/**
 * Manager for all {@link SMPServiceInformation} objects.
 *
 * @author Philip Helger
 */
public final class SQLServiceInformationManager extends AbstractSMPJPAEnabledManager implements ISMPServiceInformationManager
{
  public SQLServiceInformationManager ()
  {}

  private static void _update (@Nonnull final DBServiceMetadata aDBMetadata,
                               @Nonnull final ISMPServiceInformation aServiceInfo)
  {
    final ISMPProcess aProcess = aServiceInfo.getAllProcesses ().get (0);
    final ISMPEndpoint aEndpoint = aProcess.getAllEndpoints ().get (0);

    final Set <DBProcess> aDBProcesses = aDBMetadata.getProcesses ();

    DBProcess aUpdateDBProcess = null;
    DBEndpoint aUpdateDBEndpoint = null;
    for (final DBProcess aDBProcess : aDBProcesses)
      if (IdentifierHelper.areProcessIdentifiersEqual (aDBProcess.getId ().getAsProcessIdentifier (),
                                                       aProcess.getProcessIdentifier ()))
      {
        final Set <DBEndpoint> aDBEndpoints = aDBProcess.getEndpoints ();
        for (final DBEndpoint aDBEndpoint : aDBEndpoints)
          if (aDBEndpoint.getId ().getTransportProfile ().equals (aEndpoint.getTransportProfile ()))
          {
            aDBEndpoint.setEndpointReference (aEndpoint.getEndpointReference ());
            aDBEndpoint.setRequireBusinessLevelSignature (aEndpoint.isRequireBusinessLevelSignature ());
            aDBEndpoint.setMinimumAuthenticationLevel (aEndpoint.getMinimumAuthenticationLevel ());
            aDBEndpoint.setServiceActivationDate (aEndpoint.getServiceActivationDateTime ());
            aDBEndpoint.setServiceExpirationDate (aEndpoint.getServiceExpirationDateTime ());
            aDBEndpoint.setCertificate (aEndpoint.getCertificate ());
            aDBEndpoint.setServiceDescription (aEndpoint.getServiceDescription ());
            aDBEndpoint.setTechnicalContactUrl (aEndpoint.getTechnicalContactUrl ());
            aDBEndpoint.setTechnicalInformationUrl (aEndpoint.getTechnicalInformationUrl ());
            aUpdateDBEndpoint = aDBEndpoint;
            break;
          }
        if (aUpdateDBEndpoint == null)
        {
          // Create a new endpoint
          final DBEndpoint aDBEndpoint = new DBEndpoint (new DBEndpointID (aDBProcess.getId (),
                                                                           aEndpoint.getTransportProfile ()),
                                                         aDBProcess,
                                                         aEndpoint.getEndpointReference (),
                                                         aEndpoint.isRequireBusinessLevelSignature (),
                                                         aEndpoint.getMinimumAuthenticationLevel (),
                                                         aEndpoint.getServiceActivationDateTime (),
                                                         aEndpoint.getServiceExpirationDateTime (),
                                                         aEndpoint.getCertificate (),
                                                         aEndpoint.getServiceDescription (),
                                                         aEndpoint.getTechnicalContactUrl (),
                                                         aEndpoint.getTechnicalInformationUrl (),
                                                         aEndpoint.getExtension ());
          aDBEndpoints.add (aDBEndpoint);
        }

        aDBProcess.setServiceMetadata (aDBMetadata);
        aDBProcess.setEndpoints (aDBEndpoints);
        aDBProcess.setExtension (aProcess.getExtension ());
        aUpdateDBProcess = aDBProcess;
        break;
      }

    if (aUpdateDBProcess == null)
    {
      // Create a new process with a new endpoint
      final DBProcess aDBProcess = new DBProcess (new DBProcessID (aDBMetadata.getId (),
                                                                   aProcess.getProcessIdentifier ()),
                                                  aDBMetadata,
                                                  aProcess.getExtension ());
      final DBEndpoint aDBEndpoint = new DBEndpoint (new DBEndpointID (aDBProcess.getId (),
                                                                       aEndpoint.getTransportProfile ()),
                                                     aDBProcess,
                                                     aEndpoint.getEndpointReference (),
                                                     aEndpoint.isRequireBusinessLevelSignature (),
                                                     aEndpoint.getMinimumAuthenticationLevel (),
                                                     aEndpoint.getServiceActivationDateTime (),
                                                     aEndpoint.getServiceExpirationDateTime (),
                                                     aEndpoint.getCertificate (),
                                                     aEndpoint.getServiceDescription (),
                                                     aEndpoint.getTechnicalContactUrl (),
                                                     aEndpoint.getTechnicalInformationUrl (),
                                                     aEndpoint.getExtension ());
      aDBProcess.setEndpoints (CollectionHelper.newSet (aDBEndpoint));
      aDBProcesses.add (aDBProcess);
    }

    aDBMetadata.setProcesses (aDBProcesses);
    aDBMetadata.setExtension (aServiceInfo.getExtension ());
  }

  public void createOrUpdateSMPServiceInformation (@Nonnull final SMPServiceInformation aServiceInformation)
  {
    ValueEnforcer.notNull (aServiceInformation, "ServiceInformation");
    ValueEnforcer.isTrue (aServiceInformation.getProcessCount () == 1, "ServiceGroup must contain a single process");
    final SMPProcess aNewProcess = aServiceInformation.getAllProcesses ().get (0);
    ValueEnforcer.isTrue (aNewProcess.getEndpointCount () == 1,
                          "ServiceGroup must contain a single endpoint in the process");

    JPAExecutionResult <DBServiceMetadata> ret;
    ret = doInTransaction (new Callable <DBServiceMetadata> ()
    {
      @Nonnull
      public DBServiceMetadata call ()
      {
        final EntityManager aEM = getEntityManager ();
        final DBServiceMetadataID aDBMetadataID = new DBServiceMetadataID (aServiceInformation.getServiceGroup ()
                                                                                              .getParticpantIdentifier (),
                                                                           aServiceInformation.getDocumentTypeIdentifier ());
        DBServiceMetadata aDBMetadata = aEM.find (DBServiceMetadata.class, aDBMetadataID);
        if (aDBMetadata != null)
        {
          // Edit an existing one
          _update (aDBMetadata, aServiceInformation);
          aEM.merge (aDBMetadata);
        }
        else
        {
          // Create a new one
          final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aServiceInformation.getServiceGroup ()
                                                                                              .getParticpantIdentifier ());
          final DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, aDBServiceGroupID);
          if (aDBServiceGroup == null)
            throw new IllegalStateException ("Failed to resolve service group for " + aServiceInformation);

          aDBMetadata = new DBServiceMetadata (aDBMetadataID, aDBServiceGroup, aServiceInformation.getExtension ());
          _update (aDBMetadata, aServiceInformation);
          aEM.persist (aDBMetadata);
        }
        return aDBMetadata;
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
  }

  @Nullable
  public ISMPServiceInformation findServiceInformation (@Nullable final ISMPServiceGroup aServiceGroup,
                                                        @Nullable final IPeppolDocumentTypeIdentifier aDocTypeID,
                                                        @Nullable final IPeppolProcessIdentifier aProcessID,
                                                        @Nullable final ISMPTransportProfile aTransportProfile)
  {
    final ISMPServiceInformation aServiceInfo = getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                       aDocTypeID);
    if (aServiceInfo != null)
    {
      final ISMPProcess aProcess = aServiceInfo.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        final ISMPEndpoint aEndpoint = aProcess.getEndpointOfTransportProfile (aTransportProfile);
        if (aEndpoint != null)
          return aServiceInfo;
      }
    }
    return null;
  }

  @Nonnull
  public EChange deleteSMPServiceInformation (@Nullable final ISMPServiceInformation aSMPServiceInformation)
  {
    if (aSMPServiceInformation == null)
      return EChange.UNCHANGED;

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction (new Callable <EChange> ()
    {
      @Nonnull
      public EChange call ()
      {
        final EntityManager aEM = getEntityManager ();
        final DBServiceMetadataID aDBMetadataID = new DBServiceMetadataID (aSMPServiceInformation.getServiceGroup ()
                                                                                                 .getParticpantIdentifier (),
                                                                           aSMPServiceInformation.getDocumentTypeIdentifier ());
        final DBServiceMetadata aDBMetadata = aEM.find (DBServiceMetadata.class, aDBMetadataID);
        if (aDBMetadata == null)
          return EChange.UNCHANGED;

        aEM.remove (aDBMetadata);
        return EChange.CHANGED;
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ();
  }

  @Nonnull
  public EChange deleteAllSMPServiceInformationOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    if (aServiceGroup == null)
      return EChange.UNCHANGED;

    JPAExecutionResult <EChange> ret;
    ret = doInTransaction (new Callable <EChange> ()
    {
      @Nonnull
      public EChange call ()
      {
        final int nCnt = getEntityManager ().createQuery ("DELETE FROM DBServiceMetadata p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value",
                                                          DBServiceMetadataRedirection.class)
                                            .setParameter ("scheme",
                                                           aServiceGroup.getParticpantIdentifier ().getScheme ())
                                            .setParameter ("value",
                                                           aServiceGroup.getParticpantIdentifier ().getValue ())
                                            .executeUpdate ();
        return EChange.valueOf (nCnt > 0);
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ();
  }

  @Nonnull
  private static SMPServiceInformation _convert (@Nonnull final DBServiceMetadata aDBMetadata)
  {
    final ISMPServiceGroupManager aServiceGroupMgr = MetaManager.getServiceGroupMgr ();
    final List <SMPProcess> aProcesses = new ArrayList <> ();
    for (final DBProcess aDBProcess : aDBMetadata.getProcesses ())
    {
      final List <SMPEndpoint> aEndpoints = new ArrayList <> ();
      for (final DBEndpoint aDBEndpoint : aDBProcess.getEndpoints ())
      {
        final SMPEndpoint aEndpoint = new SMPEndpoint (aDBEndpoint.getId ().getTransportProfile (),
                                                       aDBEndpoint.getEndpointReference (),
                                                       aDBEndpoint.isRequireBusinessLevelSignature (),
                                                       aDBEndpoint.getMinimumAuthenticationLevel (),
                                                       aDBEndpoint.getServiceActivationDate (),
                                                       aDBEndpoint.getServiceExpirationDate (),
                                                       aDBEndpoint.getCertificate (),
                                                       aDBEndpoint.getServiceDescription (),
                                                       aDBEndpoint.getTechnicalContactUrl (),
                                                       aDBEndpoint.getTechnicalInformationUrl (),
                                                       aDBEndpoint.getExtension ());
        aEndpoints.add (aEndpoint);
      }
      final SMPProcess aProcess = new SMPProcess (aDBProcess.getId ().getAsProcessIdentifier (),
                                                  aEndpoints,
                                                  aDBProcess.getExtension ());
      aProcesses.add (aProcess);
    }
    return new SMPServiceInformation (aServiceGroupMgr.getSMPServiceGroupOfID (aDBMetadata.getId ()
                                                                                          .getAsBusinessIdentifier ()),
                                      aDBMetadata.getId ().getAsDocumentTypeIdentifier (),
                                      aProcesses,
                                      aDBMetadata.getExtension ());
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceInformation> getAllSMPServiceInformations ()
  {
    JPAExecutionResult <List <DBServiceMetadata>> ret;
    ret = doInTransaction (new Callable <List <DBServiceMetadata>> ()
    {
      @Nonnull
      public List <DBServiceMetadata> call ()
      {
        return getEntityManager ().createQuery ("SELECT p FROM DBServiceMetadata p", DBServiceMetadata.class)
                                  .getResultList ();
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());

    final List <SMPServiceInformation> aServiceInformations = new ArrayList <> ();
    for (final DBServiceMetadata aDBMetadata : ret.get ())
      aServiceInformations.add (_convert (aDBMetadata));
    return aServiceInformations;
  }

  @Nonnegative
  public int getSMPServiceInformationCount ()
  {
    JPAExecutionResult <Long> ret;
    ret = doSelect (new Callable <Long> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Long call () throws Exception
      {
        final long nCount = getSelectCountResult (getEntityManager ().createQuery ("SELECT COUNT(p.id) FROM DBServiceMetadata p"));
        return Long.valueOf (nCount);
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ().intValue ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <? extends ISMPServiceInformation> getAllSMPServiceInformationsOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final List <SMPServiceInformation> aServiceInformations = new ArrayList <> ();
    if (aServiceGroup != null)
    {
      JPAExecutionResult <List <DBServiceMetadata>> ret;
      ret = doInTransaction (new Callable <List <DBServiceMetadata>> ()
      {
        public List <DBServiceMetadata> call ()
        {
          return getEntityManager ().createQuery ("SELECT p FROM DBServiceMetadata p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value",
                                                  DBServiceMetadata.class)
                                    .setParameter ("scheme", aServiceGroup.getParticpantIdentifier ().getScheme ())
                                    .setParameter ("value", aServiceGroup.getParticpantIdentifier ().getValue ())
                                    .getResultList ();
        }
      });
      if (ret.hasThrowable ())
        throw new RuntimeException (ret.getThrowable ());

      for (final DBServiceMetadata aDBMetadata : ret.get ())
        aServiceInformations.add (_convert (aDBMetadata));
    }
    return aServiceInformations;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final Collection <IDocumentTypeIdentifier> ret = new ArrayList <> ();
    if (aServiceGroup != null)
    {
      for (final ISMPServiceInformation aServiceInformation : getAllSMPServiceInformationsOfServiceGroup (aServiceGroup))
        ret.add (aServiceInformation.getDocumentTypeIdentifier ());
    }
    return ret;
  }

  @Nullable
  private DBServiceMetadata _getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                                    @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    if (aServiceGroup == null || aDocTypeID == null)
      return null;

    JPAExecutionResult <DBServiceMetadata> ret;
    ret = doInTransaction (new Callable <DBServiceMetadata> ()
    {
      @Nonnull
      public DBServiceMetadata call ()
      {
        final DBServiceMetadataID aDBMetadataID = new DBServiceMetadataID (aServiceGroup.getParticpantIdentifier (),
                                                                           aDocTypeID);
        return getEntityManager ().find (DBServiceMetadata.class, aDBMetadataID);
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
    return ret.get ();
  }

  @Nullable
  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                                       @Nullable final IDocumentTypeIdentifier aDocTypeID)
  {
    final DBServiceMetadata aDBMetadata = _getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                  aDocTypeID);
    if (aDBMetadata == null)
      return null;
    return _convert (aDBMetadata);
  }
}
