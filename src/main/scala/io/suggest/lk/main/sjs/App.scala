package io.suggest.lk.main.sjs

import io.suggest.lk.flash.FlashInitRouter
import io.suggest.lk.ident.IdentInitRouter
import io.suggest.sjs.common.util.SjsLogger
import io.suggest.xadv.ext.js.AdvExtInitRouter

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
  with IdentInitRouter
  with AdvExtInitRouter
  with FlashInitRouter
