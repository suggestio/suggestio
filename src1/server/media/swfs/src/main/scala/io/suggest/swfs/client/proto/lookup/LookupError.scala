package io.suggest.swfs.client.proto.lookup

import io.suggest.swfs.client.proto.fid.{VolumeId, IVolumeId}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 14:53
 * Description: Модель ошибки Lookup'а.
 */
object LookupError {

  /** Поддержка JSON. */
  implicit val FORMAT: Format[LookupError] = (
    VolumeId.FORMAT_STR and
    (__ \ "error").format[String]
  )(apply, unlift(unapply))

}


trait ILookupError extends IVolumeId with ILookupResponse {
  /** Сообщение об ошибке. */
  def error: String

  override final def locations: Seq[IVolumeLocation] = Nil
}


case class LookupError(
  override val volumeId : Int,
  override val error    : String
)
  extends ILookupError
