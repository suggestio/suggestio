package io.suggest.swfs.client.proto.assign

import io.suggest.swfs.client.proto.lookup.{IVolumeLocation, VolumeLocation}
import io.suggest.swfs.fid.Fid
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.10.15 20:13
 * Description: Модель ответа на запрос assign'ования нового fid'а.
 */
object AssignResponse {

  implicit def FORMAT: Format[AssignResponse] = (
    (__ \ "count").format[Int] and
    (__ \ "fid").format[String] and
    VolumeLocation.URL_FORMAT and
    VolumeLocation.PUBLIC_URL_FORMAT
  )(apply, unlift(unapply))

}


final case class AssignResponse(
                                              count       : Int,
                                              fid         : String,
                                 override val url         : String,
                                 override val publicUrl   : String
                               )
  extends IVolumeLocation
{

  lazy val fidParsed = Fid(fid)

}
