package io.suggest.model.es

import com.google.inject.{ImplementedBy, Inject, Singleton}
import io.suggest.di.{IEsClient, IExecutionContext, ISioNotifier}
import io.suggest.event.SioNotifierStaticClientI
import org.elasticsearch.client.Client

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

/** Интерфейс для поля с DI-инстансом контейнера общего хлама в DI-моделях. */
trait IEsModelDi {
  val mCommonDi: IEsModelDiVal
}


/** Дефолтовая реализация [[IEsModelDiVal]]. */
@Singleton
class MEsModelDiVal @Inject() (
  override implicit val ec        : ExecutionContext,
  override implicit val esClient  : Client,
  override implicit val sn        : SioNotifierStaticClientI
)
  extends IEsModelDiVal
