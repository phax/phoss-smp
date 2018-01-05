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
package com.helger.peppol.smpserver.exception;

import javax.annotation.Nonnull;

/**
 * Base class for all SMP server specific exceptions
 * 
 * @author Philip Helger
 */
public class SMPServerException extends RuntimeException
{
  public SMPServerException (@Nonnull final String sMessage)
  {
    super (sMessage);
  }
}
