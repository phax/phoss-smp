package com.helger.phoss.smp.ui.secure.hc;

import org.jspecify.annotations.NonNull;

import com.helger.html.css.DefaultCSSClassProvider;
import com.helger.html.css.ICSSClassProvider;
import com.helger.photon.bootstrap4.CBootstrapCSS;
import com.helger.photon.bootstrap4.buttongroup.BootstrapButtonToolbar;
import com.helger.photon.core.execcontext.ILayoutExecutionContext;

/**
 * Sticky button toolbar
 * 
 * @author Philip Helger
 */
public class HCButtonToolbarSticky extends BootstrapButtonToolbar
{
  private static final ICSSClassProvider STICKY_BOTTOM = DefaultCSSClassProvider.create ("sticky-bottom");

  public HCButtonToolbarSticky (@NonNull final ILayoutExecutionContext aLEC)
  {
    super (aLEC);
    addClasses (STICKY_BOTTOM, CBootstrapCSS.BG_LIGHT, CBootstrapCSS.BORDER_TOP, CBootstrapCSS.P_3);
  }
}
