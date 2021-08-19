package models.mproj

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import io.suggest.event.SioNotifierStaticClientI
import play.api.Application
import play.api.db.slick.DatabaseConfigProvider
import io.suggest.di.ISlickDbConfig
import io.suggest.es.model.IEsModelDiVal

import scala.concurrent.ExecutionContext

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.12.15 17:13
 * Description: DI-контейнер для контроллеров с очень частоиспользуемыми DI-компонентами.
 * Сформирована для возможности быстро расширять список очень базовых зависимых компонент контроллеров и трейтов ACL.
 * Инжектить нужно именно этот [[ICommonDi]].
 */
@ImplementedBy( classOf[MCommonDi] )
trait ICommonDi
  extends IEsModelDiVal
  with ISlickDbConfig
{
  // выставляем implicit, т.к. до-DI'шные websocket'ы требуют implicit application in scope.
  // TODO После перевода вёб-сокетов на akka streams, удалить implicit у current.
  override implicit val current       : Application
  override implicit val ec            : ExecutionContext
  override val _slickConfigProvider   : DatabaseConfigProvider
}


/** Дефолтовая реализация модели common-компонентов. */
@Singleton
final class MCommonDi @Inject() (
                                  override val _slickConfigProvider   : DatabaseConfigProvider,
                                  override implicit val current       : Application,
                                  override implicit val mat           : Materializer,
                                  override implicit val ec            : ExecutionContext,
                                  override implicit val sn            : SioNotifierStaticClientI
                                )
  extends ICommonDi


/** Интерфейс к DI-полю со значением [[MCommonDi]] */
trait IMCommonDi {
  val mCommonDi: ICommonDi
}
