package io.suggest.xadv.ext.js.runner.c

import scala.scalajs.js
import scala.scalajs.js.annotation.JSName
import router._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.03.15 10:44
 * Description: Интерфейс для доступа к динамическим роутам sio.
 */

@JSName("jsRoutes")
object routes extends js.Object {
  /** Все экспортированные контроллеры. */
  def controllers: Controllers = js.native
}

