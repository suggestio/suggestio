package util.jsa

import models.jsm.DomWindowSpecs
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 03.04.15 18:58
 * Description: Фунции для работы с окном/окнами в js.
 */

case class JsWindowOpen(url: String, target: String, specs: DomWindowSpecs) extends JsAction {

  override def sbInitSz: Int = 192

  override def renderJsAction(sb: StringBuilder): StringBuilder = {
    sb.append("window.open(")
      .append( JsString(url) )
      .append(',')
      .append( JsString(target) )
      .append(',').append('"')
    specs.toSpecStringBuilder(sb = sb)
      .append("\");")
  }
}

