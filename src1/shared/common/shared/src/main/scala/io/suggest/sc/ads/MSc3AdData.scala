package io.suggest.sc.ads

import io.suggest.jd.MJdAdData
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.18 22:04
  * Description: Контейнер для рекламной карточки и каких-то других данных в контексте выдачи s3.
  * Впихивать некоторые sc3-only вещи внутрь MJdAdData очень нерационально, поэтому тут расширябельная модель-контейнер.
  */
object MSc3AdData {

  /** Поддержка play-json. */
  implicit def MSC3_AD_DATA: OFormat[MSc3AdData] = (
    (__ \ "j").format[MJdAdData] and
    (__ \ "e").formatNullable[Boolean]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MSc3AdData] = UnivEq.derive

}


/** Класс модели-контейнера данных по рекламной карточке для выдачи sc3.
  *
  * @param jd Данные для рендера карточки.
  * @param canEdit Есть ли право на редактирование карточки у юзера?
  */
case class MSc3AdData(
                       jd       : MJdAdData,
                       canEdit  : Option[Boolean] = None
                     )
