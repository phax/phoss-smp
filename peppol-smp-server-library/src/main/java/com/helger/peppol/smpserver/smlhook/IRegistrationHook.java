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
package com.helger.peppol.smpserver.smlhook;

import javax.annotation.Nonnull;

import com.helger.peppol.identifier.generic.participant.IParticipantIdentifier;

/**
 * Base interface for the callback to modify the SML.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public interface IRegistrationHook
{
  /**
   * Create a participant in the SML.
   *
   * @param aPI
   *        The participant to be created
   * @throws RegistrationHookException
   *         If something goes wrong.
   */
  void createServiceGroup (@Nonnull IParticipantIdentifier aPI) throws RegistrationHookException;

  /**
   * Delete a participant in the SML because the internal adding in the SMP
   * failed
   *
   * @param aPI
   *        The participant to be deleted
   * @throws RegistrationHookException
   *         If something goes wrong.
   */
  void undoCreateServiceGroup (@Nonnull IParticipantIdentifier aPI) throws RegistrationHookException;

  /**
   * Delete a participant in the SML.
   *
   * @param aPI
   *        The participant to be deleted
   * @throws RegistrationHookException
   *         If something goes wrong.
   */
  void deleteServiceGroup (@Nonnull IParticipantIdentifier aPI) throws RegistrationHookException;

  /**
   * Create a participant in the SML because the deletion.
   *
   * @param aPI
   *        The participant to be re-created
   * @throws RegistrationHookException
   *         If something goes wrong.
   */
  void undoDeleteServiceGroup (@Nonnull IParticipantIdentifier aPI) throws RegistrationHookException;
}
