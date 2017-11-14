package io.suggest.sc.sc3

import io.suggest.sc.ScConstants.Resp._
import io.suggest.sc.resp.MScRespActionType
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
    (__ \ ACTION_FN).format[MScRespActionType] and
    (__ \ INDEX_RESP_ACTION).formatNullable[MSc3IndexResp] and
    (__ \ ADS_TILE_RESP_ACTION).formatNullable[MSc3FindAdsResp]
  )(apply, unlift(unapply))

}


/** Модель обобщённого ответа сервера и sc-экшена одновременно.
  * Так сделано, чтобы сервер мог радикально воздействовать на выдачу
  * без нарушения формата ответа.
  *
  * @param acType Тип экшена.
  * @param index Тело index-ответа.
  * @param ads Тело ответа для плитки jd-карточек.
  */
case class MSc3RespAction(
                           acType    : MScRespActionType,
                           index     : Option[MSc3IndexResp]      = None,
                           ads       : Option[MSc3FindAdsResp]    = None
                         )
