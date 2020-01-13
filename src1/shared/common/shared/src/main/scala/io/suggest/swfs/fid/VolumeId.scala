package io.suggest.swfs.fid

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 15:04
 * Description: Аддоны для моделей, реализующих поле volumeId.
 */
object VolumeId {

  val FORMAT_STR = {
    (__ \ "volumeId").format[String]
      .inmap[SwfsVolumeId_t](_.toInt, _.toString)
  }

}


trait IVolumeId {

  /** Номер volume. */
  def volumeId  : SwfsVolumeId_t

}


