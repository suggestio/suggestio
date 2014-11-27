package util.jsa

import models.jsm.SmJsonResp

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 20.11.14 13:59
 * Description: Уведомить js выдачи о каком-то событии, которое её касается.
 */
case class SmRcvResp(data: SmJsonResp) extends JsAction {
  override def renderJsAction(sb: StringBuilder): StringBuilder = {
    sb.append("siomart.receive_response(")
      .append(data.toJson.toString())
      .append(");")
  }
}
