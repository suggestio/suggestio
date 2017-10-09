package io.suggest.lk.nodes.form.u

import io.suggest.routes.Controllers
import io.suggest.sjs.common.model.Route

import scala.language.implicitConversions
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.03.17 21:59
  * Description: Аддоны для ЛК-js-роутера на странице управления узлами.
  */

@js.native
sealed trait LkNodesRoutes extends js.Object {

  val LkNodes: LkNodesCtl = js.native

}

object LkNodesRoutes {
  implicit def toLkNodesControllers(lkJsRoutes: Controllers): LkNodesRoutes = {
    lkJsRoutes.asInstanceOf[LkNodesRoutes]
  }
}


/** Доступные экшены контроллера LkNodes. */
@js.native
sealed trait LkNodesCtl extends js.Object {

  /** Роута списка под-узлов для указанного узла. */
  def nodeInfo(nodeId: String): Route = js.native

  /** Роута сабмита формы добавления нового узла. */
  def createSubNodeSubmit(parentId: String): Route = js.native

  /** Роута сабмита нового значения флага isEnabled. */
  def setNodeEnabled(nodeId: String, isEnabled: Boolean): Route = js.native

  /** Роута для удаления узла. */
  def deleteNode(nodeId: String): Route = js.native

  /** Сабмит редактирования узла. */
  def editNode(nodeId: String): Route = js.native

  /** Сабмит обновления данных размещения какой-то карточки на каком-то узле по rcvrKey. */
  def setAdv(adId: String, isEnabled: Boolean, onNodeRcvrKey: String): Route = js.native

  /** Сабмит обновлённых данных по тарификацию размещений на узле. */
  def setTfDaily(onNodeRcvrKey: String): Route = js.native

}