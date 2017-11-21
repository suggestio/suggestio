package io.suggest.sc.sc3

import io.suggest.common.empty.EmptyUtil
import io.suggest.dev.MSzMult
import io.suggest.jd.MJdAdData
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 17:51
  * Description: Модель ответа сервера на запрос плитки из карточек.
  */
object MSc3AdsResp {

  /** Поддержка play-json. */
  implicit def MSC3_ADS_TILE_RESP: OFormat[MSc3AdsResp] = (
    (__ \ "a").formatNullable[Seq[MJdAdData]]
      .inmap[Seq[MJdAdData]](
        EmptyUtil.opt2ImplEmpty1F(Nil),
        {ads => if (ads.isEmpty) None else Some(ads) }
      ) and
    (__ \ "z").format[MSzMult]
  )(apply, unlift(unapply))

}


/** Класс модели ответа сервера на тему запрошенных jd-карточек.
  *
  * @param ads Данные для рендера плитки из jd-карточек.
  * @param szMult Мультипликатор размера, применённый ко всем карточкам.
  */
case class MSc3AdsResp(
                        ads     : Seq[MJdAdData],
                        szMult  : MSzMult
                      )
