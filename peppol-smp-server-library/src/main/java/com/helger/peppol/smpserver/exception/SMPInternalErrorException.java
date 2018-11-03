package com.helger.peppol.smpserver.exception;

import javax.annotation.Nonnull;

/**
 * Exception that is thrown to indicate an HTTP 500 error.
 *
 * @author Philip Helger
 * @since 5.1.0
 */
public class SMPInternalErrorException extends SMPServerException
{
  public SMPInternalErrorException (@Nonnull final String sMsg, @Nonnull final Throwable aCause)
  {
    super (sMsg, aCause);
  }
}
