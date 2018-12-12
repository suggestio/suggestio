package io.suggest.lk.adn.map.u

import io.suggest.proto.http.model.Route
import io.suggest.routes.Controllers

import scala.scalajs.js
import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 15:37
  * Description: Поддержка доступа к роутам до серверного контроллера LkAdnMap.
  */

object LkAdnMapControllers {

  implicit def toLkAdnMapControllers(ctl: Controllers): LkAdnMapControllers = {
    ctl.asInstanceOf[LkAdnMapControllers]
  }

}


/** Аддон поверх routes.controllers, добавляющий доступ к роутам контроллера LkAdnMap. */
@js.native
trait LkAdnMapControllers extends js.Object {

  /** Фасад роут до серверного контроллера LkAdnMap. */
  val LkAdnMap: LkAdnMapRoutes = js.native

}


/** Интерфейс роут LkAdnMap. */
@js.native
trait LkAdnMapRoutes extends js.Object {

  /** Получение ценника. */
  def getPriceSubmit(nodeId: String): Route = js.native

  /** Сабмит формы. */
  def forNodeSubmit(nodeId: String): Route = js.native

  /** Получение списка текущих размещений указанного узла. */
  def currentNodeGeoGj(nodeId: String): Route = js.native

  /** Получение инфы по item'у размещения. */
  def currentGeoItemPopup(itemId: Double): Route = js.native

}
