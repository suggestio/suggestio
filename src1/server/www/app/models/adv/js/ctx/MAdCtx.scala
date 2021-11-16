package models.adv.js.ctx

import play.api.libs.json._
import play.api.libs.functional.syntax._
import io.suggest.adv.ext.model.ctx.MAdCtx._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.01.15 14:27
 * Description: JS-контекст с данными по одной рекламной карточке, отрендеренной картинке и тд.
 */

object MAdCtx {

  /** json-маппинг сериализованного элемента MAdCtx. */
  implicit def madCtxReads: Reads[MAdCtx] = (
    (__ \ ID_FN).read[String] and
    (__ \ PICTURE_FN).readNullable[MPictureCtx] and
    (__ \ SC_URL_FN).readNullable[String]
  )(apply _)


  /** json-unmapping для сериализации экземпляров MAdCtx. */
  implicit def madCtxWrites: Writes[MAdCtx] = (
    (__ \ ID_FN).write[String] and
    (__ \ PICTURE_FN).writeNullable[MPictureCtx] and
    (__ \ SC_URL_FN).writeNullable[String]
  )(unlift(MAdCtx.unapply))

}


case class MAdCtx(
  id      : String,
  picture : Option[MPictureCtx] = None,
  scUrl   : Option[String] = None
)

