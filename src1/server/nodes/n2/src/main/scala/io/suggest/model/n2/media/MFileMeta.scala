package io.suggest.model.n2.media

import java.time.OffsetDateTime

import io.suggest.common.empty.EmptyUtil
import io.suggest.crypto.hash.MHashes
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.es.model.IGenEsMappingProps
import io.suggest.img.MImgFmts
import io.suggest.model.PrefixedFn
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 18:24
 * Description: Метаданные файла для модели [[MMedia]].
 */
object MFileMeta
  extends IEsMappingProps
  with IGenEsMappingProps
{

  object Fields {
    /** Название поля, где mime индексируется как keyword. */
    val MIME_FN             = "mi"
    /** Название поля внутри mime, где mime индексируется текстом. */
    val MIME_AS_TEXT_FN     = "mt"
    /** Старое поле, где mime индексировался только как текст. */
    private[MFileMeta] val OLD_MIME_FN = "mm"

    val SIZE_B_FN           = "sz"
    val IS_ORIGINAL_FN      = "orig"

    val HASHES_HEX_FN       = "hh"
    object HashesHexFields extends PrefixedFn {
      override protected def _PARENT_FN = HASHES_HEX_FN
      def HASH_TYPE_FN  = _fullFn( MFileMetaHash.Fields.HASH_TYPE_FN )
      def HASH_VALUE_FN = _fullFn( MFileMetaHash.Fields.HEX_VALUE_FN )
    }

    val DATE_CREATED_FN     = "dc"

    /** Имя legacy-поля, хранившего sha1-хеш. Заменено на карту хешей. */
    private[MFileMeta] val SHA1_FN     = "s1"
  }

  /** Поддержка JSON для модели. */
  implicit val FORMAT: OFormat[MFileMeta] = {
    val F = Fields

    // 2017.10.02 Костыль для поддержки старого и нового формата контрольных сумм одновременно.
    // TODO Удалить compat-костыли после MMedias.resaveMany()
    val hhFormat: OFormat[Seq[MFileMetaHash]] = {
      val newFmt = (__ \ F.HASHES_HEX_FN).formatNullable[Seq[MFileMetaHash]]
        .inmap[Seq[MFileMetaHash]](
          { EmptyUtil.opt2ImplEmpty1F(Nil) },
          { hh => if (hh.isEmpty) None else Some(hh) }
        )
      val readsCompat = newFmt.orElse {
        // Пытаемся прочитать старое поле SHA1
        (__ \ F.SHA1_FN).formatNullable[String]
          .map[Seq[MFileMetaHash]] {
            case Some(sha1hex) => MFileMetaHash(MHashes.Sha1, sha1hex.toLowerCase, Set(MFileMetaHash.Flags.TRULY_ORIGINAL)) :: Nil
            case None          => Nil
          }
      }
      OFormat(readsCompat, newFmt)
    }

    // Отрабатываем неправильный маппинг mime-типа.
    val mimeFormat: OFormat[String] = {
      val fmt0 = (__ \ F.MIME_FN).format[String]
      val readsFallback = fmt0.orElse {
        (__ \ F.OLD_MIME_FN).read[String]
      }
      OFormat(readsFallback, fmt0)
    }

    (
      mimeFormat and
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
      FieldKeyword(F.MIME_FN, index = true, include_in_all = true, fields = Seq(
        FieldText(F.MIME_AS_TEXT_FN, index = true, include_in_all = true)
      )),
      FieldNumber(F.SIZE_B_FN, fieldType = DocFieldTypes.long, index = true, include_in_all = false),
      FieldBoolean(F.IS_ORIGINAL_FN, index = true, include_in_all = false),
      FieldNestedObject(F.HASHES_HEX_FN, enabled = true, properties = MFileMetaHash.generateMappingProps),
      //FieldKeyword(F.SHA1_FN, index = false, include_in_all = false),
      FieldDate(F.DATE_CREATED_FN, index = true, include_in_all = false)
    )
  }

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.MIME_FN -> FKeyWord(
        index  = someTrue,
        fields = Some( Json.obj(
          F.MIME_AS_TEXT_FN -> FText.indexedJs,
        )),
      ),
      F.SIZE_B_FN -> FNumber(
        typ   = DocFieldTypes.Long,
        index = someTrue,
      ),
      F.IS_ORIGINAL_FN -> FBoolean.indexedJs,
      F.HASHES_HEX_FN -> FObject.nested(
        properties = MFileMetaHash.esMappingProps,
      ),
      F.DATE_CREATED_FN -> FDate.indexedJs,
    )
  }

  @inline implicit def univEq: UnivEq[MFileMeta] = UnivEq.derive

}


/** Метаданные любого файла.
  *
  * @param mime Стандартный MIME-тип.
  * @param sizeB Размер в байтах.
  * @param isOriginal Оригинал ли? false для деривативов.
  * @param hashesHex Карта хэш-сумм файла.
  * @param dateCreated Дата создания (заливки).
  */
case class MFileMeta(
  mime          : String,
  sizeB         : Long,
  isOriginal    : Boolean,
  hashesHex     : Seq[MFileMetaHash]  = Nil,
                    // TODO Сделать тип поля dateCreated более переносимым между js/jvm, и унифицировать модель с MSrvFileInfo.
  dateCreated   : OffsetDateTime      = OffsetDateTime.now()
) {

  /** Если картинка, то вернуть её формат по модели MImgFmts. */
  lazy val imgFormatOpt = MImgFmts.withMime(mime)

}
