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
package com.helger.phoss.smp.restapi;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.Nonnegative;
import com.helger.annotation.style.ReturnsMutableCopy;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPoint;
import com.helger.phoss.smp.domain.accesspoint.ISMPAccessPointManager;
import com.helger.phoss.smp.domain.accesspoint.SMPAccessPointHelper;
import com.helger.phoss.smp.domain.serviceinfo.ISMPServiceInformationManager;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPPreconditionFailedException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.photon.security.user.IUser;
import com.helger.statistics.api.IMutableStatisticsHandlerKeyedCounter;
import com.helger.statistics.impl.StatisticsManager;

/**
 * This class implements all the service methods that are provided by the Access Point management
 * REST service. Access Points are global objects that are not owned by a specific Service Group,
 * therefore all operations may only be performed by administrators.
 *
 * @author Philip Helger
 * @since 8.4.4
 */
public final class AccessPointServerAPI
{
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_INVOCATION = StatisticsManager.getKeyedCounterHandler (AccessPointServerAPI.class.getName () +
                                                                                                                                  "$call");
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_SUCCESS = StatisticsManager.getKeyedCounterHandler (AccessPointServerAPI.class.getName () +
                                                                                                                               "$success");
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_ERROR = StatisticsManager.getKeyedCounterHandler (AccessPointServerAPI.class.getName () +
                                                                                                                             "$error");
  private static final String LOG_PREFIX = "[Access Point REST API] ";
  private static final Logger LOGGER = LoggerFactory.getLogger (AccessPointServerAPI.class);

  private final ISMPServerAPIDataProvider m_aAPIProvider;

  public AccessPointServerAPI (@NonNull final ISMPServerAPIDataProvider aDataProvider)
  {
    m_aAPIProvider = ValueEnforcer.notNull (aDataProvider, "DataProvider");
  }

  /**
   * Ensure that the provided credentials belong to an existing administrator.
   *
   * @param aCredentials
   *        The credentials to check. May not be <code>null</code>.
   * @return The resolved user. Never <code>null</code>.
   * @throws SMPServerException
   *         If the credentials are invalid or the user is no administrator.
   */
  @NonNull
  private static IUser _validateAdmin (@NonNull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final IUser aUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
    if (!aUser.isAdministrator ())
      throw new SMPUnauthorizedException ("User '" +
                                          aUser.getLoginName () +
                                          "' is not an administrator and therefore not allowed to manage Access Points");
    return aUser;
  }

  @NonNull
  private ISMPAccessPoint _getExistingAccessPoint (@NonNull final String sAccessPointName) throws SMPServerException
  {
    final String sRealName = StringHelper.trim (sAccessPointName);
    if (!SMPAccessPointHelper.isValidName (sRealName))
      throw new SMPBadRequestException ("The Access Point name '" + sAccessPointName + "' is invalid",
                                        m_aAPIProvider.getCurrentURI ());

    final ISMPAccessPoint aAccessPoint = SMPMetaManager.getAccessPointMgr ().getAccessPointOfName (sRealName);
    if (aAccessPoint == null)
      throw new SMPNotFoundException ("The Access Point with name '" + sRealName + "' does not exist",
                                      m_aAPIProvider.getCurrentURI ());
    return aAccessPoint;
  }

  /**
   * Get all contained Access Points.
   *
   * @param aCredentials
   *        The credentials to use. May not be <code>null</code>.
   * @return The list of all Access Points. Never <code>null</code>.
   * @throws SMPServerException
   *         In case of error
   */
  @NonNull
  @ReturnsMutableCopy
  public ICommonsList <ISMPAccessPoint> getAllAccessPoints (@NonNull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /accesspoint/list";
    final String sAction = "getAllAccessPoints";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      _validateAdmin (aCredentials);

      final ICommonsList <ISMPAccessPoint> ret = SMPMetaManager.getAccessPointMgr ().getAllAccessPoints ();
      ret.sort (ISMPAccessPoint.comparator ());

      LOGGER.info (sLog + " SUCCESS - returning " + ret.size () + " Access Points");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return ret;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  /**
   * Get the Access Point with the provided name.
   *
   * @param sAccessPointName
   *        The Access Point name from the URL path. May not be <code>null</code>.
   * @param aCredentials
   *        The credentials to use. May not be <code>null</code>.
   * @return The matching Access Point. Never <code>null</code>.
   * @throws SMPServerException
   *         In case of error
   */
  @NonNull
  public ISMPAccessPoint getAccessPoint (@NonNull final String sAccessPointName,
                                         @NonNull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /accesspoint/name/" + sAccessPointName;
    final String sAction = "getAccessPoint";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      _validateAdmin (aCredentials);

      final ISMPAccessPoint ret = _getExistingAccessPoint (sAccessPointName);

      LOGGER.info (sLog + " SUCCESS");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return ret;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  /**
   * Create a new Access Point or update an existing one. The name of the Access Point is taken from
   * the URL path, so that an Access Point cannot be renamed via this API - renaming would silently
   * break all REST clients referencing the Access Point by name.
   *
   * @param sAccessPointName
   *        The Access Point name from the URL path. May not be <code>null</code>.
   * @param sEndpointReference
   *        The endpoint reference URL to be used. May be <code>null</code>.
   * @param sCertificate
   *        The certificate to be used. May be <code>null</code>.
   * @param aCredentials
   *        The credentials to use. May not be <code>null</code>.
   * @return The created or updated Access Point. Never <code>null</code>.
   * @throws SMPServerException
   *         In case of error
   */
  @NonNull
  public ISMPAccessPoint createOrUpdateAccessPoint (@NonNull final String sAccessPointName,
                                                    @Nullable final String sEndpointReference,
                                                    @Nullable final String sCertificate,
                                                    @NonNull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "PUT /accesspoint/name/" + sAccessPointName;
    final String sAction = "createOrUpdateAccessPoint";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      _validateAdmin (aCredentials);

      final String sRealName = StringHelper.trim (sAccessPointName);
      if (!SMPAccessPointHelper.isValidName (sRealName))
        throw new SMPBadRequestException ("The Access Point name '" + sAccessPointName + "' is invalid",
                                          m_aAPIProvider.getCurrentURI ());

      final String sRealEndpointReference = StringHelper.trim (sEndpointReference);
      if (StringHelper.isEmpty (sRealEndpointReference))
        throw new SMPBadRequestException ("The Access Point endpoint reference URL may not be empty",
                                          m_aAPIProvider.getCurrentURI ());
      if (sRealEndpointReference.length () > ISMPAccessPointManager.ENDPOINT_REFERENCE_MAX_LENGTH)
        throw new SMPBadRequestException ("The Access Point endpoint reference URL is too long. The maximum length is " +
                                          ISMPAccessPointManager.ENDPOINT_REFERENCE_MAX_LENGTH +
                                          " characters", m_aAPIProvider.getCurrentURI ());

      final String sRealCertificate = StringHelper.trim (sCertificate);
      if (StringHelper.isEmpty (sRealCertificate))
        throw new SMPBadRequestException ("The Access Point certificate may not be empty",
                                          m_aAPIProvider.getCurrentURI ());

      final ISMPAccessPointManager aAccessPointMgr = SMPMetaManager.getAccessPointMgr ();
      final ISMPAccessPoint aExisting = aAccessPointMgr.getAccessPointOfName (sRealName);

      final ISMPAccessPoint ret;
      if (aExisting == null)
      {
        ret = aAccessPointMgr.createAccessPoint (sRealName, sRealEndpointReference, sRealCertificate);
        if (ret == null)
          throw new SMPBadRequestException ("Failed to create the Access Point with name '" + sRealName + "'",
                                            m_aAPIProvider.getCurrentURI ());
        LOGGER.info (sLog + " SUCCESS - created");
      }
      else
      {
        aAccessPointMgr.updateAccessPoint (aExisting.getID (), sRealName, sRealEndpointReference, sRealCertificate);
        ret = aAccessPointMgr.getAccessPointOfID (aExisting.getID ());
        if (ret == null)
          throw new SMPNotFoundException ("The Access Point with name '" + sRealName + "' does not exist",
                                          m_aAPIProvider.getCurrentURI ());
        LOGGER.info (sLog + " SUCCESS - updated");
      }

      STATS_COUNTER_SUCCESS.increment (sAction);
      return ret;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  /**
   * Delete the Access Point with the provided name. An Access Point that is still referenced by at
   * least one endpoint cannot be deleted.
   *
   * @param sAccessPointName
   *        The Access Point name from the URL path. May not be <code>null</code>.
   * @param aCredentials
   *        The credentials to use. May not be <code>null</code>.
   * @throws SMPServerException
   *         In case of error
   */
  public void deleteAccessPoint (@NonNull final String sAccessPointName,
                                 @NonNull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "DELETE /accesspoint/name/" + sAccessPointName;
    final String sAction = "deleteAccessPoint";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      _validateAdmin (aCredentials);

      final ISMPAccessPoint aAccessPoint = _getExistingAccessPoint (sAccessPointName);

      final ISMPServiceInformationManager aServiceInfoMgr = SMPMetaManager.getServiceInformationMgr ();
      final long nUsingEndpoints = aServiceInfoMgr.getEndpointCountUsingAccessPoint (aAccessPoint.getID ());
      if (nUsingEndpoints > 0)
        throw new SMPPreconditionFailedException ("The Access Point with name '" +
                                                  aAccessPoint.getName () +
                                                  "' is still used by " +
                                                  nUsingEndpoints +
                                                  " endpoint(s) and can therefore not be deleted",
                                                  m_aAPIProvider.getCurrentURI ());

      final EChange eChange = SMPMetaManager.getAccessPointMgr ().deleteAccessPoint (aAccessPoint.getID ());
      if (eChange.isUnchanged ())
        throw new SMPNotFoundException ("The Access Point with name '" + aAccessPoint.getName () + "' does not exist",
                                        m_aAPIProvider.getCurrentURI ());

      LOGGER.info (sLog + " SUCCESS");
      STATS_COUNTER_SUCCESS.increment (sAction);
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  /**
   * Let all endpoints that currently contain the same data as the referenced Access Point use that
   * Access Point instead. This is the explicit, user triggered alternative to an automatic data
   * migration.
   *
   * @param sAccessPointName
   *        The Access Point name from the URL path. May not be <code>null</code>.
   * @param bRequireSameEndpointReference
   *        <code>true</code> if only endpoints with the same endpoint reference URL should be
   *        changed. If this is <code>false</code>, the endpoint reference URL of the matching
   *        endpoints is replaced with the one of the Access Point.
   * @param aCredentials
   *        The credentials to use. May not be <code>null</code>.
   * @return The number of changed endpoints. Always &ge; 0.
   * @throws SMPServerException
   *         In case of error
   */
  @Nonnegative
  public long useAccessPointForMatchingEndpoints (@NonNull final String sAccessPointName,
                                                  final boolean bRequireSameEndpointReference,
                                                  @NonNull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "POST /accesspoint/name/" + sAccessPointName + "/use-for-matching-endpoints";
    final String sAction = "useAccessPointForMatchingEndpoints";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      _validateAdmin (aCredentials);

      final ISMPAccessPoint aAccessPoint = _getExistingAccessPoint (sAccessPointName);
      if (!aAccessPoint.hasCertificate ())
        throw new SMPPreconditionFailedException ("The Access Point with name '" +
                                                  aAccessPoint.getName () +
                                                  "' contains no certificate and can therefore not be matched against existing endpoints",
                                                  m_aAPIProvider.getCurrentURI ());

      final long nChanged = SMPMetaManager.getServiceInformationMgr ()
                                          .useAccessPointForMatchingEndpoints (aAccessPoint.getID (),
                                                                               bRequireSameEndpointReference);

      LOGGER.info (sLog + " SUCCESS - changed " + nChanged + " endpoints");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return nChanged;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }
}
