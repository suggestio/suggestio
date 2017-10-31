package io.suggest.swfs.client.proto.assign

import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.swfs.client.proto.lookup.{IVolumeLocation, VolumeLocation}
import io.suggest.url.MHostInfo
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.10.15 20:13
 * Description: Модель ответа на запрос assign'ования нового fid'а.
 */
object AssignResponse {

  implicit val FORMAT: Format[AssignResponse] = (
    (__ \ "count").format[Int] and
    (__ \ "fid").format[String] and
    VolumeLocation.URL_FORMAT and
    VolumeLocation.PUBLIC_URL_FORMAT
  )(apply, unlift(unapply))

}


/** Интерфейс ответа. */
trait IAssignResponse extends IVolumeLocation {

  def count       : Int
  def fid         : String

  def fidParsed = Fid(fid)

}


case class AssignResponse(
  override val count       : Int,
  override val fid         : String,
  override val url         : String,
  override val publicUrl   : String
)
  extends IAssignResponse
{

  override lazy val fidParsed = super.fidParsed

}
