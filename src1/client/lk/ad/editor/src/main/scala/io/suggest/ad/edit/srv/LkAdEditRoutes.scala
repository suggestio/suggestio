package io.suggest.ad.edit.srv

import io.suggest.lk.router.Controllers
import io.suggest.sjs.common.model.Route

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.10.17 22:29
  * Description: Поддержка js-роутера на странице react-редактора рекламной карточки.
  */

object JsRoutes_Controllers_LkAdEdit {

  import scala.language.implicitConversions

  implicit def apply(controllers: Controllers): JsRoutes_Controllers_LkAdEdit = {
    controllers.asInstanceOf[JsRoutes_Controllers_LkAdEdit]
  }

}


/** Дополнить списочек контроллеров новым LkAdEdit-контроллером. */
@js.native
sealed trait JsRoutes_Controllers_LkAdEdit extends js.Object {

  /** Доступ к HTTP-роутам до серверного контроллера LkAdEdit. */
  def LkAdEdit: LkAdEditRoutes = js.native

}


/** Интерфейс js-роутера для LkAdEdit-контроллера. */
@js.native
sealed trait LkAdEditRoutes extends js.Object {

  def prepareImgUpload(adId: String = null, nodeId: String = null): Route = js.native

}



