package io.suggest.sc

import io.suggest.pwa.WebAppUtil
import io.suggest.sc.log.ScRmeLogAppender
import io.suggest.sc.router.SrvRouter
import io.suggest.sjs.common.log.Logging
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx
import io.suggest.sjs.common.vm.spa.LkPreLoader
import io.suggest.sjs.leaflet.Leaflet
import org.scalajs.dom

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 11:42
  * Description: Запускалка выдачи третьего поколения.
  */
object Sc3Main {

  /** Здесь начинается исполнение кода выдачи. */
  def main(args: Array[String]): Unit = {
    // Выпиливаем leaflet из window.L.
    Leaflet.noConflict()

    // Сразу поискать js-роутер на странице.
    val jsRouterFut = SrvRouter.ensureJsRouter()

    // 2018-02-27: После установки веб-приложения, есть проблема, что запуск приложения идёт по уже установленным
    // координатам из исходного URL. На новых девайсах это решабельно через webmanifest.start_url, а на яббле нужен доп.костыль:
    if (
      WebAppUtil.isStandalone() &&
      Option(dom.document.location.hash).exists(_.nonEmpty)
    ) {
      dom.document.location.hash = ""
    }

    val doc  = DocumentVm()
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

    // Отрендерить компонент spa-роутера в целевой контейнер.
    modules.sc3SpaRouter
      .router()
      .renderIntoDOM(rootDiv)

    jsRouterFut.andThen { case _ =>
      // Активировать отправку логов на сервер, когда js-роутер будет готов.
      Logging.LOGGERS ::= new ScRmeLogAppender
    }

    val BodyCss = modules.getScCssF().Body
    body.className += BodyCss.smBody.htmlClass //+ HtmlConstants.SPACE + BodyCss.BgLogo.ru.htmlClass

    // TODO Добавить обеление фона body.

    // TODO Запустить разные FSM: геолокация, platform, BLE. Переписав их в circuit'ы предварительно.
  }

}
