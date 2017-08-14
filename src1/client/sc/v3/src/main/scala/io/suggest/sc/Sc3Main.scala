package io.suggest.sc

import io.suggest.sc.log.ScRmeLogAppender
import io.suggest.sc.router.SrvRouter
import io.suggest.sjs.common.log.Logging
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.doc.DocumentVm
import japgolly.scalajs.react.vdom.Implicits._
import io.suggest.sjs.common.async.AsyncUtil.defaultExecCtx

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.07.17 11:42
  * Description: Запускалка выдачи третьего поколения.
  */
object Sc3Main {

  import io.suggest.sc.root.m.MScRoot.MScRootFastEq

  /** Здесь начинается исполнение кода выдачи. */
  def main(args: Array[String]): Unit = {
    // Сразу поискать js-роутер на странице.
    val jsRouterFut = SrvRouter.ensureJsRouter()

    val body = DocumentVm().body

    // Самый корневой рендер -- отрабатывается первым.
    val rootDiv = VUtil.newDiv()
    body.appendChild( rootDiv )

    val modules = new Sc3Modules

    // Подготовить центральную цепочку.
    val mainCircuit = modules.sc3CircuitModule.sc3Circuit

    mainCircuit
      .wrap(m => m)( modules.sc3Module.scRootR.apply )
      .renderIntoDOM(rootDiv)

    jsRouterFut.andThen { case _ =>
      // Активировать отправку логов на сервер, когда js-роутер будет готов.
      Logging.LOGGERS ::= new ScRmeLogAppender
    }

    val BodyCss = modules.scCssModule.getScCssF().Body
    body.className += BodyCss.smBody.htmlClass //+ HtmlConstants.SPACE + BodyCss.BgLogo.ru.htmlClass

    // TODO Добавить обеление фона body.

    // TODO Запустить разные FSM: геолокация, platform, BLE. Переписав их в circuit'ы предварительно.
  }

}
