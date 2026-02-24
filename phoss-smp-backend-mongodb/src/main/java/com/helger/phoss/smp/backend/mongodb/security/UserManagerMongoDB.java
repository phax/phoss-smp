/*
 * Copyright (C) 2019-2026 Philip Helger and contributors
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
package com.helger.phoss.smp.backend.mongodb.security;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import com.helger.annotation.Nonempty;
import com.helger.annotation.misc.DevelopersNote;
import com.helger.annotation.style.VisibleForTesting;
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
import com.mongodb.client.model.Indexes;
import com.mongodb.client.model.Updates;

public class UserManagerMongoDB extends AbstractBusinessObjectManagerMongoDB <IUser, User> implements IUserManager
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

  public UserManagerMongoDB ()
  {
    super (USER_COLLECTION_NAME);
    getCollection ().createIndex (Indexes.ascending (BSON_ID));
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

  private @NonNull PasswordHash _documentToPasswordHash (@NonNull final Document aDoc)
  {
    ValueEnforcer.notNull (aDoc, "aDoc");
    return new PasswordHash (aDoc.getString (BSON_USER_PASSWORD_ALGO),
                             PasswordSalt.createFromStringMaybe (aDoc.getString (BSON_USER_PASSWORD_SALT)),
                             aDoc.getString (BSON_USER_PASSWORD_HASH));
  }

  @Override
  protected @NonNull User toEntity (@NonNull final Document aDoc)
  {
    return new User (populateStubObject (aDoc),
                     aDoc.getString (BSON_USER_LOGIN_NAME),
                     aDoc.getString (BSON_USER_EMAIL),
                     _documentToPasswordHash (aDoc.get (BSON_USER_PASSWORD, Document.class)),
                     aDoc.getString (BSON_USER_FIRST_NAME),
                     aDoc.getString (BSON_USER_LAST_NAME),
                     aDoc.getString (BSON_USER_DESCRIPTION),
                     Locale.forLanguageTag (aDoc.getString (BSON_USER_PREFERRED_LOCALE)),
                     TypeConverter.convert (aDoc.getDate (BSON_USER_LAST_LOGIN), LocalDateTime.class),
                     aDoc.getInteger (BSON_USER_LOGIN_COUNT).intValue (),
                     aDoc.getInteger (BSON_USER_FAILED_LOGIN_COUNT).intValue (),
                     aDoc.getBoolean (BSON_USER_DISABLED).booleanValue ());
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

  @Nullable
  private User _internalCreateNewUser (@NonNull final User aUser, final boolean bPredefined)
  {
    if (!getCollection ().insertOne (toBson (aUser)).wasAcknowledged ())
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
      return null;
    }

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
                                      bPredefined ? "predefined" : "custom");

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

  @DevelopersNote ("For internal use only")
  public @Nullable IUser internalCreateMigrationUser (@NonNull final User aSrcUser)
  {
    ValueEnforcer.notNull (aSrcUser, "SrcUser");
    if (getUserOfLoginName (aSrcUser.getLoginName ()) != null)
    {
      // Another user with this login name already exists
      AuditHelper.onAuditCreateFailure (User.OT,
                                        "login-name-already-in-use",
                                        aSrcUser.getLoginName (),
                                        "migration-user");
      return null;
    }
    return _internalCreateNewUser (aSrcUser, true);
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

    // Use case-insensitive RegEx for case-insensitive comparison
    return findFirst (Filters.regex (BSON_USER_EMAIL, Pattern.quote (sEmailAddress), "i"));
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

    final EChange eChange = genericUpdateOne (sUserID,
                                              addLastModToUpdate (Updates.combine (Updates.set (BSON_USER_LOGIN_NAME,
                                                                                                sNewLoginName),
                                                                                   Updates.set (BSON_USER_EMAIL,
                                                                                                sNewEmailAddress),
                                                                                   Updates.set (BSON_USER_FIRST_NAME,
                                                                                                sNewFirstName),
                                                                                   Updates.set (BSON_USER_LAST_NAME,
                                                                                                sNewLastName),
                                                                                   Updates.set (BSON_USER_DESCRIPTION,
                                                                                                sNewDescription),
                                                                                   Updates.set (BSON_USER_PREFERRED_LOCALE,
                                                                                                aNewDesiredLocale.toLanguageTag ()),
                                                                                   Updates.set (BSON_USER_DISABLED,
                                                                                                Boolean.valueOf (bNewDisabled)),
                                                                                   Updates.set (BSON_ATTRIBUTES,
                                                                                                aNewCustomAttrs))));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (User.OT,
                                        "set-all",
                                        sUserID,
                                        sNewLoginName,
                                        sNewEmailAddress,
                                        sNewFirstName,
                                        sNewLastName,
                                        sNewDescription,
                                        aNewDesiredLocale,
                                        aNewCustomAttrs,
                                        Boolean.valueOf (bNewDisabled));

      m_aCallbacks.forEach (aCB -> aCB.onUserUpdated (sUserID));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (User.OT, "set-all", sUserID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange setUserPassword (@Nullable final String sUserID, @NonNull final String sNewPlainTextPassword)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final PasswordHash aPasswordHash = GlobalPasswordSettings.createUserDefaultPasswordHash (PasswordSalt.createRandom (),
                                                                                             sNewPlainTextPassword);
    final EChange eChange = genericUpdateOne (sUserID,
                                              addLastModToUpdate (Updates.combine (Updates.set (BSON_USER_PASSWORD +
                                                                                                "." +
                                                                                                BSON_USER_PASSWORD_ALGO,
                                                                                                aPasswordHash.getAlgorithmName ()),
                                                                                   Updates.set (BSON_USER_PASSWORD +
                                                                                                "." +
                                                                                                BSON_USER_PASSWORD_SALT,
                                                                                                aPasswordHash.getSaltAsString ()),
                                                                                   Updates.set (BSON_USER_PASSWORD +
                                                                                                "." +
                                                                                                BSON_USER_PASSWORD_HASH,
                                                                                                aPasswordHash.getPasswordHashValue ()))));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (User.OT, "set-password", sUserID);

      m_aCallbacks.forEach (aCB -> aCB.onUserPasswordChanged (sUserID));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (User.OT, "set-password", sUserID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange updateUserLastLogin (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sUserID,
                                              addLastModToUpdate (Updates.combine (Updates.set (BSON_USER_LAST_LOGIN,
                                                                                                LocalDateTime.now ()),
                                                                                   Updates.inc (BSON_USER_LOGIN_COUNT,
                                                                                                Integer.valueOf (1)),
                                                                                   Updates.set (BSON_USER_FAILED_LOGIN_COUNT,
                                                                                                Integer.valueOf (0)))));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (User.OT, "update-last-login", sUserID);

      m_aCallbacks.forEach (aCB -> aCB.onUserUpdated (sUserID));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (User.OT, "update-last-login", sUserID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange updateUserLastFailedLogin (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sUserID,
                                              addLastModToUpdate (Updates.inc (BSON_USER_FAILED_LOGIN_COUNT,
                                                                               Integer.valueOf (1))));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (User.OT, "set-last-failed-login", sUserID);

      m_aCallbacks.forEach (aCB -> aCB.onUserLastFailedLoginUpdated (sUserID));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (User.OT, "update-last-failed-login", sUserID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange deleteUser (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final EChange eChange = deleteEntity (sUserID);
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditDeleteSuccess (User.OT, sUserID);

      m_aCallbacks.forEach (aCB -> aCB.onUserDeleted (sUserID));
    }
    else
    {
      AuditHelper.onAuditDeleteFailure (User.OT, sUserID, "no-such-id");
    }
    return eChange;
  }

  @VisibleForTesting
  void internalDeleteUserNotRecoverable (@NonNull final String sUserID)
  {
    getCollection ().deleteOne (Filters.eq (BSON_ID, sUserID));
  }

  @Override
  public @NonNull EChange undeleteUser (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final EChange eChange = undeleteEntity (sUserID);
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditUndeleteSuccess (User.OT, sUserID);

      m_aCallbacks.forEach (aCB -> aCB.onUserUndeleted (sUserID));
    }
    else
    {
      AuditHelper.onAuditUndeleteFailure (User.OT, sUserID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange disableUser (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sUserID,
                                              addLastModToUpdate (Updates.set (BSON_USER_DISABLED, Boolean.TRUE)));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (User.OT, "disable", sUserID);

      m_aCallbacks.forEach (aCB -> aCB.onUserEnabled (sUserID, false));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (User.OT, "disable", sUserID, "no-such-id");
    }
    return eChange;
  }

  @Override
  public @NonNull EChange enableUser (@Nullable final String sUserID)
  {
    if (StringHelper.isEmpty (sUserID))
      return EChange.UNCHANGED;

    final EChange eChange = genericUpdateOne (sUserID,
                                              addLastModToUpdate (Updates.set (BSON_USER_DISABLED, Boolean.FALSE)));
    if (eChange.isChanged ())
    {
      AuditHelper.onAuditModifySuccess (User.OT, "enable", sUserID);

      m_aCallbacks.forEach (aCB -> aCB.onUserEnabled (sUserID, true));
    }
    else
    {
      AuditHelper.onAuditModifyFailure (User.OT, "enable", sUserID, "no-such-id");
    }
    return eChange;
  }
}
