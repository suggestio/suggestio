package io.suggest.swfs.client.proto.fid

import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.swfs.client.proto.VolumeId_t
import io.suggest.util.logs.MacroLogsDyn
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.QueryStringBindable

import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.10.15 11:47
 * Description: Модель представления fid'ов, который состоит из id файла и id раздела.
 */

object Fid extends MacroLogsDyn {

  object Fields {
    final def VOLUME_ID_FN = "v"
    final def FILE_ID_FN   = "f"
  }

  final def VOL_FID_DELIM_CH = ','


  implicit val FID_FORMAT: OFormat[Fid] = (
    (__ \ Fields.VOLUME_ID_FN).format[VolumeId_t] and
    (__ \ Fields.FILE_ID_FN).format[String]
  )(apply, unlift(unapply))


  /** Распарсить модель из строки. */
  def apply(fid: String): Fid = {
    val Array(volumeIdStr, fileId) = fid.split( VOL_FID_DELIM_CH )
    val volumeId = volumeIdStr.toInt
    new Fid(volumeId, fileId) {
      override def toString = fid
    }
  }


  /** Поддержка сырого биндинга из query-string. */
  implicit def fidQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[Fid] = {
    new QueryStringBindableImpl[Fid] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Fid]] = {
        val fidStrRaw = strB.bind(key, params)
        for (fidStrE <- fidStrRaw) yield {
          for {
            fidStr <- fidStrE
            parsed <- Try( Fid(fidStr) )
              .toEither
              .left.map { ex =>
                LOGGER.error(s"qsb: failed to bind $fidStrRaw", ex)
                ex.getMessage
              }
          } yield {
            parsed
          }
        }
      }

      override def unbind(key: String, value: Fid): String = {
        strB.unbind(key, value.toString)
      }
    }
  }

}


case class Fid(
                override val volumeId   : VolumeId_t,
                fileId                  : String
              )
  extends IVolumeId
{

  /** Сериализовать в строку. */
  override def toString: String = {
    val sb = new StringBuilder(16)
    sb.append( volumeId )
      .append(  )
    s"$volumeId${Fid.VOL_FID_DELIM_CH}$fileId"
  }

}
