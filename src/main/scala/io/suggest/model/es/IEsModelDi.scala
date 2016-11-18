package io.suggest.model.es

import com.google.inject.{ImplementedBy, Inject, Singleton}
import com.sksamuel.elastic4s.ElasticClient
import io.suggest.di._
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.playx.{CacheApiUtil, ICurrentAppHelpers, ICurrentConf}
import org.elasticsearch.client.Client
import play.api.Application

import scala.concurrent.ExecutionContext

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.04.16 14:00
  * Description: Интерфейс для минимального контейнера инжектируемых интансов у реализаций ES-моделей.
  */
@ImplementedBy( classOf[MEsModelDiVal] )
trait IEsModelDiVal
  extends IExecutionContext
  with IEsClient
  with ISioNotifier
  with ICurrentConf
  with ICurrentAppHelpers
  with ICacheApiUtil
  with ICurrentActorSystem
  with IEs4sClient


/** Интерфейс для поля с DI-инстансом контейнера общего хлама в DI-моделях. */
trait IEsModelDi {
  val mCommonDi: IEsModelDiVal
}


/** Дефолтовая реализация [[IEsModelDiVal]]. */
@Singleton
class MEsModelDiVal @Inject() (
  override val cacheApiUtil       : CacheApiUtil,
  override val current            : Application,
  override val es4sClient         : ElasticClient,
  override implicit val ec        : ExecutionContext,
  override implicit val esClient  : Client,
  override implicit val sn        : SioNotifierStaticClientI
)
  extends IEsModelDiVal

