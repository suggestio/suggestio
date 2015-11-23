package io.suggest.lk.adv.gtag.router

import io.suggest.sjs.common.model.Route

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 19.11.15 11:57
  * Description: Интерфейс контроллер jsRouter'а с url экшенов LkAdvGeoTag контроллера.
  */

@js.native
sealed trait LkAdvGeoTagCtl extends js.Object {

  /** Экшен поста добавления нового тега. */
  def tagEditorAddTag(): Route = js.native

}
