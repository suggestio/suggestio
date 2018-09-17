package io.suggest.geo

import io.suggest.i18n.{MessagesF_t, MsgCodes}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.04.17 11:12
  * Description: Утиль для моделей расстояний.
  */
object DistanceUtil {

  /** Рендер дистанции
    *
    * @param meters Расстояние в метрах.
    * @param messagesF Функция-рендерер строк.
    * @return Отрендеренная строка расстояния.
    */
  def formatDistanceM(meters: Double)(messagesF: MessagesF_t): String = {
    if (meters > 1000) {
      // Рендерить в километрах.
      val fracDigits = if (meters > 10000) 0 else 1
      val fmt = "%1." + fracDigits + "f"
      messagesF(
        MsgCodes.`n.km._kilometers`,
        fmt.format( meters / 1000d ) :: Nil
      )
    } else {
      // Рендерить в int-метрах.
      messagesF(
        MsgCodes.`n.m._meters`,
        meters.toInt :: Nil
      )
    }
  }

}
