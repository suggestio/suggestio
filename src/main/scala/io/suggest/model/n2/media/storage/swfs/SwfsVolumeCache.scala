package io.suggest.model.n2.media.storage.swfs

import com.google.inject.{Singleton, Inject}
import io.suggest.playx.CacheApiUtil
import io.suggest.swfs.client.ISwfsClient
import io.suggest.swfs.client.proto.VolumeId_t
import io.suggest.swfs.client.proto.lookup.{IVolumeLocation, LookupRequest}
import io.suggest.util.MacroLogsImpl
import play.api.Configuration
import play.api.cache.CacheApi

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.10.15 20:17
 * Description: Кеш-модель для доступа к volume urls на основе volume id.
 * Используется play-cache в качестве backend-хранилища.
 */
@Singleton
class SwfsVolumeCache @Inject() (
  cache               : CacheApi,
  cacheUtil           : CacheApiUtil,
  configuration       : Configuration,
  client              : ISwfsClient,
  implicit val ec     : ExecutionContext
)
  extends MacroLogsImpl
{

  def CACHE_PREFIX = "swfs.vloc."

  val CACHE_DURATION = configuration.getInt("swfs.vloc.cache.seconds")
    .getOrElse(3600)
    .seconds

  protected def _ck(volumeId: VolumeId_t): String = {
    CACHE_PREFIX + volumeId
  }

  /**
   * Узнать volume locations с использование кеша.
   * @param volumeId id swfs-раздела.
   * @return Фьючерс со списком volume-адресов.
   */
  def getLocations(volumeId: VolumeId_t): Future[Seq[IVolumeLocation]] = {
    val ck = _ck(volumeId)
    cacheUtil.getOrElseFut(ck, CACHE_DURATION) {
      val lr = LookupRequest(
        volumeId = volumeId
      )
      val fut = client.lookup(lr)
        .map {
          case Right(resp) =>
            LOGGER.trace("Lookup volumeId=" + volumeId + " => " + resp.locations.iterator.map(_.publicUrl).mkString(",") )
            resp.locations
          case Left(err) =>
            LOGGER.warn(s"Failed to lookup volumeId=$volumeId due to: ${err.error}")
            err.locations
        }
      // Если нет нормального результата, то нужно удалить фьючерс из кеша.
      fut.filter(_.nonEmpty)
        .onFailure { case ex: Throwable =>
          cache.remove(ck)
          if (!ex.isInstanceOf[NoSuchElementException])
            LOGGER.error(s"Volume $volumeId lookup failed", ex)
        }
      // Вернуть фьючерс результата
      fut
    }
  }

  def uncache(volumeId: VolumeId_t): Unit = {
    cache.remove( _ck(volumeId) )
  }

}
