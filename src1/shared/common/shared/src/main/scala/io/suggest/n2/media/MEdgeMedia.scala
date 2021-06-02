package io.suggest.n2.media

import io.suggest.common.empty.EmptyUtil
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.model.PrefixedFn
import io.suggest.n2.media.storage.MStorageInfo
import io.suggest.xplay.json.PlayJsonUtil
import japgolly.univeq._
import monocle.macros.GenLens
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.12.2019 14:03
  * Description: Модель media-данны внутри эджа.
  * Появилась на смену отдельной модели MMedia, чтобы файлы хранились в одной упряжке с узлами.
  * Это также шажок навстречу миграции на ES-6+, где убрали костыльное поле _type из документов.
  *
  * id файла в хранилище живёт в e.nodeId или в e.info.textNi, в зависимости от потребностей индексации.
  *
  * Явно-пустая модель (живёт внутри Option), модель несёт в себе обязательные поля.
  */
object MEdgeMedia
  extends IEsMappingProps
{

  object Fields {

    /** Имя поля базовых метаданных файла. Модель [[MFileMeta]]. */
    val FILE_FN    = "file"

    /** Поля [[io.suggest.n2.media.storage.MStorages]]. */
    val STORAGE_FN      = "storage"

    /** Поля [[MPictureMeta]] */
    val PICTURE_FN = "picture"


    /** Поля [[MFileMeta]] */
    object FileMeta extends PrefixedFn {
      override protected def _PARENT_FN = FILE_FN

      import MFileMeta.{Fields => F}

      def FM_MIME_FN               = _fullFn( F.MIME_FN )
      def FM_MIME_AS_TEXT_FN       = _fullFn( F.MIME_FN + "." + F.MIME_AS_TEXT_FN )

      /** Full FN nested-поля с хешами. */
      def FM_HASHES_FN             = _fullFn( F.HASHES_HEX_FN )

      def FM_HASHES_TYPE_FN        = _fullFn( F.HashesHexFields.HASH_TYPE_FN )
      def FM_HASHES_VALUE_FN       = _fullFn( F.HashesHexFields.HASH_VALUE_FN )

      def FM_SIZE_B_FN             = _fullFn( F.SIZE_B_FN )

      def FM_IS_ORIGINAL_FN        = _fullFn( F.IS_ORIGINAL_FN )

    }


    object StorageFns extends PrefixedFn {
      import MStorageInfo.{Fields => F}

      override protected def _PARENT_FN = STORAGE_FN

      def STORARGE_TYPE_FN = _fullFn( F.STORAGE_FN )
      def STORAGE_DATA_META_FN = _fullFn( F.Data.DATA_META_FN )
      def STORAGE_DATA_SHARDS_FN = _fullFn( F.Data.DATA_SHARDS_FN )
    }


    object Picture extends PrefixedFn {
      import MPictureMeta.Fields.{Wh => F}
      override protected def _PARENT_FN = PICTURE_FN
      def WH_WIDTH_FN = _fullFn( F.WIDTH_FN )
      def WH_HEIGHT_FN = _fullFn( F.HEIGHT_FN )
    }

  }


  /** Поддержка JSON для сериализации-десериализации тела документа elasticsearch. */
  implicit def edgeMediaJson: OFormat[MEdgeMedia] = {
    val F = Fields
    (
      PlayJsonUtil.fallbackPathFormat[MFileMeta]( F.FILE_FN, "fm" ) and
      PlayJsonUtil.fallbackPathFormatNullable[MStorageInfo]( F.STORAGE_FN, "o" ) and
      PlayJsonUtil.fallbackPathFormatNullable[MPictureMeta]( F.PICTURE_FN, "pm" )
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
      F.FILE_FN -> FObject.plain( MFileMeta ),
      F.STORAGE_FN   -> FObject.plain( MStorageInfo ),
      F.PICTURE_FN -> FObject.plain( MPictureMeta ),
    )
  }

  def file        = GenLens[MEdgeMedia](_.file)
  def storage     = GenLens[MEdgeMedia](_.storage)
  def picture     = GenLens[MEdgeMedia](_.picture)

  @inline implicit def univEq: UnivEq[MEdgeMedia] = UnivEq.derive

}


/** Класс модели mmedia, хранящей инфу о файле.
  *
  * @param file Данные по файлу.
  * @param storage Хранилище файла.
  * @param picture Метаданные картинки, если это картинка.
  */
final case class MEdgeMedia(
                             file             : MFileMeta,
                             storage          : Option[MStorageInfo]  = None,
                             picture          : MPictureMeta          = MPictureMeta.empty,
                           ) {

  override def toString: String = {
    val sb = new StringBuilder(256, "Media")
      .append('(')

    for (s <- storage)
      sb.append(s).append(',')

    sb.append( file )

    if (picture.nonEmpty)
      sb.append(',').append(picture)

    sb.append(')')
      .toString()
  }

}
