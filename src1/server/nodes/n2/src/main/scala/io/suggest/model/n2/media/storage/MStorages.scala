package io.suggest.model.n2.media.storage

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.enum2.{EnumeratumJvmUtil, EnumeratumUtil}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.mvc.QueryStringBindable

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

  override def values = findValues

}


/** Класс одного элемента модели. */
sealed abstract class MStorage(override val value: String) extends StringEnumEntry


object MStorage {

  implicit val MSTORAGE_FORMAT: Format[MStorage] = {
    EnumeratumUtil.valueEnumEntryFormat( MStorages )
  }

  /** JSON format для поля типа storage модели MMedia. */
  val STYPE_FN_FORMAT: OFormat[MStorage] = {
    (__ \ MStorFns.STYPE.fn).format[MStorage]
  }


  /** QSB для инстансов [[MStorage]]. */
  implicit def mStorageQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MStorage] = {
    EnumeratumJvmUtil.valueEnumQsb( MStorages )
  }

  @inline implicit def univEq: UnivEq[MStorage] = UnivEq.derive

}
