package io.suggest.up

import io.suggest.crypto.hash.HashesHex
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import HashesHex._
import io.suggest.common.empty.EmptyUtil
import io.suggest.math.MathConst
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import scalaz.Validation
import scalaz.syntax.apply._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.06.2020 7:01
  * Description: Модель qs-аргументов для flow.js-закачки или иной chunked-закачки на сервер.
  *
  * Неявно-пустая модель, несмотря на наличие вроде бы обязательных полей.
  *
  */
object MUploadChunkQs {

  object Fields {
    final def CHUNK_NUMBER    = "flowChunkNumber"
    final def TOTAL_CHUNKS    = "flowTotalChunks"
    /** The general chunk size. */
    final def CHUNK_SIZE_GEN  = "flowChunkSize"
    final def CHUNK_SIZE_CUR  = "flowCurrentChunkSize"
    final def TOTAL_SIZE      = "flowTotalSize"
    final def IDENTIFIER      = "flowIdentifier"
    final def FILENAME        = "flowFilename"
    final def RELATIVE_PATH   = "flowRelativePath"

    final def HASHES_HEX      = "h"
  }

  implicit def uploadChunkQsJson: OFormat[MUploadChunkQs] = {
    val F = Fields
    (
      (__ \ F.CHUNK_NUMBER).formatNullable[Int] and
      (__ \ F.TOTAL_CHUNKS).formatNullable[Int] and
      (__ \ F.CHUNK_SIZE_GEN).formatNullable[MUploadChunkSize] and
      (__ \ F.CHUNK_SIZE_CUR).formatNullable[Long] and
      (__ \ F.TOTAL_SIZE).formatNullable[Long] and
      (__ \ F.IDENTIFIER).formatNullable[String] and
      (__ \ F.FILENAME).formatNullable[String] and
      (__ \ F.RELATIVE_PATH).formatNullable[String] and
      (__ \ F.HASHES_HEX).formatNullable[HashesHex]
        .inmap[HashesHex](
          EmptyUtil.opt2ImplEmptyF( Map.empty ),
          hh => Option.when( hh.nonEmpty )(hh)
        )
    )(apply, unlift(unapply))
  }

  @inline implicit def univEq: UnivEq[MUploadChunkQs] = UnivEq.derive

  /** Номер первого chunk'а. */
  final def FIRST_CHUNK_NUMBER = 1


  implicit final class UpChunkOpsExt( private val chunkQs: MUploadChunkQs ) extends AnyVal {

    def chunkSizeGeneral: MUploadChunkSize =
      chunkQs.chunkSizeGeneralO getOrElse MUploadChunkSizes.default

    def chunkNumber: Int = chunkQs.chunkNumberO.get

    def chunkNumber0: Int =
      chunkNumber1 - FIRST_CHUNK_NUMBER

    def chunkNumber1: Int =
      chunkQs.chunkNumber

  }


  def validate(qs: MUploadChunkQs): StringValidationNel[MUploadChunkQs] = {
    val maxTotalSizeB = UploadConstants.TOTAL_SIZE_LIMIT_BYTES
    val maxTotalChunks = maxTotalSizeB / qs.chunkSizeGeneral.value + 1

    (
      ScalazUtil.liftNelSome( qs.chunkNumberO, Fields.CHUNK_NUMBER ) { chunkNumber =>
        MathConst.Counts.validateMinMax( chunkNumber, min = FIRST_CHUNK_NUMBER, max = maxTotalChunks, Fields.TOTAL_CHUNKS )
      } |@|
      ScalazUtil.liftNelSome( qs.totalChunks, Fields.TOTAL_CHUNKS ) { totalChunks =>
        MathConst.Counts.validateMinMax( totalChunks, min = 1, max = maxTotalChunks, Fields.TOTAL_CHUNKS )
      } |@|
      ScalazUtil.liftNelSome( qs.chunkSizeGeneralO, Fields.CHUNK_SIZE_GEN ) {
        Validation.success
      } |@|
      ScalazUtil.liftNelOpt( qs.chunkSizeCurrent ) {
        Validation.success
      } |@|
      ScalazUtil.liftNelSome( qs.totalSize, Fields.TOTAL_SIZE ) { totalSizeB =>
        MathConst.Counts.validateMinMax( totalSizeB, min = UploadConstants.MIN_FILE_SIZE_BYTES, max = maxTotalSizeB, Fields.TOTAL_SIZE )
      } |@|
      ScalazUtil.liftNelOpt( qs.identifier ) { identifier =>
        // TODO Валидировать регэкспом.
        MathConst.Counts.validateMinMax( identifier.length, min = 0, max = 512, Fields.IDENTIFIER )
          .map(_ => identifier)
      } |@|
      ScalazUtil.liftNelOpt( qs.fileName ) { fileName =>
        // TODO Валидировать регэкспом.
        MathConst.Counts.validateMinMax( fileName.length, min = 1, max = 256, Fields.FILENAME )
          .map(_ => fileName)
      } |@|
      ScalazUtil.liftNelOpt( qs.relativePath ) { relativePath =>
        // TODO Валидировать регэкспом
        MathConst.Counts.validateMinMax( relativePath.length, min = 0, max = 256, Fields.RELATIVE_PATH )
          .map(_ => relativePath)
      } |@|
      HashesHex.hashesHexV( qs.hashesHex, Set.empty + UploadConstants.CleverUp.UPLOAD_CHUNK_HASH )
    )(apply)
  }

}


/** Контейнер qs-аргументов от flow.js.
  * Многие параметры необязательные, т.к. приходят ещё на первой фазе.
  * В будущем можно будет переписать flow.js на scala.js.
  *
  * @param chunkNumberO Порядковый номер chunk'а.
  *                     Первый chunk = это 1, НЕ 0 (особенность flow.js).
  *                     None тут нужно для совместимости с flow.js, который сам рендерит queryString,
  *                       а нам её нужно только немного дополнить недостающими полями.
  * @param totalChunks Общее кол-во chunk'ов.
  * @param chunkSizeGeneralO Размер одного chunk'а.
  *                          Размер последнего chunk'а будет отличаться в любую сторону.
  * @param totalSize Общий размер закачки.
  * @param identifier Идентификатор закачки.
  * @param fileName Имя файла.
  * @param relativePath Относительный путь, если закачивается много файлов или целая папка.
                        The file's relative path when selecting a directory
                        (defaults to file name in all browsers except Chrome).
  * @param hashesHex Контрольные суммы chunk'а.
  *                  Обычно не более одной, но кто знает...
  */
final case class MUploadChunkQs(
                                 chunkNumberO       : Option[Int],
                                 totalChunks        : Option[Int] = None,
                                 chunkSizeGeneralO  : Option[MUploadChunkSize] = None,
                                 chunkSizeCurrent   : Option[Long]   = None,
                                 totalSize          : Option[Long]   = None,
                                 identifier         : Option[String] = None,
                                 fileName           : Option[String] = None,
                                 relativePath       : Option[String] = None,
                                 hashesHex          : HashesHex = Map.empty,
                               )
