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
import java.util.concurrent.Callable;

import javax.annotation.Nonnegative;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.state.EChange;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.peppol.identifier.IDocumentTypeIdentifier;
import com.helger.peppol.identifier.doctype.IPeppolDocumentTypeIdentifier;
import com.helger.peppol.identifier.process.IPeppolProcessIdentifier;
import com.helger.peppol.smp.ISMPTransportProfile;
import com.helger.peppol.smpserver.data.sql.AbstractSMPJPAEnabledManager;
import com.helger.peppol.smpserver.data.sql.model.DBEndpoint;
import com.helger.peppol.smpserver.data.sql.model.DBProcess;
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
  private final Map <String, SMPServiceInformation> m_aMap = new HashMap <String, SMPServiceInformation> ();

  public SQLServiceInformationManager ()
  {}

  @Nonnull
  private ISMPServiceInformation _createSMPServiceInformation (@Nonnull final SMPServiceInformation aSMPServiceInformation)
  {
    ValueEnforcer.notNull (aSMPServiceInformation, "SMPServiceInformation");

    final String sSMPServiceInformationID = aSMPServiceInformation.getID ();
    if (m_aMap.containsKey (sSMPServiceInformationID))
      throw new IllegalArgumentException ("SMPServiceInformation ID '" +
                                          sSMPServiceInformationID +
                                          "' is already in use!");
    m_aMap.put (aSMPServiceInformation.getID (), aSMPServiceInformation);
    return aSMPServiceInformation;
  }

  @Nonnull
  private ISMPServiceInformation _updateSMPServiceInformation (@Nonnull final ISMPServiceInformation aSMPServiceInformation)
  {
    return aSMPServiceInformation;
  }

  public void markSMPServiceInformationChanged (@Nonnull final ISMPServiceInformation aServiceInfo)
  {
    ValueEnforcer.notNull (aServiceInfo, "ServiceInfo");

    JPAExecutionResult <?> ret;
    ret = doInTransaction (new Runnable ()
    {
      public void run ()
      {
        // TODO
      }
    });
    if (ret.hasThrowable ())
      throw new RuntimeException (ret.getThrowable ());
  }

  public void createOrUpdateSMPServiceInformation (@Nonnull final SMPServiceInformation aServiceInformation)
  {
    ValueEnforcer.notNull (aServiceInformation, "ServiceInformation");
    ValueEnforcer.isTrue (aServiceInformation.getProcessCount () == 1, "ServiceGroup must contain a single process");
    final SMPProcess aNewProcess = aServiceInformation.getAllProcesses ().get (0);
    ValueEnforcer.isTrue (aNewProcess.getEndpointCount () == 1,
                          "ServiceGroup must contain a single endpoint in the process");

    // Check for an update
    boolean bChangedExisting = false;
    final SMPServiceInformation aOldInformation = (SMPServiceInformation) getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceInformation.getServiceGroup (),
                                                                                                                                 aServiceInformation.getDocumentTypeIdentifier ());
    if (aOldInformation != null)
    {
      final SMPProcess aOldProcess = aOldInformation.getProcessOfID (aNewProcess.getProcessIdentifier ());
      if (aOldProcess != null)
      {
        final SMPEndpoint aNewEndpoint = aNewProcess.getAllEndpoints ().get (0);
        final ISMPEndpoint aOldEndpoint = aOldProcess.getEndpointOfTransportProfile (aNewEndpoint.getTransportProfile ());
        if (aOldEndpoint != null)
        {
          // Overwrite existing endpoint
          aOldProcess.setEndpoint (aNewEndpoint.getTransportProfile (), aNewEndpoint);
        }
        else
        {
          // Add endpoint to existing process
          aOldProcess.addEndpoint (aNewEndpoint);
        }
      }
      else
      {
        // Add process to existing service information
        aOldInformation.addProcess (aNewProcess);
      }
      bChangedExisting = true;
    }

    if (bChangedExisting)
      _updateSMPServiceInformation (aOldInformation);

    _createSMPServiceInformation (aServiceInformation);
  }

  @Nullable
  public ISMPServiceInformation findServiceInformation (@Nullable final ISMPServiceGroup aServiceGroup,
                                                        @Nullable final IPeppolDocumentTypeIdentifier aDocTypeID,
                                                        @Nullable final IPeppolProcessIdentifier aProcessID,
                                                        @Nullable final ISMPTransportProfile aTransportProfile)
  {
    final ISMPServiceInformation aOldInformation = getSMPServiceInformationOfServiceGroupAndDocumentType (aServiceGroup,
                                                                                                          aDocTypeID);
    if (aOldInformation != null)
    {
      final ISMPProcess aProcess = aOldInformation.getProcessOfID (aProcessID);
      if (aProcess != null)
      {
        final ISMPEndpoint aEndpoint = aProcess.getEndpointOfTransportProfile (aTransportProfile);
        if (aEndpoint != null)
          return aOldInformation;
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
                                                       aDBEndpoint.getId ().getEndpointReference (),
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

    final List <SMPServiceInformation> aServiceInformations = new ArrayList <> ();
    for (final DBServiceMetadata aDBMetadata : ret.get ())
      aServiceInformations.add (_convert (aDBMetadata));
    return aServiceInformations;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <IDocumentTypeIdentifier> getAllSMPDocumentTypesOfServiceGroup (@Nullable final ISMPServiceGroup aServiceGroup)
  {
    final Collection <IDocumentTypeIdentifier> ret = new ArrayList <> ();
    if (aServiceGroup != null)
    {
      for (final ISMPServiceInformation aServiceInformation : m_aMap.values ())
        if (aServiceInformation.getServiceGroupID ().equals (aServiceGroup.getID ()))
          ret.add (aServiceInformation.getDocumentTypeIdentifier ());
    }
    return ret;
  }

  @Nullable
  public ISMPServiceInformation getSMPServiceInformationOfServiceGroupAndDocumentType (@Nullable final ISMPServiceGroup aServiceGroup,
                                                                                       @Nullable final IDocumentTypeIdentifier aDocumentTypeIdentifier)
  {
    if (aServiceGroup == null)
      return null;
    if (aDocumentTypeIdentifier == null)
      return null;

    final List <ISMPServiceInformation> ret = new ArrayList <ISMPServiceInformation> ();

    for (final ISMPServiceInformation aServiceInformation : m_aMap.values ())
      if (aServiceInformation.getServiceGroupID ().equals (aServiceGroup.getID ()) &&
          aServiceInformation.getDocumentTypeIdentifier ().equals (aDocumentTypeIdentifier))
      {
        ret.add (aServiceInformation);
      }

    if (ret.isEmpty ())
      return null;
    if (ret.size () > 1)
      s_aLogger.warn ("Found more than one entry for service group '" +
                      aServiceGroup.getID () +
                      "' and document type '" +
                      aDocumentTypeIdentifier.getValue () +
                      "'. This seems to be a bug! Using the first one.");
    return ret.get (0);
  }
}
