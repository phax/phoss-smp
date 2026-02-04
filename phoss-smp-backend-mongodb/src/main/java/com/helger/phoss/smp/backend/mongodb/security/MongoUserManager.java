package com.helger.phoss.smp.backend.mongodb.security;

import com.helger.annotation.Nonempty;
import com.helger.base.callback.CallbackList;
import com.helger.base.enforce.ValueEnforcer;
import com.helger.base.state.EChange;
import com.helger.base.string.StringHelper;
import com.helger.collection.commons.ICommonsList;
import com.helger.photon.audit.AuditHelper;
import com.helger.photon.security.password.GlobalPasswordSettings;
import com.helger.photon.security.user.IUser;
import com.helger.photon.security.user.IUserManager;
import com.helger.photon.security.user.IUserModificationCallback;
import com.helger.photon.security.user.User;
import com.helger.security.password.hash.PasswordHash;
import com.helger.security.password.salt.PasswordSalt;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;

public class MongoUserManager extends AbstractMongoManager <IUser> implements IUserManager
{

  public static final String USER_COLLECTION_NAME = "users";

  private static final String BSON_USER_LOGIN_NAME = "loginName";
  private static final String BSON_USER_EMAIL = "email";
  private static final String BSON_USER_PASSWORD = "password";
  private static final String BSON_USER_PASSWORD_ALGO = "algo";
  private static final String BSON_USER_PASSWORD_SALT = "salt";
  private static final String BSON_USER_PASSWORD_HASH = "hash";
  private static final String BSON_USER_FIRST_NAME = "firstName";
  private static final String BSON_USER_LAST_NAME = "lastName";
  private static final String BSON_USER_DESCRIPTION = "description";
  private static final String BSON_USER_PREFERRED_LOCALE = "locale";
  private static final String BSON_USER_LAST_LOGIN = "lastLogin";
  private static final String BSON_USER_LOGIN_COUNT = "loginCount";
  private static final String BSON_USER_FAILED_LOGIN_COUNT = "failedLoginCount";
  private static final String BSON_USER_DISABLED = "disabled";


  private final CallbackList <IUserModificationCallback> m_aCallbacks = new CallbackList <> ();

  public MongoUserManager ()
  {
    super (USER_COLLECTION_NAME);
  }


  @Override
  protected @NonNull Document toBson (@NonNull IUser aUser)
  {
    return getDefaultBusinessDocument (aUser)
                               .append (BSON_USER_LOGIN_NAME, aUser.getLoginName ())
                               .append (BSON_USER_PASSWORD, passwordHashToDocument (aUser.getPasswordHash ()))
                               .append (BSON_USER_EMAIL, aUser.getEmailAddress ())
                               .append (BSON_USER_FIRST_NAME, aUser.getFirstName ())
                               .append (BSON_USER_LAST_NAME, aUser.getLastName ())
                               .append (BSON_USER_DESCRIPTION, aUser.getDescription ())
                               .append (BSON_USER_PREFERRED_LOCALE, aUser.getDesiredLocale ().toLanguageTag ())
                               .append (BSON_USER_LAST_LOGIN, convertLocalDateTimeToDate (aUser.getLastLoginDateTime ()))
                               .append (BSON_USER_LOGIN_COUNT, aUser.getLoginCount ())
                               .append (BSON_USER_FAILED_LOGIN_COUNT, aUser.getConsecutiveFailedLoginCount ())
                               .append (BSON_USER_DISABLED, aUser.isDisabled ());
  }

  private Document passwordHashToDocument (@NonNull PasswordHash passwordHash)
  {
    return new Document ().append (BSON_USER_PASSWORD_ALGO, passwordHash.getAlgorithmName ())
                               .append (BSON_USER_PASSWORD_SALT, passwordHash.getSaltAsString ())
                               .append (BSON_USER_PASSWORD_HASH, passwordHash.getPasswordHashValue ());
  }

  @Override
  protected @NonNull IUser toEntity (@NonNull Document aDoc)
  {
    return new User (populateStubObject (aDoc),
                               aDoc.getString (BSON_USER_LOGIN_NAME),
                               aDoc.getString (BSON_USER_EMAIL),
                               documentToPasswordHash (aDoc.get (BSON_USER_PASSWORD, Document.class)),
                               aDoc.getString (BSON_USER_FIRST_NAME),
                               aDoc.getString (BSON_USER_LAST_NAME),
                               aDoc.getString (BSON_USER_DESCRIPTION),
                               Locale.forLanguageTag (aDoc.getString (BSON_USER_PREFERRED_LOCALE)),
                               convertDatenToLocalDateTime (aDoc.getDate (BSON_USER_LAST_LOGIN)),
                               aDoc.getInteger (BSON_USER_LOGIN_COUNT),
                               aDoc.getInteger (BSON_USER_FAILED_LOGIN_COUNT),
                               aDoc.getBoolean (BSON_USER_DISABLED)
    );


  }

  private @NonNull PasswordHash documentToPasswordHash (Document aDoc)
  {
    ValueEnforcer.notNull (aDoc, "aDoc");
    return new PasswordHash (
                               aDoc.getString (BSON_USER_PASSWORD_ALGO),
                               PasswordSalt.createFromStringMaybe (aDoc.getString (BSON_USER_PASSWORD_SALT)),
                               aDoc.getString (BSON_USER_PASSWORD_HASH)
    );
  }

  @Override
  public void createDefaultsForTest ()
  {
    //ignored for now
  }

  @Override
  public @NonNull CallbackList <IUserModificationCallback> userModificationCallbacks ()
  {
    return m_aCallbacks;
  }

  @Override
  public @Nullable IUser createNewUser (@NonNull @Nonempty String sLoginName,
                                        @Nullable String sEmailAddress,
                                        @NonNull String sPlainTextPassword,
                                        @Nullable String sFirstName,
                                        @Nullable String sLastName,
                                        @Nullable String sDescription,
                                        @Nullable Locale aDesiredLocale,
                                        @Nullable Map <String, String> aCustomAttrs,
                                        boolean bDisabled)
  {
    ValueEnforcer.notEmpty (sLoginName, "LoginName");
    ValueEnforcer.notNull (sPlainTextPassword, "PlainTextPassword");
    if (getUserOfLoginName (sLoginName) != null)
    {
      // Another user with this login name already exists
      AuditHelper.onAuditCreateFailure (User.OT, "login-name-already-in-use", sLoginName);
      return null;
    }

    final User aUser = new User (sLoginName,
                               sEmailAddress,
                               GlobalPasswordSettings.createUserDefaultPasswordHash (PasswordSalt.createRandom (), sPlainTextPassword),
                               sFirstName,
                               sLastName,
                               sDescription,
                               aDesiredLocale,
                               aCustomAttrs,
                               bDisabled);

    return internalCreateNewUser (aUser, false);
  }

  @Override
  public @Nullable IUser createPredefinedUser (@NonNull @Nonempty String sID, @NonNull @Nonempty String sLoginName, @Nullable String sEmailAddress, @NonNull String sPlainTextPassword, @Nullable String sFirstName, @Nullable String sLastName, @Nullable String sDescription, @Nullable Locale aDesiredLocale, @Nullable Map <String, String> aCustomAttrs, boolean bDisabled)
  {
    ValueEnforcer.notEmpty (sLoginName, "LoginName");
    ValueEnforcer.notNull (sPlainTextPassword, "PlainTextPassword");
    if (getUserOfLoginName (sLoginName) != null)
    {
      // Another user with this login name already exists
      AuditHelper.onAuditCreateFailure (User.OT, "login-name-already-in-use", sLoginName, "predefined-user");
      return null;
    }
    User aUser = User.createdPredefinedUser (sID,
                               sLoginName,
                               sEmailAddress,
                               GlobalPasswordSettings.createUserDefaultPasswordHash (PasswordSalt.createRandom (),
                                                          sPlainTextPassword),
                               sFirstName,
                               sLastName,
                               sDescription,
                               aDesiredLocale,
                               aCustomAttrs,
                               bDisabled);
    return internalCreateNewUser (aUser, true);
  }

  protected User internalCreateNewUser (@NonNull final User aUser, final boolean bPredefined)
  {
    try
    {
      getCollection ().insertOne (toBson (aUser));
      m_aCallbacks.forEach (aCB -> aCB.onUserCreated (aUser, bPredefined));
      return aUser;
    } catch (Exception e)
    {
      AuditHelper.onAuditCreateFailure (User.OT,
                                 aUser.getID (),
                                 aUser.getLoginName (),
                                 aUser.getEmailAddress (),
                                 aUser.getFirstName (),
                                 aUser.getLastName (),
                                 aUser.getDescription (),
                                 aUser.getDesiredLocale (),
                                 aUser.attrs (),
                                 Boolean.valueOf (aUser.isDisabled ()),
                                 bPredefined ? "predefined" : "custom",
                                 "database-error");
    }
    return null;
  }

  @Override
  public @Nullable IUser getUserOfID (@Nullable String sUserID)
  {
    return findById (sUserID);
  }

  @Override
  public @Nullable IUser getUserOfLoginName (@Nullable String sLoginName)
  {
    if (StringHelper.isEmpty (sLoginName))
      return null;

    return findByFilter (Filters.eq (BSON_USER_LOGIN_NAME, sLoginName));
  }

  @Override
  public @Nullable IUser getUserOfEmailAddress (@Nullable String sEmailAddress)
  {
    if (StringHelper.isEmpty (sEmailAddress))
      return null;
    return findByFilter (Filters.eq (BSON_USER_EMAIL, sEmailAddress));
  }

  @Override
  public @Nullable IUser getUserOfEmailAddressIgnoreCase (@Nullable String sEmailAddress)
  {
    if (StringHelper.isEmpty (sEmailAddress))
      return null;

    for (Document currentUser: getCollection ().find ())
    {
      if (sEmailAddress.equalsIgnoreCase (currentUser.getString (BSON_USER_EMAIL)))
      {
        return toEntity (currentUser);
      }
    }
    return null;

  }

  private @Nullable IUser findByFilter (Bson filter)
  {
    Document aDocument = getCollection ().find (filter).first ();
    if (aDocument == null)
      return null;

    return toEntity (aDocument);
  }

  private static final Bson ACTIVE_FILTER = Filters.and (Filters.eq (BSON_USER_DISABLED, false), Filters.eq (BSON_DELETED_TIME, null));

  @Override
  public @NonNull ICommonsList <IUser> getAllActiveUsers ()
  {
    return findInternal (ACTIVE_FILTER);
  }

  @Override
  public long getActiveUserCount ()
  {
    return getCollection ().countDocuments (ACTIVE_FILTER);
  }

  @Override
  public @NonNull ICommonsList <IUser> getAllDisabledUsers ()
  {
    return findInternal (Filters.and (Filters.eq (BSON_USER_DISABLED, true), Filters.eq (BSON_DELETED_TIME, null)));
  }

  @Override
  public @NonNull ICommonsList <IUser> getAllNotDeletedUsers ()
  {
    return findInternal (Filters.eq (BSON_DELETED_TIME, null));
  }

  @Override
  public @NonNull ICommonsList <IUser> getAllDeletedUsers ()
  {
    return findInternal (Filters.ne (BSON_DELETED_TIME, null));
  }

  @Override
  public boolean containsAnyActiveUser ()
  {
    return getActiveUserCount () > 0;
  }

  @Override
  public @NonNull EChange setUserData (@Nullable String sUserID,
                                       @NonNull @Nonempty String sNewLoginName,
                                       @Nullable String sNewEmailAddress,
                                       @Nullable String sNewFirstName,
                                       @Nullable String sNewLastName,
                                       @Nullable String sNewDescription,
                                       @Nullable Locale aNewDesiredLocale,
                                       @Nullable Map <String, String> aNewCustomAttrs,
                                       boolean bNewDisabled)
  {

    Bson update = Updates.combine (
                               Updates.set (BSON_USER_LOGIN_NAME, sNewLoginName),
                               Updates.set (BSON_USER_EMAIL, sNewEmailAddress),
                               Updates.set (BSON_USER_FIRST_NAME, sNewFirstName),
                               Updates.set (BSON_USER_LAST_NAME, sNewLastName),
                               Updates.set (BSON_USER_DESCRIPTION, sNewDescription),
                               Updates.set (BSON_USER_PREFERRED_LOCALE, aNewDesiredLocale.toLanguageTag ()),
                               Updates.set (BSON_USER_DISABLED, bNewDisabled),
                               Updates.set (BSON_ATTRIBUTES, aNewCustomAttrs)
    );

    return genericUpdate (sUserID, update, true, () -> m_aCallbacks.forEach (aCB -> aCB.onUserUpdated (sUserID)));
  }

  @Override
  public @NonNull EChange setUserPassword (@Nullable String sUserID, @NonNull String sNewPlainTextPassword)
  {
    PasswordHash userDefaultPasswordHash = GlobalPasswordSettings.createUserDefaultPasswordHash (PasswordSalt.createRandom (), sNewPlainTextPassword);

    Bson update = Updates.combine (
                               Updates.set (BSON_USER_PASSWORD + "." + BSON_USER_PASSWORD_ALGO, userDefaultPasswordHash.getAlgorithmName ()),
                               Updates.set (BSON_USER_PASSWORD + "." + BSON_USER_PASSWORD_SALT, userDefaultPasswordHash.getSaltAsString ()),
                               Updates.set (BSON_USER_PASSWORD + "." + BSON_USER_PASSWORD_HASH, userDefaultPasswordHash.getPasswordHashValue ())
    );

    return genericUpdate (sUserID, update, true, () -> m_aCallbacks.forEach (aCB -> aCB.onUserPasswordChanged (sUserID)));
  }

  @Override
  public @NonNull EChange updateUserLastLogin (@Nullable String sUserID)
  {
    return genericUpdate (sUserID, Updates.combine (
                               Updates.set (BSON_USER_LAST_LOGIN, LocalDateTime.now ()),
                               Updates.inc (BSON_USER_LOGIN_COUNT, 1),
                               Updates.inc (BSON_USER_FAILED_LOGIN_COUNT, 0)
    ), true, () -> m_aCallbacks.forEach (aCB -> aCB.onUserUpdated (sUserID)));
  }

  @Override
  public @NonNull EChange updateUserLastFailedLogin (@Nullable String sUserID)
  {
    return genericUpdate (sUserID, Updates.inc (BSON_USER_FAILED_LOGIN_COUNT, 1), true, ()
                               -> m_aCallbacks.forEach (aCB -> aCB.onUserLastFailedLoginUpdated (sUserID)));
  }

  @Override
  public @NonNull EChange deleteUser (@Nullable String sUserID)
  {
    return deleteEntity (sUserID, () -> m_aCallbacks.forEach (aCB -> aCB.onUserDeleted (sUserID)));
  }

  @Override
  public @NonNull EChange undeleteUser (@Nullable String sUserID)
  {
    return undeleteEntity (sUserID, () -> m_aCallbacks.forEach (aCB -> aCB.onUserUndeleted (sUserID)));
  }

  @Override
  public @NonNull EChange disableUser (@Nullable String sUserID)
  {
    return genericUpdate (sUserID, Updates.set (BSON_USER_DISABLED, true), true, ()
                               -> m_aCallbacks.forEach (aCB -> aCB.onUserUpdated (sUserID)));
  }

  @Override
  public @NonNull EChange enableUser (@Nullable String sUserID)
  {
    return genericUpdate (sUserID, Updates.set (BSON_USER_DISABLED, false), true, ()
                               -> m_aCallbacks.forEach (aCB -> aCB.onUserUpdated (sUserID)));
  }
}
