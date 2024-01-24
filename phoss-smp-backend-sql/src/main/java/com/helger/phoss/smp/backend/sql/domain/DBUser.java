/*
 * Copyright (C) 2019-2024 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.sql.domain;

import java.io.Serializable;

import javax.annotation.Nonnull;

/**
 * Represents a single user within the SMP database.
 *
 * @author Philip Helger
 */
public class DBUser implements Serializable
{
  private final String m_sUserName;
  private final String m_sPassword;

  public DBUser (@Nonnull final String sUserName, @Nonnull final String sPassword)
  {
    m_sUserName = sUserName;
    m_sPassword = sPassword;
  }

  public String getUserName ()
  {
    return m_sUserName;
  }

  public String getPassword ()
  {
    return m_sPassword;
  }
}
