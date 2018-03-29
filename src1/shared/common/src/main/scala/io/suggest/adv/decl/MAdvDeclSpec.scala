package io.suggest.adv.decl

import io.suggest.dt.MAdvPeriod
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.03.18 15:06
  * Description: Модель описания запроса на некое абстрактное размещение карточки.
  */
object MAdvDeclSpec {

  implicit def mAdvDeclSpecFormat: OFormat[MAdvDeclSpec] = (
    (__ \ "o").formatNullable[Boolean] and
    (__ \ "p").formatNullable[MAdvPeriod]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MAdvDeclSpec] = UnivEq.derive

}


/** Контейнер данных описания размещения карточки на абстрактном узле/теге/geo.
  *
  * @param isShowOpened Отображать сразу раскрытой?
  *                     None, значит что в данном контексте в этом смысла нет.
  * @param advPeriod Период размещения, если задан.
  */
case class MAdvDeclSpec(
                         isShowOpened  : Option[Boolean],
                         advPeriod     : Option[MAdvPeriod]
                       )
