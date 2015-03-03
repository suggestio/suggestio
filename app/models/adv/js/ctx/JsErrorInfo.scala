package models.adv.js.ctx

import io.suggest.model.EsModel.FieldsJsonAcc
import models.event.IErrorInfo
import play.api.libs.json._
import io.suggest.adv.ext.model.ctx.MErrorInfo._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 17:38
 * Description: Инфа по ошибке, присланная из JSON.
 */
object JsErrorInfo {

  /** mapper из JSON. */
  implicit def reads = new Reads[JsErrorInfo] {
    override def reads(json: JsValue): JsResult[JsErrorInfo] = {
      try {
        val ei = JsErrorInfo(
          msg = (json \ MSG_FN)
            .as[String],
          args = (json \ ARGS_FN)
            .asOpt[Seq[String]]
            .getOrElse(Seq.empty),
          other = (json \ INFO_FN)
            .asOpt[JsValue]
        )
        JsSuccess(ei)

      } catch {
        case ex: Exception => JsError("error.cannot.parse.json")
      }
    }
  }

  /** Сериализация в JSON. */
  implicit def writes = new Writes[JsErrorInfo] {
    override def writes(o: JsErrorInfo): JsValue = {
      var acc: FieldsJsonAcc = List(
        MSG_FN -> JsString(o.msg)
      )
      if (o.args.nonEmpty)
        acc ::= ARGS_FN -> JsArray(o.args.map(JsString.apply))
      if (o.other.nonEmpty)
        acc ::= INFO_FN -> o.other.get
      JsObject(acc)
    }
  }

}


case class JsErrorInfo(
  msg   : String,
  args  : Seq[String]       = Seq.empty,
  other : Option[JsValue]   = None
) extends IErrorInfo {
  override def info: Option[String] = other.map(_.toString())
}
