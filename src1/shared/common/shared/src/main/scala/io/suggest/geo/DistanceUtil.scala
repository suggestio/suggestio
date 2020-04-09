package io.suggest.geo

import io.suggest.i18n.{MMessage, MsgCodes}
import play.api.libs.json.Json

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.17 11:12
  * Description: Утиль для моделей расстояний.
  */
object DistanceUtil {

  /** Рендер в сантиметрах.
    *
    * @param centiMeters Сантиметры.
    * @return Меседж.
    */
  def formatDistanceCM(centiMeters: Int): MMessage = {
    if (centiMeters < 100) {
      MMessage(
        MsgCodes.`n.cm._centimeters`,
        Json.arr( centiMeters )
      )
    } else {
      formatDistanceM( centiMeters / 100 )
    }
  }


  /** Рендер дистанции
    *
    * @param meters Расстояние в метрах.
    * @return Отрендеренная строка расстояния.
    */
  def formatDistanceM(meters: Double): MMessage = {
    if (meters > 1000) {
      // Рендерить в километрах.
      val fracDigits = if (meters > 10000) 0 else 1
      val fmt = "%1." + fracDigits + "f"
      MMessage(
        MsgCodes.`n.km._kilometers`,
        Json.arr( fmt.format( meters / 1000d ) ),
      )
    } else {
      // Рендерить в int-метрах.
      MMessage(
        MsgCodes.`n.m._meters`,
        Json.arr( meters.toInt ),
      )
    }
  }

}
