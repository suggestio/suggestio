package io.suggest.n2.media.storage

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.EnumeratumUtil
import japgolly.univeq._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:04
 * Description: Модель типов используемых хранилищь для media-файлов.
 */
object MStorages extends StringEnum[MStorage] {

  /** SeaWeedFS.
    * Хранилище на смену кассандре (oct.2015-...). */
  case object SeaWeedFs extends MStorage("s")

  /** Используется для Asset'ов. */
  case object ClassPathResource extends MStorage("c")

  // HostPath?

  override def values = findValues

}


/** Класс одного элемента модели. */
sealed abstract class MStorage(override val value: String) extends StringEnumEntry


object MStorage {

  implicit def MSTORAGE_FORMAT: Format[MStorage] =
    EnumeratumUtil.valueEnumEntryFormat( MStorages )


  @inline implicit def univEq: UnivEq[MStorage] = UnivEq.derive

}
