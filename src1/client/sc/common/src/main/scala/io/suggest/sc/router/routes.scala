package io.suggest.sc.router

import io.suggest.routes.StaticRoutesController
import io.suggest.sc.ScConstants.JsRouter.NAME
import io.suggest.sjs.common.model.Route

import scala.scalajs.js
import scala.scalajs.js.annotation.JSGlobal
import scala.scalajs.js.{Any, Dictionary}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.05.15 15:40
 * Description: Доступ к роутеру запросов к серверу suggest.io.
 */

@js.native
@JSGlobal(NAME)
object routes extends js.Object {
  def controllers: Ctls = js.native
}


/** Контроллеры роутера. */
@js.native
sealed trait Ctls extends StaticRoutesController {
  def Sc: ScCtl = js.native
}


/** Контроллер выдачи, а точнее его экшены. */
@js.native
sealed trait ScCtl extends js.Object {

  /** index выдачи для любой ситуации. */
  def index(args: Dictionary[Any]): Route = js.native

  /** Поиск рекламных карточек для плитки выдачи. */
  def findAds(adSearch: Dictionary[Any]): Route = js.native

  /** Роута для запроса списка узлов. */
  def findNodes(args: Dictionary[Any]): Route = js.native

  /** Роута для поиска focused-карточек. */
  def focusedAds(args: Dictionary[Any]): Route = js.native

  /** Роута поиска тегов. */
  def tagsSearch(args: Dictionary[Any]): Route = js.native

  /** Роута для автоматического сабмита ошибок на сервер. */
  def handleScError(): Route = js.native

}
