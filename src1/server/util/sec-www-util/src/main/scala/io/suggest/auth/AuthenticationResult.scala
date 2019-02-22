package io.suggest.auth

import play.api.mvc.Result

/**
  * Represents different results of the authentication flow.
  */
sealed trait AuthenticationResult


object AuthenticationResult {

  /**
   * A user denied access to their account while authenticating with an external provider (eg: Twitter)
   */
  case class AccessDenied() extends AuthenticationResult

  /**
   * An intermetiate result during the authentication flow (maybe a redirection to the external provider page)
   */
  case class NavigationFlow(result: Result) extends AuthenticationResult

  /**
   * Returned when the user was succesfully authenticated
   * @param profile the authenticated user profile
   */
  case class Authenticated(profile: UserProfile) extends AuthenticationResult

  /**
   * Returned when the authentication process failed for some reason.
   * @param error a description of the error
   */
  case class Failed(error: String) extends AuthenticationResult

}

