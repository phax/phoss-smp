/*
 * Copyright (C) 2015-2025 Philip Helger and contributors
 * philip[at]helger[dot]com
 *
 * The Original Code is Copyright The Peppol project (http://www.peppol.eu)
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/.
 */
package com.helger.phoss.smp.smlhook;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.peppolid.IParticipantIdentifier;

/**
 * An implementation of {@link IRegistrationHook} that does nothing.
 *
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
@Immutable
public class RegistrationHookDoNothing implements IRegistrationHook
{
  public void createServiceGroup (@Nonnull final IParticipantIdentifier aPI)
  {}

  public void undoCreateServiceGroup (@Nonnull final IParticipantIdentifier aPI)
  {}

  public void deleteServiceGroup (@Nonnull final IParticipantIdentifier aPI)
  {}

  public void undoDeleteServiceGroup (@Nonnull final IParticipantIdentifier aPI)
  {}
}
