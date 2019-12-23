package io.suggest.model.n2.media.storage

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.swfs.client.proto.fid.Fid
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 29.09.15 20:57
 * Description: Модель имён полей для моделей [[IMediaStorage]].
 */
object MStoragesFieldNames extends StringEnum[MStoragesFieldName] {

  // common
  case object STYPE extends MStoragesFieldName("t") {

    override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
      import dsl._
      Json.obj(
        fn -> FKeyWord.indexedJs,
      )
    }

  }


  // seaweedfs
  case object FID extends MStoragesFieldName("i") {

    override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
      import dsl._
      val F = Fid.Fields
      Json.obj(
        fn -> FObject.plain(
          enabled    = someTrue,
          properties = Some( Json.obj(
            F.VOLUME_ID_FN -> FNumber(
              typ   = DocFieldTypes.Integer,
              index = someTrue,
            ),
            F.FILE_ID_FN -> FKeyWord.notIndexedJs,
          ))
        )
      )
    }

  }


  /** JSON format для поля типа storage модели MMedia. */
  def STYPE_FN_FORMAT: OFormat[MStorage] =
    (__ \ MStoragesFieldNames.STYPE.fn).format[MStorage]

  override def values = findValues

}


/** Класс модели названий полей storage-моделей. */
sealed abstract class MStoragesFieldName(override val value: String) extends StringEnumEntry with IEsMappingProps {

  /** Идентификатор (название) поля на стороне ES. */
  final def fn: String = value

}

