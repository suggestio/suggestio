package io.suggest.lk.ad.form.router

import io.suggest.sjs.common.model.Route

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

  /** Экшен поста добавления нового тега. */
  def tagEditorAddTag(index: Int): Route = js.native

}
