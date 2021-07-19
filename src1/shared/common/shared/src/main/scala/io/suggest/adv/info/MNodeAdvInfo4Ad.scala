package io.suggest.adv.info

import io.suggest.bill.price.dsl.MReasonType
import io.suggest.bill.tf.daily.MTfDailyInfo
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.04.17 9:37
  * Description: Модель данных по размещению на узле в контексте какой-то карточки.
  */

/** Статическая поддержка контейнера инфы по размещению какой-то (текущей) карточки на текущем узле. */
object MNodeAdvInfo4Ad {

  implicit def nodeAdvInfo4AdJson: OFormat[MNodeAdvInfo4Ad] = {
    (
      (__ \ "b").format[Int] and
      (__ \ "d").formatNullable {
          val valueFormat = implicitly[Format[MTfDailyInfo]]
          val kw = MReasonType.keyReadsWrites
          val r = Reads.keyMapReads( kw, valueFormat )
          val w = Writes.keyMapWrites[MReasonType, MTfDailyInfo, Map]( kw, valueFormat )
          OFormat( r, w )
        }
          .inmap[Map[MReasonType, MTfDailyInfo]](
            mapOpt => mapOpt getOrElse Map.empty,
            map => Option.when( map.nonEmpty )(map),
          )
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MNodeAdvInfo4Ad] = UnivEq.derive

}


/** Класс-контейнер инфы по размещению на узле в контексте какой-то текущей карточки.
  *
  * @param blockModulesCount Кол-во блоков карточки.
  * @param tfDaily Данные по тарификации.
  */
case class MNodeAdvInfo4Ad(
                            blockModulesCount : Int,
                            tfDaily           : Map[MReasonType, MTfDailyInfo],
                          )
