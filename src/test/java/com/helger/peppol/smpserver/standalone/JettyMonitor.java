/**
 * Copyright (C) 2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Version: MPL 1.1/EUPL 1.1
 *
 * The contents of this file are subject to the Mozilla Public License Version
 * 1.1 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at:
 * http://www.mozilla.org/MPL/
 *
 * Software distributed under the License is distributed on an "AS IS" basis,
 * WITHOUT WARRANTY OF ANY KIND, either express or implied. See the License
 * for the specific language governing rights and limitations under the
 * License.
 *
 * The Original Code is Copyright The PEPPOL project (http://www.peppol.eu)
 *
 * Alternatively, the contents of this file may be used under the
 * terms of the EUPL, Version 1.1 or - as soon they will be approved
 * by the European Commission - subsequent versions of the EUPL
 * (the "Licence"); You may not use this work except in compliance
 * with the Licence.
 * You may obtain a copy of the Licence at:
 * http://joinup.ec.europa.eu/software/page/eupl/licence-eupl
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Licence is distributed on an "AS IS" basis,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Licence for the specific language governing permissions and
 * limitations under the Licence.
 *
 * If you wish to allow use of your version of this file only
 * under the terms of the EUPL License and not to allow others to use
 * your version of this file under the MPL, indicate your decision by
 * deleting the provisions above and replace them with the notice and
 * other provisions required by the EUPL License. If you do not delete
 * the provisions above, a recipient may use your version of this file
 * under either the MPL or the EUPL License.
 */
package com.helger.peppol.smpserver.standalone;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author PEPPOL.AT, BRZ, Philip Helger
 */
public final class JettyMonitor extends Thread
{
  public static final int STOP_PORT = 8078;
  public static final String STOP_KEY = "secret";

  private static final Logger s_aLogger = LoggerFactory.getLogger (JettyMonitor.class);
  private final int m_nPort;
  private final String m_sKey;
  private final ServerSocket m_aServerSocket;

  public JettyMonitor () throws IOException
  {
    this (STOP_PORT, STOP_KEY);
  }

  private JettyMonitor (final int nPort, final String sKey) throws IOException
  {
    m_nPort = nPort;
    m_sKey = sKey;
    setDaemon (true);
    setName ("JettyStopMonitor");
    m_aServerSocket = new ServerSocket (m_nPort, 1, InetAddress.getByName (null));
    if (m_aServerSocket == null)
      s_aLogger.error ("WARN: Not listening on monitor port: " + m_nPort);
  }

  @Override
  public void run ()
  {
    while (true)
    {
      Socket aSocket = null;
      try
      {
        aSocket = m_aServerSocket.accept ();

        final LineNumberReader lin = new LineNumberReader (new InputStreamReader (aSocket.getInputStream ()));
        final String sKey = lin.readLine ();
        if (!m_sKey.equals (sKey))
          continue;

        final String sCmd = lin.readLine ();
        if ("stop".equals (sCmd))
        {
          try
          {
            aSocket.close ();
            m_aServerSocket.close ();
          }
          catch (final Exception e)
          {
            s_aLogger.error ("Failed to close socket", e);
          }
          System.exit (0);
        }
      }
      catch (final Exception e)
      {
        s_aLogger.error ("Error reading from socket", e);
      }
      finally
      {
        if (aSocket != null)
          try
          {
            aSocket.close ();
          }
          catch (final IOException e)
          {}
        aSocket = null;
      }
    }
  }
}
