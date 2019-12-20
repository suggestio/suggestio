package io.suggest.model.n2.media

import io.suggest.common.empty.EmptyUtil
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.model.PrefixedFn
import io.suggest.model.n2.media.storage.MStorage
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.2019 14:03
  * Description: Модель media-данны внутри эджа.
  * Появилась на смену отдельной модели MMedia, чтобы файлы хранились в одной упряжке с узлами.
  * Это также шажок навстречу миграции на ES-6+, где убрали костыльное поле _type из документов.
  *
  * Явно-пустая модель, т.к. несёт в себе обязательные поля.
  */
object MEdgeMedia
  extends IEsMappingProps
{

  object Fields {

    /** Поля [[MFileMeta]] */
    object FileMeta extends PrefixedFn {

      val FILE_META_FN    = "fm"
      override protected def _PARENT_FN = FILE_META_FN

      def MIME_FN               = _fullFn( MFileMeta.Fields.MIME_FN )
      def MIME_AS_TEXT_FN       = _fullFn( MFileMeta.Fields.MIME_FN + "." + MFileMeta.Fields.MIME_AS_TEXT_FN )

      /** Full FN nested-поля с хешами. */
      def HASHES_FN             = _fullFn( MFileMeta.Fields.HASHES_HEX_FN )

      def HASHES_TYPE_FN        = _fullFn( MFileMeta.Fields.HashesHexFields.HASH_TYPE_FN )
      def HASHES_VALUE_FN       = _fullFn( MFileMeta.Fields.HashesHexFields.HASH_VALUE_FN )

      def SIZE_B_FN             = _fullFn( MFileMeta.Fields.SIZE_B_FN )

      def IS_ORIGINAL_FN        = _fullFn( MFileMeta.Fields.IS_ORIGINAL_FN )

    }

    /** Поля [[MPictureMeta]] */
    object PictureMeta {
      val PICTURE_META_FN = "pm"
    }

    /** Поля [[io.suggest.model.n2.media.storage.MStorages]]. */
    object Storage {
      val STORAGE_FN      = "st"
    }

  }


  /** Поддержка JSON для сериализации-десериализации тела документа elasticsearch. */
  val FORMAT_DATA: OFormat[MEdgeMedia] = {
    val F = Fields
    (
      (__ \ F.Storage.STORAGE_FN).format[MStorage] and
      (__ \ F.FileMeta.FILE_META_FN).format[MFileMeta] and
      (__ \ F.PictureMeta.PICTURE_META_FN).formatNullable[MPictureMeta]
        .inmap[MPictureMeta](
          EmptyUtil.opt2ImplMEmptyF(MPictureMeta),
          EmptyUtil.implEmpty2OptF[MPictureMeta]
        )
    )( apply, unlift(unapply) )
  }

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.FileMeta.FILE_META_FN -> FObject.plain(
        enabled     = someTrue,
        properties  = Some( MFileMeta.esMappingProps ),
      ),
      F.Storage.STORAGE_FN -> FKeyWord.indexedJs,
      F.PictureMeta.PICTURE_META_FN -> FObject.plain(
        enabled     = someTrue,
        properties  = Some( MPictureMeta.esMappingProps ),
      ),
    )
  }

  val file        = GenLens[MEdgeMedia](_.file)
  val storage     = GenLens[MEdgeMedia](_.storage)
  val picture     = GenLens[MEdgeMedia](_.picture)

  @inline implicit def univEq: UnivEq[MEdgeMedia] = UnivEq.derive

}


/** Класс модели mmedia, хранящей инфу о файле.
  *
  * @param file Данные по файлу.
  * @param storage Хранилище файла.
  * @param picture Метаданные картинки, если это картинка.
  */
case class MEdgeMedia(
                       storage          : MStorage,
                       file             : MFileMeta,
                       picture          : MPictureMeta        = MPictureMeta.empty,
                     )
