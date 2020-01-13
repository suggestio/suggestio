package io.suggest.swfs.fid

import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 11:47
 * Description: Модель представления fid'ов, который состоит из id файла и id раздела.
 */

object Fid {

  object Fields {
    final def VOLUME_ID_FN = "v"
    final def FILE_ID_FN   = "f"
  }

  final def VOL_FID_DELIM_CH = ','


  implicit def FID_FORMAT: OFormat[Fid] = (
    (__ \ Fields.VOLUME_ID_FN).format[SwfsVolumeId_t] and
    (__ \ Fields.FILE_ID_FN).format[String]
  )(apply, unlift(unapply))


  def parse(fidStr: String): Option[Fid] = {
    fidStr.split( VOL_FID_DELIM_CH ) match {
      case Array(volumeIdStr, fileId) =>
        val volumeId = volumeIdStr.toInt
        val fid = new Fid(volumeId, fileId) {
          override def toString = fidStr
        }
        Some(fid)
      case _ =>
        //LOGGER.debug(s"parse($fidStr): Cannot tokenize Swfs FID: ${other.mkString(" ")}")
        None
    }
  }


  /** Распарсить модель из строки. */
  def apply(fid: String): Fid =
    parse(fid).get

}


sealed case class Fid(
                       override val volumeId   : SwfsVolumeId_t,
                       fileId                  : String,
                     )
  extends IVolumeId
{

  /** Сериализовать в строку. */
  override def toString: String =
    s"$volumeId${Fid.VOL_FID_DELIM_CH}$fileId"

}
