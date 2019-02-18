package io.suggest.sc

import io.suggest.common.empty.OptionUtil
import io.suggest.geo._
import io.suggest.msg.{ErrorMsgs, WarnMsgs}
import io.suggest.sc.m.{MScReactCtx, RouteTo}
import io.suggest.sc.v.ScRootR
import io.suggest.sjs.common.log.Log
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.spa.MGen
import io.suggest.text.{UrlUtil2, UrlUtilJs}
import japgolly.scalajs.react.extra.router.{BaseUrl, Path, Redirect, Router, RouterConfigDsl, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^._
import OptionUtil.BoolOptOps
import io.suggest.sc.sc3.Sc3Pages
import io.suggest.sc.sc3.Sc3Pages.MainScreen
import japgolly.scalajs.react.React

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
                    sc3CircuitF       : RouterCtl[Sc3Pages] => Sc3Circuit,
                    scRootR           : () => ScRootR,
                  )
  extends Log
{

  val routerAndCtl: (Router[Sc3Pages], RouterCtl[Sc3Pages]) = {
    val baseUrlSuffix = "#!"

    /** Конфиг роутера второго поколения.
      * Он больше похож на sc2-роутер, который вручную токенайзит и парсит qs-части.
      */
    val routerCfg = RouterConfigDsl[Sc3Pages].buildConfig { dsl =>
      import dsl._

      val keys = ScConstants.ScJsState

      val mainScreenOptRoute = ("?" ~ string(".+"))
        .option
        .pmap { qsOpt =>
          for (qs <- qsOpt) yield {
            // Токенайзер и парсер скопирован из sc2 (sc-sjs/MScSd)
            val tokens = qs.split('&')
              .iterator
              .flatMap { kvStr =>
                if (kvStr.isEmpty) {
                  Nil
                } else {
                  kvStr.split('=') match {
                    case arr if arr.length == 2 =>
                      val arr2 = arr.iterator
                        .map(URIUtils.decodeURIComponent)
                      val k2 = arr2.next()
                      val v2 = arr2.next()
                      (k2 -> v2) :: Nil

                    case other =>
                      LOG.warn( WarnMsgs.MSC_STATE_URL_HASH_UNKNOWN_TOKEN, msg = other )
                      Nil
                  }
                }
              }
              .toMap

            def _boolOrFalseTok(key: String): Boolean = {
              tokens.get( key )
                .flatMap { boolStr =>
                  Try(boolStr.toBoolean).toOption
                }
                .getOrElseFalse
            }

            MainScreen(
              nodeId = tokens.get( keys.NODE_ID_FN ),
              searchOpened = _boolOrFalseTok( keys.CAT_SCR_OPENED_FN ),
              generation = tokens.get( keys.GENERATION_FN )
                .flatMap( MGen.parse ),
              tagNodeId = tokens.get( keys.TAG_NODE_ID_FN ),
              locEnv = tokens.get( keys.LOC_ENV_FN )
                .flatMap( MGeoPoint.fromString ),
              menuOpened = _boolOrFalseTok( keys.GEO_SCR_OPENED_FN ),
              focusedAdId = tokens.get( keys.FOCUSED_AD_ID_FN ),
              firstRunOpen = _boolOrFalseTok( keys.FIRST_RUN_OPEN_FN ),
            )
          }
        } { mainScreen =>
          // Портировано из sc v2 MScSd.toUrlHashAcc()
          // Сериализация роуты назад в строку.
          var acc: List[(String, String)] = Nil

          // Пока пишем generation, но наверное это лучше отключить, чтобы в режиме iOS webapp не было повторов.
          for (gen <- mainScreen.generation)
            acc ::= keys.GENERATION_FN -> MGen.serialize(gen)

          // TODO Отработка состояния левой панели.
          //val npo = sd0.nav.panelOpened
          //if (npo)
          //  acc ::= GEO_SCR_OPENED_FN -> npo

          // Сериализация loc-env.
          for (geoLoc <- mainScreen.locEnv)
            acc ::= keys.LOC_ENV_FN -> geoLoc.toString

          // Отработать id текущего узла.
          for (nodeId <- mainScreen.nodeId)
            acc ::= keys.NODE_ID_FN -> nodeId

          // TODO Использовать GeoLoc для маячков. Проблема в том, что функция-сериализатор JSON в QS _o2qs() лежит в js-роутере, а не здесь.
          //val locEnv: ILocEnv = mainScreen.???
          //if (MLocEnv.nonEmpty(locEnv))
          //  acc ::= keys.LOC_ENV_FN -> MLocEnv.toJson(locEnv)

          // Сериализовать данные по тегам.
          for (tagNodeId <- mainScreen.tagNodeId) {
            acc ::= keys.TAG_NODE_ID_FN -> tagNodeId
            // TODO  TAG_FACE_FN -> tagInfo.face
          }

          // Отработать focused-выдачу, если она активна:
          for (focusedAdId <- mainScreen.focusedAdId) yield {
            acc ::= keys.FOCUSED_AD_ID_FN -> focusedAdId
          }

          // Отрабатываем состояние правой панели.
          if (mainScreen.searchOpened)
            acc ::= keys.CAT_SCR_OPENED_FN -> mainScreen.searchOpened.toString

          // Отработать открытое меню.
          if (mainScreen.menuOpened)
            acc ::= keys.GEO_SCR_OPENED_FN -> mainScreen.menuOpened.toString

          // Открыт диалог первого запуска?
          if (mainScreen.firstRunOpen)
            acc ::= keys.FIRST_RUN_OPEN_FN -> mainScreen.firstRunOpen.toString

          val queryString = UrlUtilJs.qsPairsToString(acc)
          Some( queryString )
        }
        .option

      // var-флаг для подавления повторных десериализаций состояния из link rel=canonical.
      // Выставляется в true после подобной десериализации.
      var isAlreadyUsedCanonical = false
      // Поддержка 3p-доменов: выдача тут инициализируется через link rel canonical.
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
                .buffered
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
      val mainScreenRule = dynamicRouteCT[MainScreen](mainScreenDfltRoute) ~> dynRender( _renderScMainScreen )

      mainScreenRule
        .notFound {
          redirectToPage(MainScreen.empty)( Redirect.Replace )
        }
    }

    Router.componentAndCtl[Sc3Pages](
      // TODO Когда v3 выдача станет дефолтом, лучше будет использовать fromWindowOrigin() НАВЕРНОЕ.
      //BaseUrl.fromWindowOrigin / "#!",
      baseUrl = BaseUrl.until_# + baseUrlSuffix,
      cfg     = routerCfg
    )
  }
  def router = routerAndCtl._1
  def routerCtl = routerAndCtl._2


  // ----- MAIN SCREEN ------

  // Готовые инстансы вызываются только из функций роутера, поэтому их безопасно дёргать отсюда.
  val sc3Circuit = sc3CircuitF( routerCtl )

  /** Сборка контекста. val по возможности, но может быть и def. */
  val mkScReactCtx = MScReactCtx( sc3Circuit.scCssRO.apply, routerCtl )

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
