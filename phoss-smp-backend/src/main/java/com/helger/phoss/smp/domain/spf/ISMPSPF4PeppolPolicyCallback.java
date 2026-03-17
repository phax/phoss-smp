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
package com.helger.phoss.smp.domain.spf;

import org.jspecify.annotations.NonNull;

import com.helger.base.callback.ICallback;

/**
 * Callback interface for SPF4Peppol policy changes.
 *
 * @author Steven Noels
 */
public interface ISMPSPF4PeppolPolicyCallback extends ICallback
{
  /**
   * Invoked after an SPF4Peppol policy was created or updated.
   *
   * @param aPolicy
   *        The created or updated policy. Never <code>null</code>.
   */
  default void onSMPSPFPolicyCreatedOrUpdated (@NonNull final ISMPSPF4PeppolPolicy aPolicy)
  {}

  /**
   * Invoked after an SPF4Peppol policy was deleted.
   *
   * @param aPolicy
   *        The deleted policy. Never <code>null</code>.
   */
  default void onSMPSPFPolicyDeleted (@NonNull final ISMPSPF4PeppolPolicy aPolicy)
  {}
}
