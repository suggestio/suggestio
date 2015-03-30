package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.{FromJsonT, IToJsonDict}

import scala.scalajs.js
import scala.scalajs.js.{WrappedDictionary, Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.15 11:09
 * Description: Модель параметров запуска процедуры логина.
 */

case class FbLoginArgs(
  scopes        : TraversableOnce[FbPermission] = Nil,
  returnScopes  : Option[Boolean] = None,
  authType      : Option[FbAuthType] = None
) extends IToJsonDict {
  override def toJson: Dictionary[Any] = {
    val d = Dictionary[Any]()
    if (scopes.nonEmpty)
      d.update("scope", FbPermissions.permsToString(scopes))
    if (returnScopes.nonEmpty)
      d.update("return_scopes", returnScopes.get)
    if (authType.nonEmpty)
      d.update("auth_type", authType.get.fbType)
    d
  }
}


/**
 * Модель результатов вызовов login() и getLoginStatus().
 * @see [[https://developers.facebook.com/docs/reference/javascript/FB.getLoginStatus#response_and_session_objects]]
 */
object FbLoginResult {

  /**
   * Десериализация из fb login response:
   * @param resp Результат работы FB.login().
   * @return Экземпляр [[FbLoginResult]].
   */
  def fromLoginResp(resp: js.Dictionary[js.Any]): FbLoginResult = {
    val d = resp: WrappedDictionary[js.Any]
    FbLoginResult(
      status = d.get("status")
        .flatMap { v => FbLoginStatuses.maybeWithName(v.toString) }
        .get,
      authResp = d.get("authResponse")
        .map(FbAuthResponse.fromJson)
    )
  }

}

/** Результат логина. */
case class FbLoginResult(
  status    : FbLoginStatus,
  authResp  : Option[FbAuthResponse]
)


