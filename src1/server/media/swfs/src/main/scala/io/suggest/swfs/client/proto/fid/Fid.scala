package io.suggest.swfs.client.proto.fid

import io.suggest.swfs.client.proto.VolumeId_t
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 11:47
 * Description: Модель представления fid'ов, который состоит из id файла и id раздела.
 */


trait IFid extends IVolumeId {

  /** id файла внутри volume. */
  def fileId    : String

  /** Сериализовать в строку. */
  def toFid: String = s"$volumeId,$fileId"
  override def toString = toFid
}


object Fid {

  val VOLUME_ID_FN = "v"
  val FILE_ID_FN   = "f"

  implicit val FID_FORMAT: OFormat[Fid] = (
    (__ \ VOLUME_ID_FN).format[VolumeId_t] and
    (__ \ FILE_ID_FN).format[String]
  )(apply, unlift(unapply))


  /** Распарсить модель из строки. */
  def apply(fid: String): Fid = {
    val Array(volumeIdStr, fileId) = fid.split(',')
    val volumeId = volumeIdStr.toInt
    new Fid(volumeId, fileId) {
      override def toFid = fid
    }
  }

}


case class Fid(
  override val volumeId  : VolumeId_t,
  override val fileId    : String
)
  extends IFid
