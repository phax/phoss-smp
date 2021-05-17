package com.helger.phoss.smp.config;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.typeconvert.ITypeConverterRegistrarSPI;
import com.helger.commons.typeconvert.ITypeConverterRegistry;

@IsSPIImplementation
public class SMPTypeConverterRegistrar implements ITypeConverterRegistrarSPI
{
  public void registerTypeConverter (@Nonnull final ITypeConverterRegistry aRegistry)
  {}
}
