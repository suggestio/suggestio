package io.suggest.routes

import io.suggest.proto.http.model.Route

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 17:49
 * Description: Роутер для формы создания/редактирования карточки.
 */
@js.native
sealed trait MarketAdFormCtl extends js.Object {

  /** Вебсокет редактора для связи с сервером. */
  def ws(wsId: String): Route = js.native

}
