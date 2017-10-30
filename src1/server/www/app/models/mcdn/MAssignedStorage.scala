package models.mcdn

import io.suggest.model.n2.media.storage.MStorage
import io.suggest.model.play.qsb.QueryStringBindableImpl
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
    def HOST_EXT_FN = "he"
    def HOST_INT_FN = "hi"
    def STORAGE_TYPE_FN = "st"
    def STORAGE_INFO_FN = "si"
  }

  /** Поддержка биндинга для URL qs. */
  implicit def mDistAssignRespQsb(implicit
                                  strB      : QueryStringBindable[String],
                                  storageB  : QueryStringBindable[MStorage]
                                 ): QueryStringBindable[MAssignedStorage] = {
    new QueryStringBindableImpl[MAssignedStorage] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MAssignedStorage]] = {
        val k = key1F(key)
        val F = Fields
        for {
          hostExtE          <- strB.bind( k(F.HOST_EXT_FN), params )
          hostIntE          <- strB.bind( k(F.HOST_INT_FN), params )
          storageTypeE      <- storageB.bind( k(F.STORAGE_TYPE_FN), params )
          storageInfoE      <- strB.bind( k(F.STORAGE_INFO_FN), params )
        } yield {
          for {
            hostExt         <- hostExtE.right
            hostInt         <- hostIntE.right
            storageType     <- storageTypeE.right
            storageInfo     <- storageInfoE.right
          } yield {
            MAssignedStorage(
              hostExt       = hostExt,
              hostInt       = hostInt,
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
          strB.unbind     ( k(F.HOST_EXT_FN),     value.hostExt ),
          strB.unbind     ( k(F.HOST_INT_FN),     value.hostInt ),
          storageB.unbind ( k(F.STORAGE_TYPE_FN), value.storageType ),
          strB.unbind     ( k(F.STORAGE_INFO_FN), value.storageInfo )
        )
      }

    }
  }

}


/** Класс-контейнер данных по хосту и стораджу для распределённого хранения данных.
  *
  * @param hostExt Внешний (публичный) хостнейм, доступный для всех.
  * @param hostInt Внутренний хост.
  * @param storageType Используемый сторадж.
  * @param storageInfo Данные хранения, понятные конкретному стораджу.
  */
case class MAssignedStorage(
                             hostExt     : String,
                             hostInt     : String,
                             storageType : MStorage,
                             storageInfo : String
                           )
