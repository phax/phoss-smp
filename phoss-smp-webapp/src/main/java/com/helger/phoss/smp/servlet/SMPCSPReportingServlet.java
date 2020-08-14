package com.helger.phoss.smp.servlet;

import com.helger.commons.http.EHttpMethod;
import com.helger.json.serialize.IJsonWriterSettings;
import com.helger.json.serialize.JsonWriterSettings;
import com.helger.phoss.smp.app.SMPWebAppConfiguration;
import com.helger.photon.core.interror.InternalErrorBuilder;
import com.helger.photon.core.servlet.CSPReportingXServletHandler;
import com.helger.xservlet.AbstractXServlet;

/**
 * Internal servlet to post CSP violations to.
 *
 * @author Philip Helger
 * @since 5.2.6
 */
public class SMPCSPReportingServlet extends AbstractXServlet
{
  public static final String SERVLET_DEFAULT_NAME = "smp-cspreporting";
  public static final String SERVLET_DEFAULT_PATH = '/' + SERVLET_DEFAULT_NAME;

  static final class ERBCSPReportingXServletHandler extends CSPReportingXServletHandler
  {
    private static final IJsonWriterSettings JWS = new JsonWriterSettings ().setIndentEnabled (true);

    public ERBCSPReportingXServletHandler ()
    {
      super (aJson -> {
        // As done in super class
        CSPReportingXServletHandler.logCSPReport (aJson);
        // Notify ourselves
        new InternalErrorBuilder ().addErrorMessage ("CSP error").addCustomData ("CSP-Report", aJson.getAsJsonString (JWS)).handle ();
      });
      // Avoid spamming us
      setFilterDuplicates (true);
    }
  }

  public SMPCSPReportingServlet ()
  {
    handlerRegistry ().registerHandler (EHttpMethod.POST, new ERBCSPReportingXServletHandler (), false);
    if (SMPWebAppConfiguration.isHttpOptionsDisabled ())
      handlerRegistry ().unregisterHandler (EHttpMethod.OPTIONS);
  }
}
