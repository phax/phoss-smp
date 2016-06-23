package com.helger.peppol.smpserver.settings;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;

import com.helger.commons.annotation.UsedViaReflection;
import com.helger.commons.annotation.WorkInProgress;
import com.helger.commons.scope.singleton.AbstractGlobalSingleton;
import com.helger.commons.state.EChange;
import com.helger.photon.basic.app.dao.impl.AbstractSimpleDAO;
import com.helger.photon.basic.app.dao.impl.DAOException;
import com.helger.settings.ISettings;
import com.helger.settings.exchange.xml.SettingsMicroDocumentConverter;
import com.helger.settings.factory.ISettingsFactory;
import com.helger.xml.microdom.IMicroDocument;
import com.helger.xml.microdom.MicroDocument;

/**
 * This class manages and persists the SMP settings.
 * 
 * @author Philip Helger
 */
@ThreadSafe
@WorkInProgress
public class SMPSettingsManager extends AbstractGlobalSingleton
{
  private final class DAO extends AbstractSimpleDAO
  {
    protected DAO (@Nullable final String sFilename) throws DAOException
    {
      super (sFilename);
      initialRead ();
    }

    @Override
    @Nonnull
    protected EChange onRead (final IMicroDocument aDoc)
    {
      final SettingsMicroDocumentConverter aConverter = new SettingsMicroDocumentConverter (ISettingsFactory.newInstance ());
      final ISettings aSettings = aConverter.convertToNative (aDoc.getDocumentElement ());
      m_aSMPS.setFromSettings (aSettings);
      return EChange.UNCHANGED;
    }

    @Override
    protected IMicroDocument createWriteData ()
    {
      final IMicroDocument ret = new MicroDocument ();
      final SettingsMicroDocumentConverter aConverter = new SettingsMicroDocumentConverter (ISettingsFactory.newInstance ());
      ret.appendChild (aConverter.convertToMicroElement (m_aSMPS.getAsSettings (), null, "root"));
      return ret;
    }

    // Make method visible and lock it
    private void markAsChanged0 ()
    {
      super.m_aRWLock.writeLocked ( () -> super.markAsChanged ());
    }
  }

  private final SMPSettings m_aSMPS = new SMPSettings ();
  private final DAO m_aDAO;

  @Deprecated
  @UsedViaReflection
  public SMPSettingsManager () throws DAOException
  {
    m_aDAO = new DAO ("smp-settings.xml");
  }

  @Nonnull
  public static SMPSettingsManager getInstance ()
  {
    return getGlobalSingleton (SMPSettingsManager.class);
  }

  @Nonnull
  public ISMPSettings getSettings ()
  {
    return m_aSMPS;
  }

  @Nonnull
  public EChange updateSettings (final boolean bPEPPOLDirectoryIntegrationEnabled)
  {
    EChange eChange = EChange.UNCHANGED;
    m_aRWLock.writeLock ().lock ();
    try
    {
      eChange = eChange.or (m_aSMPS.setPEPPOLDirectoryIntegrationEnabled (bPEPPOLDirectoryIntegrationEnabled));
    }
    finally
    {
      m_aRWLock.writeLock ().unlock ();
    }
    if (eChange.isChanged ())
      m_aDAO.markAsChanged0 ();
    return eChange;
  }
}
