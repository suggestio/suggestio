package models.mproj

import akka.stream.Materializer
import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import controllers.ErrorHandler
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.playx.CacheApiUtil
import models.mctx.Context2Factory
import play.api.Application
import play.api.cache.AsyncCacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.i18n.{Langs, MessagesApi}
import io.suggest.di.{ICacheApi, ISlickDbConfig}
import io.suggest.es.model.{EsScrollPublisherFactory, IEsModelDiVal}
import io.suggest.sec.util.Csrf
import util.tpl.HtmlCompressUtil

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
  with ICacheApi
  with ISlickDbConfig
{
  // Для возможно оптимизации, всё объявляем как val, т.к. по сути так оно и есть.
  val contextFactory                  : Context2Factory
  val messagesApi                     : MessagesApi
  val htmlCompressUtil                : HtmlCompressUtil
  // DI-модель языков Langs необходима внутри SioController (и следовательно почти везде):
  val langs                           : Langs
  val csrf                            : Csrf
  // выставляем implicit, т.к. до-DI'шные websocket'ы требуют implicit application in scope.
  // TODO После перевода вёб-сокетов на akka streams, удалить implicit у current.
  override implicit val current       : Application
  val errorHandler                    : ErrorHandler
  override implicit val ec            : ExecutionContext
  override val cache                  : AsyncCacheApi
  override val cacheApiUtil           : CacheApiUtil
  override val _slickConfigProvider   : DatabaseConfigProvider
}


/** Дефолтовая реализация модели common-компонентов. */
@Singleton
final class MCommonDi @Inject() (
                                  override val errorHandler           : ErrorHandler,
                                  override val contextFactory         : Context2Factory,
                                  override val htmlCompressUtil       : HtmlCompressUtil,
                                  override val langs                  : Langs,
                                  override val csrf                   : Csrf,
                                  override val cache                  : AsyncCacheApi,
                                  override val cacheApiUtil           : CacheApiUtil,
                                  override val esScrollPublisherFactory: EsScrollPublisherFactory,
                                  override val _slickConfigProvider   : DatabaseConfigProvider,
                                  override val messagesApi            : MessagesApi,
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
