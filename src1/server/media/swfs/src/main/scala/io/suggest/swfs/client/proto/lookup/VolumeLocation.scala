package io.suggest.swfs.client.proto.lookup

import io.suggest.url.MHostInfo
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 14:33
 * Description: Модель данных по локейшену одной volume.
 */
object VolumeLocation {

  def PUBLIC_URL_FORMAT = (__ \ "publicUrl").format[String]
  def URL_FORMAT        = (__ \ "url").format[String]

  implicit def FORMAT: Format[VolumeLocation] = (
    PUBLIC_URL_FORMAT and
    URL_FORMAT
  )(apply, unlift(unapply))

}


trait IVolumeLocation {

  /** Внешний URL сервера, обслуживающего текущий volume. */
  def publicUrl : String
  /** Внутренний URL сервера, обслуживающего текущий volume. */
  def url       : String

  def hostInfo = MHostInfo(nameInt = url, namePublic = publicUrl)

}


final case class VolumeLocation(
                                 override val publicUrl : String,
                                 override val url       : String
                               )
  extends IVolumeLocation
