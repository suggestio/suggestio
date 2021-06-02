package io.suggest.n2.media

import io.suggest.common.empty.EmptyUtil
import io.suggest.crypto.hash.{HashesHex, MHash}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.img.MImgFormats
import io.suggest.model.PrefixedFn
import japgolly.univeq._
import io.suggest.math.MathConst
import io.suggest.pick.{ContentTypeCheck, MimeConst}
import io.suggest.scalaz.ScalazUtil
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._
import scalaz.{Validation, ValidationNel}
import io.suggest.common.empty.OptionUtil.BoolOptOps
import scalaz.syntax.apply._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 27.09.15 18:24
 * Description: Метаданные файла для media-модели.
 */
object MFileMeta
  extends IEsMappingProps
{

  // Как-бы-пустой инстанс.
  def empty = apply()

  object Fields {
    /** Название поля, где mime индексируется как keyword. */
    val MIME_FN             = "mi"
    /** Название поля внутри mime, где mime индексируется текстом. */
    val MIME_AS_TEXT_FN     = "mt"

    val SIZE_B_FN           = "sz"
    val IS_ORIGINAL_FN      = "orig"

    val HASHES_HEX_FN       = "hh"
    object HashesHexFields extends PrefixedFn {
      override protected def _PARENT_FN = HASHES_HEX_FN
      def HASH_TYPE_FN  = _fullFn( MFileMetaHash.Fields.HASH_TYPE_FN )
      def HASH_VALUE_FN = _fullFn( MFileMetaHash.Fields.HEX_VALUE_FN )
    }
  }

  /** Поддержка JSON для модели. */
  implicit def fileMetaJson: OFormat[MFileMeta] = {
    val F = Fields
    (
      (__ \ F.MIME_FN).formatNullable[String] and
      (__ \ F.SIZE_B_FN).formatNullable[Long] and
      // isOrig: до 2020-11-12 здесь было только false|undefined, но для поиска оригиналов надо сохранять точное значение: false|true.
      (__ \ F.IS_ORIGINAL_FN).formatNullable[Boolean]
        .inmap[Boolean](_.getOrElseTrue, Some.apply) and
      (__ \ F.HASHES_HEX_FN).formatNullable[Seq[MFileMetaHash]]
        .inmap[Seq[MFileMetaHash]](
          EmptyUtil.opt2ImplEmpty1F(Nil),
          hh => Option.when(hh.nonEmpty)(hh)
        )
    )(apply, unlift(unapply))
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
    )
  }

  @inline implicit def univEq: UnivEq[MFileMeta] = UnivEq.derive


  def mime         = GenLens[MFileMeta](_.mime)
  def sizeB        = GenLens[MFileMeta](_.sizeB)
  def isOriginal   = GenLens[MFileMeta](_.isOriginal)
  def hashesHex    = GenLens[MFileMeta](_.hashesHex)



  /** Сборка валидатора и валидация данных файла для аплоада.
    *
    * @param m Валидируемая модель
    * @param minSizeB Минимальный размер файла.
    * @param maxSizeB Максимальный размер файла.
    * @param mimeVerifierF Фунция для сверки MIME-типа, см. MimeConst.
    * @param mustHashes Список hash-алгоритмов, которые должны быть уже вычислены.
    * @return ValidationNel с исходным инстансом модели, если всё ок.
    */
  def validateUpload(m              : MFileMeta,
                     minSizeB       : Long,
                     maxSizeB       : Long,
                     mimeVerifierF  : ContentTypeCheck,
                     mustHashes     : Set[MHash]
                    ): ValidationNel[String, MFileMeta] = {
    (
      ScalazUtil.liftNelOpt( m.mime ) { mime =>
        MimeConst.validateMimeUsing( mime, mimeVerifierF )
      } |@|
      ScalazUtil.liftNelSome( m.sizeB, _eFileSizePrefix + "empty" ) { sizeB =>
        MathConst.Counts.validateMinMax(sizeB, minSizeB, maxSizeB, _eFileSizePrefix)
      } |@|
      Validation.liftNel( m.isOriginal )(!_, "!e.file.isOrig") |@|
      HashesHex.hashesHexV( MFileMetaHash.toHashesHex(m.hashesHex), mustHashes )
        .map( MFileMetaHash.fromHashesHex )
    )(apply)
  }

  private def _eFileSizePrefix = "e.file.size.too."

}


/** Метаданные любого файла.
  *
  * @param mime Стандартный MIME-тип.
  * @param sizeB Размер в байтах.
  *              None возможно для файлов из ClassPath, которые меняются сами без ведома этой системы.
  * @param isOriginal Оригинал ли? false для деривативов.
  * @param hashesHex Карта хэш-сумм файла.
  */
final case class MFileMeta(
                            mime          : Option[String]      = None,
                            sizeB         : Option[Long]        = None,
                            isOriginal    : Boolean             = true,
                            hashesHex     : Seq[MFileMetaHash]  = Nil,
                          ) {

  /** Если картинка, то вернуть её формат по модели MImgFmts. */
  lazy val imgFormatOpt = mime.flatMap( MImgFormats.withMime )

  override def toString: String = {
    val sb = new StringBuilder(64, productPrefix)
      .append('(')
    for (m <- mime)
      sb.append(m).append(',')
    for (sz <- sizeB)
      sb.append(sz).append("b,")
    if (!isOriginal)
      sb.append("!orig,")
    if (hashesHex.nonEmpty) {
      sb.append('[')
      for (fmh <- hashesHex)
        sb.append(fmh).append(' ')
      sb.append(']')
    }
    sb.append(')')
      .toString()
  }

}
