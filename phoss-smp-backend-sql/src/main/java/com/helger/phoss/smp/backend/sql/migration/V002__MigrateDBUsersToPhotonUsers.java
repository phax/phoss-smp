package com.helger.phoss.smp.backend.sql.migration;

import javax.annotation.Nonnull;

import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.helger.commons.collection.impl.CommonsLinkedHashMap;
import com.helger.commons.collection.impl.ICommonsList;
import com.helger.commons.collection.impl.ICommonsOrderedMap;
import com.helger.commons.io.resource.FileSystemResource;
import com.helger.phoss.smp.CSMPServer;
import com.helger.phoss.smp.backend.sql.mgr.SMPUserManagerJDBC;
import com.helger.phoss.smp.backend.sql.model.DBUser;
import com.helger.phoss.smp.domain.user.ISMPUser;
import com.helger.photon.app.io.WebFileIO;
import com.helger.photon.security.mgr.PhotonSecurityManager;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.web.scope.mgr.WebScoped;
import com.helger.xml.microdom.util.XMLMapHandler;

public class V002__MigrateDBUsersToPhotonUsers extends BaseJavaMigration
{
  private static final Logger LOGGER = LoggerFactory.getLogger (V002__MigrateDBUsersToPhotonUsers.class);

  public void migrate (@Nonnull final Context context)
  {
    try (final WebScoped aWS = new WebScoped ())
    {
      LOGGER.info ("Migrating all DB users to ph-oton users");

      final SMPUserManagerJDBC aSQLUserMgr = new SMPUserManagerJDBC ();
      final ICommonsList <ISMPUser> aSQLUsers = aSQLUserMgr.getAllUsers ();
      LOGGER.info ("Found " + aSQLUsers.size () + " DB user to migrate");

      final ICommonsOrderedMap <String, String> aCreatedMappings = new CommonsLinkedHashMap <> ();

      final IUserManager aPhotonUserMgr = PhotonSecurityManager.getUserMgr ();
      for (final ISMPUser aSQLUser : aSQLUsers)
      {
        final DBUser aDBUser = (DBUser) aSQLUser;
        IUser aPhotonUser = null;
        int nIndex = 0;
        while (true)
        {
          final String sUserName = aDBUser.getUserName () + (nIndex > 0 ? Integer.toString (nIndex) : "");
          final String sEmailAddress = sUserName + "@example.org";
          aPhotonUser = aPhotonUserMgr.createNewUser (sEmailAddress,
                                                      sEmailAddress,
                                                      aDBUser.getPassword (),
                                                      null,
                                                      sUserName,
                                                      null,
                                                      CSMPServer.DEFAULT_LOCALE,
                                                      null,
                                                      false);
          if (aPhotonUser != null)
            break;

          // User name already taken
          ++nIndex;
          if (nIndex > 1000)
          {
            // Avoid endless loop
            throw new IllegalStateException ("Too many iterations mapping the DB user '" + aDBUser.getUserName () + "' to a ph-oton user");
          }
        }
        aCreatedMappings.put (aDBUser.getUserName (), aPhotonUser.getID ());
        LOGGER.info ("Mapped DB user '" + aDBUser.getUserName () + "' to ph-oton user " + aPhotonUser.getID ());
      }

      // Update the ownership in "smp_ownership"
      // Remove the table "smp_user"
      aSQLUserMgr.onMigrationUpdateOwnershipsAndKillUsers (aCreatedMappings);

      if (XMLMapHandler.writeMap (aCreatedMappings,
                                  new FileSystemResource (WebFileIO.getDataIO ().getFile ("migrations/db-photon-user-mapping.xml")))
                       .isFailure ())
        LOGGER.error ("Failed to store mapping of DB users to ph-oton users as XML");
      LOGGER.info ("Finished migrating all DB users to ph-oton users");
    }
  }
}
