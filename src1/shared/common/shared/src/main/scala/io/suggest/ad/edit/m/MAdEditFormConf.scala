package io.suggest.ad.edit.m

import io.suggest.ctx.ICtxIdStrOpt
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.08.17 18:17
  * Description: Модель данных server-side конфигурации формы редактирования карточки.
  */
object MAdEditFormConf {

  object Fields {
    val PRODUCER_ID_FN = "p"
    val AD_ID_FN       = "a"
    val SRV_CTX_ID     = "c"
  }

  implicit val MAD_EDIT_FORM_CONF_FORMAT: OFormat[MAdEditFormConf] = (
    (__ \ Fields.PRODUCER_ID_FN).format[String] and
    (__ \ Fields.AD_ID_FN).formatNullable[String] and
    (__ \ Fields.SRV_CTX_ID).format[String]
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MAdEditFormConf] = UnivEq.derive

}


/** Класс модели-контейнера данных конфигурации формы редактирования карточки.
  *
  * @param producerId id текущего узла-владельца карточки.
  * @param adId id текущей рекламной карточки.
  * @param ctxId Значение Context.ctxIdStr, заданное на сервере.
  */
case class MAdEditFormConf(
                            producerId  : String,
                            adId        : Option[String],
                            ctxId       : String
                          )
  extends ICtxIdStrOpt
{
  override def ctxIdOpt = Some(ctxId)
}
