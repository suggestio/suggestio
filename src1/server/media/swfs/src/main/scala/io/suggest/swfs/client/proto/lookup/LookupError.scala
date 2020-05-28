package io.suggest.swfs.client.proto.lookup

import io.suggest.swfs.fid.{IVolumeId, VolumeId}
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
  implicit def FORMAT: Format[LookupError] = (
    VolumeId.FORMAT_STR and
    (__ \ "error").format[String]
  )(apply, unlift(unapply))

}


final case class LookupError(
                              override val volumeId : Int,
                                           error    : String
                            )
  extends IVolumeId with ILookupResponse
{
  override def locations: Seq[IVolumeLocation] = Nil
}
