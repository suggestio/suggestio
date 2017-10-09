package io.suggest.file.up

import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 14:11
  * Description: Описание URL из хоста и относительной ссылки.
  */

object MUploadUrlData {

  object Fields {
    val HOST_FN     = "h"
    val REL_URL_FN  = "p"
  }

  /** Поддержка play-json. */
  implicit val MHOST_PATH_FORMAT: OFormat[MUploadUrlData] = {
    val F = Fields
    (
      (__ \ F.HOST_FN).format[String] and
      (__ \ F.REL_URL_FN).format[String]
    )(apply, unlift(unapply))
  }

  implicit def univEq: UnivEq[MUploadUrlData] = UnivEq.derive

}


case class MUploadUrlData(
                           host    : String,
                           relUrl  : String
                         )
