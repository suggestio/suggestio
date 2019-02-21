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
)

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
