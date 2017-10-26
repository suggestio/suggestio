package io.suggest.vid.ext

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.10.17 14:29
  * Description: Модель инфы о "внешнем" видео.
  */
object MVideoExtInfo {

  object Fields {
    val VIDEO_SERVICE_FN = "s"
    val REMOTE_ID_FN     = "i"
  }


  /** Поддержка play-json. */
  implicit def MEXT_VIDEO_INFO_FORMAT: OFormat[MVideoExtInfo] = {
    val F = Fields
    (
      (__ \ F.VIDEO_SERVICE_FN).format[MVideoService] and
      (__ \ F.REMOTE_ID_FN).format[String]
    )(apply, unlift(unapply))
  }

}


/** Класс модели данных по видео на внешнем видео-сервисе.
  *
  * @param videoService Видео-сервис.
  * @param remoteId id видео на внешнем сервисе.
  */
case class MVideoExtInfo(
                          videoService  : MVideoService,
                          remoteId      : String
                        ) {


  /** Собрать строковой id n2-узла для узла, представляющего это видео. */
  def toNodeId: String = {
    val delim = "."
    delim + videoService + delim + remoteId
  }

}
