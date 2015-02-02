/**
 * Copyright 2012-2014 Jorge Aliss (jaliss at gmail dot com) - twitter: @jaliss
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package securesocial.core

/**
 * A minimal user profile
 */
trait IProfileBase {
  def providerId: String
  def userId: String
}
trait IProfileBaseWrap extends IProfileBase {
  def _underlying: IProfileBase
  override def providerId = _underlying.providerId
  override def userId     = _underlying.userId
}

/**
 * A generic profile
 */
trait IProfile extends IProfileBase {
  def firstName     : Option[String]
  def lastName      : Option[String]
  def fullName      : Option[String]
  def email         : Option[String]
  def avatarUrl     : Option[String]
  def authMethod    : AuthenticationMethod
  def oAuth1Info    : Option[OAuth1Info]
  def oAuth2Info    : Option[OAuth2Info]
  def passwordInfo  : Option[PasswordInfo]
}

trait IProfileWrap extends IProfile with IProfileBaseWrap {
  override def _underlying: IProfile

  override def firstName    = _underlying.firstName
  override def lastName     = _underlying.lastName
  override def fullName     = _underlying.fullName
  override def oAuth1Info   = _underlying.oAuth1Info
  override def oAuth2Info   = _underlying.oAuth2Info
  override def avatarUrl    = _underlying.avatarUrl
  override def passwordInfo = _underlying.passwordInfo
  override def authMethod   = _underlying.authMethod
  override def email        = _underlying.email
}

trait IProfileDflt extends IProfile {
  override def firstName    : Option[String]        = None
  override def lastName     : Option[String]        = None
  override def fullName     : Option[String]        = None
  override def email        : Option[String]        = None
  override def avatarUrl    : Option[String]        = None
  override def oAuth2Info   : Option[OAuth2Info]    = None
  override def oAuth1Info   : Option[OAuth1Info]    = None
  override def passwordInfo : Option[PasswordInfo]  = None
}


/**
 * An implementation of the GenericProfile
 */
case class Profile(
  providerId    : String,
  userId        : String,
  firstName     : Option[String]        = None,
  lastName      : Option[String]        = None,
  fullName      : Option[String]        = None,
  email         : Option[String]        = None,
  avatarUrl     : Option[String]        = None,
  authMethod    : AuthenticationMethod,
  oAuth1Info    : Option[OAuth1Info]    = None,
  oAuth2Info    : Option[OAuth2Info]    = None,
  passwordInfo  : Option[PasswordInfo]  = None
) extends IProfile

/**
 * The OAuth 1 details
 *
 * @param token the token
 * @param secret the secret
 */
case class OAuth1Info(token: String, secret: String)

/**
 * The Oauth2 details
 *
 * @param accessToken the access token
 * @param tokenType the token type
 * @param expiresIn the number of seconds before the token expires
 * @param refreshToken the refresh token
 */
case class OAuth2Info(accessToken: String, tokenType: Option[String] = None,
  expiresIn: Option[Int] = None, refreshToken: Option[String] = None)

/**
 * The password details
 *
 * @param hasher the id of the hasher used to hash this password
 * @param password the hashed password
 * @param salt the optional salt used when hashing
 */
case class PasswordInfo(hasher: String, password: String, salt: Option[String] = None)
