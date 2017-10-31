package io.suggest.model.n2.media.storage

import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.url.MHostInfo
import play.api.mvc.QueryStringBindable

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.10.17 16:11
  * Description: Модель данных с инфой по распределённом стораджу.
  * Появилась в связи с необходимостью объеденить данные по dist-хостам
  * с возможностью проброса этих данных через upload qs.
  */
object MAssignedStorage {

  object Fields {
    def HOST_FN           = "h"
    def STORAGE_TYPE_FN   = "t"
    def STORAGE_INFO_FN   = "i"
  }

  /** Поддержка биндинга для URL qs. */
  implicit def mDistAssignRespQsb(implicit
                                  hostB     : QueryStringBindable[MHostInfo],
                                  strB      : QueryStringBindable[String],
                                  storageB  : QueryStringBindable[MStorage]
                                 ): QueryStringBindable[MAssignedStorage] = {
    new QueryStringBindableImpl[MAssignedStorage] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MAssignedStorage]] = {
        val k = key1F(key)
        val F = Fields
        for {
          hostE             <- hostB.bind( k(F.HOST_FN), params )
          storageTypeE      <- storageB.bind( k(F.STORAGE_TYPE_FN), params )
          storageInfoE      <- strB.bind( k(F.STORAGE_INFO_FN), params )
        } yield {
          for {
            host            <- hostE.right
            storageType     <- storageTypeE.right
            storageInfo     <- storageInfoE.right
          } yield {
            MAssignedStorage(
              host          = host,
              storageType   = storageType,
              storageInfo   = storageInfo
            )
          }
        }
      }

      override def unbind(key: String, value: MAssignedStorage): String = {
        val k = key1F(key)
        val F = Fields
        _mergeUnbinded1(
          hostB.unbind    ( k(F.HOST_FN),         value.host ),
          storageB.unbind ( k(F.STORAGE_TYPE_FN), value.storageType ),
          strB.unbind     ( k(F.STORAGE_INFO_FN), value.storageInfo )
        )
      }

    }
  }

}


/** Класс-контейнер данных по хосту и стораджу для распределённого хранения данных.
  *
  * @param storageType Используемый сторадж.
  * @param storageInfo Данные хранения, понятные конкретному стораджу.
  */
case class MAssignedStorage(
                             host        : MHostInfo,
                             storageType : MStorage,
                             storageInfo : String
                           )
