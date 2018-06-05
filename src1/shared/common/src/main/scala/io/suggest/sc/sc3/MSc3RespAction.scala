package io.suggest.sc.sc3

import io.suggest.sc.ads.MSc3AdsResp
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.sc.search.MSc3TagsResp
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.07.17 15:01
  * Description: Модель sc3-экшена с данными ответа сервера.
  * Наследница v2-модели из [www]:MScResp.
  */
object MSc3RespAction {

  /** Поддержка play-json. */
  implicit def MSC3_RESP_ACTION_FORMAT: OFormat[MSc3RespAction] = (
    (__ \ "action").format[MScRespActionType] and
    (__ \ MScRespActionTypes.Index.value).formatNullable[MSc3IndexResp] and
    (__ \ MScRespActionTypes.AdsTile.value).formatNullable[MSc3AdsResp] and
    (__ \ MScRespActionTypes.SearchRes.value).formatNullable[MSc3TagsResp]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MSc3RespAction] = UnivEq.derive

}


/** Модель обобщённого ответа сервера и sc-экшена одновременно.
  * Так сделано, чтобы сервер мог радикально воздействовать на выдачу
  * без нарушения формата ответа.
  *
  * @param acType Тип экшена.
  * @param index Тело index-ответа.
  * @param ads Тело ответа для плитки jd-карточек.
  * @param search Тело ответа с результатами поиска тегов/узлов/etc.
  */
case class MSc3RespAction(
                           acType    : MScRespActionType,
                           index     : Option[MSc3IndexResp]  = None,
                           ads       : Option[MSc3AdsResp]    = None,
                           search    : Option[MSc3TagsResp]   = None,
                         )
