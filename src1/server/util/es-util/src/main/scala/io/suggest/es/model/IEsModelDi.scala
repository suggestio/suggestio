package io.suggest.es.model

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.ICommonDiValBase
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



/** Дефолтовая реализация [[IEsModelDiVal]]. */
@Singleton
class MEsModelDiVal @Inject() (
  override val current            : Application,
  override implicit val ec        : ExecutionContext,
  override implicit val sn        : SioNotifierStaticClientI,
  override implicit val mat       : Materializer
)
  extends IEsModelDiVal


