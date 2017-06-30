package io.suggest.es.model

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import io.suggest.es.util.IEsClient
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.{ICommonDiValBase, IMCommonDiBase}
import io.suggest.playx.CacheApiUtil
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
  extends ICommonDiValBase
  with IEsClient
{
  def esScrollPublisherFactory: EsScrollPublisherFactory
}


/** Интерфейс для поля с DI-инстансом контейнера общего хлама в DI-моделях. */
trait IEsModelDi extends IMCommonDiBase {
  override val mCommonDi: IEsModelDiVal
}


/** Дефолтовая реализация [[IEsModelDiVal]]. */
@Singleton
class MEsModelDiVal @Inject() (
  override val cacheApiUtil       : CacheApiUtil,
  override val current            : Application,
  override val esScrollPublisherFactory: EsScrollPublisherFactory,
  override implicit val ec        : ExecutionContext,
  override implicit val esClient  : Client,
  override implicit val sn        : SioNotifierStaticClientI,
  override implicit val mat       : Materializer
)
  extends IEsModelDiVal

