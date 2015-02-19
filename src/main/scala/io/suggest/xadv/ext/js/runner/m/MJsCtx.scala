package io.suggest.xadv.ext.js.runner.m

import scala.scalajs.js

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.02.15 13:48
 * Description: Состояние запроса. Через этого также происходит обмен данными.
 * @param domain Список доменов.
 */
case class MJsCtx(
  action  : String,
  service : MService,
  domain  : List[String]      = Nil,
  status  : Option[String]    = None
) {

  def toJson: js.Dynamic = {
    js.Dynamic.literal(
      action  = action,
      service = service.toJson,
      domain  = domain,
      status  = status.get
    )
  }

}


object MJsCtx {

  // TODO выкинуть upickle, заменив на что-то типа https://groups.google.com/d/msg/scala-js/xqUVSF3JZFE/qxRTcCBT7LAJ
  def fromString(raw: String): MJsCtx = {
    upickle.read[MJsCtx](raw)
  }

}


case class MService(name: String, appId: Option[String]) {
  def toJson: js.Dynamic = {
    val lit = js.Dynamic.literal(name  = this.name)
    if (appId.isDefined) {
      lit.appId = appId.get
    }
    lit
  }
}
