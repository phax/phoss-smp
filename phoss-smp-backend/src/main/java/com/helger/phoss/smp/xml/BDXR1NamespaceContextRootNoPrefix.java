package com.helger.phoss.smp.xml;

import com.helger.smpclient.bdxr1.marshal.BDXR1NamespaceContext;
import com.helger.xsds.bdxr.smp1.CBDXRSMP1;

import jakarta.annotation.Nonnull;

/**
 * Special version of {@link BDXR1NamespaceContext} where the root element uses the default prefix.
 * 
 * @author Philip Helger
 * @since 8.0.1
 */
public class BDXR1NamespaceContextRootNoPrefix extends BDXR1NamespaceContext
{
  private static final class SingletonHolder
  {
    static final BDXR1NamespaceContextRootNoPrefix INSTANCE = new BDXR1NamespaceContextRootNoPrefix ();
  }

  /**
   * Deprecated constructor.
   *
   * @deprecated Use {@link BDXR1NamespaceContextRootNoPrefix#getInstance()} instead.
   */
  @Deprecated (forRemoval = false)
  public BDXR1NamespaceContextRootNoPrefix ()
  {
    removeMapping (CBDXRSMP1.DEFAULT_PREFIX);
    addDefaultNamespaceURI (CBDXRSMP1.NAMESPACE_URI);
  }

  @Nonnull
  public static BDXR1NamespaceContextRootNoPrefix getInstance ()
  {
    return SingletonHolder.INSTANCE;
  }
}
