/**
 * Copyright (C) 2014-2015 Philip Helger (www.helger.com)
 * philip[at]helger[dot]com
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *         http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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

import com.helger.commons.charset.CCharset;

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
      try (final Socket aSocket = m_aServerSocket.accept ())
      {
        final LineNumberReader lin = new LineNumberReader (new InputStreamReader (aSocket.getInputStream (),
                                                                                  CCharset.CHARSET_ISO_8859_1_OBJ));
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
    }
  }
}
