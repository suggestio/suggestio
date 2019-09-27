package io.suggest.ad.edit.m

import io.suggest.common.empty.OptionUtil.BoolOptJsonFormatOps
import io.suggest.ctx.ICtxIdStrOpt
import japgolly.univeq.UnivEq
import monocle.macros.GenLens
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
    val TOUCH_DEV_FN   = "t"
  }

  implicit val MAD_EDIT_FORM_CONF_FORMAT: OFormat[MAdEditFormConf] = (
    (__ \ Fields.PRODUCER_ID_FN).format[String] and
    (__ \ Fields.AD_ID_FN).formatNullable[String] and
    (__ \ Fields.SRV_CTX_ID).format[String] and
    (__ \ Fields.TOUCH_DEV_FN).formatNullable[Boolean].formatBooleanOrFalse
  )(apply, unlift(unapply))

  @inline implicit def univEq: UnivEq[MAdEditFormConf] = UnivEq.derive

  val ctxId     = GenLens[MAdEditFormConf](_.ctxId)
  val touchDev  = GenLens[MAdEditFormConf](_.touchDev)

}


/** Класс модели-контейнера данных конфигурации формы редактирования карточки.
  *
  * @param producerId id текущего узла-владельца карточки.
  * @param adId id текущей рекламной карточки.
  * @param ctxId Значение Context.ctxIdStr, заданное на сервере.
  * @param touchDev Это touch-устройство? Это в основном определяется в рантайме.
  *                 При обнаружении touch-событий происходит переключение react-dnd на touch-backend.
  */
case class MAdEditFormConf(
                            producerId  : String,
                            adId        : Option[String],
                            ctxId       : String,
                            touchDev    : Boolean          = false,
                          )
  extends ICtxIdStrOpt
{
  override def ctxIdOpt = Some(ctxId)
}
