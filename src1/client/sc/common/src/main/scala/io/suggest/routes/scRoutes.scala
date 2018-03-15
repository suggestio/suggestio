package io.suggest.routes

import io.suggest.sc.ScConstants.JsRouter.NAME
import io.suggest.sjs.common.model.Route

import scala.language.implicitConversions
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
object scRoutes extends IJsRouter


@js.native
sealed trait JsRoutes_ScControllers extends js.Object {
  val Sc: ScController = js.native
}
object JsRoutes_ScControllers {
  implicit def toScControllers(controllers: Controllers): JsRoutes_ScControllers = {
    controllers.asInstanceOf[JsRoutes_ScControllers]
  }
}


/** Контроллер выдачи, а точнее его экшены. */
@js.native
sealed trait ScController extends js.Object {

  /** Ссылка на корень. */
  //def geoSite(scJsState: js.Any = js.undefined, siteQsArgs: js.Any = js.undefined): Route = js.native

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
