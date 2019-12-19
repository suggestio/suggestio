package io.suggest.model.n2.media.storage

import enumeratum.values.{StringEnum, StringEnumEntry}
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.swfs.client.proto.fid.Fid
import io.suggest.es.util.SioEsUtil._
import play.api.libs.json.{JsObject, Json}

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

    override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
      import dsl._
      Json.obj(
        fn -> FKeyWord.indexedJs,
      )
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


  override def values = findValues

}


/** Класс модели названий полей storage-моделей. */
sealed abstract class MStorFn(override val value: String) extends StringEnumEntry with IEsMappingProps {

  /** Идентификатор (название) поля на стороне ES. */
  final def fn: String = value

  /** ES-описание поля. */
  def esMappingProp: DocField

}

