package models.adv.js.ctx

import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json._
import io.suggest.adv.ext.model.ctx.MErrorInfo._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 17:38
 * Description: Инфа по ошибке, присланная из JSON.
 */
object JsErrorInfo {

  /** Все поддерживаемые моделью поля JSON. */
  def FIELDS = Set(MSG_FN, ARGS_FN)

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
          other = {
            val fns = FIELDS
            val fs = json.asInstanceOf[JsObject]
              .fields
              .filter { case (k, _) => !(fns contains k) }
            JsObject(fs)
          }
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
      if (acc.isEmpty) {
        o.other
      } else if (o.other.fields.isEmpty) {
        JsObject(acc)
      } else {
        JsObject(acc ++ o.other.fields)
      }
    }
  }

}

case class JsErrorInfo(
  msg   : String,
  args  : Seq[String] = Seq.empty,
  other : JsObject    = JsObject(Seq.empty)
)
