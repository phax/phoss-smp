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
package com.helger.peppol.smpserver.ui;

import javax.annotation.Nonnull;
import javax.annotation.concurrent.Immutable;

import com.helger.html.jscode.JSExpr;
import com.helger.html.jscode.JSInvocation;
import com.helger.html.jscode.JSRef;

@Immutable
public final class CAppJS
{
  private CAppJS ()
  {}

  @Nonnull
  public static JSRef getApp ()
  {
    // Match the JS file in src/main/webapp/js
    return JSExpr.ref ("App");
  }

  @Nonnull
  public static JSInvocation viewLogin ()
  {
    // Invoke the JS function "viewLogin" on the object
    return getApp ().invoke ("viewLogin");
  }
}
