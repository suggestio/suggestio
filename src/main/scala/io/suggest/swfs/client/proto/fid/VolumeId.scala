package io.suggest.swfs.client.proto.fid

import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 09.10.15 15:04
 * Description:
 */
object VolumeId {

  val FORMAT_STR = {
    (__ \ "volumeId").format[String]
      .inmap[Int](_.toInt, _.toString)
  }

}


trait IVolumeId {

  /** Номер volume. */
  def volumeId  : Int

}


