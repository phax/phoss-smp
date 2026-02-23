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
import com.helger.base.enforce.ValueEnforcer;
import com.helger.peppolid.IParticipantIdentifier;
import com.helger.peppolid.factory.IIdentifierFactory;
import com.helger.phoss.smp.domain.SMPMetaManager;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroup;
import com.helger.phoss.smp.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.phoss.smp.domain.sgprops.SGCustomProperty;
import com.helger.phoss.smp.domain.sgprops.SGCustomPropertyList;
import com.helger.phoss.smp.domain.user.SMPUserManagerPhoton;
import com.helger.phoss.smp.exception.SMPBadRequestException;
import com.helger.phoss.smp.exception.SMPNotFoundException;
import com.helger.phoss.smp.exception.SMPServerException;
import com.helger.phoss.smp.exception.SMPUnauthorizedException;
import com.helger.photon.security.user.IUser;
import com.helger.statistics.api.IMutableStatisticsHandlerKeyedCounter;
import com.helger.statistics.impl.StatisticsManager;

/**
 * This class implements all the service methods, that must be provided by the Custom Properties
 * REST service.
 *
 * @author Philip Helger
 * @since 8.1.0
 */
public final class CustomPropertiesServerAPI
{
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_INVOCATION = StatisticsManager.getKeyedCounterHandler (CustomPropertiesServerAPI.class.getName () +
                                                                                                                                  "$call");
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_SUCCESS = StatisticsManager.getKeyedCounterHandler (CustomPropertiesServerAPI.class.getName () +
                                                                                                                               "$success");
  private static final IMutableStatisticsHandlerKeyedCounter STATS_COUNTER_ERROR = StatisticsManager.getKeyedCounterHandler (CustomPropertiesServerAPI.class.getName () +
                                                                                                                             "$error");
  private static final String LOG_PREFIX = "[Custom Properties REST API] ";
  private static final Logger LOGGER = LoggerFactory.getLogger (CustomPropertiesServerAPI.class);

  private final ISMPServerAPIDataProvider m_aAPIProvider;

  public CustomPropertiesServerAPI (@NonNull final ISMPServerAPIDataProvider aDataProvider)
  {
    m_aAPIProvider = ValueEnforcer.notNull (aDataProvider, "DataProvider");
  }

  /**
   * Get all custom properties for a service group. If credentials are provided and the user is the
   * owner, all properties are returned. Otherwise only public properties are returned.
   *
   * @param sPathServiceGroupID
   *        The service group ID from the URL path. May not be <code>null</code>.
   * @param aCredentials
   *        Optional credentials. May be <code>null</code> for unauthenticated access.
   * @return The effective custom property list. Never <code>null</code>.
   * @throws SMPServerException
   *         In case of error
   */
  @NonNull
  public SGCustomPropertyList getCustomProperties (@NonNull final String sPathServiceGroupID,
                                                   @Nullable final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /customproperties/" + sPathServiceGroupID;
    final String sAction = "getCustomProperties";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aServiceGroupID == null)
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
        throw SMPNotFoundException.unknownSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());

      // Check if authenticated - if so, return all properties; otherwise only public
      boolean bAuthenticated = false;
      if (aCredentials != null)
      {
        try
        {
          final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
          SMPUserManagerPhoton.verifyOwnership (aServiceGroupID, aSMPUser);
          bAuthenticated = true;
        }
        catch (final SMPUnauthorizedException ex)
        {
          // Not authenticated - that's fine for GET
          // Only the public properties will be listed
        }
      }

      final SGCustomPropertyList aCustomProperties = aServiceGroup.getCustomProperties ();
      final SGCustomPropertyList aEffectiveCustomProperties;
      if (aCustomProperties == null)
        aEffectiveCustomProperties = new SGCustomPropertyList ();
      else
        if (bAuthenticated)
        {
          // Return all properties
          aEffectiveCustomProperties = aCustomProperties;
        }
        else
        {
          // Return only public properties
          aEffectiveCustomProperties = aCustomProperties.getFiltered (SGCustomProperty::isPublic);
        }

      LOGGER.info (sLog +
                   (bAuthenticated ? " [authenticated]" : "") +
                   " SUCCESS - returning " +
                   aEffectiveCustomProperties.size () +
                   " properties");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return aEffectiveCustomProperties;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  /**
   * Get a single custom property by name.
   *
   * @param sPathServiceGroupID
   *        The service group ID from the URL path. May not be <code>null</code>.
   * @param sPropertyName
   *        The property name. May not be <code>null</code>.
   * @param aCredentials
   *        Optional credentials. May be <code>null</code> for unauthenticated access.
   * @return The custom property. Never <code>null</code>.
   * @throws SMPServerException
   *         In case of error
   */
  @NonNull
  public SGCustomProperty getCustomProperty (@NonNull final String sPathServiceGroupID,
                                             @NonNull final String sPropertyName,
                                             @Nullable final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "GET /customproperties/" + sPathServiceGroupID + "/" + sPropertyName;
    final String sAction = "getCustomProperty";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aServiceGroupID == null)
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
        throw SMPNotFoundException.unknownSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());

      // Check if authenticated
      boolean bAuthenticated = false;
      if (aCredentials != null)
      {
        try
        {
          final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
          SMPUserManagerPhoton.verifyOwnership (aServiceGroupID, aSMPUser);
          bAuthenticated = true;
        }
        catch (final SMPUnauthorizedException ex)
        {
          // Not authenticated - that's fine for GET
        }
      }

      final SGCustomPropertyList aCustomProperties = aServiceGroup.getCustomProperties ();
      final SGCustomProperty aCustomProperty = aCustomProperties == null ? null : bAuthenticated ? aCustomProperties
                                                                                                                    .findFirst (x -> x.getName ()
                                                                                                                                      .equals (sPropertyName))
                                                                                                 : aCustomProperties.findFirst (x -> x.isPublic () &&
                                                                                                                                     x.getName ()
                                                                                                                                      .equals (sPropertyName));
      if (aCustomProperty == null)
        throw new SMPNotFoundException ("Custom property '" +
                                        sPropertyName +
                                        "' not found in Service Group '" +
                                        sPathServiceGroupID +
                                        "'",
                                        m_aAPIProvider.getCurrentURI ());

      LOGGER.info (sLog + (bAuthenticated ? " [authenticated]" : "") + " SUCCESS");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return aCustomProperty;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  /**
   * Replace all custom properties for a service group.
   *
   * @param sPathServiceGroupID
   *        The service group ID from the URL path. May not be <code>null</code>.
   * @param aCustomProperties
   *        The new custom properties. May not be <code>null</code>.
   * @param aCredentials
   *        The credentials to be used. May not be <code>null</code>.
   * @throws SMPServerException
   *         In case of error
   */
  public void setCustomProperties (@NonNull final String sPathServiceGroupID,
                                   @NonNull final SGCustomPropertyList aCustomProperties,
                                   @NonNull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "PUT /customproperties/" + sPathServiceGroupID;
    final String sAction = "setCustomProperties";

    LOGGER.info (sLog + " ==> " + aCustomProperties.size () + " custom properties");
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aServiceGroupID == null)
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
        throw SMPNotFoundException.unknownSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());

      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
      SMPUserManagerPhoton.verifyOwnership (aServiceGroupID, aSMPUser);

      // Update the service group with the new custom properties
      aServiceGroupMgr.updateSMPServiceGroup (aServiceGroupID,
                                              aServiceGroup.getOwnerID (),
                                              aServiceGroup.getExtensions ().getExtensionsAsJsonString (),
                                              aCustomProperties);

      LOGGER.info (sLog + " SUCCESS - " + aCustomProperties.size () + " properties set");
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
   * Delete all custom properties for a service group.
   *
   * @param sPathServiceGroupID
   *        The service group ID from the URL path. May not be <code>null</code>.
   * @param aCredentials
   *        The credentials to be used. May not be <code>null</code>.
   * @return The number of deleted properties. Always &ge; 0.
   * @throws SMPServerException
   *         In case of error
   */
  @Nonnegative
  public int deleteCustomProperties (@NonNull final String sPathServiceGroupID,
                                     @NonNull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "DELETE /customproperties/" + sPathServiceGroupID;
    final String sAction = "deleteCustomProperties";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aServiceGroupID == null)
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
        throw SMPNotFoundException.unknownSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());

      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
      SMPUserManagerPhoton.verifyOwnership (aServiceGroupID, aSMPUser);

      final SGCustomPropertyList aCustomProperties = aServiceGroup.getCustomProperties ();
      int nDeletedProperties;
      if (aCustomProperties != null && aCustomProperties.isNotEmpty ())
      {
        nDeletedProperties = aCustomProperties.size ();
        // Update the service group but setting no properties
        aServiceGroupMgr.updateSMPServiceGroup (aServiceGroupID,
                                                aServiceGroup.getOwnerID (),
                                                aServiceGroup.getExtensions ().getExtensionsAsJsonString (),
                                                null);
      }
      else
        nDeletedProperties = 0;

      LOGGER.info (sLog + " SUCCESS - " + nDeletedProperties + " properties deleted");
      STATS_COUNTER_SUCCESS.increment (sAction);
      return nDeletedProperties;
    }
    catch (final SMPServerException ex)
    {
      LOGGER.warn (sLog + " ERROR - " + ex.getMessage ());
      STATS_COUNTER_ERROR.increment (sAction);
      throw ex;
    }
  }

  /**
   * Delete a single custom property by name.
   *
   * @param sPathServiceGroupID
   *        The service group ID from the URL path. May not be <code>null</code>.
   * @param sPropertyName
   *        The property name. May not be <code>null</code>.
   * @param aCredentials
   *        The credentials to be used. May not be <code>null</code>.
   * @throws SMPServerException
   *         In case of error
   */
  public void deleteCustomProperty (@NonNull final String sPathServiceGroupID,
                                    @NonNull final String sPropertyName,
                                    @NonNull final SMPAPICredentials aCredentials) throws SMPServerException
  {
    final String sLog = LOG_PREFIX + "DELETE /customproperties/" + sPathServiceGroupID + "/" + sPropertyName;
    final String sAction = "deleteCustomProperty";

    LOGGER.info (sLog);
    STATS_COUNTER_INVOCATION.increment (sAction);
    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sPathServiceGroupID);
      if (aServiceGroupID == null)
        throw SMPBadRequestException.failedToParseSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
        throw SMPNotFoundException.unknownSG (sPathServiceGroupID, m_aAPIProvider.getCurrentURI ());

      final IUser aSMPUser = SMPUserManagerPhoton.validateUserCredentials (aCredentials);
      SMPUserManagerPhoton.verifyOwnership (aServiceGroupID, aSMPUser);

      if (!SGCustomProperty.isValidName (sPropertyName))
        throw new SMPBadRequestException ("Invalid custom property name '" + sPropertyName + "'",
                                          m_aAPIProvider.getCurrentURI ());

      // Remove the property
      final SGCustomPropertyList aCustomProperties = aServiceGroup.getCustomProperties ();
      if (aCustomProperties == null || aCustomProperties.remove (sPropertyName).isUnchanged ())
        throw new SMPNotFoundException ("Custom property '" +
                                        sPropertyName +
                                        "' not found in Service Group '" +
                                        sPathServiceGroupID +
                                        "'",
                                        m_aAPIProvider.getCurrentURI ());

      // Update the service group
      aServiceGroupMgr.updateSMPServiceGroup (aServiceGroupID,
                                              aServiceGroup.getOwnerID (),
                                              aServiceGroup.getExtensions ().getExtensionsAsJsonString (),
                                              aCustomProperties);

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
}
