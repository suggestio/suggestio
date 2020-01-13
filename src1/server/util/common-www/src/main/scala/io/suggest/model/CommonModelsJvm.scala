package io.suggest.model

import io.suggest.enum2.EnumeratumJvmUtil
import io.suggest.n2.media.storage.{MStorage, MStorageInfo, MStorageInfoData, MStorages}
import _root_.play.api.mvc.QueryStringBindable
import io.suggest.sc.{MScApiVsn, MScApiVsns}
import io.suggest.swfs.fid.Fid
import io.suggest.util.logs.MacroLogsDyn
import io.suggest.xplay.qsb.QueryStringBindableImpl

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.12.2019 8:25
  * Description: Контейнер jvm-only данных для разных пошаренных common-моделей.
  */
object CommonModelsJvm extends MacroLogsDyn {

  /** Биндинги для url query string. */
  implicit def mScApiVsnQsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[MScApiVsn] =
    EnumeratumJvmUtil.valueEnumQsb( MScApiVsns )

  /** QSB для инстансов [[MStorage]]. */
  implicit def mStorageQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[MStorage] =
    EnumeratumJvmUtil.valueEnumQsb( MStorages )


  /** Поддержка сырого биндинга из query-string. */
  implicit def fidQsb(implicit strB: QueryStringBindable[String]): QueryStringBindable[Fid] = {
    new QueryStringBindableImpl[Fid] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Fid]] = {
        val fidStrRaw = strB.bind(key, params)
        for (fidStrE <- fidStrRaw) yield {
          for {
            fidStr <- fidStrE
            parsed <- Try( Fid(fidStr) )
              .toEither
              .left.map { ex =>
                LOGGER.error(s"qsb: failed to bind $fidStrRaw", ex)
                ex.getMessage
              }
          } yield {
            parsed
          }
        }
      }

      override def unbind(key: String, value: Fid): String = {
        strB.unbind(key, value.toString)
      }
    }
  }


  implicit def storageInfoDataQsb(implicit
                                  strB         : QueryStringBindable[String],
                                  strSetB      : QueryStringBindable[Seq[String]],
                                 ): QueryStringBindable[MStorageInfoData] = {
    new QueryStringBindableImpl[MStorageInfoData] {

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MStorageInfoData]] = {
        val F = MStorageInfoData.Fields
        val k = key1F(key)
        for {
          dataE       <- strB.bind( k(F.DATA_FN), params )
          hostsE      <- strSetB.bind( k(F.HOST_FN), params )
        } yield {
          for {
            data      <- dataE
            hosts     <- hostsE
          } yield {
            MStorageInfoData(
              data  = data,
              hosts = hosts,
            )
          }
        }
      }

      override def unbind(key: String, value: MStorageInfoData): String = {
        val F = MStorageInfoData.Fields
        val k = key1F(key)
        _mergeUnbinded1(
          strB.unbind( k(F.DATA_FN), value.data ),
          strSetB.unbind( k(F.HOST_FN), value.hosts ),
        )
      }

    }
  }


  implicit def storageInfoQsb(implicit
                              storageB   : QueryStringBindable[MStorage],
                              infoDataB  : QueryStringBindable[MStorageInfoData],
                             ): QueryStringBindable[MStorageInfo] = {
    new QueryStringBindableImpl[MStorageInfo] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MStorageInfo]] = {
        val F = MStorageInfo.Fields
        val k = key1F(key)
        for {
          storageE <- storageB.bind( k(F.STORAGE_FN), params )
          infoE    <- infoDataB.bind( k(F.DATA_FN), params )
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
        val F = MStorageInfo.Fields
        val k = key1F(key)
        _mergeUnbinded1(
          storageB.unbind( k(F.STORAGE_FN), value.storage ),
          infoDataB.unbind( k(F.DATA_FN), value.data ),
        )
      }
    }
  }

}
