package io.suggest.n2.media.storage

import io.suggest.url.MHostInfo
import io.suggest.xplay.qsb.AbstractQueryStringBindable
import play.api.mvc.QueryStringBindable
import io.suggest.url.bind.QueryStringBindableUtil._

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
    def STORAGE_FN        = "s"
  }

  /** Поддержка биндинга для URL qs. */
  implicit def mAssignedStorageQsb(implicit
                                   hostB           : QueryStringBindable[MHostInfo],
                                   mediaStorageB   : QueryStringBindable[MStorageInfo],
                                  ): QueryStringBindable[MAssignedStorage] = {
    new AbstractQueryStringBindable[MAssignedStorage] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MAssignedStorage]] = {
        val k = key1F(key)
        val F = Fields
        for {
          hostE             <- hostB.bind( k(F.HOST_FN), params )
          storageE          <- mediaStorageB.bind( k(F.STORAGE_FN), params )
        } yield {
          for {
            host            <- hostE
            storage         <- storageE
          } yield {
            MAssignedStorage(
              host          = host,
              storage       = storage,
            )
          }
        }
      }

      override def unbind(key: String, value: MAssignedStorage): String = {
        val k = key1F(key)
        val F = Fields
        _mergeUnbinded1(
          hostB.unbind          ( k(F.HOST_FN),         value.host ),
          mediaStorageB.unbind  ( k(F.STORAGE_FN),      value.storage ),
        )
      }

    }
  }

}


/** Класс-контейнер данных по хосту и стораджу для распределённого хранения данных.
  *
  * @param storage Данные назначенного media-хранилища.
  */
case class MAssignedStorage(
                             host        : MHostInfo,
                             storage     : MStorageInfo,
                           )
