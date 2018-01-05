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
package com.helger.peppol.smpserver.domain.user;

import javax.annotation.Nullable;

/**
 * Base interface for a user resolver
 *
 * @author Philip Helger
 */
public interface ISMPUserProvider
{
  /**
   * Get the user with the specified ID.
   *
   * @param sUserID
   *        The user ID to search. May be <code>null</code>.
   * @return <code>null</code> if no such user exists.
   */
  @Nullable
  ISMPUser getUserOfID (@Nullable String sUserID);
}
