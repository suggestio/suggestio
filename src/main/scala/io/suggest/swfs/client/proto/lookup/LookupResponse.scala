package io.suggest.swfs.client.proto.lookup

import io.suggest.swfs.client.proto.fid.{IVolumeId, VolumeId}
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 14:29
 * Description: Модель ответов сервера на lookup-запросы.
 *
 * Положительный ответ выглядит так (HTTP 200):
 * {{{
 *   {"locations":[{"publicUrl":"localhost:8080","url":"localhost:8080"}]}
 * }}}
 *
 * Негативный ответ такой (HTTP 404):
 * {{{
 *   {"volumeId":"35","error":"volumeId not found."}
 * }}}
 */
object LookupResponse {

  /** Поддержка JSON. */
  implicit val FORMAT: Format[LookupResponse] = (
    VolumeId.FORMAT_STR and
    (__ \ "locations").format[Seq[VolumeLocation]]
  )(apply, unlift(unapply))

}


trait ILookupResponse extends IVolumeId {

  /** Доступные сервера, обслуживающие текущий volume. */
  def locations: Seq[IVolumeLocation]

}


case class LookupResponse(
  override val volumeId : Int,
  override val locations: Seq[VolumeLocation]
)
  extends ILookupResponse
