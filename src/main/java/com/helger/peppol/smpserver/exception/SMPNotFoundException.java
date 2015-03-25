package com.helger.peppol.smpserver.exception;

import java.net.URI;

import javax.annotation.Nonnull;

public class SMPNotFoundException extends SMPServerException
{
  /**
   * Create a HTTP 404 (Not Found) exception.
   *
   * @param sMessage
   *        the String that is the entity of the 404 response.
   */
  public SMPNotFoundException (@Nonnull final String sMessage)
  {
    super ("Not found: " + sMessage);
  }

  /**
   * Create a HTTP 404 (Not Found) exception.
   *
   * @param sMessage
   *        the String that is the entity of the 404 response.
   * @param aNotFoundURI
   *        The URI that was not found.
   */
  public SMPNotFoundException (@Nonnull final String sMessage, @Nonnull final URI aNotFoundURI)
  {
    super ("Not found: " + sMessage + " at " + aNotFoundURI.toString ());
  }
}
