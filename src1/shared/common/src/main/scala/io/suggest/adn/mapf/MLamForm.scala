package io.suggest.adn.mapf

import io.suggest.adn.mapf.opts.MLamOpts
import io.suggest.adv.geo.MMapProps
import io.suggest.dt.MAdvPeriod
import io.suggest.geo.{MGeoCircle, MGeoPoint}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.04.17 14:20
  * Description: Модель данных для обмена между сервером и клиенком в форме размещения узла на карте.
  */
object MLamForm {

  import boopickle.Default._

  /** Поддержка бинарной сериализации модели между клиентом и сервером. */
  implicit val mLamFormPickler: Pickler[MLamForm] = {
    implicit val mMapProps = MMapProps.mmapsPickler
    implicit val mGeoPointP = MGeoPoint.pickler
    implicit val mGeoCircle = MGeoCircle.mGeoCirlePickler
    implicit val mAdvPeriodP = MAdvPeriod.mAdvPeriodPickler
    implicit val mLamOptsP = MLamOpts.mLamOptsP
    generatePickler[MLamForm]
  }

}


/** Класс-контейнер данных полезной нагрузки формы размещения узла на карте..
  *
  * @param datePeriod Начальные данные периода размещения. Для них же и вычислена начальное цена.
  * @param mapCursor Состояние "курсора" на гео-карте.
  * @param opts Активные опции формы.
  */
case class MLamForm(
                     mapProps         : MMapProps,
                     mapCursor        : MGeoCircle,
                     datePeriod       : MAdvPeriod,
                     adv4freeChecked  : Option[Boolean],
                     opts             : MLamOpts,
                     tzOffsetMinutes  : Int = 0
                   )

