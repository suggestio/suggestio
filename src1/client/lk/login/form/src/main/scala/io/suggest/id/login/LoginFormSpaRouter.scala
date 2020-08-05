package io.suggest.id.login

import io.suggest.spa.SioPages
import japgolly.scalajs.react.extra.router.{BaseUrl, Redirect, Router, RouterConfigDsl, RouterCtl}
import japgolly.scalajs.react.vdom.html_<^._
import japgolly.univeq._

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
                          renderLoginFormF: (SioPages.Login) => VdomElement,
                        ) {

  /** Сборка инстансов SPA-роутера и контроллер роутера. */
  val routerAndCtl: (Router[SioPages.Login], RouterCtl[SioPages.Login]) = {
    val qMark = "?"

    val routerCfg = RouterConfigDsl[SioPages.Login].buildConfig { dsl =>
      import SioPages.Login.Fields._
      import dsl._

      val loginFormRoute = (qMark ~ string(".+"))
        .pmap { qsStr =>
          val eqSign = '='
          var updatesAcc = List.empty[SioPages.Login => SioPages.Login]
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
                updatesAcc ::= (SioPages.Login.currTab set tab)
              }
            } else if (k2 ==* RETURN_URL_FN) {
              updatesAcc ::= (SioPages.Login.returnUrl set Some(v))
            }
          }

          for {
            updateF <- updatesAcc.reduceOption(_ andThen _)
          } yield {
            updateF( SioPages.Login.default )
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
          SioPages.Login.default
        }

      val loginFormRule = dynamicRouteCT( loginFormRoute ) ~> dynRender( renderLoginFormF )

      loginFormRule
        .notFound { _ =>
          redirectToPage( SioPages.Login.default )( Redirect.Replace )
        }
    }

    Router.componentAndCtl[SioPages.Login](
      baseUrl = BaseUrl.until( stopAt = qMark ),
      cfg     = routerCfg
    )
  }


  def router = routerAndCtl._1
  def routerCtl = routerAndCtl._2

}
