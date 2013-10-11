package models

import play.api.cache.Cache
import play.api.Play.current
import io.suggest.model.{DomainSettingsT, DomainSettingsStaticT}
import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.06.13 16:33
 * Description: Враппер для sioutil-модели работы с DomainSettings.
 */

object MDomainSettings extends DomainSettingsStaticT {

  val CACHE_SECONDS = 60

  def cacheKey(dkey: String) = dkey + "/ds"

  /**
   * С помощью кеша и HDFS получить данные по домену.
   * @param dkey Ключ домена.
   * @return
   */
  def getForDkeyCached(dkey: String)(implicit ec:ExecutionContext) : Future[Option[DomainSettingsT]] = {
    val ck = cacheKey(dkey)
    Cache.getAs[Option[DomainSettingsT]](ck) match {
      case Some(dsOpt) =>
        Future.successful(dsOpt)

      case None =>
        val fut = super.getForDkey(dkey)
        fut onSuccess {
          case result => Cache.set(ck, result, 60)
        }
        fut
    }
  }

}
