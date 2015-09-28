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
package com.helger.peppol.smpserver.data.sql;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.persistence.EntityManager;
import javax.xml.ws.wsaddressing.W3CEndpointReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.annotation.Nonempty;
import com.helger.commons.annotation.ReturnsMutableCopy;
import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.callback.IThrowingCallable;
import com.helger.commons.callback.exception.LoggingExceptionCallback;
import com.helger.commons.debug.GlobalDebug;
import com.helger.commons.state.EChange;
import com.helger.db.jpa.IHasEntityManager;
import com.helger.db.jpa.JPAEnabledManager;
import com.helger.db.jpa.JPAExecutionResult;
import com.helger.peppol.identifier.DocumentIdentifierType;
import com.helger.peppol.identifier.IdentifierHelper;
import com.helger.peppol.identifier.ParticipantIdentifierType;
import com.helger.peppol.smp.EndpointType;
import com.helger.peppol.smp.ExtensionType;
import com.helger.peppol.smp.ProcessListType;
import com.helger.peppol.smp.ProcessType;
import com.helger.peppol.smp.RedirectType;
import com.helger.peppol.smp.SMPExtensionConverter;
import com.helger.peppol.smp.ServiceEndpointList;
import com.helger.peppol.smp.ServiceGroupType;
import com.helger.peppol.smp.ServiceInformationType;
import com.helger.peppol.smp.ServiceMetadataType;
import com.helger.peppol.smpserver.data.IDataManagerSPI;
import com.helger.peppol.smpserver.data.IDataUser;
import com.helger.peppol.smpserver.data.sql.model.DBEndpoint;
import com.helger.peppol.smpserver.data.sql.model.DBEndpointID;
import com.helger.peppol.smpserver.data.sql.model.DBOwnership;
import com.helger.peppol.smpserver.data.sql.model.DBOwnershipID;
import com.helger.peppol.smpserver.data.sql.model.DBProcess;
import com.helger.peppol.smpserver.data.sql.model.DBProcessID;
import com.helger.peppol.smpserver.data.sql.model.DBServiceGroup;
import com.helger.peppol.smpserver.data.sql.model.DBServiceGroupID;
import com.helger.peppol.smpserver.data.sql.model.DBServiceMetadata;
import com.helger.peppol.smpserver.data.sql.model.DBServiceMetadataID;
import com.helger.peppol.smpserver.data.sql.model.DBServiceMetadataRedirection;
import com.helger.peppol.smpserver.data.sql.model.DBServiceMetadataRedirectionID;
import com.helger.peppol.smpserver.data.sql.model.DBUser;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;
import com.helger.peppol.smpserver.exception.SMPServerException;
import com.helger.peppol.smpserver.exception.SMPUnauthorizedException;
import com.helger.peppol.smpserver.exception.SMPUnknownUserException;
import com.helger.peppol.smpserver.smlhook.IRegistrationHook;
import com.helger.peppol.smpserver.smlhook.RegistrationHookFactory;
import com.helger.peppol.utils.CertificateHelper;
import com.helger.peppol.utils.W3CEndpointReferenceHelper;
import com.helger.web.http.basicauth.BasicAuthClientCredentials;

/**
 * A EclipseLink based implementation of the {@link IDataManagerSPI} interface.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@IsSPIImplementation
public final class DBMSDataManager extends JPAEnabledManager implements IDataManagerSPI
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (DBMSDataManager.class);

  private final IRegistrationHook m_aHook;

  @Deprecated
  @UsedViaReflection
  public DBMSDataManager ()
  {
    this (RegistrationHookFactory.getOrCreateInstance ());
  }

  public DBMSDataManager (@Nonnull final IRegistrationHook aHook)
  {
    super (new IHasEntityManager ()
    {
      // This additional indirection level is required!!!
      // So that for every request the correct getInstance is invoked!
      @Nonnull
      public EntityManager getEntityManager ()
      {
        return SMPEntityManagerWrapper.getInstance ().getEntityManager ();
      }
    });

    // Exceptions are handled by logging them
    setCustomExceptionCallback (new LoggingExceptionCallback ());

    // To avoid some EclipseLink logging issues
    setUseTransactionsForSelect (true);

    m_aHook = ValueEnforcer.notNull (aHook, "Hook");
  }

  @Nonnull
  public DBUser getUserFromCredentials (@Nonnull final BasicAuthClientCredentials aCredentials) throws Throwable
  {
    JPAExecutionResult <DBUser> ret;
    ret = doSelect (new IThrowingCallable <DBUser, SMPServerException> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public DBUser call () throws SMPServerException
      {
        final String sUserName = aCredentials.getUserName ();
        final DBUser aDBUser = getEntityManager ().find (DBUser.class, sUserName);

        // Check that the user exists
        if (aDBUser == null)
          throw new SMPUnknownUserException (sUserName);

        // Check that the password is correct
        if (!aDBUser.getPassword ().equals (aCredentials.getPassword ()))
          throw new SMPUnauthorizedException ("Illegal password for user '" + sUserName + "'");

        if (s_aLogger.isDebugEnabled ())
          s_aLogger.debug ("Verified credentials of user '" + sUserName + "' successfully");
        return aDBUser;
      }
    });
    return ret.getOrThrow ();
  }

  @Nonnull
  public DBUser createPreAuthenticatedUser (@Nonnull @Nonempty final String sUserName)
  {
    final DBUser ret = doSelect (new Callable <DBUser> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public DBUser call ()
      {
        return getEntityManager ().find (DBUser.class, sUserName);
      }
    }).getIfSuccessOrNull ();
    if (ret == null)
      throw new IllegalArgumentException ("Invalid DB user '" + sUserName + "' provided!");
    return ret;
  }

  /**
   * Verify that the passed service group is owned by the user specified in the
   * credentials.
   *
   * @param aServiceGroupID
   *        The service group to be verified
   * @param aCredentials
   *        The credentials to be checked
   * @return The non-<code>null</code> ownership object
   * @throws SMPUnauthorizedException
   *         If the participant identifier is not owned by the user specified in
   *         the credentials
   */
  @Nonnull
  private DBOwnership _verifyOwnership (@Nonnull final ParticipantIdentifierType aServiceGroupID,
                                        @Nonnull final IDataUser aCredentials) throws SMPUnauthorizedException
  {
    final DBOwnershipID aOwnershipID = new DBOwnershipID (aCredentials.getUserName (), aServiceGroupID);
    final DBOwnership aOwnership = getEntityManager ().find (DBOwnership.class, aOwnershipID);
    if (aOwnership == null)
    {
      throw new SMPUnauthorizedException ("User '" +
                                          aCredentials.getUserName () +
                                          "' does not own " +
                                          IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID));
    }

    if (s_aLogger.isDebugEnabled ())
      s_aLogger.debug ("Verified service group ID " +
                       IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID) +
                       " is owned by user '" +
                       aCredentials.getUserName () +
                       "'");
    return aOwnership;
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <ParticipantIdentifierType> getServiceGroupList (@Nonnull final IDataUser aDataUser) throws Throwable
  {
    final DBUser aDBUser = (DBUser) aDataUser;

    JPAExecutionResult <Collection <ParticipantIdentifierType>> ret;
    ret = doSelect (new Callable <Collection <ParticipantIdentifierType>> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Collection <ParticipantIdentifierType> call () throws Exception
      {
        final List <DBOwnership> aDBOwnerships = getEntityManager ().createQuery ("SELECT p FROM DBOwnership p WHERE p.user = :user",
                                                                                  DBOwnership.class)
                                                                    .setParameter ("user", aDBUser)
                                                                    .getResultList ();

        final Collection <ParticipantIdentifierType> aList = new ArrayList <ParticipantIdentifierType> ();
        for (final DBOwnership aDBOwnership : aDBOwnerships)
        {
          final DBServiceGroupID aDBServiceGroupID = aDBOwnership.getServiceGroup ().getId ();
          aList.add (aDBServiceGroupID.asBusinessIdentifier ());
        }
        return aList;
      }
    });
    return ret.getOrThrow ();
  }

  @Nullable
  public ServiceGroupType getServiceGroup (@Nonnull final ParticipantIdentifierType aServiceGroupID) throws Throwable
  {
    JPAExecutionResult <ServiceGroupType> ret;
    ret = doInTransaction (new Callable <ServiceGroupType> ()
    {
      @Nullable
      public ServiceGroupType call () throws Exception
      {
        final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aServiceGroupID);
        final DBServiceGroup aDBServiceGroup = getEntityManager ().find (DBServiceGroup.class, aDBServiceGroupID);
        if (aDBServiceGroup == null)
        {
          s_aLogger.warn ("No such service group to retrieve: " +
                          IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID));
          return null;
        }

        // Convert service group DB to service group service
        final ServiceGroupType aServiceGroup = new ServiceGroupType ();
        aServiceGroup.setParticipantIdentifier (aServiceGroupID);
        aServiceGroup.setExtension (SMPExtensionConverter.convertOrNull (aDBServiceGroup.getExtension ()));
        // This is set by the REST interface:
        // ret.setServiceMetadataReferenceCollection(value)
        return aServiceGroup;
      }
    });
    return ret.getOrThrow ();
  }

  public void saveServiceGroup (@Nonnull final ServiceGroupType aServiceGroup,
                                @Nonnull final IDataUser aDataUser) throws Throwable
  {
    final DBUser aDBUser = (DBUser) aDataUser;

    JPAExecutionResult <?> ret;
    ret = doInTransaction (new Runnable ()
    {
      public void run ()
      {
        final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aServiceGroup.getParticipantIdentifier ());
        final DBOwnershipID aDBOwnershipID = new DBOwnershipID (aDBUser.getUserName (),
                                                                aServiceGroup.getParticipantIdentifier ());

        // Check if the passed service group ID is already in use
        final EntityManager aEM = getEntityManager ();
        DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, aDBServiceGroupID);

        if (aDBServiceGroup != null)
        {
          // The business did exist. So it must be owned by the passed user.
          if (aEM.find (DBOwnership.class, aDBOwnershipID) == null)
          {
            throw new SMPUnauthorizedException ("The passed service group " +
                                                IdentifierHelper.getIdentifierURIEncoded (aServiceGroup.getParticipantIdentifier ()) +
                                                " is not owned by '" +
                                                aDBUser.getUserName () +
                                                "'");
          }

          // Simply update the extension
          aDBServiceGroup.setExtension (aServiceGroup.getExtension ());
          aEM.merge (aDBServiceGroup);
        }
        else
        {
          // It's a new service group - throws exception in case of an error
          m_aHook.createServiceGroup (aServiceGroup.getParticipantIdentifier ());

          try
          {
            // Did not exist. Create it.
            aDBServiceGroup = new DBServiceGroup (aDBServiceGroupID);
            aDBServiceGroup.setExtension (aServiceGroup.getExtension ());
            aEM.persist (aDBServiceGroup);

            // Save the ownership information
            final DBOwnership aDBOwnership = new DBOwnership (aDBOwnershipID, aDBUser, aDBServiceGroup);
            aEM.persist (aDBOwnership);
          }
          catch (final RuntimeException ex)
          {
            // An error occurred - remove from SML again
            m_aHook.undoCreateServiceGroup (aServiceGroup.getParticipantIdentifier ());
            throw ex;
          }
        }
      }
    });
    if (ret.hasThrowable ())
      throw ret.getThrowable ();
  }

  public void deleteServiceGroup (@Nonnull final ParticipantIdentifierType aServiceGroupID,
                                  @Nonnull final IDataUser aDataUser) throws Throwable
  {
    JPAExecutionResult <EChange> ret;
    ret = doInTransaction (new Callable <EChange> ()
    {
      @Nonnull
      public EChange call ()
      {
        // Check if the service group is existing
        final EntityManager aEM = getEntityManager ();
        final DBServiceGroupID aDBServiceGroupID = new DBServiceGroupID (aServiceGroupID);
        final DBServiceGroup aDBServiceGroup = aEM.find (DBServiceGroup.class, aDBServiceGroupID);
        if (aDBServiceGroup == null)
        {
          s_aLogger.warn ("No such service group to delete: " +
                          IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID));
          return EChange.UNCHANGED;
        }

        // Check the ownership afterwards, so that only existing serviceGroups
        // are checked
        final DBOwnership aDBOwnership = _verifyOwnership (aServiceGroupID, aDataUser);

        // Delete in SML - throws exception in case of error
        m_aHook.deleteServiceGroup (aServiceGroupID);
        try
        {
          aEM.remove (aDBOwnership);
          aEM.remove (aDBServiceGroup);
          return EChange.CHANGED;
        }
        catch (final RuntimeException ex)
        {
          // An error occurred - remove from SML again
          m_aHook.undoDeleteServiceGroup (aServiceGroupID);
          throw ex;
        }
      }
    });
    if (ret.hasThrowable ())
      throw ret.getThrowable ();
    if (ret.get ().isUnchanged ())
      throw new SMPNotFoundException (IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID));
  }

  @Nonnull
  @ReturnsMutableCopy
  public List <DocumentIdentifierType> getDocumentTypes (@Nonnull final ParticipantIdentifierType aServiceGroupID) throws Throwable
  {
    JPAExecutionResult <List <DocumentIdentifierType>> ret;
    ret = doSelect (new Callable <List <DocumentIdentifierType>> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public List <DocumentIdentifierType> call () throws Exception
      {
        final List <DBServiceMetadata> aServices = getEntityManager ().createQuery ("SELECT p FROM DBServiceMetadata p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value",
                                                                                    DBServiceMetadata.class)
                                                                      .setParameter ("scheme",
                                                                                     aServiceGroupID.getScheme ())
                                                                      .setParameter ("value",
                                                                                     aServiceGroupID.getValue ())
                                                                      .getResultList ();

        final List <DocumentIdentifierType> aList = new ArrayList <DocumentIdentifierType> ();
        for (final DBServiceMetadata aService : aServices)
          aList.add (aService.getId ().asDocumentTypeIdentifier ());
        return aList;
      }
    });
    return ret.getOrThrow ();
  }

  @Nonnull
  @ReturnsMutableCopy
  public Collection <ServiceMetadataType> getServices (@Nonnull final ParticipantIdentifierType aServiceGroupID) throws Throwable
  {
    JPAExecutionResult <Collection <ServiceMetadataType>> ret;
    ret = doSelect (new Callable <Collection <ServiceMetadataType>> ()
    {
      @Nonnull
      @ReturnsMutableCopy
      public Collection <ServiceMetadataType> call () throws Exception
      {
        final List <DBServiceMetadata> aServices = getEntityManager ().createQuery ("SELECT p FROM DBServiceMetadata p WHERE p.id.businessIdentifierScheme = :scheme AND p.id.businessIdentifier = :value",
                                                                                    DBServiceMetadata.class)
                                                                      .setParameter ("scheme",
                                                                                     aServiceGroupID.getScheme ())
                                                                      .setParameter ("value",
                                                                                     aServiceGroupID.getValue ())
                                                                      .getResultList ();

        final List <ServiceMetadataType> aList = new ArrayList <ServiceMetadataType> ();
        for (final DBServiceMetadata aService : aServices)
        {
          final ServiceMetadataType aServiceMetadata = new ServiceMetadataType ();
          _convertFromDBToService (aService, aServiceMetadata);
          aList.add (aServiceMetadata);
        }
        return aList;
      }
    });
    return ret.getOrThrow ();
  }

  @Nullable
  public ServiceMetadataType getService (@Nonnull final ParticipantIdentifierType aServiceGroupID,
                                         @Nonnull final DocumentIdentifierType aDocTypeID) throws Throwable
  {
    JPAExecutionResult <ServiceMetadataType> ret;
    ret = doSelect (new Callable <ServiceMetadataType> ()
    {
      public ServiceMetadataType call () throws Exception
      {
        final DBServiceMetadataID aDBServiceMetadataID = new DBServiceMetadataID (aServiceGroupID, aDocTypeID);
        final DBServiceMetadata aDBServiceMetadata = getEntityManager ().find (DBServiceMetadata.class,
                                                                               aDBServiceMetadataID);

        if (aDBServiceMetadata == null)
        {
          s_aLogger.info ("Service metadata with ID " +
                          IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID) +
                          " / " +
                          IdentifierHelper.getIdentifierURIEncoded (aDocTypeID) +
                          " not found");
          return null;
        }

        final ServiceMetadataType serviceMetadata = new ServiceMetadataType ();
        _convertFromDBToService (aDBServiceMetadata, serviceMetadata);
        return serviceMetadata;
      }
    });
    return ret.getOrThrow ();
  }

  public void saveService (@Nonnull final ServiceInformationType aServiceMetadata,
                           @Nonnull final IDataUser aDataUser) throws Throwable
  {
    final ParticipantIdentifierType aServiceGroupID = aServiceMetadata.getParticipantIdentifier ();
    final DocumentIdentifierType aDocTypeID = aServiceMetadata.getDocumentIdentifier ();

    _verifyOwnership (aServiceGroupID, aDataUser);

    // Delete an eventually contained previous service in a separate transaction
    _deleteService (aServiceGroupID, aDocTypeID);

    // Create a new entry
    JPAExecutionResult <?> ret;
    ret = doInTransaction (new Runnable ()
    {
      public void run ()
      {
        final EntityManager aEM = getEntityManager ();

        // Check if an existing service is already contained
        // This should have been deleted previously!
        final DBServiceMetadataID aDBServiceMetadataID = new DBServiceMetadataID (aServiceGroupID, aDocTypeID);
        DBServiceMetadata aDBServiceMetadata = aEM.find (DBServiceMetadata.class, aDBServiceMetadataID);
        if (aDBServiceMetadata != null)
          throw new IllegalStateException ("No DB ServiceMeta data with ID " +
                                           IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID) +
                                           " should be present!");

        // Create a new entry
        aDBServiceMetadata = new DBServiceMetadata ();
        aDBServiceMetadata.setId (aDBServiceMetadataID);
        _convertFromServiceToDB (aServiceMetadata, aDBServiceMetadata);
        aEM.persist (aDBServiceMetadata);

        // For all processes
        for (final DBProcess aDBProcess : aDBServiceMetadata.getProcesses ())
        {
          aEM.persist (aDBProcess);

          // For all endpoints
          for (final DBEndpoint aDBEndpoint : aDBProcess.getEndpoints ())
            aEM.persist (aDBEndpoint);
        }
      }
    });
    if (ret.hasThrowable ())
      throw ret.getThrowable ();
  }

  @Nonnull
  private EChange _deleteService (@Nonnull final ParticipantIdentifierType aServiceGroupID,
                                  @Nonnull final DocumentIdentifierType aDocTypeID) throws Throwable
  {
    JPAExecutionResult <EChange> ret;
    ret = doInTransaction (new Callable <EChange> ()
    {
      public EChange call ()
      {
        final EntityManager aEM = getEntityManager ();

        final DBServiceMetadataID aDBServiceMetadataID = new DBServiceMetadataID (aServiceGroupID, aDocTypeID);
        final DBServiceMetadata aDBServiceMetadata = aEM.find (DBServiceMetadata.class, aDBServiceMetadataID);
        if (aDBServiceMetadata == null)
        {
          // There were no service to delete.
          s_aLogger.warn ("No such service to delete: " +
                          IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID) +
                          " / " +
                          IdentifierHelper.getIdentifierURIEncoded (aDocTypeID));
          return EChange.UNCHANGED;
        }

        // Remove all attached processes incl. their endpoints
        for (final DBProcess aDBProcess : aDBServiceMetadata.getProcesses ())
        {
          // First endpoints
          for (final DBEndpoint aDBEndpoint : aDBProcess.getEndpoints ())
            aEM.remove (aDBEndpoint);

          // Than process
          aEM.remove (aDBProcess);
        }

        // Remove main service data
        aEM.remove (aDBServiceMetadata);
        return EChange.CHANGED;
      }
    });
    return ret.getOrThrow ();
  }

  public void deleteService (@Nonnull final ParticipantIdentifierType aServiceGroupID,
                             @Nonnull final DocumentIdentifierType aDocTypeID,
                             @Nonnull final IDataUser aDataUser) throws Throwable
  {
    _verifyOwnership (aServiceGroupID, aDataUser);

    final EChange eChange = _deleteService (aServiceGroupID, aDocTypeID);
    if (eChange.isUnchanged ())
      throw new SMPNotFoundException (IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID) +
                                      " / " +
                                      IdentifierHelper.getIdentifierURIEncoded (aDocTypeID));
  }

  @Nullable
  public ServiceMetadataType getRedirection (@Nonnull final ParticipantIdentifierType aServiceGroupID,
                                             @Nonnull final DocumentIdentifierType aDocTypeID) throws Throwable
  {
    JPAExecutionResult <ServiceMetadataType> ret;
    ret = doSelect (new Callable <ServiceMetadataType> ()
    {
      @Nullable
      public ServiceMetadataType call () throws Exception
      {
        final DBServiceMetadataRedirectionID aDBRedirectID = new DBServiceMetadataRedirectionID (aServiceGroupID,
                                                                                                 aDocTypeID);
        final DBServiceMetadataRedirection aDBServiceMetadataRedirection = getEntityManager ().find (DBServiceMetadataRedirection.class,
                                                                                                     aDBRedirectID);

        if (aDBServiceMetadataRedirection == null)
        {
          if (GlobalDebug.isDebugMode ())
            s_aLogger.info ("No redirection service group id: " +
                            IdentifierHelper.getIdentifierURIEncoded (aServiceGroupID));
          return null;
        }

        // First check whether an redirect exists.
        final ServiceMetadataType aServiceMetadata = new ServiceMetadataType ();

        // Then return a redirect instead.
        final RedirectType aRedirect = new RedirectType ();
        aRedirect.setCertificateUID (aDBServiceMetadataRedirection.getCertificateUid ());
        aRedirect.setHref (aDBServiceMetadataRedirection.getRedirectionUrl ());
        aRedirect.setExtension (SMPExtensionConverter.convertOrNull (aDBServiceMetadataRedirection.getExtension ()));
        aServiceMetadata.setRedirect (aRedirect);

        return aServiceMetadata;
      }
    });
    return ret.getOrThrow ();
  }

  private void _convertFromDBToService (@Nonnull final DBServiceMetadata aDBServiceMetadata,
                                        @Nonnull final ServiceMetadataType aServiceMetadata)
  {
    final ParticipantIdentifierType aBusinessID = aDBServiceMetadata.getId ().asBusinessIdentifier ();
    final ExtensionType aExtension = SMPExtensionConverter.convertOrNull (aDBServiceMetadata.getExtension ());

    final DocumentIdentifierType aDocTypeID = aDBServiceMetadata.getId ().asDocumentTypeIdentifier ();

    final ServiceInformationType aServiceInformation = new ServiceInformationType ();
    aServiceInformation.setParticipantIdentifier (aBusinessID);
    // serviceInformationType.setCertificateUID(serviceMetadataDB.g));
    aServiceInformation.setExtension (aExtension);
    aServiceInformation.setDocumentIdentifier (aDocTypeID);

    aServiceMetadata.setServiceInformation (aServiceInformation);

    final ProcessListType aProcessList = new ProcessListType ();
    for (final DBProcess aDBProcess : aDBServiceMetadata.getProcesses ())
    {
      final ProcessType aProcessType = new ProcessType ();

      final ServiceEndpointList endpoints = new ServiceEndpointList ();
      for (final DBEndpoint aDBEndpoint : aDBProcess.getEndpoints ())
      {
        final EndpointType aEndpointType = new EndpointType ();

        aEndpointType.setTransportProfile (aDBEndpoint.getId ().getTransportProfile ());
        aEndpointType.setExtension (SMPExtensionConverter.convertOrNull (aDBEndpoint.getExtension ()));

        final W3CEndpointReference endpointRef = W3CEndpointReferenceHelper.createEndpointReference (aDBEndpoint.getId ()
                                                                                                                .getEndpointReference ());
        aEndpointType.setEndpointReference (endpointRef);

        aEndpointType.setServiceActivationDate (aDBEndpoint.getServiceActivationDate ());
        aEndpointType.setServiceDescription (aDBEndpoint.getServiceDescription ());
        aEndpointType.setServiceExpirationDate (aDBEndpoint.getServiceExpirationDate ());
        aEndpointType.setTechnicalContactUrl (aDBEndpoint.getTechnicalContactUrl ());
        aEndpointType.setTechnicalInformationUrl (aDBEndpoint.getTechnicalInformationUrl ());
        aEndpointType.setCertificate (CertificateHelper.getRFC1421CompliantString (aDBEndpoint.getCertificate ()));
        aEndpointType.setMinimumAuthenticationLevel (aDBEndpoint.getMinimumAuthenticationLevel ());
        aEndpointType.setRequireBusinessLevelSignature (aDBEndpoint.isRequireBusinessLevelSignature ());

        endpoints.getEndpoint ().add (aEndpointType);
      }

      aProcessType.setServiceEndpointList (endpoints);
      aProcessType.setExtension (SMPExtensionConverter.convertOrNull (aDBProcess.getExtension ()));
      aProcessType.setProcessIdentifier (aDBProcess.getId ().asProcessIdentifier ());

      aProcessList.getProcess ().add (aProcessType);
    }

    aServiceInformation.setProcessList (aProcessList);
  }

  private static void _convertFromServiceToDB (@Nonnull final ServiceInformationType aServiceInformation,
                                               @Nonnull final DBServiceMetadata aDBServiceMetadata)
  {
    // Update it.
    aDBServiceMetadata.setExtension (aServiceInformation.getExtension ());

    final Set <DBProcess> aDBProcesses = new HashSet <DBProcess> ();
    for (final ProcessType aProcess : aServiceInformation.getProcessList ().getProcess ())
    {
      final DBProcessID aDBProcessID = new DBProcessID (aDBServiceMetadata.getId (), aProcess.getProcessIdentifier ());
      final DBProcess aDBProcess = new DBProcess (aDBProcessID);

      final Set <DBEndpoint> aDBEndpoints = new HashSet <DBEndpoint> ();
      for (final EndpointType aEndpoint : aProcess.getServiceEndpointList ().getEndpoint ())
      {
        final DBEndpointID aDBEndpointID = new DBEndpointID (aDBProcessID,
                                                             W3CEndpointReferenceHelper.getAddress (aEndpoint.getEndpointReference ()),
                                                             aEndpoint.getTransportProfile ());

        final DBEndpoint aDBEndpoint = new DBEndpoint ();
        aDBEndpoint.setExtension (aEndpoint.getExtension ());
        aDBEndpoint.setId (aDBEndpointID);
        aDBEndpoint.setServiceActivationDate (aEndpoint.getServiceActivationDate ());
        aDBEndpoint.setServiceDescription (aEndpoint.getServiceDescription ());
        aDBEndpoint.setServiceExpirationDate (aEndpoint.getServiceExpirationDate ());
        aDBEndpoint.setTechnicalContactUrl (aEndpoint.getTechnicalContactUrl ());
        aDBEndpoint.setTechnicalInformationUrl (aEndpoint.getTechnicalInformationUrl ());
        aDBEndpoint.setCertificate (aEndpoint.getCertificate ());
        aDBEndpoint.setMinimumAuthenticationLevel (aEndpoint.getMinimumAuthenticationLevel ());
        aDBEndpoint.setRequireBusinessLevelSignature (aEndpoint.isRequireBusinessLevelSignature ());

        aDBEndpoints.add (aDBEndpoint);
      }

      aDBProcess.setEndpoints (aDBEndpoints);
      aDBProcess.setExtension (aProcess.getExtension ());

      aDBProcesses.add (aDBProcess);
    }

    aDBServiceMetadata.setProcesses (aDBProcesses);
  }
}
