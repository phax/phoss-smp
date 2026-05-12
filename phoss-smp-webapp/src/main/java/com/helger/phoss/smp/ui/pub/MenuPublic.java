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
package com.helger.phoss.smp.ui.pub;

import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.annotation.concurrent.Immutable;
import com.helger.base.spi.ServiceLoaderHelper;
import com.helger.phoss.smp.ui.ISMPMenuExtensionSPI;
import com.helger.photon.core.menu.IMenuTree;

/**
 * This class contains the menu structure for the public application.
 *
 * @author Philip Helger
 */
@Immutable
public final class MenuPublic
{
  private static final Logger LOGGER = LoggerFactory.getLogger (MenuPublic.class);

  private MenuPublic ()
  {}

  public static void init (@NonNull final IMenuTree aMenuTree)
  {
    aMenuTree.createRootItem (new PagePublicStart (CMenuPublic.MENU_START));

    // Set default
    aMenuTree.setDefaultMenuItemID (CMenuPublic.MENU_START);

    // Invoke registered menu extensions
    for (final ISMPMenuExtensionSPI aSPI : ServiceLoaderHelper.getAllSPIImplementations (ISMPMenuExtensionSPI.class))
    {
      if (LOGGER.isDebugEnabled ())
        LOGGER.debug ("Calling extendPublicMenu on " + aSPI.getClass ().getName ());
      aSPI.customizePublicMenu (aMenuTree);
    }
  }
}
