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
package com.helger.phoss.smp.domain.servicegroup;

import java.time.OffsetDateTime;

import org.jspecify.annotations.Nullable;

/**
 * Base interface for objects having an optional last modification date time with an offset. Other
 * than <code>IHasLastModificationDateTime</code> from ph-commons, the value is unambiguous, so it
 * can be compared across time zones and daylight saving time changes.
 *
 * @author Philip Helger
 * @since 8.9.0
 */
public interface ISMPHasLastModificationDateTime
{
  /**
   * @return The last modification date time or <code>null</code> if it is unknown.
   */
  @Nullable
  OffsetDateTime getLastModificationDateTime ();

  /**
   * @return <code>true</code> if a last modification date time is present, <code>false</code> if
   *         not.
   */
  default boolean hasLastModificationDateTime ()
  {
    return getLastModificationDateTime () != null;
  }
}
