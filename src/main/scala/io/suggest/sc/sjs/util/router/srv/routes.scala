package io.suggest.sc.sjs.util.router.srv

import io.suggest.sc.ScConstants.JsRouter.NAME
import io.suggest.sjs.common.model.Route

import scala.scalajs.js
import scala.scalajs.js.{Dictionary, Any}

import scala.scalajs.js.annotation.JSName

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 15:40
 * Description: Доступ к роутеру запросов к серверу suggest.io.
 */

@js.native
@JSName(NAME)
object routes extends js.Object {
  def controllers: Ctls = js.native
}


/** Контроллеры роутера. */
@js.native
sealed trait Ctls extends js.Object {
  def MarketShowcase: ScCtl = js.native
}


/** Контроллер выдачи, а точнее его экшены. */
@js.native
sealed trait ScCtl extends js.Object {

  /**
   * index выдачи при известном id узла.
   * @param adnId id узла.
   */
  @JSName("showcase")
  def nodeIndex(adnId: String, args: Dictionary[Any]): Route = js.native

  /** index, когда узел неизвестен, и нужно, чтобы сервер сам определил узел. */
  @JSName("geoShowcase")
  def geoIndex(args: Dictionary[Any]): Route = js.native

  /** Поиск рекламных карточек для плитки выдачи. */
  def findAds(adSearch: Dictionary[Any]): Route = js.native

  /** Роута для запроса списка узлов. */
  def findNodes(args: Dictionary[Any]): Route = js.native

  /** Роута для поиска focused-карточек. */
  def focusedAds(args: Dictionary[Any]): Route = js.native

  /** Роута поиска тегов. */
  def tagsSearch(args: Dictionary[Any]): Route = js.native

}
