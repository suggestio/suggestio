package io.suggest.model.n2.media.storage

import io.suggest.common.menum.EnumMaybeWithName
import io.suggest.model.menum.EnumJsonReadsT
import io.suggest.model.n2.media.storage.swfs.SwfsStorages
import play.api.libs.json.__

import scala.reflect.ClassTag

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:04
 * Description: Модель типов используемых хранилищь для media-файлов.
 */
object MStorages extends EnumMaybeWithName with EnumJsonReadsT {

  /** Экземпляр модели. */
  protected[this] abstract class Val(val strId: String)
    extends super.Val(strId)
  {
    /** Данные по классу-компаниону модели для возможности инжекции класса по типу. */
    def companionCt: ClassTag[IMediaStorageStaticImpl]
  }

  override type T = Val


  /** SeaWeedFS.
    * Хранилище на смену кассандре (oct.2015-...). */
  val SeaWeedFs: T = new Val("s") {
    override def companionCt = ClassTag( classOf[SwfsStorages] )
  }


  /** JSON format для поля типа storage модели MMedia. */
  val STYPE_FN_FORMAT = (__ \ MStorFns.STYPE.fn).format[T]

}
