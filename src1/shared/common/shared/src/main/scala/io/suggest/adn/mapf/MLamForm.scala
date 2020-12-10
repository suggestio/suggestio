package io.suggest.adn.mapf

import io.suggest.dt.MAdvPeriod
import io.suggest.geo.{CircleGs, IGeoShape}
import io.suggest.maps.MMapProps
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 14:20
  * Description: Модель данных для обмена между сервером и клиенком в форме размещения узла на карте.
  */
object MLamForm {

  implicit def lamFormJson: OFormat[MLamForm] = {
    val minFmt = IGeoShape.JsonFormats.minimalFormatter
    import minFmt.circle

    (
      (__ \ "mp").format[MMapProps] and
      (__ \ "mc").format[CircleGs] and
      (__ \ "dp").format[MAdvPeriod] and
      (__ \ "a").formatNullable[Boolean] and
      (__ \ "tz").format[Int]
    )(apply, unlift(unapply))
  }

}


/** Класс-контейнер данных полезной нагрузки формы размещения узла на карте..
  *
  * @param datePeriod Начальные данные периода размещения. Для них же и вычислена начальное цена.
  * @param mapCursor Состояние "курсора" на гео-карте.
  */
case class MLamForm(
                     mapProps         : MMapProps,
                     mapCursor        : CircleGs,
                     datePeriod       : MAdvPeriod,
                     adv4freeChecked  : Option[Boolean],
                     tzOffsetMinutes  : Int = 0
                   )

