package io.suggest.adv.info

import io.suggest.bill.tf.daily.MTfDailyInfo
import io.suggest.media.MMediaInfo
import io.suggest.n2.node.meta.MMetaPub
import japgolly.univeq.UnivEq
import io.suggest.ueq.UnivEqUtil._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.17 17:03
  * Description: Кроссплатформеная модель описательной инфы по какому-то узлу.
  * Заимплеменчена для нужд рендера попапа с прайсингом и мета-данными.
  */
object MNodeAdvInfo {

  implicit def nodeAdvInfoJson: OFormat[MNodeAdvInfo] = (
    (__ \ "b").format[String] and
    (__ \ "n").format[String] and
    (__ \ "t").formatNullable[MTfDailyInfo] and
    (__ \ "a").formatNullable[MNodeAdvInfo4Ad] and
    (__ \ "m").format[MMetaPub] and
    (__ \ "g").format[Seq[MMediaInfo]]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MNodeAdvInfo] = UnivEq.derive

}


/** Класс-контейнер кросс-платформенной инфы по узлу.
  *
  * @param nodeName Название узла.
  * @param tfDaily Текущий тариф узла (для 1 модуля), если есть.
  * @param tfDaily4Ad Данные тарификации к контексте текущей карточки.
  * @param meta Публичные мета-данные узла.
  * @param gallery Галерея узла. Или Nil, если её нет.
  */
case class MNodeAdvInfo(
                         nodeNameBasic  : String,
                         nodeName       : String,
                         tfDaily        : Option[MTfDailyInfo],
                         tfDaily4Ad     : Option[MNodeAdvInfo4Ad],
                         meta           : MMetaPub,
                         gallery        : Seq[MMediaInfo]
                       )

