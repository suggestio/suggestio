package io.suggest.sc.ads

import io.suggest.common.empty.EmptyUtil
import io.suggest.jd.MJdData
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.03.18 22:04 Description: Контейнер для рекламной карточки и каких-то других данных в контексте выдачи s3.
  * Впихивать некоторые sc3-only вещи внутрь MJdAdData очень нерационально, поэтому тут расширябельная модель-контейнер.
  */
object MSc3AdData {

  /** Поддержка play-json. */
  implicit def MSC3_AD_DATA: OFormat[MSc3AdData] = (
    (__ \ "j").format[MJdData] and
    (__ \ "i").formatNullable[MScAdInfo]
      .inmap[MScAdInfo](
        EmptyUtil.opt2ImplMEmptyF( MScAdInfo ),
        EmptyUtil.implEmpty2OptF
      )
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MSc3AdData] = UnivEq.derive

  def jd = GenLens[MSc3AdData](_.jd)
  def info = GenLens[MSc3AdData](_.info)

}


/** Класс модели-контейнера данных по рекламной карточке для выдачи sc3.
  *
  * @param jd Данные для рендера карточки.
  */
final case class MSc3AdData(
                             jd       : MJdData,
                             info     : MScAdInfo,
                           )
