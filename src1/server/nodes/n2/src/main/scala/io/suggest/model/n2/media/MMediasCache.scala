package io.suggest.model.n2.media

import javax.inject.{Inject, Singleton}

import io.suggest.es.model.EsModelCache
import play.api.cache.AsyncCacheApi

import scala.concurrent.ExecutionContext
import scala.concurrent.duration._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.10.17 15:13
  * Description: Кеш для media-инстансов.
  * Изначально его не существовало, однако потребность всё-таки возникла.
  *
  * Т.е. media очень много (пара десятков на каждую картинку-узел, например), а сами инстансы
  * обычно не модифицируются после создания, то кэш короткий и НЕ мониторит обновления инстансов.
  */
@Singleton
class MMediasCache @Inject()(
                              mMedias                         : MMedias,
                              override val cache              : AsyncCacheApi,
                              override implicit val ec        : ExecutionContext
                            )
  extends EsModelCache[MMedia]
{

  override val EXPIRE = 10.seconds

  override val CACHE_KEY_SUFFIX = ".mme"

  override def companion = mMedias

}

trait IMediasCacheDi {
  def mMediasCache: MMediasCache
}
