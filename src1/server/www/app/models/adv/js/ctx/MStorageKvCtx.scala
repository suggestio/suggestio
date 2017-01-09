package models.adv.js.ctx

import io.suggest.adv.ext.model.ctx.MStorageKvCtx._
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.04.15 11:01
 * Description: Контекст общения с kv-операциями хранилища на клиенте.
 */
object MStorageKvCtx {

  /** Десериализация из JSON. */
  implicit def reads: Reads[MStorageKvCtx] = (
    (__ \ KEY_FN).read[String] and
    (__ \ VALUE_FN).readNullable[String]
  )(apply _)

  /** Сериализация экземпляра модели в JSON. */
  implicit def writes: Writes[MStorageKvCtx] = (
    (__ \ KEY_FN).write[String] and
    (__ \ VALUE_FN).writeNullable[String]
  )(unlift(unapply))

}

case class MStorageKvCtx(
  key   : String,
  value : Option[String] = None
)
