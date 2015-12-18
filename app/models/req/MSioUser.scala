package models.req

import com.google.inject.{Singleton, Inject}
import com.google.inject.assistedinject.Assisted
import io.suggest.common.fut.FutureUtil
import io.suggest.mbill2.m.balance.{MBalances, MBalance}
import io.suggest.mbill2.m.contract.{MContracts, MContract}
import io.suggest.model.n2.node.MNodeTypes
import models.{MNodeCache, MNode}
import models.event.MEvent
import models.event.search.MEventsSearchArgs
import models.msession.{LoginTimestamp, Keys}
import models.usr.MSuperUsers
import org.elasticsearch.client.Client
import play.api.db.slick.DatabaseConfigProvider
import play.api.mvc.{Session, RequestHeader}
import util.PlayMacroLogsImpl
import util.di.ISlickDbConfig

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 15:19
 * Description: Над-модель с расширенными данными, контекстными к пользовательской сессии.
 *
 * Модель может быть пустой внутри, но инстас всегда существует независимо от данных в сессии,
 * т.е. в Option[] заворачиваеть нет нужды.
 *
 * Модель живёт в контексте реквеста и пришла на смену SioReqMd, Option[PersonWrapper],
 * RichRequestHeader, но ещё и c расширением возможностей.
 *
 * Некоторые поля инстансов модели могут быть асинхронными.
 */
trait ISioUser {

  /** id текущего юзера, если известен. */
  def personIdOpt: Option[String]

  /** Является ли текущий клиент залогиненным юзером? */
  def isAuth: Boolean

  /** Инстанс узла юзера в модели MNode. Не меняется при многократных запросах.
    * В реализациях инстанса может быть как lazy val, так и val, или другие подходящие варианты. */
  def personNodeOptFut: Future[Option[MNode]]

  /** Является ли текущий юзер суперпользователем? */
  def isSuperUser: Boolean

  /** id контракта, записанный к узле юзера. */
  def contractIdOptFut: Future[Option[Long]]

  /** Контракт юзера, если есть. */
  def mContractOptFut: Future[Option[MContract]]

  /**
   * Остатки по балансов текущего юзера.
   * Реализуется через lazy val, val или что-то подобное.
   * Часто используется в личном кабинете, поэтому живёт прямо здесь.
   * @return Фьючерс со списком остатков на балансах в различных валютах.
   */
  def mBalanceOptFut: Future[Seq[MBalance]]

  /** Кол-во непрочитанных событий. */
  def unSeenEventsCountFut: Future[Option[Int]]

}


/** Частичная реализация [[ISioUser]] для дальнейших ижектируемых реализаций. */
trait ISioUserT extends ISioUser {

  // DI-инжектируемые контейнер со статическими моделями.
  protected val msuStatics: MsuStatic
  import msuStatics._


  override def isAuth = personIdOpt.isDefined

  override def isSuperUser: Boolean = {
    personIdOpt.exists { personId =>
      mSuperUsers.isSuperuserId(personId)
    }
  }

  override def personNodeOptFut: Future[Option[MNode]] = {
    FutureUtil.optFut2futOpt( personIdOpt ) { personId =>
      mNodeCache.getByIdType(personId, MNodeTypes.Person)
    }
  }

  override def contractIdOptFut: Future[Option[Long]] = {
    for (mnodeOpt <- personNodeOptFut) yield {
      mnodeOpt.flatMap(_.billing.contractId)
    }
  }

  override def mContractOptFut: Future[Option[MContract]] = {
    contractIdOptFut.flatMap { contractIdOpt =>
      FutureUtil.optFut2futOpt( contractIdOpt ) { contractId =>
        val action = mContracts.getById(contractId)
        dbConfig.db.run(action)
      }
    }
  }

  override def unSeenEventsCountFut: Future[Option[Int]] = {
    FutureUtil.optFut2futOpt(personIdOpt) { personId =>
      // TODO Нужно портировать события на MNode и тут искать их.
      val search = new MEventsSearchArgs(ownerId = Some(personId))
      for (cnt <- MEvent.dynCount(search)) yield {
        Some(cnt.toInt)
      }
    }
  }

  override def mBalanceOptFut: Future[Seq[MBalance]] = {
    // Мысленный эксперимент показал, что кеш здесь практически НЕ нужен. Работаем без кеша, заодно и проблем меньше.
    contractIdOptFut.flatMap { contractIdOpt =>
      contractIdOpt.fold [Future[Seq[MBalance]]] (Future.successful(Nil)) { contractId =>
        val action = mBalances.findByContractId(contractId)
        dbConfig.db.run(action)
      }
    }
  }

}


/** DI-factory для быстрой сборки экземпляров [[MSioUserLazy]]. */
trait MSioUserLazyFactory {
  def create(personIdOpt: Option[String]): MSioUserLazy
}

/** Контейнер со статическими моделями для инстансов [[MSioUserLazy]]. */
@Singleton
class MsuStatic @Inject()(
  val mSuperUsers               : MSuperUsers,
  val mContracts                : MContracts,
  val mBalances                 : MBalances,
  // Не следует тут юзать MCommonDi, т.к. тут живёт слишком фундаментальный для проекта компонент.
  val mNodeCache                : MNodeCache,
  override val dbConfigProvider : DatabaseConfigProvider,
  implicit val ec               : ExecutionContext,
  implicit val esClient         : Client
)
  extends ISlickDbConfig



/** Реализация модели [[ISioUser]], где все future-поля реализованы как lazy val. */
case class MSioUserLazy @Inject() (
  @Assisted override val personIdOpt  : Option[String],
  override val msuStatics             : MsuStatic
)
  extends ISioUserT
{
  // Заворачиваем поля в lazy val для возможности кеширования значений.
  override lazy val personNodeOptFut  = super.personNodeOptFut
  override lazy val contractIdOptFut  = super.contractIdOptFut
  override lazy val mContractOptFut   = super.mContractOptFut
  override lazy val mBalanceOptFut    = super.mBalanceOptFut
  override lazy val isSuperUser       = super.isSuperUser
}


/**
 * Статическая сторона модели пользователя sio, здесь живут высокоуровневые методы.
 * Именно этот класс необходимо юзать для всех задач.
 */
@Singleton
class MSioUsers @Inject() (
  factory: MSioUserLazyFactory
)
  extends PlayMacroLogsImpl
{

  /**
   * Извечь из реквеста данные о залогиненности юзера. Функция враппер над getPersonWrapperFromSession().
   * @param request Реквест.
   * @return Option[PersonWrapper].
   */
  def fromRequest(implicit request: RequestHeader): ISioUser = {
    fromSession(request.session)
  }

  /**
   * Извечь данные о залогиненности юзера из сессии.
   * @param session Сессия.
   * @return Option[PersonWrapper].
   */
  def fromSession(session: Session): ISioUser = {
    val personIdOpt = session.get(Keys.PersonId.name)
      // Если выставлен timestamp, то проверить валидность защищенного session ttl.
      .filter { personId =>
        val tstampOpt = LoginTimestamp.fromSession(session)
        val result = tstampOpt
          .exists { _.isTimestampValid() }
        if (!result)
          LOGGER.trace(s"getFromSession(): Session expired for user $personId. tstampRaw = $tstampOpt")
        result
      }
    factory.create(personIdOpt)
  }

}
