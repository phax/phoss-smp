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
