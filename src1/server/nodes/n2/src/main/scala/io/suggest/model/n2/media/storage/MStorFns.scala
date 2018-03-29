package io.suggest.model.n2.media.storage

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.es.util.SioEsUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:57
 * Description: Модель имён полей для моделей [[IMediaStorage]].
 */
object MStorFns extends StringEnum[MStorFn] {

  // common
  case object STYPE extends MStorFn("t") {

    override def esMappingProp: DocField = {
      FieldKeyword(fn, index = true, include_in_all = false)
    }
  }


  // seaweedfs
  case object FID extends MStorFn("i") {

    override def esMappingProp: DocField = {
      val F = Fid.Fields
      FieldObject(fn, enabled = true, properties = Seq(
        FieldNumber(F.VOLUME_ID_FN, fieldType = DocFieldTypes.integer, index = true, include_in_all = false),
        FieldKeyword(F.FILE_ID_FN, index = false, include_in_all = false)
      ))
    }
  }


  override val values = findValues

}


/** Класс модели названий полей storage-моделей. */
sealed abstract class MStorFn(override val value: String) extends StringEnumEntry {

  /** Идентификатор (название) поля на стороне ES. */
  final def fn: String = value

  /** ES-описание поля. */
  def esMappingProp: DocField

}

