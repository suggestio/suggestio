package io.suggest.url

import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 09.10.17 14:11
  * Description: Модель описания данных URL, состоит из хоста и относительной ссылки.
  *
  * Используется для передачи почти всех данных для сборки ссылки, но когда цельная ссылка неуместна.
  */

object MHostUrl {

  object Fields {
    val HOST_FN     = "h"
    val REL_URL_FN  = "p"
  }

  /** Поддержка play-json. */
  implicit val MHOST_PATH_FORMAT: OFormat[MHostUrl] = {
    val F = Fields
    (
      (__ \ F.HOST_FN).format[String] and
      (__ \ F.REL_URL_FN).format[String]
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MHostUrl] = UnivEq.derive

}


/** Данные по хосту и относительной ссылке.
  *
  * @param host Адрес хоста.
  * @param relUrl URL path и qs.
  */
case class MHostUrl(
                     host    : String,
                     relUrl  : String
                   ) {

  override def toString = host + relUrl

  def withHost(host: String)          = copy(host = host)
  def withRelUrl(relUrl: String)      = copy(relUrl = relUrl)

}
