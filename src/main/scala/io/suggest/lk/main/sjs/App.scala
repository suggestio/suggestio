package io.suggest.lk.main.sjs

import io.suggest.lk.ident.IdentInitRouter
import io.suggest.sjs.common.util.SjsLogs
import io.suggest.xadv.ext.js.AdvExtRiController

import scala.scalajs.js.annotation.JSExport
import scala.scalajs.js.JSApp
import scala.scalajs.concurrent.JSExecutionContext.Implicits.runNow

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.03.15 15:09
 * Description: Запуск js личного кабинета.
 */
object App extends JSApp with SjsLogs {

  /** Запуск скрипта на исполнение. Нужно произвести направленную инициализацию. */
  @JSExport
  override def main(): Unit = {
    new RoutedInitImpl()
      .init()
      .onFailure {
        case ex  => error("Init failed", ex)
      }
  }

}


/** Stackable-реализация routed init. */
class RoutedInitImpl
  extends IdentInitRouter
  with AdvExtRiController
