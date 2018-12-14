package io.suggest.sc.sc3

import io.suggest.sc.ads.MSc3AdsResp
import io.suggest.sc.search.MSc3NodeSearchResp
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
    (__ \ MScRespActionTypes.AdsTile.value).formatNullable[MSc3AdsResp] and
    (__ \ MScRespActionTypes.SearchNodes.value).formatNullable[MSc3NodeSearchResp] and
    (__ \ MScRespActionTypes.ConfUpdate.value).formatNullable[MScConfUpdate]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MSc3RespAction] = UnivEq.derive

}


/** Модель обобщённого ответа сервера и sc-экшена одновременно.
  * Так сделано, чтобы сервер мог радикально воздействовать на выдачу
  * без нарушения формата ответа.
  *
  * @param acType Тип экшена.
  * @param index Тело index-ответа.
  * @param ads Тело ответа для плитки jd-карточек.
  * @param search Тело ответа с результатами поиска тегов/узлов/etc.
  * @param confUpdate Данные для обновления конфига.
  */
case class MSc3RespAction(
                           acType    : MScRespActionType,
                           ads       : Option[MSc3AdsResp]          = None,
                           search    : Option[MSc3NodeSearchResp]   = None,
                           confUpdate: Option[MScConfUpdate]        = None,
                         )
