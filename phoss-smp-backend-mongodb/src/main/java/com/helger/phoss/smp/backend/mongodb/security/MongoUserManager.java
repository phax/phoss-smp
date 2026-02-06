package com.helger.phoss.smp.backend.mongodb.security;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

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
import com.helger.typeconvert.impl.TypeConverter;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;

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

  private static final Bson ACTIVE_FILTER = Filters.and (Filters.eq (BSON_USER_DISABLED, Boolean.FALSE),
                                                         Filters.eq (BSON_DELETED_TIME, null));

  private final CallbackList <IUserModificationCallback> m_aCallbacks = new CallbackList <> ();

  public MongoUserManager ()
  {
    super (USER_COLLECTION_NAME);
  }

  @NonNull
  private Document _passwordHashToDocument (@NonNull final PasswordHash passwordHash)
  {
    return new Document ().append (BSON_USER_PASSWORD_ALGO, passwordHash.getAlgorithmName ())
                          .append (BSON_USER_PASSWORD_SALT, passwordHash.getSaltAsString ())
                          .append (BSON_USER_PASSWORD_HASH, passwordHash.getPasswordHashValue ());
  }

  @Override
  protected @NonNull Document toBson (@NonNull final IUser aUser)
  {
    return getDefaultBusinessDocument (aUser).append (BSON_USER_LOGIN_NAME, aUser.getLoginName ())
                                             .append (BSON_USER_PASSWORD,
                                                      _passwordHashToDocument (aUser.getPasswordHash ()))
                                             .append (BSON_USER_EMAIL, aUser.getEmailAddress ())
                                             .append (BSON_USER_FIRST_NAME, aUser.getFirstName ())
                                             .append (BSON_USER_LAST_NAME, aUser.getLastName ())
                                             .append (BSON_USER_DESCRIPTION, aUser.getDescription ())
                                             .append (BSON_USER_PREFERRED_LOCALE,
                                                      aUser.getDesiredLocale ().toLanguageTag ())
                                             .append (BSON_USER_LAST_LOGIN,
                                                      TypeConverter.convert (aUser.getLastLoginDateTime (), Date.class))
                                             .append (BSON_USER_LOGIN_COUNT, Integer.valueOf (aUser.getLoginCount ()))
                                             .append (BSON_USER_FAILED_LOGIN_COUNT,
                                                      Integer.valueOf (aUser.getConsecutiveFailedLoginCount ()))
                                             .append (BSON_USER_DISABLED, Boolean.valueOf (aUser.isDisabled ()));
  }

  @Override
  protected @NonNull IUser toEntity (@NonNull final Document aDoc)
  {
    return new User (populateStubObject (aDoc),
                     aDoc.getString (BSON_USER_LOGIN_NAME),
                     aDoc.getString (BSON_USER_EMAIL),
                     documentToPasswordHash (aDoc.get (BSON_USER_PASSWORD, Document.class)),
                     aDoc.getString (BSON_USER_FIRST_NAME),
                     aDoc.getString (BSON_USER_LAST_NAME),
                     aDoc.getString (BSON_USER_DESCRIPTION),
                     Locale.forLanguageTag (aDoc.getString (BSON_USER_PREFERRED_LOCALE)),
                     TypeConverter.convert (aDoc.getDate (BSON_USER_LAST_LOGIN), LocalDateTime.class),
                     aDoc.getInteger (BSON_USER_LOGIN_COUNT).intValue (),
                     aDoc.getInteger (BSON_USER_FAILED_LOGIN_COUNT).intValue (),
                     aDoc.getBoolean (BSON_USER_DISABLED).booleanValue ());
  }

  private @NonNull PasswordHash documentToPasswordHash (final Document aDoc)
  {
    ValueEnforcer.notNull (aDoc, "aDoc");
    return new PasswordHash (aDoc.getString (BSON_USER_PASSWORD_ALGO),
                             PasswordSalt.createFromStringMaybe (aDoc.getString (BSON_USER_PASSWORD_SALT)),
                             aDoc.getString (BSON_USER_PASSWORD_HASH));
  }

  @Override
  public void createDefaultsForTest ()
  {
    // ignored for now
  }

  @Override
  public @NonNull CallbackList <IUserModificationCallback> userModificationCallbacks ()
  {
    return m_aCallbacks;
  }

  @NonNull
  private User _internalCreateNewUser (@NonNull final User aUser, final boolean bPredefined)
  {
    if (!getCollection ().insertOne (toBson (aUser)).wasAcknowledged ())
      throw new IllegalStateException ("Failed to insert into MongoDB Collection");

    AuditHelper.onAuditCreateSuccess (User.OT,
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

    m_aCallbacks.forEach (aCB -> aCB.onUserCreated (aUser, bPredefined));
    return aUser;
  }

  @Override
  public @Nullable IUser createNewUser (@NonNull @Nonempty final String sLoginName,
                                        @Nullable final String sEmailAddress,
                                        @NonNull final String sPlainTextPassword,
                                        @Nullable final String sFirstName,
                                        @Nullable final String sLastName,
                                        @Nullable final String sDescription,
                                        @Nullable final Locale aDesiredLocale,
                                        @Nullable final Map <String, String> aCustomAttrs,
                                        final boolean bDisabled)
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
                                 GlobalPasswordSettings.createUserDefaultPasswordHash (PasswordSalt.createRandom (),
                                                                                       sPlainTextPassword),
                                 sFirstName,
                                 sLastName,
                                 sDescription,
                                 aDesiredLocale,
                                 aCustomAttrs,
                                 bDisabled);

    return _internalCreateNewUser (aUser, false);
  }

  @Override
  public @Nullable IUser createPredefinedUser (@NonNull @Nonempty final String sID,
                                               @NonNull @Nonempty final String sLoginName,
                                               @Nullable final String sEmailAddress,
                                               @NonNull final String sPlainTextPassword,
                                               @Nullable final String sFirstName,
                                               @Nullable final String sLastName,
                                               @Nullable final String sDescription,
                                               @Nullable final Locale aDesiredLocale,
                                               @Nullable final Map <String, String> aCustomAttrs,
                                               final boolean bDisabled)
  {
    ValueEnforcer.notEmpty (sLoginName, "LoginName");
    ValueEnforcer.notNull (sPlainTextPassword, "PlainTextPassword");
    if (getUserOfLoginName (sLoginName) != null)
    {
      // Another user with this login name already exists
      AuditHelper.onAuditCreateFailure (User.OT, "login-name-already-in-use", sLoginName, "predefined-user");
      return null;
    }
    final User aUser = User.createdPredefinedUser (sID,
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
    return _internalCreateNewUser (aUser, true);
  }

  @Override
  public @Nullable IUser getUserOfID (@Nullable final String sUserID)
  {
    return findByID (sUserID);
  }

  @Override
  public @Nullable IUser getUserOfLoginName (@Nullable final String sLoginName)
  {
    if (StringHelper.isEmpty (sLoginName))
      return null;

    return findFirst (Filters.eq (BSON_USER_LOGIN_NAME, sLoginName));
  }

  @Override
  public @Nullable IUser getUserOfEmailAddress (@Nullable final String sEmailAddress)
  {
    if (StringHelper.isEmpty (sEmailAddress))
      return null;
    return findFirst (Filters.eq (BSON_USER_EMAIL, sEmailAddress));
  }

  @Override
  public @Nullable IUser getUserOfEmailAddressIgnoreCase (@Nullable final String sEmailAddress)
  {
    if (StringHelper.isEmpty (sEmailAddress))
      return null;

    for (final Document currentUser : getCollection ().find ())
    {
      if (sEmailAddress.equalsIgnoreCase (currentUser.getString (BSON_USER_EMAIL)))
      {
        return toEntity (currentUser);
      }
    }
    return null;
  }

  @Override
  public @NonNull ICommonsList <IUser> getAllActiveUsers ()
  {
    return findAll (ACTIVE_FILTER);
  }

  @Override
  public long getActiveUserCount ()
  {
    return getCollection ().countDocuments (ACTIVE_FILTER);
  }

  @Override
  public @NonNull ICommonsList <IUser> getAllDisabledUsers ()
  {
    return findAll (Filters.and (Filters.eq (BSON_USER_DISABLED, Boolean.TRUE), Filters.eq (BSON_DELETED_TIME, null)));
  }

  @Override
  public @NonNull ICommonsList <IUser> getAllNotDeletedUsers ()
  {
    return findAll (Filters.eq (BSON_DELETED_TIME, null));
  }

  @Override
  public @NonNull ICommonsList <IUser> getAllDeletedUsers ()
  {
    return findAll (Filters.ne (BSON_DELETED_TIME, null));
  }

  @Override
  public boolean containsAnyActiveUser ()
  {
    return findFirst (ACTIVE_FILTER) != null;
  }

  @Override
  public @NonNull EChange setUserData (@Nullable final String sUserID,
                                       @NonNull @Nonempty final String sNewLoginName,
                                       @Nullable final String sNewEmailAddress,
                                       @Nullable final String sNewFirstName,
                                       @Nullable final String sNewLastName,
                                       @Nullable final String sNewDescription,
                                       @Nullable final Locale aNewDesiredLocale,
                                       @Nullable final Map <String, String> aNewCustomAttrs,
                                       final boolean bNewDisabled)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final Bson update = Updates.combine (Updates.set (BSON_USER_LOGIN_NAME, sNewLoginName),
                                         Updates.set (BSON_USER_EMAIL, sNewEmailAddress),
                                         Updates.set (BSON_USER_FIRST_NAME, sNewFirstName),
                                         Updates.set (BSON_USER_LAST_NAME, sNewLastName),
                                         Updates.set (BSON_USER_DESCRIPTION, sNewDescription),
                                         Updates.set (BSON_USER_PREFERRED_LOCALE, aNewDesiredLocale.toLanguageTag ()),
                                         Updates.set (BSON_USER_DISABLED, Boolean.valueOf (bNewDisabled)),
                                         Updates.set (BSON_ATTRIBUTES, aNewCustomAttrs));

    return genericUpdate (sUserID, update, true, () -> m_aCallbacks.forEach (aCB -> aCB.onUserUpdated (sUserID)));
  }

  @Override
  public @NonNull EChange setUserPassword (@Nullable final String sUserID, @NonNull final String sNewPlainTextPassword)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final PasswordHash userDefaultPasswordHash = GlobalPasswordSettings.createUserDefaultPasswordHash (PasswordSalt.createRandom (),
                                                                                                       sNewPlainTextPassword);

    final Bson update = Updates.combine (Updates.set (BSON_USER_PASSWORD + "." + BSON_USER_PASSWORD_ALGO,
                                                      userDefaultPasswordHash.getAlgorithmName ()),
                                         Updates.set (BSON_USER_PASSWORD + "." + BSON_USER_PASSWORD_SALT,
                                                      userDefaultPasswordHash.getSaltAsString ()),
                                         Updates.set (BSON_USER_PASSWORD + "." + BSON_USER_PASSWORD_HASH,
                                                      userDefaultPasswordHash.getPasswordHashValue ()));

    return genericUpdate (sUserID,
                          update,
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserPasswordChanged (sUserID)));
  }

  @Override
  public @NonNull EChange updateUserLastLogin (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    return genericUpdate (sUserID,
                          Updates.combine (Updates.set (BSON_USER_LAST_LOGIN, LocalDateTime.now ()),
                                           Updates.inc (BSON_USER_LOGIN_COUNT, Integer.valueOf (1)),
                                           Updates.set (BSON_USER_FAILED_LOGIN_COUNT, Integer.valueOf (0))),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserUpdated (sUserID)));
  }

  @Override
  public @NonNull EChange updateUserLastFailedLogin (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    return genericUpdate (sUserID,
                          Updates.inc (BSON_USER_FAILED_LOGIN_COUNT, Integer.valueOf (1)),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserLastFailedLoginUpdated (sUserID)));
  }

  @Override
  public @NonNull EChange deleteUser (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    return deleteEntity (sUserID, () -> m_aCallbacks.forEach (aCB -> aCB.onUserDeleted (sUserID)));
  }

  @Override
  public @NonNull EChange undeleteUser (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    return undeleteEntity (sUserID, () -> m_aCallbacks.forEach (aCB -> aCB.onUserUndeleted (sUserID)));
  }

  @Override
  public @NonNull EChange disableUser (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    return genericUpdate (sUserID,
                          Updates.set (BSON_USER_DISABLED, Boolean.TRUE),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserUpdated (sUserID)));
  }

  @Override
  public @NonNull EChange enableUser (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    return genericUpdate (sUserID,
                          Updates.set (BSON_USER_DISABLED, Boolean.FALSE),
                          true,
                          () -> m_aCallbacks.forEach (aCB -> aCB.onUserUpdated (sUserID)));
  }
}
