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

import javax.annotation.Nonnull;

/**
 * Extended user interface for an SMP user that can be edited on the user
 * interface.
 *
 * @author Philip Helger
 */
public interface ISMPUserEditable extends ISMPUser
{
  void setUserName (@Nonnull String sUserName);

  @Nonnull
  String getPassword ();

  void setPassword (@Nonnull String sPassword);
}
