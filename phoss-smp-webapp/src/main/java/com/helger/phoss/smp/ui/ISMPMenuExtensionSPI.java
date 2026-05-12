/*
 * Copyright (C) 2014-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.ui;

import org.jspecify.annotations.NonNull;

import com.helger.annotation.style.IsSPIInterface;
import com.helger.photon.core.menu.IMenuTree;

/**
 * SPI interface for contributing additional menu items to the SMP web application after the
 * built-in menu has been constructed. Implementations are discovered through the standard Java
 * {@link java.util.ServiceLoader} mechanism and invoked once per menu tree, after the built-in
 * items have been added but before the default menu item is selected.
 *
 * @author Philip Helger
 * @since 8.1.6
 */
@IsSPIInterface
public interface ISMPMenuExtensionSPI
{
  /**
   * Contribute items to the secure (authenticated) menu tree. The default implementation does
   * nothing.
   *
   * @param aMenuTree
   *        The secure menu tree to extend. Never <code>null</code>.
   */
  default void customizeSecureMenu (@NonNull final IMenuTree aMenuTree)
  {}

  /**
   * Contribute items to the public menu tree. The default implementation does nothing.
   *
   * @param aMenuTree
   *        The public menu tree to extend. Never <code>null</code>.
   */
  default void customizePublicMenu (@NonNull final IMenuTree aMenuTree)
  {}
}
