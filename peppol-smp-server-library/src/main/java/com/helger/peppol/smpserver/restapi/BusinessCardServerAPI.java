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
package com.helger.peppol.smpserver.restapi;

import javax.annotation.Nonnull;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.ValueEnforcer;
import com.helger.commons.lang.ClassHelper;
import com.helger.commons.statistics.IMutableStatisticsHandlerKeyedCounter;
import com.helger.commons.statistics.IStatisticsHandlerKeyedCounter;
import com.helger.commons.statistics.StatisticsManager;
import com.helger.pd.businesscard.PDBusinessCardType;
import com.helger.peppol.identifier.factory.IIdentifierFactory;
import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;
import com.helger.peppol.smpserver.domain.SMPMetaManager;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCard;
import com.helger.peppol.smpserver.domain.businesscard.ISMPBusinessCardManager;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroup;
import com.helger.peppol.smpserver.domain.servicegroup.ISMPServiceGroupManager;
import com.helger.peppol.smpserver.exception.SMPNotFoundException;

/**
 * This class implements all the service methods, that must be provided by the
 * BusinessCard REST service - this service is the same for BDXR and SMP.
 *
 * @author Philip Helger
 */
public final class BusinessCardServerAPI
{
  private static final Logger s_aLogger = LoggerFactory.getLogger (BusinessCardServerAPI.class);
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterInvocation = StatisticsManager.getKeyedCounterHandler (BusinessCardServerAPI.class.getName () +
                                                                                                                                   "$call");
  private static final IMutableStatisticsHandlerKeyedCounter s_aStatsCounterSuccess = StatisticsManager.getKeyedCounterHandler (BusinessCardServerAPI.class.getName () +
                                                                                                                                "$success");
  private static final String LOG_PREFIX = "[BusinessCard REST API] ";

  private final ISMPServerAPIDataProvider m_aAPIProvider;

  public BusinessCardServerAPI (@Nonnull final ISMPServerAPIDataProvider aDataProvider)
  {
    m_aAPIProvider = ValueEnforcer.notNull (aDataProvider, "DataProvider");
  }

  @Nonnull
  public PDBusinessCardType getBusinessCard (final String sServiceGroupID) throws Throwable
  {
    s_aLogger.info (LOG_PREFIX + "GET /businesscard/" + sServiceGroupID);
    s_aStatsCounterInvocation.increment ("getBusinessCard");

    try
    {
      final IIdentifierFactory aIdentifierFactory = SMPMetaManager.getIdentifierFactory ();
      final IParticipantIdentifier aServiceGroupID = aIdentifierFactory.parseParticipantIdentifier (sServiceGroupID);
      if (aServiceGroupID == null)
      {
        // Invalid identifier
        throw new SMPNotFoundException ("Failed to parse serviceGroup '" +
                                        sServiceGroupID +
                                        "'",
                                        m_aAPIProvider.getCurrentURI ());
      }

      final ISMPServiceGroupManager aServiceGroupMgr = SMPMetaManager.getServiceGroupMgr ();
      final ISMPServiceGroup aServiceGroup = aServiceGroupMgr.getSMPServiceGroupOfID (aServiceGroupID);
      if (aServiceGroup == null)
      {
        // No such service group
        throw new SMPNotFoundException ("Unknown serviceGroup '" +
                                        sServiceGroupID +
                                        "'",
                                        m_aAPIProvider.getCurrentURI ());
      }

      final ISMPBusinessCardManager aBusinessCardMgr = SMPMetaManager.getBusinessCardMgr ();
      if (aBusinessCardMgr == null)
      {
        // No such business card
        throw new SMPNotFoundException ("This SMP server does not support the BusinessCard API",
                                        m_aAPIProvider.getCurrentURI ());
      }
      final ISMPBusinessCard aBusinessCard = aBusinessCardMgr.getSMPBusinessCardOfServiceGroup (aServiceGroup);
      if (aBusinessCard == null)
      {
        // No such business card
        throw new SMPNotFoundException ("No BusinessCard assigned to serviceGroup '" +
                                        sServiceGroupID +
                                        "'",
                                        m_aAPIProvider.getCurrentURI ());
      }

      s_aLogger.info (LOG_PREFIX + "Finished getBusinessCard(" + sServiceGroupID + ")");
      s_aStatsCounterSuccess.increment ("getBusinessCard");
      return aBusinessCard.getAsJAXBObject ();
    }
    catch (final Throwable t)
    {
      s_aLogger.warn (LOG_PREFIX +
                      "Error in getBusinessCard(" +
                      sServiceGroupID +
                      ") - " +
                      ClassHelper.getClassLocalName (t) +
                      " - " +
                      t.getMessage ());
      throw t;
    }
  }

  /**
   * @return The statistics data with the invocation counter.
   */
  @Nonnull
  public static IStatisticsHandlerKeyedCounter getInvocationCounter ()
  {
    return s_aStatsCounterInvocation;
  }

  /**
   * @return The statistics data with the successful invocation counter.
   */
  @Nonnull
  public static IStatisticsHandlerKeyedCounter getSuccessCounter ()
  {
    return s_aStatsCounterSuccess;
  }
}
