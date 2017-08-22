package io.suggest.model.n2.media.storage

import enumeratum._
import io.suggest.enum2.EnumeratumUtil
import io.suggest.model.n2.media.storage.swfs.SwfsStorages
import io.suggest.primo.IStrId
import play.api.libs.json.{Format, OFormat, __}

import scala.reflect.ClassTag

object MStorage {

  implicit val MSTORAGE_FORMAT: Format[MStorage] = {
    EnumeratumUtil.enumEntryFormat( MStorages )
  }

  /** JSON format для поля типа storage модели MMedia. */
  val STYPE_FN_FORMAT: OFormat[MStorage] = {
    (__ \ MStorFns.STYPE.fn).format[MStorage]
  }

}


/** Класс одного элемента модели. */
sealed abstract class MStorage extends EnumEntry with IStrId {

  override def entryName = strId

  /** Данные по классу-компаниону модели для возможности инжекции класса по типу. */
  def companionCt: ClassTag[IMediaStorageStaticImpl]

}


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:04
 * Description: Модель типов используемых хранилищь для media-файлов.
 */
object MStorages extends Enum[MStorage] {

  /** SeaWeedFS.
    * Хранилище на смену кассандре (oct.2015-...). */
  case object SeaWeedFs extends MStorage {
    /** Данные по классу-компаниону модели для возможности инжекции класса по типу. */
    override def companionCt = ClassTag( classOf[SwfsStorages] )
    override def strId = "s"
  }

  override val values = findValues

}
