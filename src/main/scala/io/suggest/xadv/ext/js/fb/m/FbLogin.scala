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
  scope         : String,
  returnScopes  : Boolean
) extends IToJsonDict {
  override def toJson: Dictionary[Any] = {
    Dictionary[Any](
      "scope"         -> scope,
      "return_scopes" -> returnScopes
    )
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


