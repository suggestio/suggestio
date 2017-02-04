package models.mproj

import akka.stream.Materializer
import com.google.inject.{ImplementedBy, Inject, Singleton}
import controllers.{ErrorHandler, IErrorHandler}
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.playx.CacheApiUtil
import models.mctx.Context2Factory
import models.req.MSioUsers
import org.elasticsearch.client.Client
import play.api.Application
import play.api.cache.CacheApi
import play.api.db.slick.DatabaseConfigProvider
import play.api.i18n.{Langs, MessagesApi}
import play.filters.csrf.{CSRFAddToken, CSRFCheck}
import com.sksamuel.elastic4s.ElasticClient
import io.suggest.di.ICacheApi
import io.suggest.es.model.IEsModelDiVal
import io.suggest.model.n2.node.{INodeCache, MNodesCache}
import io.suggest.www.util.di.ISlickDbConfig
import util.HtmlCompressUtil
import util.secure.SessionUtil

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
  extends IErrorHandler
  with IEsModelDiVal
  with ICacheApi
  with ISlickDbConfig
  with INodeCache
{
  // Для возможно оптимизации, всё объявляем как val, т.к. по сути так оно и есть.
  val mSioUsers                       : MSioUsers
  val sessionUtil                     : SessionUtil
  val contextFactory                  : Context2Factory
  val messagesApi                     : MessagesApi
  val htmlCompressUtil                : HtmlCompressUtil
  // DI-модель языков Langs необходима внутри SioController (и следовательно почти везде):
  val langs                           : Langs
  // play-2.5: Это нужно инжектить иначе deprecation warning.
  val csrfAddToken                    : CSRFAddToken
  val csrfCheck                       : CSRFCheck
  // выставляем implicit, т.к. до-DI'шные websocket'ы требуют implicit application in scope.
  // TODO После перевода вёб-сокетов на akka streams, удалить implicit у current.
  override implicit val current       : Application
  override implicit val esClient      : Client
  override val errorHandler           : ErrorHandler
  override implicit val ec            : ExecutionContext
  override val cache                  : CacheApi
  override val cacheApiUtil           : CacheApiUtil
  override val _slickConfigProvider   : DatabaseConfigProvider
}


/** Дефолтовая реализация модели common-компонентов. */
@Singleton
final class MCommonDi @Inject() (
                                  override val errorHandler       : ErrorHandler,
                                  override val contextFactory     : Context2Factory,
                                  override val messagesApi        : MessagesApi,
                                  override val htmlCompressUtil   : HtmlCompressUtil,
                                  override val langs              : Langs,
                                  override val csrfAddToken       : CSRFAddToken,
                                  override val csrfCheck          : CSRFCheck,
                                  //override val actorSystem        : ActorSystem,
                                  override val cache              : CacheApi,
                                  override val cacheApiUtil       : CacheApiUtil,
                                  override val mNodesCache        : MNodesCache,
                                  override val sessionUtil        : SessionUtil,
                                  override val mSioUsers          : MSioUsers,
                                  override val es4sClient         : ElasticClient,
                                  override val _slickConfigProvider   : DatabaseConfigProvider,
                                  override implicit val current   : Application,
                                  override implicit val mat       : Materializer,
                                  override implicit val ec        : ExecutionContext,
                                  override implicit val esClient  : Client,
                                  override implicit val sn        : SioNotifierStaticClientI
)
  extends ICommonDi


/** Интерфейс к DI-полю со значением [[MCommonDi]] */
trait IMCommonDi {
  val mCommonDi: ICommonDi
}
