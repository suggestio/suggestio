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
    try {
      Leaflet.noConflict()
    } catch {
      case _: Throwable => // do nothing
    }

    // Сразу поискать js-роутер на странице.
    val jsRouterFut = SrvRouter.ensureJsRouter()

    // 2018-02-27: После установки веб-приложения, есть проблема, что запуск приложения идёт по уже установленным
    // координатам из исходного URL. На новых девайсах это решабельно через webmanifest.start_url, а на яббле нужен доп.костыль:
    if (
      WebAppUtil.isStandalone() &&
      Option(dom.document.location.hash)
        .exists(_.nonEmpty)
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
