package io.suggest.id.login.v

import io.suggest.ReactCommonModule
import io.suggest.i18n.MCommonReactCtx
import io.suggest.id.login.LoginFormCircuit
import io.suggest.id.login.m.{ILoginFormPages, MLoginRootS, MLoginTabs}
import io.suggest.routes.routes
import japgolly.univeq._
import japgolly.scalajs.react.extra.router.{BaseUrl, Redirect, Router, RouterConfigDsl, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^._

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
    val routerCfg = RouterConfigDsl[ILoginFormPages].buildConfig { dsl =>
      import dsl._

      val currTabK = "t"
      val returnUrlK = "r"

      val loginFormRoute = ("?" ~ string(".+"))
        .pmap { qsStr =>
          val eqSign = '='
          var updatesAcc = List.empty[ILoginFormPages.NormalLogin => ILoginFormPages.NormalLogin]
          for {
            qsKv <- qsStr.split('&')
            if qsKv.nonEmpty
            (k, v) <- {
              val pos = qsKv.indexOf(eqSign)
              if (pos > 0) {
                val k1 = qsKv.substring(0, pos)
                val v1 = qsKv.substring(pos + 1)
                (k1, v1) :: Nil
              } else {
                Nil
              }
            }
            if k.nonEmpty && v.nonEmpty
          } {
            if (k ==* currTabK) {
              for {
                tabId <- Try(v.toInt).toOption
                tab   <- MLoginTabs.withValueOpt( tabId )
              } {
                updatesAcc ::= ILoginFormPages.NormalLogin.currTab.set( tab )
              }
            } else if (k ==* returnUrlK) {
              updatesAcc ::= ILoginFormPages.NormalLogin.returnUrl.set( Some(v) )
            }
          }

          for {
            updateF <- updatesAcc.reduceOption(_ andThen _)
          } yield {
            updateF( ILoginFormPages.NormalLogin() )
          }
        } { nl =>
          val eqSign = "="
          var url = currTabK + eqSign + nl.currTab.value
          for (returnUrl <- nl.returnUrl)
            url = url + "&" + returnUrlK + eqSign + returnUrl
          url
        }
        .option
        .withDefault {
          ILoginFormPages.NormalLogin()
        }

      val loginFormRule = dynamicRouteCT( loginFormRoute ) ~> dynRender( _renderNormalLogin )

      loginFormRule
        .notFound { r =>
          redirectToPage( ILoginFormPages.NormalLogin() )( Redirect.Replace )
        }
    }

    Router.componentAndCtl[ILoginFormPages](
      baseUrl = BaseUrl.until( "?" ),
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

  private def _renderNormalLogin(nl: ILoginFormPages.NormalLogin): VdomElement = {
    // Уведомить о смене вкладки.
    circuit.dispatch( nl )

    // Отрендерить тело основного диалога логина:
    normalLoginRender
  }

}

