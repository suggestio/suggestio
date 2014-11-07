package controllers.sc

import controllers.SioController
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.Jsonp
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.11.14 19:57
 * Description:
 */
trait ScController extends SioController {

  val JSONP_CB_FUN: String

  /** Метод для генерации json-ответа с html внутри. */
  protected def jsonOk(action: String, html: Option[JsString] = None, blocks: Seq[JsString] = Nil, acc0: FieldsJsonAcc = Nil) = {
    var acc: FieldsJsonAcc = acc0
    if (html.isDefined)
      acc ::= "html" -> html.get
    if (blocks.nonEmpty)
      acc ::= "blocks" -> JsArray(blocks)
    // action добавляем в начало списка
    acc ::= "action" -> JsString(action)
    val json = JsObject(acc)
    Ok( Jsonp(JSONP_CB_FUN, json) )
  }

}
