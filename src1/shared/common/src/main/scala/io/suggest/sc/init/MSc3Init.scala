package io.suggest.sc.init

import io.suggest.maps.MMapProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.07.17 19:01
  * Description: Модель с данными инициализации выдачи.
  * Появилась в react-выдаче для получения начальных данных состояния формы выдачи.
  */
object MSc3Init {

  /** Поддержка play-json. */
  implicit val MSC_INIT_FORMAT: OFormat[MSc3Init] = {
    (__ \ "m")
      .format[MMapProps]
      .inmap(apply, _.mapProps)
  }

}


/** Класс модели данных инициализации.
  *
  * @param mapProps Начальные настройки карты.
  */
case class MSc3Init(
                     mapProps: MMapProps
                   )
