package io.suggest.swfs.client.proto.lookup

import io.suggest.common.empty.EmptyUtil
import io.suggest.swfs.fid.{IVolumeId, VolumeId}
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
 *   {"volumeId":"35"}                  (2019 год)
 * }}}
 */
object LookupResponse {

  def empty = LookupResponse(-1, Nil)

  /** Поддержка JSON. */
  implicit def FORMAT: Format[LookupResponse] = (
    VolumeId.FORMAT_STR and
    (__ \ "locations").formatNullable[Seq[VolumeLocation]]
      .inmap[Seq[VolumeLocation]](
        EmptyUtil.opt2ImplEmptyF( Nil ),
        { vols => if (vols.isEmpty) None else Some(vols) }
      )
  )(apply, unlift(unapply))

}


trait ILookupResponse extends IVolumeId {

  /** Доступные сервера, обслуживающие текущий volume. */
  def locations: Seq[IVolumeLocation]

}


final case class LookupResponse(
                                 override val volumeId : Int,
                                 override val locations: Seq[VolumeLocation]
                               )
  extends ILookupResponse
