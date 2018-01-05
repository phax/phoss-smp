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

import java.io.Serializable;
import java.util.Comparator;
import java.util.Locale;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.Nonempty;
import com.helger.commons.compare.IComparator;
import com.helger.commons.id.IHasID;

/**
 * Interface representing an SMP user. For the SQL backend this is a separate
 * domain object whereas for the XML backend this is a wrapper for the photon
 * user.
 *
 * @author Philip Helger
 */
public interface ISMPUser extends IHasID <String>, Serializable
{
  /**
   * @return The user ID.
   */
  @Nonnull
  @Nonempty
  String getID ();

  /**
   * @return The user display name.
   */
  @Nonnull
  @Nonempty
  String getUserName ();

  @Nonnull
  static Comparator <ISMPUser> comparator (@Nonnull final Locale aSortLocale)
  {
    return IComparator.getComparatorCollating (ISMPUser::getUserName, aSortLocale);
  }
}
