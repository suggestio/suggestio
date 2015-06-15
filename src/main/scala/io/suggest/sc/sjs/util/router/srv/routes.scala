package io.suggest.sc.sjs.util.router.srv

import io.suggest.sc.ScConstants.JS_ROUTER_NAME
import io.suggest.sc.sjs.m.msrv.index.MScReqArgsJson
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

@JSName(JS_ROUTER_NAME)
object routes extends js.Object {
  def controllers: Ctls = js.native
}


/** Контроллеры роутера. */
trait Ctls extends js.Object {
  def MarketShowcase: ScCtl = js.native
}


/** Контроллер выдачи, а точнее его экшены. */
trait ScCtl extends js.Object {

  /**
   * index выдачи при известном id узла.
   * @param adnId id узла.
   */
  @JSName("showcase")
  def nodeIndex(adnId: String, args: MScReqArgsJson): Route = js.native

  /** index, когда узел неизвестен, и нужно, чтобы сервер сам определил узел. */
  @JSName("geoShowcase")
  def geoIndex(args: MScReqArgsJson): Route = js.native

  /** Поиск рекламных карточек для плитки выдачи. */
  def findAds(adSearch: Dictionary[Any]): Route = js.native

  /** Роута для запроса списка узлов. */
  def findNodes(args: Dictionary[Any]): Route = js.native

  /** Роута для поиска focused-карточек. */
  def focusedAds(args: Dictionary[Any]): Route = js.native

}
