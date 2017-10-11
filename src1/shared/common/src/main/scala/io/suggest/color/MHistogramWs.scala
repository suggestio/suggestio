package io.suggest.color

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 11.10.17 11:35
  * Description: Модель WebSocket-сообщения с гистограммой цветов по картинке.
  */
object MHistogramWs {

  object Fields {
    val NODE_ID_FN    = "n"
    val HISTOGRAM_FN  = "h"
  }

  implicit val MHISTOGRAM_WS_FORMAT: OFormat[MHistogramWs] = {
    val F = Fields
    (
      (__ \ F.NODE_ID_FN).format[String] and
      (__ \ F.HISTOGRAM_FN).format[MHistogram]
    )(apply, unlift(unapply))
  }

  implicit def univEq: UnivEq[MHistogramWs] = UnivEq.derive

}


/** Класс websocket-сообщения с данными по гистограмме для картинки.
  *
  * @param nodeId id узла картинки.
  * @param hist Гистограмма цветов.
  */
case class MHistogramWs(
                         nodeId: String,
                         hist  : MHistogram
                       )
