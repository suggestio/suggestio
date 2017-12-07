package io.suggest.sc

import io.suggest.geo.MGeoPoint
import io.suggest.sc.root.m.RouteTo
import io.suggest.sc.root.v.ScRootR
import japgolly.scalajs.react.extra.router.{BaseUrl, Redirect, Router, RouterConfigDsl}
import japgolly.scalajs.react.extra.router.StaticDsl.RouteB
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq.UnivEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.17 14:25
  * Description: react-роутер для [[Sc3Main]].
  * Вынесен в трейт, чтобы не разводить кашу в коде.
  */

sealed trait Sc3Pages

object Sc3Pages {

  implicit def univEq: UnivEq[Sc3Pages] = UnivEq.derive

  object MainScreen {
    def empty = apply()
    implicit def univEq: UnivEq[MainScreen] = UnivEq.derive
  }
  /** Роута для основного экрана с какими-то доп.аргументами. */
  case class MainScreen(
                         nodeId         : Option[String]      = None,
                         generation     : Option[Long]        = None,
                         searchOpened   : Boolean             = false
                         //geoPoint       : Option[MGeoPoint]   = None
                       )
    extends Sc3Pages

}


/** Статическая сборка sjs-react-роутера. */
class Sc3Router(
                 sc3Circuit   : Sc3Circuit,
                 scRootR      : ScRootR
               ) {

  import Sc3Pages._
  import io.suggest.sc.root.m.MScRoot.MScRootFastEq

  val routerCfg = RouterConfigDsl[Sc3Pages].buildConfig { dsl =>

    import dsl._

    val keys = ScConstants.ScJsState

    def __mkOptRoute[T](key: String, t: RouteB[T]): RouteB[Option[T]] = {
      (key ~ "=" ~ t ~ "&").option
    }

    // Собираем роуту из кусков. Используем qs-формат для совместимости.

    val nodeIdP = string("[._a-zA-Z0-9-]+")

    val rcvrIdOptP = __mkOptRoute(keys.ADN_ID_FN, nodeIdP)

    val generationOptP = __mkOptRoute(keys.GENERATION_FN, long)

    // TODO Вынести эту роуту в отдельную утиль для sjs-react-ext.
    val booleanP = new RouteB[Boolean]("(true|false)", 1, g => Some(g(0).toBoolean), _.toString)

    /** URL-парсер/генератор для выяснения состояния открытости вкладки  */
    val searchOpenedP = __mkOptRoute(keys.CAT_SCR_OPENED_FN, booleanP)
      .withDefault(false)


    val mainScreenRoute = ("?" ~ rcvrIdOptP ~ generationOptP ~ searchOpenedP)
      .caseClass[MainScreen]
      .option  // Вообще ничего нет, всё равно это отхватываем.
      .withDefault( MainScreen.empty )

    // Кэшируем компонент ScRootR вне функций роутера, т.к. за ним следит только Sc3Circuit, а не роутер.
    val scRootWrapped = sc3Circuit.wrap(m => m)(scRootR.apply)
    (
      dynamicRouteCT[MainScreen](mainScreenRoute) ~> dynRender { page =>
        // Отправить распарсенные данные URL в circuit:
        sc3Circuit.dispatch( RouteTo(page) )
        // Вернуть исходный компонент. circuit сама перестроит её при необходимости:
        scRootWrapped
      }
    ).notFound {
      redirectToPage(MainScreen.empty)( Redirect.Replace )
    }
  }


  val (router, routerCtl) = Router.componentAndCtl(
    // TODO Когда v3 выдача станет дефолтом, лучше будет использовать fromWindowOrigin() НАВЕРНОЕ.
    //BaseUrl.fromWindowOrigin / "#!",
    baseUrl = BaseUrl.until_# + "#!",
    cfg     = routerCfg
  )

}
