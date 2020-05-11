package io.suggest.sc

import io.suggest.common.empty.OptionUtil
import io.suggest.geo._
import io.suggest.msg.ErrorMsgs
import io.suggest.sc.m.{MScReactCtx, RouteTo}
import io.suggest.sc.v.ScRootR
import io.suggest.log.Log
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.spa.MGen
import io.suggest.text.{UrlUtil2, UrlUtilJs}
import japgolly.scalajs.react.extra.router.{BaseUrl, Path, Redirect, Router, RouterConfigDsl}
import japgolly.scalajs.react.vdom.html_<^._
import OptionUtil.BoolOptOps
import io.suggest.sc.m.boot.MSpaRouterState
import io.suggest.sc.sc3.Sc3Pages
import io.suggest.sc.sc3.Sc3Pages.MainScreen
import japgolly.scalajs.react.React
import japgolly.univeq._

import scala.scalajs.js.URIUtils
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.12.17 14:25
  * Description: react-роутер для [[Sc3Main]].
  * В отличии от scalajs-spa-tutorial, этот роутер живёт за пределами [[Sc3Main]], чтобы не разводить кашу в коде.
  */
class Sc3SpaRouter(
                    scReactCtxContF   : () => React.Context[MScReactCtx],
                    sc3CircuitF       : MSpaRouterState => Sc3Circuit,
                    scRootR           : () => ScRootR,
                  )
  extends Log
{

  /** Всё состояние роутера и связанные данные живут здесь: */
  val state: MSpaRouterState = {
    val dsl = new RouterConfigDsl[Sc3Pages]
    import dsl._

    val baseUrlSuffix = "#!"

    /** Конфиг роутера второго поколения.
      * Он больше похож на sc2-роутер, который вручную токенайзит и парсит qs-части.
      */

    val mainScreenOptRoute = ("?" ~ string(".+"))
      .option
      .pmap { qsOpt =>
        for (qs <- qsOpt) yield {
          // Токенайзер и парсер скопирован из sc2 (sc-sjs/MScSd)
          val tokens = qs
            .split('&')
            .iterator
            .flatMap { kvStr =>
              if (kvStr.isEmpty) {
                Nil
              } else {
                kvStr.split('=') match {
                  case arr if arr.length ==* 2 =>
                    val arr2 = arr.iterator
                      .map(URIUtils.decodeURIComponent)
                    val k2 = arr2.next()
                    val v2 = arr2.next()
                    (k2 -> v2) :: Nil

                  case other =>
                    logger.warn( ErrorMsgs.SC_URL_HASH_UNKNOWN_TOKEN, msg = other )
                    Nil
                }
              }
            }
            .toMap

          def _boolOrFalseTok(key: String): Boolean = {
            tokens
              .get( key )
              .flatMap { boolStr =>
                Try(boolStr.toBoolean).toOption
              }
              .getOrElseFalse
          }

          val K = ScConstants.ScJsState
          MainScreen(
            nodeId = tokens.get( K.NODE_ID_FN ),
            searchOpened = _boolOrFalseTok( K.CAT_SCR_OPENED_FN ),
            generation = tokens.get( K.GENERATION_FN )
              .flatMap( MGen.parse ),
            tagNodeId = tokens.get( K.TAG_NODE_ID_FN ),
            locEnv = tokens.get( K.LOC_ENV_FN )
              .flatMap( MGeoPoint.fromString ),
            menuOpened = _boolOrFalseTok( K.GEO_SCR_OPENED_FN ),
            focusedAdId = tokens.get( K.FOCUSED_AD_ID_FN ),
            firstRunOpen = _boolOrFalseTok( K.FIRST_RUN_OPEN_FN ),
          )
        }
      } { mainScreen =>
        val K = ScConstants.ScJsState

        // Портировано из sc v2 MScSd.toUrlHashAcc()
        // Сериализация роуты назад в строку.
        var acc: List[(String, String)] = Nil

        // Пока пишем generation, но наверное это лучше отключить, чтобы в режиме iOS webapp не было повторов.
        for (gen <- mainScreen.generation)
          acc ::= K.GENERATION_FN -> MGen.serialize(gen)

        // TODO Отработка состояния левой панели.
        //val npo = sd0.nav.panelOpened
        //if (npo)
        //  acc ::= GEO_SCR_OPENED_FN -> npo

        // Сериализация loc-env.
        for (geoLoc <- mainScreen.locEnv)
          acc ::= K.LOC_ENV_FN -> geoLoc.toString

        // Отработать id текущего узла.
        for (nodeId <- mainScreen.nodeId)
          acc ::= K.NODE_ID_FN -> nodeId

        // TODO Использовать GeoLoc для маячков. Проблема в том, что функция-сериализатор JSON в QS _o2qs() лежит в js-роутере, а не здесь.
        //val locEnv: ILocEnv = mainScreen.???
        //if (MLocEnv.nonEmpty(locEnv))
        //  acc ::= keys.LOC_ENV_FN -> MLocEnv.toJson(locEnv)

        // Сериализовать данные по тегам.
        for (tagNodeId <- mainScreen.tagNodeId) {
          acc ::= K.TAG_NODE_ID_FN -> tagNodeId
          // TODO  TAG_FACE_FN -> tagInfo.face
        }

        // Отработать focused-выдачу, если она активна:
        for (focusedAdId <- mainScreen.focusedAdId) yield {
          acc ::= K.FOCUSED_AD_ID_FN -> focusedAdId
        }

        // Отрабатываем состояние правой панели.
        if (mainScreen.searchOpened)
          acc ::= K.CAT_SCR_OPENED_FN -> mainScreen.searchOpened.toString

        // Отработать открытое меню.
        if (mainScreen.menuOpened)
          acc ::= K.GEO_SCR_OPENED_FN -> mainScreen.menuOpened.toString

        // Открыт диалог первого запуска?
        if (mainScreen.firstRunOpen)
          acc ::= K.FIRST_RUN_OPEN_FN -> mainScreen.firstRunOpen.toString

        val queryString = UrlUtilJs.qsPairsToString(acc)
        Some( queryString )
      }
      .option

    // Сразу парсим роуту, т.к. она передаётся в circuit.
    val canonicalRoute = try {
      (for {
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
      })
        .nextOption()
    } catch {
      case ex: Throwable =>
        logger.error( ErrorMsgs.CANONICAL_URL_FAILURE, ex )
        None
    }

    // var-флаг для подавления повторных десериализаций состояния из link rel=canonical.
    // Выставляется в true после подобной десериализации.
    var isAlreadyUsedCanonical = false
    // Поддержка 3p-доменов: выдача тут инициализируется через link rel canonical.
    val mainScreenDfltRoute = mainScreenOptRoute
      .withDefault {
        // Залезть в link rel=canonical и распарсить там чего-нибудь.
        // Это используется при подключении сторонних доменов к s.io на уровне siteTpl.
        OptionUtil.maybeOpt( !isAlreadyUsedCanonical ) {
          isAlreadyUsedCanonical = true
          canonicalRoute
        }
          .getOrElse( MainScreen.empty )
      }

    // Кэшируем компонент ScRootR вне функций роутера, т.к. за ним следит только Sc3Circuit, а не роутер.
    val mainScreenRule = dynamicRouteCT[MainScreen](mainScreenDfltRoute) ~> dynRender( _renderScMainScreen )

    val routerCfg = mainScreenRule
      .notFound {
        redirectToPage(MainScreen.empty)( Redirect.Replace )
      }

    val (r, rCtl) = Router.componentAndCtl[Sc3Pages](
      // TODO Когда v3 выдача станет дефолтом, лучше будет использовать fromWindowOrigin() НАВЕРНОЕ.
      //BaseUrl.fromWindowOrigin / "#!",
      baseUrl = BaseUrl.until_# + baseUrlSuffix,
      cfg     = routerCfg
    )

    MSpaRouterState(
      router          = r,
      routerCtl       = rCtl,
      canonicalRoute  = canonicalRoute
    )
  }


  // ----- MAIN SCREEN ------

  // Готовые инстансы вызываются только из функций роутера, поэтому их безопасно дёргать отсюда.
  val sc3Circuit = sc3CircuitF( state )

  /** Сборка контекста. val по возможности, но может быть и def. */
  val mkScReactCtx = MScReactCtx( sc3Circuit.scCssRO.apply, state.routerCtl )

  /** Отрендеренный компонент ScRootR с инициализированным глобальным react-контекстом выдачи.
    * Лениво! чтобы запретить доступ к инстансам sc-контекста и ScRootR до завершения конструктора [[Sc3SpaRouter]],
    * иначе будет зацикливание, т.к. шаблоны (react-компоненты) и роутер взаимно нуждаются в инстансах друг друга.
    */
  private lazy val _scRootWrapped = {
    // Внутренняя react-подписка нижележащих компонентов на новый инстанс цепи.
    scReactCtxContF().provide( mkScReactCtx )(
      sc3Circuit.wrap( identity(_) ) {
        scRootR().apply
      }
    )
  }

  /** Функция рендера выдачи, чтобы явно разделить в конструкторе val router-конфига и остальные поля конструктора. */
  private def _renderScMainScreen(page: MainScreen) = {
    // Отправить распарсенные данные URL в circuit:
    sc3Circuit.dispatch( RouteTo(page) )
    // Вернуть исходный компонент. circuit сама перестроит её при необходимости:
    _scRootWrapped
  }

  // ----- END MAIN SCREEN ------

}
