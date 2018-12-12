package io.suggest.sc

import io.suggest.common.html.HtmlConstants
import io.suggest.proto.http.HttpConst
import io.suggest.pwa.WebAppUtil
import io.suggest.sc.log.ScRmeLogAppender
import io.suggest.sc.router.SrvRouter
import io.suggest.sjs.common.log.Logging
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.vm.spa.LkPreLoader
import io.suggest.sjs.leaflet.Leaflet
import io.suggest.spa.StateInp
import io.suggest.sw.SwOptions
import org.scalajs.dom.experimental.serviceworkers._
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLInputElement

import scala.scalajs.js
import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 11:42
  * Description: Запускалка выдачи третьего поколения.
  */
object Sc3Main {

  /** Здесь начинается исполнение кода выдачи. */
  def main(args: Array[String]): Unit = {
    try {
      Leaflet.noConflict()
    } catch {
      case _: Throwable => // do nothing
    }

    // Сразу поискать js-роутер на странице.
    val jsRouterFut = SrvRouter.ensureJsRouter()
    val doc  = DocumentVm()
    val docLoc = doc._underlying.location

    // 2018-02-27: После установки веб-приложения, есть проблема, что запуск приложения идёт по уже установленным
    // координатам из исходного URL. На новых девайсах это решабельно через webmanifest.start_url, а на яббле нужен доп.костыль:
    if (
      WebAppUtil.isStandalone() &&
      Option(docLoc.hash)
        .exists(_.nonEmpty)
    ) {
      docLoc.hash = ""
    }

    // Запустить фоновую установку ServiceWorker'а:
    try {
      for {
        // Найти на странице input ссылки скрипта воркера.
        inputEl <- Option( doc._underlying.getElementById(ScConstants.Sw.URL_INPUT_ID) )
        sw = dom.window.navigator.serviceWorker
        if !js.isUndefined( sw )
        // Не переустанавливать уже установленный sw. TODO получать версию SW с сервера в хидерах ответов, и перерегистрировать.
        swInp = StateInp( inputEl.asInstanceOf[HTMLInputElement] )
        swUrl2 <- swInp.value
        // Проверить URL service-worker'а, надо ли устанавливать/обновлять sw.js.
        if Try {
          val swCtl = sw.controller
          swCtl == null || {
            val swUrl0 = swCtl.scriptURL
            //println("swUrl0 = " + swUrl0 + "\nswUrl2 = " + swUrl2)
            (swUrl0 == null) ||
            !(swUrl0 endsWith swUrl2) ||
            // Для http-протокола разрешить установку всегда, т.к. это локалхост в dev-режиме выдачи. В prod-режиме может быть только https
            (docLoc.protocol equalsIgnoreCase HttpConst.Proto.HTTP_)
          }
        }.getOrElse(true)
      } {
        //println("installing sw")
        val swOpts = new SwOptions {
          override val scope = HtmlConstants.`.`
        }
        // Установка service-worker:
        for {
          ex <- sw
            .register( swUrl2, swOpts )
            .toFuture
            .failed
        } {
          println("!sw " + ex)
        }
      }
    } catch {
      case ex: Throwable =>
        println("!!SW " + ex)
    }

    val body = doc.body

    // Самый корневой рендер -- отрабатывается первым.
    val rootDiv = {
      Option {
        doc._underlying
          .getElementById(ScConstants.Layout.ROOT_ID)
      }
        .getOrElse {
          // TODO Удалить анимированную крутилку со страницы
          val div = VUtil.newDiv()
          body.appendChild( div )
          div
        }
    }

    val modules = new Sc3Module

    def __activateRmeLogger(): Unit = {
      // Активировать отправку логов на сервер, когда js-роутер будет готов.
      Logging.LOGGERS ::= new ScRmeLogAppender
    }

    if (jsRouterFut.isCompleted) {
      __activateRmeLogger()
    } else {
      jsRouterFut.andThen { case _ =>
        __activateRmeLogger()
      }
    }

    // Отрендерить компонент spa-роутера в целевой контейнер.
    modules.sc3SpaRouter
      .router()
      .renderIntoDOM(rootDiv)

    val BodyCss = modules.getScCssF().Body
    body.className += BodyCss.smBody.htmlClass //+ HtmlConstants.SPACE + BodyCss.BgLogo.ru.htmlClass

    // Инициализировать LkPreLoader:
    for {
      lkPreLoader <- LkPreLoader.find()
    } yield {
      // TODO Opt дважды вызывается find() тут.
      LkPreLoader.PRELOADER_IMG_URL
      lkPreLoader.remove()
    }

  }

}
