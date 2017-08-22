package io.suggest.model.n2.media.storage

import enumeratum._
import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.es.util.SioEsUtil._

/** Класс модели названий полей storage-моделей. */
sealed abstract class MStorFn extends EnumEntry {

  /** Идентификатор (название) поля на стороне ES. */
  def fn: String

  /** ES-описание поля. */
  def esMappingProp: DocField

  override def entryName = fn

}


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:57
 * Description: Модель имён полей для моделей [[IMediaStorage]].
 */
object MStorFns extends Enum[MStorFn] {

  // common
  case object STYPE extends MStorFn {

    override def fn: String = "t"

    override def esMappingProp: DocField = {
      FieldKeyword(fn, index = true, include_in_all = false)
    }
  }


  // seaweedfs
  case object FID extends MStorFn {

    override def fn: String = "i"

    override def esMappingProp: DocField = {
      FieldObject(fn, enabled = true, properties = Seq(
        FieldNumber(Fid.VOLUME_ID_FN, fieldType = DocFieldTypes.integer, index = true, include_in_all = false),
        FieldKeyword(Fid.FILE_ID_FN, index = false, include_in_all = false)
      ))
    }
  }


  override val values = findValues

}
