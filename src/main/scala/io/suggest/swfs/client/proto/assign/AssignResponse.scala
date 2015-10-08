package io.suggest.swfs.client.proto.assign

import io.suggest.swfs.client.proto.fid.Fid
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
    (__ \ "url").format[String] and
    (__ \ "publicUrl").formatNullable[String]
  )(apply, unlift(unapply))

}


/** Интерфейс ответа. */
trait IAssignResponse {
  def count       : Int
  def fid         : String
  def url         : String
  def publicUrl   : Option[String]

  def fidParsed = Fid(fid)
}


case class AssignResponse(
  count       : Int,
  fid         : String,
  url         : String,
  publicUrl   : Option[String]
)
  extends IAssignResponse
{

  override lazy val fidParsed = super.fidParsed

}
