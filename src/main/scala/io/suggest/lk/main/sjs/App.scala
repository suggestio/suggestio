package io.suggest.lk.main.sjs

import io.suggest.lk.ad.form.init.AdFormInitRouter
import io.suggest.lk.adn.edit.init.NodeEditInitRouter
import io.suggest.lk.flash.FlashInitRouter
import io.suggest.lk.ident.center.CenterContentInitRouter
import io.suggest.lk.popup.PopupsInitRouter
import io.suggest.sjs.common.controller.jshidden.JsHiddenInitRouter
import io.suggest.sjs.common.controller.vlines.VerticalLinesInitRouter
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.xadv.ext.js.form.FormEventsInitRouter
import io.suggest.xadv.ext.js.runner.c.RunnerInitRouter

import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.JSApp
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.03.15 15:09
 * Description: Запуск js личного кабинета.
 */
object App extends JSApp with SjsLogger {

  /** Запуск скрипта на исполнение. Нужно произвести направленную инициализацию. */
  @JSExport
  override def main(): Unit = {
    new LkInitRouter()
      .init()
      .onFailure {
        case ex  => error("Init failed", ex)
      }
  }

}


/** Stackable-реализация routed init. */
class LkInitRouter
  extends SjsLogger
  with CenterContentInitRouter
  with RunnerInitRouter
  with FormEventsInitRouter
  with AdFormInitRouter
  with FlashInitRouter
  with VerticalLinesInitRouter
  with JsHiddenInitRouter
  with PopupsInitRouter
  with NodeEditInitRouter
