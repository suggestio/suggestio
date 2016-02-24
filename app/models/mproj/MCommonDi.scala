package models.mproj

import akka.actor.ActorSystem
import com.google.inject.{Singleton, ImplementedBy, Inject}
import controllers.ErrorHandler
import io.suggest.di.{ICacheApiUtil, IActorSystem, IEsClient, IExecutionContext}
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.playx.{CacheApiUtil, ICurrentConf}
import models.mctx.Context2Factory
import models.req.MSioUsers
import models.MNodeCache
import org.elasticsearch.client.Client
import play.api.Application
import play.api.cache.CacheApi
import play.api.db.Database
import play.api.db.slick.DatabaseConfigProvider
import play.api.i18n.MessagesApi
import util.di._
import util.secure.SessionUtil
import util.xplay.ICacheApi

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
  with ICurrentConf
  with IExecutionContext
  with IEsClient
  with ISioNotifier
  with IActorSystem
  with ICacheApi
  with ICacheApiUtil
  with IDb
  with ISlickDbConfig
  with INodeCache
{
  // Для возможно оптимизации, всё объявляем как val, т.к. по сути так оно и есть.
  val mSioUsers                   : MSioUsers
  val sessionUtil                 : SessionUtil
  val contextFactory              : Context2Factory
  val messagesApi                 : MessagesApi
  override implicit val esClient  : Client
  override val errorHandler       : ErrorHandler
  override implicit val ec        : ExecutionContext
  override val actorSystem        : ActorSystem
  override val cache              : CacheApi
  override val cacheApiUtil       : CacheApiUtil
  override val db                 : Database
  override val mNodeCache         : MNodeCache
  override val _dbConfigProvider   : DatabaseConfigProvider
  override implicit val current   : Application
}


/** Дефолтовая реализация модели common-компонентов. */
@Singleton
class MCommonDi @Inject() (
  override val errorHandler       : ErrorHandler,
  override val contextFactory     : Context2Factory,
  override val messagesApi        : MessagesApi,
  override val actorSystem        : ActorSystem,
  override val cache              : CacheApi,
  override val cacheApiUtil       : CacheApiUtil,
  override val mNodeCache         : MNodeCache,
  override val sessionUtil        : SessionUtil,
  override val mSioUsers          : MSioUsers,
  override val db                 : Database, // Anorm, спилить потом.
  override val _dbConfigProvider   : DatabaseConfigProvider,
  override implicit val current   : Application,
  override implicit val ec        : ExecutionContext,
  override implicit val esClient  : Client,
  override implicit val sn        : SioNotifierStaticClientI
)
  extends ICommonDi


/** Интерфейс к DI-полю со значением [[MCommonDi]] */
trait IMCommonDi {
  val mCommonDi: ICommonDi
}
