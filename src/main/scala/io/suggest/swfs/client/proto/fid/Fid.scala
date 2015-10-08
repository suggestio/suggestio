package io.suggest.swfs.client.proto.fid

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 11:47
 * Description: Модель представления fid'ов, который состоит из id файла и id раздела.
 */

trait IFid {

  /** Номер volume. */
  def volumeId  : Int

  /** id файла внутри volume. */
  def fileId    : String

  /** Сериализовать в строку. */
  def toFid: String = s"$volumeId,$fileId"
  override def toString = toFid
}


object Fid {

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
  override val volumeId  : Int,
  override val fileId    : String
)
  extends IFid
