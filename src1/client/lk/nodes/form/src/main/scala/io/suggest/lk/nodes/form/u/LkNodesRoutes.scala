package io.suggest.lk.nodes.form.u

import io.suggest.lk
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
  implicit def apply(lkJsRoutes: lk.router.Controllers): LkNodesRoutes = {
    lkJsRoutes.asInstanceOf[LkNodesRoutes]
  }
}


@js.native
sealed trait LkNodesCtl extends js.Object {

  /** Запрос списка под-узлов для указанного узла. */
  def subNodesOf(nodeId: String): Route = js.native

}