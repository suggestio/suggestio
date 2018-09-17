package io.suggest.adv.info

import boopickle.Default._
import io.suggest.bill.tf.daily.MTfDailyInfo

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 9:37
  * Description: Модель данных по размещению на узле в контексте какой-то карточки.
  */

/** Статическая поддержка контейнера инфы по размещению какой-то (текущей) карточки на текущем узле. */
object MNodeAdvInfo4Ad {

  /** Поддержка бинарной сериализации между клиентом и сервером. */
  implicit val mAdvInfo4AdPickler: Pickler[MNodeAdvInfo4Ad] = {
    implicit val mTfDailyInfoP = MTfDailyInfo.mTfDailyInfoPickler
    generatePickler[MNodeAdvInfo4Ad]
  }

}


/** Класс-контейнер инфы по размещению на узле в контексте какой-то текущей карточки.
  *
  * @param blockModulesCount Кол-во блоков карточки.
  * @param tfDaily Данные по тарификации.
  */
case class MNodeAdvInfo4Ad(
                            blockModulesCount : Int,
                            tfDaily           : MTfDailyInfo
                          )
