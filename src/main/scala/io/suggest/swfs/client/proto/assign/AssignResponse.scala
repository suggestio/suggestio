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


case class AssignResponse(
  count       : Int,
  fid         : String,
  url         : String,
  publicUrl   : Option[String]
) {

  lazy val fidParsed = Fid(fid)

}
