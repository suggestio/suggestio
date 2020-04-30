package io.suggest.sjs.common.dt

import io.suggest.dt.MYmd
import io.suggest.log.Log
import JsDateUtil.JsDateHelper
import io.suggest.msg.ErrorMsgs

import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.12.16 13:26
  * Description: Парсинг даты силами браузера.
  */
object MYmdJs extends Log {

  def parse(raw: String): Option[MYmd] = {
    val res = try {
      js.Date.parse(raw)
    } catch { case _: Throwable =>
      Double.NaN
    }

    if (res.isNaN) {
      logger.error( ErrorMsgs.JS_DATE_PARSE_FAILED, msg = raw )
      None
    } else {

      val date = new js.Date(res)
      val ymd = MYmdJs(date)
      Some( ymd )
    }
  }


  def apply(jsDate: js.Date): MYmd = {
    JsDateHelper.toYmd(jsDate)
  }

  def toJsDate(ymd: MYmd): js.Date = {
    JsDateHelper.toDate(ymd)
  }

}
