package io.suggest.sc.ads

import io.suggest.common.empty.EmptyUtil
import io.suggest.dev.MSzMult
import io.suggest.text.StringUtil
import io.suggest.xplay.json.PlayJsonUtil
import japgolly.univeq._
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 14.11.17 17:51
  * Description: Модель ответа сервера на запрос плитки из карточек.
  */
object MSc3AdsResp {

  /** Поддержка play-json. */
  implicit def MSC3_ADS_TILE_RESP_FORMAT: OFormat[MSc3AdsResp] = (
    (__ \ "a")
      .formatNullable[Seq[MSc3AdData]] {
        PlayJsonUtil.readsSeqNoErrorFormat[MSc3AdData]
      }
      .inmap[Seq[MSc3AdData]](
        EmptyUtil.opt2ImplEmpty1F(Nil),
        {ads => if (ads.isEmpty) None else Some(ads) }
      ) and
    (__ \ "z").format[MSzMult]
  )(apply, unlift(unapply))


  @inline implicit def univEq: UnivEq[MSc3AdsResp] = {
    import io.suggest.ueq.UnivEqUtil._
    UnivEq.derive
  }

}


/** Класс модели ответа сервера на тему запрошенных jd-карточек.
  *
  * @param ads Данные для рендера плитки из jd-карточек.
  * @param szMult Мультипликатор размера, применённый ко всем карточкам.
  */
case class MSc3AdsResp(
                        ads     : Seq[MSc3AdData],
                        szMult  : MSzMult
                      ) {

  override def toString: String = StringUtil.toStringHelper(this) { renderF =>
    val noFieldName = renderF("")
    noFieldName( "[" + ads.length + "]=[" + ads.iterator.map(_.jd.doc.tagId.toString).mkString(", ") + "]" )
    noFieldName( szMult )
  }

}
