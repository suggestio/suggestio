package io.suggest.xadv.ext.js.fb.m

import io.suggest.xadv.ext.js.runner.m.IToJsonDict

import scala.scalajs.js
import scala.scalajs.js.{WrappedDictionary, Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.03.15 11:09
 * Description: Модель параметров запуска процедуры логина.
 */
object FbLoginArgs {

  /** Разрешение на публикацию. */
  def SCOPE_PUBLISH_ACTIONS = "publish_actions"

  /** Разрешение на доступ к страницам. */
  def SCOPE_MANAGE_PAGES    = "manage_pages"

  /** Все перечисленные права доступа одной строкой. */
  def ALL_RIGHTS = SCOPE_PUBLISH_ACTIONS + "," + SCOPE_MANAGE_PAGES
}


case class FbLoginArgs(scope: String) extends IToJsonDict {
  override def toJson: Dictionary[Any] = {
    Dictionary[Any](
      "scope" -> scope
    )
  }
}


object FbLoginResult {

  /**
   * Десериализация из fb login response.
   * @param resp Результат работы FB.login().
   * @return Экземпляр [[FbLoginResult]].
   */
  def fromLoginResp(resp: js.Dictionary[js.Any]): FbLoginResult = {
    val d = resp: WrappedDictionary[js.Any]
    FbLoginResult(
      hasAuthResp = d contains "authResponse"
    )
  }

}

/** Результат логина. */
case class FbLoginResult(hasAuthResp: Boolean)
