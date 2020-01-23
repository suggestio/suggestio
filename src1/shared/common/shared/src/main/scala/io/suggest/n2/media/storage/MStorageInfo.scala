package io.suggest.n2.media.storage

import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.model.PrefixedFn
import japgolly.univeq.UnivEq
import play.api.libs.functional.syntax._
import play.api.libs.json._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.12.2019 22:41
  * Description: Инфа по хранилищу. НЕ сохраняется в БД, живёт только в рамках запросов.
  * На сервере - данные из этой модели как-то распихивается внутри эджа.
  */
object MStorageInfo extends IEsMappingProps {

  object Fields {
    def STORAGE_FN = "s"
    def DATA_FN = "i"
    object Data extends PrefixedFn {
      import MStorageInfoData.{Fields => F}
      override protected def _PARENT_FN = DATA_FN
      def DATA_META_FN = _fullFn( F.META_FN )
    }
  }

  implicit def mediaStorageInfoJson: OFormat[MStorageInfo] = {
    val F = Fields
    (
      (__ \ F.STORAGE_FN).format[MStorage] and
      (__ \ F.DATA_FN).format[MStorageInfoData]
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[MStorageInfo] = UnivEq.derive

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.STORAGE_FN -> FKeyWord.indexedJs,
      F.DATA_FN    -> FObject.plain( MStorageInfoData ),
    )
  }


  implicit class StorageInfoOpsExt( val storage1: MStorageInfo ) extends AnyVal {

    def isSameFile( storage2: MStorageInfo ): Boolean = {
      (storage1.storage ==* storage2.storage) &&
      (storage1.data.meta ==* storage2.data.meta)
    }

  }

  lazy val storage = GenLens[MStorageInfo](_.storage)
  lazy val data = GenLens[MStorageInfo](_.data)

}


/** Данные любого хранилища.
  *
  * @param storage Тип хранилища.
  *                На основе типа хранилища будет предоставлен инстанс клиента для доступа к хранилищу.
  * @param data "Координаты" объекта в конкретном сторадже.
  *             Данные передаются в клиент конкретного хранилища, и понятны этому хранилищу.
  */
case class MStorageInfo(
                         storage   : MStorage,
                         data      : MStorageInfoData,
                       )
