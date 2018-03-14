package io.suggest.sc.sc3

import io.suggest.maps.MMapProps
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 19:01
  * Description: Модель с данными инициализации выдачи.
  * Появилась в react-выдаче для получения начальных данных состояния формы выдачи.
  */
object MSc3Init {

  /** Поддержка play-json. */
  implicit def MSC3_INIT_FORMAT: OFormat[MSc3Init] = (
    (__ \ "m").format[MMapProps] and
    (__ \ "r").format[MSc3Conf]
  )(apply, unlift(unapply))

}


/** Класс модели данных инициализации.
  *
  * @param mapProps Начальные настройки карты.
  */
case class MSc3Init(
                     mapProps     : MMapProps,
                     conf         : MSc3Conf
                   )
