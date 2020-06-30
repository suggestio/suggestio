package io.suggest.up

import io.suggest.crypto.hash.HashesHex
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import HashesHex._
import io.suggest.common.empty.EmptyUtil

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.06.2020 7:01
  * Description: Модель qs-аргументов для resumable.js-закачки или иной chunked-закачки на сервер.
  */
object MUploadChunkQs {

  object Fields {
    final def CHUNK_NUMBER    = "cn"
    final def TOTAL_CHUNKS    = "ct"
    final def CHUNK_SIZE      = "cs"
    final def TOTAL_SIZE      = "s"
    final def IDENTIFIER      = "i"
    final def FILENAME        = "fn"
    final def RELATIVE_PATH   = "rp"
    final def HASHES_HEX      = "h"
  }

  implicit def uploadChunkQsJson: OFormat[MUploadChunkQs] = {
    val F = Fields
    (
      (__ \ F.CHUNK_NUMBER).format[Int] and
      (__ \ F.TOTAL_CHUNKS).formatNullable[Int] and
      (__ \ F.CHUNK_SIZE).format[MUploadChunkSize] and
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

    def chunkNumber0: Int =
      chunkNumber1 - FIRST_CHUNK_NUMBER

    def chunkNumber1: Int =
      chunkQs.chunkNumber

  }

}


/** Контейнер qs-аргументов от resumable.js.
  * Многие параметры необязательные, т.к. приходят ещё на первой фазе.
  * В будущем можно будет переписать resumable.js на scala.js.
  *
  * @param chunkNumber Порядковый номер chunk'а.
  *                    Первый chunk = это 1, НЕ 0 (особенность resumable.js).
  * @param totalChunks Общее кол-во chunk'ов.
  * @param chunkSizeGeneral Размер одного chunk'а.
  *                         Размер последнего chunk'а будет отличаться в любую сторону.
  * @param totalSize Общий размер закачки.
  * @param identifier Идентификатор закачки.
  * @param fileName Имя файла.
  * @param relativePath Относительный путь, если закачивается много файлов или целая папка.
  * @param hashesHex Контрольные суммы chunk'а.
  *                  Обычно не более одной, но кто знает...
  */
case class MUploadChunkQs(
                           chunkNumber        : Int,
                           totalChunks        : Option[Int] = None,
                           chunkSizeGeneral   : MUploadChunkSize = MUploadChunkSizes.default,
                           totalSize          : Option[Long]   = None,
                           identifier         : Option[String] = None,
                           fileName           : Option[String] = None,
                           relativePath       : Option[String] = None,
                           hashesHex          : HashesHex,
                         )
