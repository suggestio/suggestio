package io.suggest.id.login.v

import io.suggest.ReactCommonModule
import io.suggest.i18n.MCommonReactCtx
import io.suggest.id.login.{ILoginFormPages, LoginFormCircuit, MLoginTabs}
import io.suggest.id.login.m.MLoginRootS
import japgolly.univeq._
import japgolly.scalajs.react.extra.router.{BaseUrl, Redirect, Router, RouterConfigDsl, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^._

import scala.scalajs.js.URIUtils
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.03.19 16:18
  * Description: SPA-роутер для формы логина.
  * Сделан для поддержки return-url (?r=...), для возможности сборки ссылок сразу на нужную страницу
  * (из /sys полезно например - там всё по паролю), и просто так лучше.
  */
class LoginFormSpaRouter(
                          loginFormR    : () => LoginFormR,
                          circuitF      : RouterCtl[ILoginFormPages] => LoginFormCircuit,
                        ) {

  /** Сборка инстансов SPA-роутера и контроллер роутера. */
  val routerAndCtl: (Router[ILoginFormPages], RouterCtl[ILoginFormPages]) = {
    val qMark = "?"

    val routerCfg = RouterConfigDsl[ILoginFormPages].buildConfig { dsl =>
      import dsl._
      import ILoginFormPages.Login.Fields._

      val loginFormRoute = (qMark ~ string(".+"))
        .pmap { qsStr =>
          val eqSign = '='
          var updatesAcc = List.empty[ILoginFormPages.Login => ILoginFormPages.Login]
          for {
            qsKv <- qsStr.split('&')
            if qsKv.nonEmpty
            (k, v) <- {
              val pos = qsKv.indexOf(eqSign)
              if (pos > 0) {
                val k1 = URIUtils.decodeURIComponent( qsKv.substring(0, pos) )
                val v1 = URIUtils.decodeURIComponent( qsKv.substring(pos + 1) )
                (k1, v1) :: Nil
              } else {
                Nil
              }
            }
            if k.nonEmpty && v.nonEmpty
          } {
            // Убрать первый префикс названия параметра.
            val k2 = k.replaceFirst("^[^.]+\\.", "")
            if (k2 ==* CURR_TAB_FN) {
              for {
                tabId <- Try(v.toInt).toOption
                tab   <- MLoginTabs.withValueOpt( tabId )
              } {
                updatesAcc ::= ILoginFormPages.Login.currTab.set( tab )
              }
            } else if (k2 ==* RETURN_URL_FN) {
              updatesAcc ::= ILoginFormPages.Login.returnUrl.set( Some(v) )
            }
          }

          for {
            updateF <- updatesAcc.reduceOption(_ andThen _)
          } yield {
            updateF( ILoginFormPages.Login.default )
          }
        } { nl =>
          val eqSign = "="
          var url = CURR_TAB_FN + eqSign + nl.currTab.value
          for (returnUrl <- nl.returnUrl)
            url = url + "&" + RETURN_URL_FN + eqSign + returnUrl
          url
        }
        .option
        .withDefault {
          ILoginFormPages.Login.default
        }

      val loginFormRule = dynamicRouteCT( loginFormRoute ) ~> dynRender( _renderNormalLogin )

      loginFormRule
        .notFound { _ =>
          redirectToPage( ILoginFormPages.Login.default )( Redirect.Replace )
        }
    }

    Router.componentAndCtl[ILoginFormPages](
      baseUrl = BaseUrl.until( stopAt = qMark ),
      cfg     = routerCfg
    )
  }


  def router = routerAndCtl._1
  def routerCtl = routerAndCtl._2

  val circuit = circuitF( routerCtl )

  /** Лениво! Иначе будет зацикливание. */
  private lazy val normalLoginRender = {
    ReactCommonModule.commonReactCtx.provide( MCommonReactCtx.default )(
      circuit.wrap(identity(_))(
        loginFormR().apply
      )(implicitly, MLoginRootS.MLoginRootSFastEq)
    )
  }

  private def _renderNormalLogin(nl: ILoginFormPages.Login): VdomElement = {
    // Уведомить о смене вкладки.
    circuit.dispatch( nl )

    // Отрендерить тело основного диалога логина:
    normalLoginRender
  }

}

