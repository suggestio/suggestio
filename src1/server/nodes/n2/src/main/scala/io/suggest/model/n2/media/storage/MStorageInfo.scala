package io.suggest.model.n2.media.storage

import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.xplay.qsb.QueryStringBindableImpl
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.mvc.QueryStringBindable

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
  }

  implicit def mediaStorageInfoJson: OFormat[MStorageInfo] = {
    val F = Fields
    (
      (__ \ F.STORAGE_FN).format[MStorage] and
      (__ \ F.DATA_FN).format[MStorageInfoData]
    )(apply, unlift(unapply))
  }


  @inline implicit def univEq: UnivEq[MStorageInfo] = UnivEq.derive

  implicit def storageInfoQsb(implicit
                              storageB   : QueryStringBindable[MStorage],
                              infoDataB  : QueryStringBindable[MStorageInfoData],
                             ): QueryStringBindable[MStorageInfo] = {
    new QueryStringBindableImpl[MStorageInfo] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MStorageInfo]] = {
        val k = key1F(key)
        for {
          storageE <- storageB.bind( k(Fields.STORAGE_FN), params )
          infoE    <- infoDataB.bind( k(Fields.DATA_FN), params )
        } yield {
          for {
            storage <- storageE
            info    <- infoE
          } yield {
            MStorageInfo(
              storage = storage,
              data    = info,
            )
          }
        }
      }

      override def unbind(key: String, value: MStorageInfo): String = {
        val k = key1F(key)
        _mergeUnbinded1(
          storageB.unbind( k(Fields.STORAGE_FN), value.storage ),
          infoDataB.unbind( k(Fields.DATA_FN), value.data ),
        )
      }
    }
  }

  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl._
    val F = Fields
    Json.obj(
      F.STORAGE_FN -> FKeyWord.indexedJs,
      F.DATA_FN    -> FObject.plain( MStorageInfoData ),
    )
  }

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
