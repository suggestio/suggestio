package io.suggest.sc

import java.net.URI

import io.suggest.common.empty.OptionUtil
import io.suggest.geo._
import io.suggest.msg.ErrorMsgs
import io.suggest.log.Log
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.spa.{MGen, SioPages}
import io.suggest.text.UrlUtilJs
import japgolly.scalajs.react.extra.router.{BaseUrl, Path, Redirect, Router, RouterConfigDsl}
import japgolly.scalajs.react.vdom.html_<^._
import OptionUtil.BoolOptOps
import io.suggest.id.login.MLoginTabs
import io.suggest.sc.m.boot.MSpaRouterState
import japgolly.univeq._
import io.suggest.spa.DiodeUtil.Implicits._

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
                    renderSc3F        : (SioPages.Sc3) => VdomElement,
                  )
  extends Log
{

  /** Всё состояние роутера и связанные данные живут здесь: */
  val state: MSpaRouterState = RouterConfigDsl[SioPages.Sc3].use { dsl =>
    import dsl._

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

          // TODO Тут дублируется MainScreen.FORMAT.

          def _boolOptTok(key: String): Option[Boolean] = {
            tokens
              .get( key )
              .flatMap { boolStr =>
                Try(boolStr.toBoolean).toOption
              }
          }
          def _boolOrFalseTok(key: String): Boolean =
            _boolOptTok(key).getOrElseFalse

          val K = ScConstants.ScJsState
          SioPages.Sc3(
            nodeId = tokens.get( K.NODE_ID_FN ),
            searchOpened = _boolOrFalseTok( K.SEARCH_OPENED_FN ),
            generation = tokens.get( K.GENERATION_FN )
              .flatMap( MGen.parse ),
            tagNodeId = tokens.get( K.TAG_NODE_ID_FN ),
            locEnv = tokens.get( K.LOC_ENV_FN )
              .flatMap( MGeoPoint.fromString ),
            menuOpened = _boolOrFalseTok( K.MENU_OPENED_FN ),
            focusedAdId = tokens.get( K.FOCUSED_AD_ID_FN ),
            firstRunOpen = _boolOrFalseTok( K.FIRST_RUN_OPEN_FN ),
            dlAppOpen = _boolOrFalseTok( K.DL_APP_OPEN_FN ),
            settingsOpen = _boolOrFalseTok( K.SETTINGS_OPEN_FN ),
            showWelcome = _boolOptTok( K.SHOW_WELCOME_FN ).getOrElseTrue,
            virtBeacons = tokens.view
              .filterKeys(_ startsWith K.VIRT_BEACONS_FN)
              .valuesIterator
              .toSet,
            login = for {
              currTabIdRaw <- tokens.get( K.LOGIN_FN )
              currTabId <- Try( currTabIdRaw.toInt ).toOption
              currTab   <- MLoginTabs.withValueOpt( currTabId )
            } yield {
              SioPages.Login( currTab )
            },
          )
        }
      } { mainScreen =>
        val K = ScConstants.ScJsState

        // Портировано из sc v2 MScSd.toUrlHashAcc()
        // Сериализация роуты назад в строку.
        var acc: List[(String, String)] = Nil

        // TODO Тут дублируется логика MainScreen.FORMAT + jsRouter._o2qs(). Может как-то унифицировать это дело?

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
          acc ::= K.SEARCH_OPENED_FN -> mainScreen.searchOpened.toString

        // Отработать открытое меню.
        if (mainScreen.menuOpened)
          acc ::= K.MENU_OPENED_FN -> mainScreen.menuOpened.toString

        // Открыт диалог первого запуска?
        if (mainScreen.firstRunOpen)
          acc ::= K.FIRST_RUN_OPEN_FN -> mainScreen.firstRunOpen.toString

        if (mainScreen.dlAppOpen)
          acc ::= K.DL_APP_OPEN_FN -> mainScreen.dlAppOpen.toString

        if (mainScreen.settingsOpen)
          acc ::= K.SETTINGS_OPEN_FN -> mainScreen.settingsOpen.toString

        if (!mainScreen.showWelcome)
          acc ::= K.SHOW_WELCOME_FN -> mainScreen.showWelcome.toString

        for {
          (bcnId, i) <- mainScreen.virtBeacons.iterator.zipWithIndex
        }
          acc ::= s"${K.VIRT_BEACONS_FN}[$i]" -> bcnId

        for (login <- mainScreen.login)
          acc ::= K.LOGIN_FN -> login.currTab.value.toString

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
        hrefUrl = new URI( href )
        urlQuery     <- Option( hrefUrl.getRawQuery )
        if urlQuery.nonEmpty
        urlHash2 = "?" + urlQuery + "&"
        parsed <- {
          val r = mainScreenOptRoute.route
            .parse( Path(urlHash2) )
            .flatten
          if (r.isEmpty)
            logger.error( ErrorMsgs.URL_PARSE_ERROR, msg = urlHash2 )
          r
        }
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
          .getOrElse( SioPages.Sc3.empty )
      }

    // Кэшируем компонент ScRootR вне функций роутера, т.к. за ним следит только Sc3Circuit, а не роутер.
    val mainScreenRule = dynamicRouteCT[SioPages.Sc3](mainScreenDfltRoute) ~> dynRender( renderSc3F )

    val routerCfg = mainScreenRule
      .notFound {
        redirectToPage( SioPages.Sc3.empty )( Redirect.Replace )
      }

    val (r, rCtl) = Router.componentAndCtl[SioPages.Sc3](
      baseUrl = BaseUrl.fromWindowOrigin_/,
      cfg     = routerCfg
    )

    MSpaRouterState(
      router          = r,
      routerCtl       = rCtl,
      canonicalRoute  = canonicalRoute
    )
  }

}
