package models.mdt

import models.Context
import play.api.libs.json.{JsString, JsArray, Json, JsObject}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 28.01.16 12:13
 * Description: Статическая утиль для шаблона, которому требуется собрать
 * JSON параметров инициализации jquery-datetimepicker.
 */
object DtPickArgs {

  def args()(implicit ctx: Context): JsObject = {
    import ctx.messages
    def jsStr(code: String) = JsString(messages(code))
    Json.obj(
      "lang"        -> messages.lang.language,
      "timepicker"  -> false,
      "format"      -> "Y-m-d",
      "dayOfWeekStart" -> 1,
      "i18n"  -> Json.obj(
        "months" -> JsArray(
          (for(m <- 1 to 12) yield jsStr("Month.N." + m)).toSeq
        ),
        "dayOfWeek" -> JsArray(
          (for (d <- Iterator(7) ++ (1 to 6)) yield jsStr("DayOfW.N." + d)).toSeq
        )
      )
    )
  }

}
