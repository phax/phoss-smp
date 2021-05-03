package com.helger.phoss.smp.config;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.OffsetTime;
import java.time.Year;
import java.time.YearMonth;
import java.time.ZonedDateTime;
import java.util.Date;
import java.util.GregorianCalendar;

import javax.annotation.Nonnull;

import com.helger.commons.annotation.IsSPIImplementation;
import com.helger.commons.datetime.OffsetDate;
import com.helger.commons.datetime.PDTFactory;
import com.helger.commons.datetime.XMLOffsetDate;
import com.helger.commons.datetime.XMLOffsetDateTime;
import com.helger.commons.datetime.XMLOffsetTime;
import com.helger.commons.typeconvert.ITypeConverterRegistrarSPI;
import com.helger.commons.typeconvert.ITypeConverterRegistry;

@IsSPIImplementation
public class SMPTypeConverterRegistrar implements ITypeConverterRegistrarSPI
{
  public void registerTypeConverter (@Nonnull final ITypeConverterRegistry aRegistry)
  {
    // TODO remove when using ph-commons 10.1.2
    // Destination: String
    aRegistry.registerTypeConverter (XMLOffsetDate.class, String.class, XMLOffsetDate::getAsString);
    aRegistry.registerTypeConverter (XMLOffsetTime.class, String.class, XMLOffsetTime::getAsString);
    aRegistry.registerTypeConverter (XMLOffsetDateTime.class, String.class, XMLOffsetDateTime::getAsString);

    aRegistry.registerTypeConverter (GregorianCalendar.class,
                                     XMLOffsetDateTime.class,
                                     PDTFactory::createXMLOffsetDateTime);
    aRegistry.registerTypeConverter (String.class, XMLOffsetDateTime.class, XMLOffsetDateTime::parse);

    aRegistry.registerTypeConverter (ZonedDateTime.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);
    aRegistry.registerTypeConverter (OffsetDateTime.class, XMLOffsetDateTime.class, XMLOffsetDateTime::of);

    aRegistry.registerTypeConverter (OffsetDate.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);
    aRegistry.registerTypeConverter (XMLOffsetDate.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);

    aRegistry.registerTypeConverter (OffsetTime.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);
    aRegistry.registerTypeConverter (XMLOffsetTime.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);

    aRegistry.registerTypeConverter (LocalDateTime.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);
    aRegistry.registerTypeConverter (LocalDate.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);
    aRegistry.registerTypeConverter (LocalTime.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);

    aRegistry.registerTypeConverter (YearMonth.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);
    aRegistry.registerTypeConverter (Year.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);
    aRegistry.registerTypeConverter (Instant.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);
    aRegistry.registerTypeConverter (Date.class, XMLOffsetDateTime.class, PDTFactory::createXMLOffsetDateTime);
    aRegistry.registerTypeConverter (java.sql.Timestamp.class,
                                     XMLOffsetDateTime.class,
                                     PDTFactory::createXMLOffsetDateTime);
    aRegistry.registerTypeConverterRuleAssignableSourceFixedDestination (Number.class,
                                                                         XMLOffsetDateTime.class,
                                                                         PDTFactory::createXMLOffsetDateTime);
  }
}
