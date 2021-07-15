package models.req

import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import com.google.inject.assistedinject.Assisted
import io.suggest.bill.{MCurrencies, MPrice}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.ctx.CtxData
import io.suggest.di.ISlickDbConfig
import io.suggest.es.model.EsModel
import io.suggest.mbill2.m.balance.{MBalance, MBalances}
import io.suggest.mbill2.m.contract.{MContract, MContracts}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.session.MSessionKeys
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import models.usr.MSuperUsers
import play.api.db.slick.DatabaseConfigProvider
import play.api.inject.Injector
import play.api.mvc.RequestHeader
import util.adn.NodesUtil
import util.billing.Bill2Util
import util.mdr.MdrUtil

import scala.concurrent.{ExecutionContext, Future}
import scala.util.Try

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 17.12.15 15:19
 * Description: Над-модель с расширенными данными, контекстными к пользовательской сессии.
 * Это НЕ модель пользователя!
 *
 * Модель может быть пустой внутри, но инстас всегда существует независимо от данных в сессии,
 * т.е. в Option[] заворачиваеть нет нужды.
 *
 * Модель живёт в контексте реквеста и пришла на смену SioReqMd, Option[PersonWrapper],
 * RichRequestHeader, но ещё и c расширением возможностей.
 *
 * При добавлении новых полей в модель, нужно добавлять их в [[ISioUser]] и в apply()-конструкторы ниже.
 */
sealed trait ISioUser {

  /** Optional request header instance, if any. */
  def requestHeaderOpt: Option[RequestHeader]

  /** id текущего юзера, если известен. */
  def personIdOpt: Option[String]

  /** Является ли текущий клиент залогиненным юзером? */
  def isAuth: Boolean

  def isAnon = !isAuth

  /** Инстанс узла юзера в модели MNode. Не меняется при многократных запросах.
    * В реализациях инстанса может быть как lazy val, так и val, или другие подходящие варианты. */
  def personNodeOptFut: Future[Option[MNode]]

  /** @return MNode || NoSuchElementException. */
  def personNodeFut: Future[MNode]

  /** Является ли текущий юзер суперпользователем? */
  def personIdIsSuperUser: Boolean

  /** id контракта, записанный к узле юзера. */
  def contractIdOptFut: Future[Option[Gid_t]]

  /** Контракт юзера, если есть. */
  def mContractOptFut: Future[Option[MContract]]

  /**
   * Остатки по балансов текущего юзера.
   * Реализуется через lazy val, val или что-то подобное.
   * Часто используется в личном кабинете, поэтому живёт прямо здесь.
    *
    * @return Фьючерс со списком остатков на балансах в различных валютах.
   */
  def balancesFut: Future[Seq[MBalance]]

  /** Частый экземпяр CtxData для нужд ЛК. */
  def lkCtxDataFut: Future[CtxData]

  /** Кол-во элементов для модерации. */
  def lkMdrCountOptFut: Future[Option[Int]]

  /** Кол-во элементов в корзине. 0 возвращает None. */
  def cartItemsCountOptFut: Future[Option[Int]]

  override def toString: String = s"U(${personIdOpt.getOrElse("")})"

}
object ISioUser {
  implicit final class SioUserExt( private val user: ISioUser ) extends AnyVal {

    /** Is user have session NoSu flag inside session? */
    def isForceNoSu: Boolean = {
      user
        .requestHeaderOpt
        .exists {
          _.session
            .get( MSessionKeys.NoSu.value )
            .exists { noSuValue =>
              Try( noSuValue.toBoolean ) getOrElse false
            }
        }
    }


    /** Is current user have superuser capabilities? */
    def isSuper: Boolean = {
      user.personIdIsSuperUser && !isForceNoSu
    }

  }
}


/** Empty user context. Used as fast shared singleton implementation of [[ISioUser]] for anonymous users. */
final class MSioUserEmpty extends ISioUser {

  override def requestHeaderOpt = None

  private def _futOptOk[T] = Future.successful(Option.empty[T])

  override def personIdOpt          = None
  override def mContractOptFut      = _futOptOk[MContract]
  override def personNodeOptFut     = _futOptOk[MNode]
  override def personIdIsSuperUser  = false
  override def contractIdOptFut     = _futOptOk[Gid_t]
  override def isAuth               = false
  override def balancesFut         = Future.successful(Nil)
  override def personNodeFut: Future[MNode] = {
    Future.failed( new NoSuchElementException("personIdOpt is empty") )
  }
  override def lkMdrCountOptFut = Future.successful(None)
  override def cartItemsCountOptFut = Future.successful(None)

  override def lkCtxDataFut = Future.successful(CtxData.empty)
}


/** Partial implementation of [[ISioUser]]. */
@ImplementedBy( classOf[MSioUserLazy] )
sealed trait ISioUserT extends ISioUser with MacroLogsDyn {

  // Injected container with static models.
  protected def msuStatics: MsuStatic


  override def isAuth = personIdOpt.isDefined

  override def personIdIsSuperUser: Boolean = {
    personIdOpt.exists { personId =>
      msuStatics.mSuperUsers.isSuperuserId(personId)
    }
  }

  override def personNodeOptFut: Future[Option[MNode]] = {
    FutureUtil.optFut2futOpt( personIdOpt ) { personId =>
      val s = msuStatics
      import s.esModel.api._
      implicit val ec = s.ec

      val optFut0 = s.mNodes
        .getByIdCache(personId)
        .withNodeType(MNodeTypes.Person)
      for (resOpt <- optFut0 if resOpt.isEmpty) {
        // should never happen
        LOGGER.warn(s"personNodeOptFut(): Person[$personId] doesn't exist, but it should!")
      }
      optFut0
    }
  }

  override def personNodeFut: Future[MNode] = {
    val s = msuStatics
    implicit val ec = s.ec
    personNodeOptFut
      .map(_.get)
  }

  override def contractIdOptFut: Future[Option[Gid_t]] = {
    val s = msuStatics
    implicit val ec = s.ec
    for (mnodeOpt <- personNodeOptFut) yield
      mnodeOpt.flatMap(_.billing.contractId)
  }

  override def mContractOptFut: Future[Option[MContract]] = {
    val s = msuStatics
    implicit val ec = s.ec

    contractIdOptFut.flatMap { contractIdOpt =>
      FutureUtil.optFut2futOpt( contractIdOpt ) { contractId =>
        val action = s.mContracts.getById(contractId)
        s.slick.db.run(action)
      }
    }
  }

  override def balancesFut: Future[Seq[MBalance]] = {
    val s = msuStatics
    implicit val ec = s.ec

    // Мысленный эксперимент показал, что кеш здесь практически НЕ нужен. Работаем без кеша, заодно и проблем меньше.
    // Если баланса не найдено, то надо его сочинить в уме. Реальный баланс будет создан во время фактической оплаты.
    for {
      contractIdOpt <- contractIdOptFut
      balances <- contractIdOpt.fold [Future[Seq[MBalance]]] (Future.successful(Nil)) { contractId =>
        val action = s.mBalances.findByContractId(contractId)
        s.slick.db.run(action)
      }
    } yield {
      if (balances.nonEmpty) {
        balances
      } else {
        // Вернуть эфемерный баланс, пригодный для отображения в шаблонах.
        val mb = MBalance(-1L, MPrice(0L, MCurrencies.default))
        mb :: Nil
      }
    }
  }

  override def lkCtxDataFut: Future[CtxData] = {
    val _balancesFut = balancesFut
    val _mdrCountOptFut = lkMdrCountOptFut
    val _cartItemsCountOptFut = cartItemsCountOptFut

    val s = msuStatics
    implicit val ec = s.ec

    for {
      balances            <- _balancesFut
      mdrCountOpt         <- _mdrCountOptFut
      cartItemsCountOpt   <- _cartItemsCountOptFut
    } yield {
      CtxData(
        mUsrBalances      = balances,
        mdrNodesCount     = mdrCountOpt,
        cartItemsCount    = cartItemsCountOpt,
      )
    }
  }

  override def lkMdrCountOptFut: Future[Option[Int]] = {
    val s = msuStatics
    implicit val ec = s.ec

    // Нужно собрать всех ресиверов, которыми владеет текущий юзер.
    FutureUtil.optFut2futOpt( personIdOpt ) { personId =>
      val futOrNsee = for {
        // Собрать все id дочерних узлов текущего юзера.
        childIds <- s.nodesUtil.collectChildIds(
          parentNodeIds = Set.empty + personId,
          maxLevels     = s.mdrUtil.maxLevelsDeepFor(isPerson = true)
        )
        if childIds.nonEmpty
        // Запустить подсчёт кол-ва узлов по биллингу:
        paidNodesSql = s.mdrUtil.findPaidNodeIds4MdrQ( rcvrIds = childIds )
        paidMdrNodesCount <- s.slick.db.run {
          import s.slick.profile.api._
          paidNodesSql.size.result
        }
      } yield {
        OptionUtil.maybe( paidMdrNodesCount > 0 )( paidMdrNodesCount )
      }

      futOrNsee.recover { case _: Throwable => None }
    }
  }

  override def cartItemsCountOptFut: Future[Option[Int]] = {
    val s = msuStatics
    implicit val ec = s.ec

    val futOrEx = for {
      // Узнать id контракта юзера.
      contractIdOpt <- contractIdOptFut
      contractId = contractIdOpt.get

      // Поиск id ордера-корзины
      itemsCount <- s.slick.db.run {
        for {
          cartOrderIdOpt  <- s.bill2Util.getCartOrderId( contractId )
          cartOrderId = cartOrderIdOpt.get
          // Посчитать item'ы в ордере:
          itemsCount      <- s.mItems.countByOrderId(cartOrderId)
        } yield {
          itemsCount
        }
      }
    } yield {
      OptionUtil.maybe(itemsCount > 0)(itemsCount)
    }
    // Исключение - это нормально здесь. Гасим NSEE и заодно остальные исключения.
    futOrEx
      .recover { case _: Throwable => None }
  }

}


/** Guice factory для быстрой сборки экземпляров [[MSioUserLazy]]. */
trait MSioUserLazyFactory {
  /**
    * factory-метод для сборки инстансов [[MSioUserLazy]].
    *
    * @param personIdOpt id текущего юзера, если есть.
    */
  def apply( personIdOpt: Option[String], requestHeader: RequestHeader ): MSioUserLazy
}

/** Контейнер со статическими моделями для инстансов [[MSioUserLazy]]. */
final class MsuStatic @Inject()(
                                 injector: Injector,
                               )
  extends ISlickDbConfig
{
  lazy val mSuperUsers = injector.instanceOf[MSuperUsers]
  lazy val mContracts = injector.instanceOf[MContracts]
  lazy val mBalances = injector.instanceOf[MBalances]
  // Не следует тут юзать MCommonDi, т.к. тут живёт слишком фундаментальный для проекта компонент.
  lazy val mNodes = injector.instanceOf[MNodes]
  lazy val mdrUtil = injector.instanceOf[MdrUtil]
  lazy val nodesUtil = injector.instanceOf[NodesUtil]
  lazy val bill2Util = injector.instanceOf[Bill2Util]
  lazy val mItems = injector.instanceOf[MItems]
  lazy val esModel = injector.instanceOf[EsModel]
  override lazy val _slickConfigProvider = injector.instanceOf[DatabaseConfigProvider]
  implicit lazy val ec = injector.instanceOf[ExecutionContext]
}


/**
  * Реализация модели [[ISioUser]], где все future-поля реализованы как lazy val.
  *
  * @param personIdOpt id текущего юзера.
  */
final case class MSioUserLazy @Inject() (
                                          @Assisted override val personIdOpt  : Option[String],
                                          @Assisted requestHeader: RequestHeader,
                                          injector: Injector,
                                        )
  extends ISioUserT
{

  override def requestHeaderOpt = Some( requestHeader )

  /** Зависимости, необходимые для успешной работы ленивых полей инстанса. */
  override lazy val msuStatics = injector.instanceOf[MsuStatic]

  // Заворачиваем поля в lazy val для возможности кеширования значений.
  override lazy val personNodeOptFut  = super.personNodeOptFut
  override lazy val contractIdOptFut  = super.contractIdOptFut
  override lazy val mContractOptFut   = super.mContractOptFut
  override lazy val balancesFut       = super.balancesFut
  override lazy val personIdIsSuperUser = super.personIdIsSuperUser

  override lazy val lkCtxDataFut      = super.lkCtxDataFut
  override lazy val lkMdrCountOptFut  = super.lkMdrCountOptFut
  override lazy val cartItemsCountOptFut = super.cartItemsCountOptFut

  override def toString =
    s"U(${personIdOpt.getOrElse("")})"

}


/**
 * Статическая сторона модели пользователя sio, здесь живут высокоуровневые методы.
 * Именно этот класс необходимо юзать для всех задач.
 * Этот класс по сути враппер над [[MSioUserLazyFactory]], имеющий дефолтовые значения некоторых аргументов.
 */
@Singleton
class MSioUsers @Inject() (
  factory: MSioUserLazyFactory
)
  extends MacroLogsImpl
{

  val empty: ISioUser = new MSioUserEmpty

  /**
    * factory-метод с дефолтовыми значениями некоторых аргументов.
    *
    * @param personIdOpt Опциональный id юзера. Экстрактиться из сессии с помощью SessionUtil.
    * @return Инстанс какой-то реализации [[ISioUser]].
    */
  def apply(personIdOpt: Option[String], requestHeader: RequestHeader): ISioUser = {
    // Частые анонимные запросы можно огулять одним общим инстансом ISioUser.
    personIdOpt.fold( empty ) { _ =>
      factory( personIdOpt, requestHeader )
    }
  }

}
