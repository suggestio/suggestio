package io.suggest.sc

import io.suggest.common.empty.OptionUtil
import io.suggest.geo._
import io.suggest.text.parse.ParserUtil
import io.suggest.common.html.HtmlConstants.{`(`, `)`}
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.m.{RouteTo, Sc3Pages}
import io.suggest.sc.m.Sc3Pages._
import io.suggest.sc.m.search.MSearchTabs
import io.suggest.sc.v.ScRootR
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.text.UrlUtil2
import japgolly.scalajs.react.extra.router.{BaseUrl, Path, Redirect, Router, RouterConfigDsl}
import japgolly.scalajs.react.extra.router.StaticDsl.RouteB
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.17 14:25
  * Description: react-роутер для [[Sc3Main]].
  * В отличии от scalajs-spa-tutorial, этот роутер живёт за пределами [[Sc3Main]], чтобы не разводить кашу в коде.
  */
class Sc3Router(
                 sc3Circuit   : Sc3Circuit,
                 scRootR      : ScRootR
               )
  extends Log
{

  import io.suggest.sc.m.MScRoot.MScRootFastEq

  val baseUrlSuffix = "#!"
  val baseUrl = BaseUrl.until_# + baseUrlSuffix

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

    // + в регэкспе на случай будущего идентификатора таба длиной в две и более букв.
    val searchTabP = string("[" + MSearchTabs.values.iterator.map(_.value).mkString + "]+")
      .pmap( MSearchTabs.withValueOpt )(_.value)

    val currentTabP = __mkOptRoute(keys.SEARCH_TAB_FN, searchTabP)

    val mGeoPointP = string {
      val doubleRE = ParserUtil.DOUBLE_RE_STR
      `(` + doubleRE + """\""" + GeoConstants.Qs.LAT_LON_DELIM_FN + doubleRE + `)`
    }
      .pmap { raw =>
        MGeoPoint.fromString(raw)
          .filter( MGeoPoint.isValid )
      }(_.toString)

    val locEnvOptP = __mkOptRoute(keys.LOC_ENV_FN, mGeoPointP)

    val tagNodeIdP = __mkOptRoute(keys.TAG_NODE_ID_FN, nodeIdP)

    val mainScreenOptRoute = ("?" ~ rcvrIdOptP ~ searchOpenedP ~ currentTabP ~ generationOptP ~ tagNodeIdP ~ locEnvOptP)
      .caseClass[MainScreen]
      .option

    // Ковырять значение
    var isAlreadyUsedCanonical = false

    val mainScreenDfltRoute = mainScreenOptRoute
      .withDefault {
        // Залезть в link rel=canonical и распарсить там чего-нибудь.
        // Это используется при подключении сторонних доменов к s.io на уровне siteTpl.
        val mainScrOpt = OptionUtil.maybeOpt( !isAlreadyUsedCanonical ) {
          isAlreadyUsedCanonical = true
          try {
            val iter = for {
              link        <- DocumentVm().head.links
              if link.isCanonical
              href        <- link.href
              urlHash     <- UrlUtil2.getUrlHash(href)
              if urlHash.nonEmpty
              // TODO Надо нормальный парсер, не капризный к порядку или &
              urlHash2 = urlHash.replace(baseUrlSuffix, "") + "&"
              parsedOpt   <- mainScreenOptRoute.route.parse( Path(urlHash2) )
              parsed      <- parsedOpt
            } yield {
              parsed
            }
            iter
              .toStream
              .headOption
          } catch {
            case ex: Throwable =>
              LOG.error( ErrorMsgs.CANONICAL_URL_FAILURE, ex )
              None
          }
        }
        mainScrOpt.getOrElse( MainScreen.empty )
      }

    // Кэшируем компонент ScRootR вне функций роутера, т.к. за ним следит только Sc3Circuit, а не роутер.
    val scRootWrapped = sc3Circuit.wrap(m => m)(scRootR.apply)

    val mainScreenRule = dynamicRouteCT[MainScreen](mainScreenDfltRoute) ~> dynRender { page =>
      // Отправить распарсенные данные URL в circuit:
      sc3Circuit.dispatch( RouteTo(page) )
      // Вернуть исходный компонент. circuit сама перестроит её при необходимости:
      scRootWrapped
    }

    mainScreenRule
      .notFound {
        redirectToPage(MainScreen.empty)( Redirect.Replace )
      }
  }


  val (router, routerCtl) = Router.componentAndCtl(
    // TODO Когда v3 выдача станет дефолтом, лучше будет использовать fromWindowOrigin() НАВЕРНОЕ.
    //BaseUrl.fromWindowOrigin / "#!",
    baseUrl = baseUrl,
    cfg     = routerCfg
  )

}
