package io.suggest.es.model

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.ICommonDiValBase
import io.suggest.playx.CacheApiUtil
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
{
  //def esClientP: IEsClient
  //override implicit final def esClient = esClientP.esClient
  def esScrollPublisherFactory: EsScrollPublisherFactory
}


/** Дефолтовая реализация [[IEsModelDiVal]]. */
@Singleton
class MEsModelDiVal @Inject() (
  override val cacheApiUtil       : CacheApiUtil,
  override val current            : Application,
  override val esScrollPublisherFactory: EsScrollPublisherFactory,
  override implicit val ec        : ExecutionContext,
  override implicit val sn        : SioNotifierStaticClientI,
  override implicit val mat       : Materializer
)
  extends IEsModelDiVal


