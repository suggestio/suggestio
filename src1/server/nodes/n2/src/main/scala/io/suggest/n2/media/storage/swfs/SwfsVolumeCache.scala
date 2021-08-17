package io.suggest.n2.media.storage.swfs

import akka.actor.ActorSystem

import javax.inject.Inject
import io.suggest.playx.CacheApiUtil
import io.suggest.swfs.client.ISwfsClient
import io.suggest.swfs.client.proto.lookup.{IVolumeLocation, LookupRequest}
import io.suggest.swfs.fid.SwfsVolumeId_t
import io.suggest.util.logs.MacroLogsImpl
import play.api.cache.AsyncCacheApi
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 20:17
 * Description: Кеш-модель для доступа к volume urls на основе volume id.
 * Используется play-cache в качестве backend-хранилища.
 */
final class SwfsVolumeCache @Inject() (
                                        injector                  : Injector,
                                      )
  extends MacroLogsImpl
{

  private lazy val asyncCacheApi = injector.instanceOf[AsyncCacheApi]
  private lazy val cacheUtil = injector.instanceOf[CacheApiUtil]
  private lazy val client = injector.instanceOf[ISwfsClient]
  private lazy val actorSystem = injector.instanceOf[ActorSystem]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  private def CACHE_PREFIX = "swfs.vloc."

  private def CACHE_DURATION = 300.seconds

  /**
   * Сгенерить строку ключа кеша.
   * @param volumeId id раздела swfs.
   * @return Ключ кеша.
   */
  def _ck(volumeId: SwfsVolumeId_t): String =
    CACHE_PREFIX + volumeId


  /**
   * Узнать volume locations с использование кеша.
   * @param volumeId id swfs-раздела.
   * @return Фьючерс со списком volume-адресов.
   */
  def getLocations(volumeId: SwfsVolumeId_t): Future[Seq[IVolumeLocation]] = {
    val ck = _ck(volumeId)

    cacheUtil.getOrElseFut(ck, CACHE_DURATION) {
      val lr = LookupRequest(
        volumeId = volumeId
      )

      client
        .lookup(lr)
        .map { tryRes =>
          tryRes.fold(
            {err =>
              LOGGER.warn(s"Failed to lookup volumeId=$volumeId due to: ${err.error}")
              err.locations
            },
            {resp =>
              if (resp.locations.nonEmpty)
                LOGGER.trace("Lookup volumeId=" + volumeId + " => " + resp.locations.iterator.map(_.publicUrl).mkString(",") )
              else
                LOGGER.error(s"Volume $volumeId lookup failed")

              resp.locations
            }
          )
        }
        .recover { case ex: Throwable =>
          LOGGER.error(s"Volume $volumeId lookup failed", ex)
          // Удаляем из кэша ошибку, чтобы система могла по-скорее попробовать ещё раз.
          actorSystem.scheduler.scheduleOnce( 1.second ) {
            // Наверное, не надо перепроверять фьючерс в кэше - наврядли он может быть заменён.
            asyncCacheApi.remove(ck)
          }
          Nil
        }
    }
  }

  def uncache(volumeId: SwfsVolumeId_t): Unit = {
    asyncCacheApi.remove( _ck(volumeId) )
  }

}

