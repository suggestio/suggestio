package io.suggest.model.n2.media

import java.time.OffsetDateTime

import io.suggest.common.empty.EmptyUtil
import io.suggest.crypto.hash.{HashesHex, HashesHexEs, MHashes}
import io.suggest.es.model.IGenEsMappingProps
import play.api.libs.json._
import play.api.libs.functional.syntax._
import HashesHexEs.MHASHES_HEX_FORMAT_ES

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 18:24
 * Description: Метаданные файла для модели [[MMedia]].
 */
object MFileMeta extends IGenEsMappingProps {

  object Fields {
    val MIME_FN             = "mm"
    val SIZE_B_FN           = "sz"
    val IS_ORIGINAL_FN      = "orig"
    val HASHES_HEX_FN       = "hh"
    val DATE_CREATED_FN     = "dc"

    /** Имя legacy-поля, хранившего sha1-хеш. Заменено на карту хешей. */
    private[MFileMeta] val SHA1_FN     = "s1"
  }

  /** Поддержка JSON для модели. */
  implicit val FORMAT: OFormat[MFileMeta] = {
    val F = Fields

    // 2017.10.02 Костыль для поддержки старого и нового формата контрольных сумм одновременно.
    // TODO Удалить compat-костыли после MMedias.resaveMany()
    val hhFormat: OFormat[HashesHex] = {
      val newFmt = (__ \ F.HASHES_HEX_FN).formatNullable[HashesHex]
        .inmap[HashesHex](
          { EmptyUtil.opt2ImplEmpty1F(Map.empty) },
          { hh => if (hh.isEmpty) None else Some(hh) }
        )
      val readsCompat = newFmt.orElse {
        // Пытаемся прочитать старое поле SHA1
        (__ \ F.SHA1_FN).formatNullable[String]
          .map[HashesHex] {
            case Some(sha1hex) => Map(MHashes.Sha1 -> sha1hex.toLowerCase)
            case None          => Map.empty
          }
      }
      OFormat(readsCompat, newFmt)
    }

    (
      (__ \ F.MIME_FN).format[String] and
      (__ \ F.SIZE_B_FN).format[Long] and
      (__ \ F.IS_ORIGINAL_FN).format[Boolean] and
      hhFormat and
      (__ \ F.DATE_CREATED_FN).format[OffsetDateTime]
    )(apply, unlift(unapply))
  }


  import io.suggest.es.util.SioEsUtil._

  override def generateMappingProps: List[DocField] = {
    val F = Fields
    List(
      FieldText(F.MIME_FN, index = true, include_in_all = true),
      FieldNumber(F.SIZE_B_FN, fieldType = DocFieldTypes.long, index = true, include_in_all = false),
      FieldBoolean(F.IS_ORIGINAL_FN, index = true, include_in_all = false),
      FieldNestedObject(F.HASHES_HEX_FN, enabled = true, properties = HashesHexEs.generateMappingProps),
      //FieldKeyword(F.SHA1_FN, index = false, include_in_all = false),
      FieldDate(F.DATE_CREATED_FN, index = true, include_in_all = false)
    )
  }

}


case class MFileMeta(
  mime          : String,
  sizeB         : Long,
  isOriginal    : Boolean,
  hashesHex     : HashesHex,
  dateCreated   : OffsetDateTime = OffsetDateTime.now()
)
