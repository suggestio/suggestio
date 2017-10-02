package io.suggest.file.up

import io.suggest.crypto.hash.HashesHex.MHASHES_HEX_FORMAT_TRASPORT
import io.suggest.crypto.hash.{HashesHex, MHash}
import io.suggest.js.UploadConstants
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 02.10.17 17:09
  * Description: Модель-контейнер данных по файлу, который планируется загружать на сервер.
  */

object MFile4UpProps {

  object Fields {
    val SIZE_B_FN       = "s"
    val HASHES_HEX_FN   = "h"
  }

  /** Поддержка play-json. */
  implicit val MUP_FILE_PROPS_FORMAT: OFormat[MFile4UpProps] = {
    val F = Fields
    (
      (__ \ F.SIZE_B_FN).format[Long] and
      (__ \ F.HASHES_HEX_FN).format[Map[MHash, String]]
    )(apply, unlift(unapply))
  }

  implicit def univEq: UnivEq[MFile4UpProps] = UnivEq.derive


  import scalaz._
  import scalaz.syntax.apply._

  /** Валидатор данных файла для аплоада.
    *
    * @param m Валидируемая модель
    * @param minSizeB Минимальный размер файла.
    * @param maxSizeB Максимальный размер файла.
    * @param mustHashes Список hash-алгоритмов, которые должны быть уже вычислены.
    * @return ValidationNel с исходным инстансом модели, если всё ок.
    */
  def validate(m: MFile4UpProps, minSizeB: Long, maxSizeB: Long,
               mustHashes: Set[MHash] = UploadConstants.CleverUp.PICTURE_FILE_HASHES): ValidationNel[String, MFile4UpProps] = {
    (
      Validation.liftNel(m.sizeB)( _ < minSizeB, _eFileSizePrefix + "low" ) |@|
      Validation.liftNel(m.sizeB)( _ > maxSizeB, _eFileSizePrefix + "big" ) |@|
      HashesHex.hashesHexV( m.hashesHex, mustHashes )
    ) { (_,_,_) => m }
  }

  private def _eFileSizePrefix = "e.file.size.too."

}


/** Класс модели клиентских данных по одному файлу.
  *
  * @param sizeB Размер загружаемого файла в байтах.
  * @param hashesHex Карта hex-хэшей файла.
  */
case class MFile4UpProps(
                          sizeB      : Long,
                          hashesHex  : Map[MHash, String]
                        )
