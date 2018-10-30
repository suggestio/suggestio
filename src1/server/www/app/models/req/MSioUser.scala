package models.req

import javax.inject.{Inject, Singleton}
import com.google.inject.ImplementedBy
import com.google.inject.assistedinject.Assisted
import io.suggest.bill.{MCurrencies, MPrice}
import io.suggest.common.empty.OptionUtil
import io.suggest.common.fut.FutureUtil
import io.suggest.ctx.CtxData
import io.suggest.di.ISlickDbConfig
import io.suggest.init.routed.MJsInitTarget
import io.suggest.mbill2.m.balance.{MBalance, MBalances}
import io.suggest.mbill2.m.contract.{MContract, MContracts}
import io.suggest.mbill2.m.gid.Gid_t
import io.suggest.mbill2.m.item.MItems
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodesCache}
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import models.usr.MSuperUsers
import org.elasticsearch.client.Client
import play.api.db.slick.DatabaseConfigProvider
import util.adn.NodesUtil
import util.billing.Bill2Util
import util.mdr.MdrUtil

import scala.concurrent.{ExecutionContext, Future}

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
  def isSuper: Boolean

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

  /** Дополнительные цели js-инициализации по мнению ActionBuilder'а. */
  def jsiTgs: List[MJsInitTarget]

  /** Частый экземпяр CtxData для нужд ЛК. */
  def lkCtxDataFut: Future[CtxData]

  /** Кол-во элементов для модерации. */
  def lkMdrCountOptFut: Future[Option[Int]]

  /** Кол-во элементов в корзине. */
  def cartItemsCountOptFut: Future[Option[Int]]

  override def toString: String = s"U(${personIdOpt.getOrElse("")})"

}


/** Пустая инфа по юзеру. Инстанс кешируется в factory. Полезно для анонимусов без js-init-таргетов. */
class MSioUserEmpty extends ISioUser {
  private def _futOptOk[T] = Future.successful(Option.empty[T])

  override def personIdOpt          = None
  override def mContractOptFut      = _futOptOk[MContract]
  override def personNodeOptFut     = _futOptOk[MNode]
  override def isSuper              = false
  override def contractIdOptFut     = _futOptOk[Gid_t]
  override def isAuth               = false
  override def jsiTgs               = Nil
  override def balancesFut         = Future.successful(Nil)
  override def personNodeFut: Future[MNode] = {
    Future.failed( new NoSuchElementException("personIdOpt is empty") )
  }
  override def lkMdrCountOptFut = Future.successful(None)
  override def cartItemsCountOptFut = Future.successful(None)

  override def lkCtxDataFut = Future.successful(CtxData.empty)
}


/** Частичная реализация [[ISioUser]] для дальнейших ижектируемых реализаций. */
@ImplementedBy( classOf[MSioUserLazy] )
sealed trait ISioUserT extends ISioUser with MacroLogsDyn {

  // DI-инжектируемые контейнер со статическими моделями.
  protected val msuStatics: MsuStatic
  import msuStatics._
  import slick.profile.api._


  override def isAuth = personIdOpt.isDefined

  override def isSuper: Boolean = {
    personIdOpt.exists { personId =>
      mSuperUsers.isSuperuserId(personId)
    }
  }

  override def personNodeOptFut: Future[Option[MNode]] = {
    FutureUtil.optFut2futOpt( personIdOpt ) { personId =>
      val optFut0 = mNodeCache.getByIdType(personId, MNodeTypes.Person)
      for (resOpt <- optFut0 if resOpt.isEmpty) {
        // should never happen
        LOGGER.warn(s"personNodeOptFut(): Person[$personId] doesn't exist, but it should!")
      }
      optFut0
    }
  }

  override def personNodeFut: Future[MNode] = {
    personNodeOptFut
      .map(_.get)
  }

  override def contractIdOptFut: Future[Option[Gid_t]] = {
    for (mnodeOpt <- personNodeOptFut) yield
      mnodeOpt.flatMap(_.billing.contractId)
  }

  override def mContractOptFut: Future[Option[MContract]] = {
    contractIdOptFut.flatMap { contractIdOpt =>
      FutureUtil.optFut2futOpt( contractIdOpt ) { contractId =>
        val action = mContracts.getById(contractId)
        slick.db.run(action)
      }
    }
  }

  override def balancesFut: Future[Seq[MBalance]] = {
    // Мысленный эксперимент показал, что кеш здесь практически НЕ нужен. Работаем без кеша, заодно и проблем меньше.
    // Если баланса не найдено, то надо его сочинить в уме. Реальный баланс будет создан во время фактической оплаты.
    for {
      contractIdOpt <- contractIdOptFut
      balances <- contractIdOpt.fold [Future[Seq[MBalance]]] (Future.successful(Nil)) { contractId =>
        val action = mBalances.findByContractId(contractId)
        slick.db.run(action)
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
    // Нужно собрать всех ресиверов, которыми владеет текущий юзер.
    FutureUtil.optFut2futOpt( personIdOpt ) { personId =>
      val futOrNsee = for {
        // Собрать все id дочерних узлов текущего юзера.
        childIds <- nodesUtil.collectChildIds(
          parentNodeIds = Set(personId),
          maxLevels     = mdrUtil.maxLevelsDeepFor(isPerson = true)
        )
        if childIds.nonEmpty
        // Запустить подсчёт кол-ва узлов по биллингу:
        paidNodesSql = mdrUtil.findPaidNodeIds4MdrQ( rcvrIds = childIds )
        paidMdrNodesCount <- slick.db.run {
          paidNodesSql.size.result
        }
      } yield {
        OptionUtil.maybe( paidMdrNodesCount > 0 )( paidMdrNodesCount )
      }

      futOrNsee.recover { case _: Throwable => None }
    }
  }

  override def cartItemsCountOptFut: Future[Option[Int]] = {
    val futOrEx = for {
      // Узнать id контракта юзера.
      contractIdOpt <- contractIdOptFut
      contractId = contractIdOpt.get

      // Поиск id ордера-корзины
      itemsCount <- slick.db.run {
        for {
          cartOrderIdOpt  <- bill2Util.getCartOrderId( contractId )
          cartOrderId      = cartOrderIdOpt.get
          // Посчитать item'ы в ордере:
          itemsCount      <- mItems.countByOrderId(cartOrderId)
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
    * @param jsiTgs js init targets, выставленные ActionBuilder'ом, если есть.
    */
  def apply(personIdOpt: Option[String],
            jsiTgs: List[MJsInitTarget]): MSioUserLazy
}

/** Контейнер со статическими моделями для инстансов [[MSioUserLazy]]. */
@Singleton
class MsuStatic @Inject()(
                           val mSuperUsers               : MSuperUsers,
                           val mContracts                : MContracts,
                           val mBalances                 : MBalances,
                           // Не следует тут юзать MCommonDi, т.к. тут живёт слишком фундаментальный для проекта компонент.
                           val mNodeCache                : MNodesCache,
                           val mdrUtil                   : MdrUtil,
                           val nodesUtil                 : NodesUtil,
                           val bill2Util                 : Bill2Util,
                           val mItems                    : MItems,
                           override val _slickConfigProvider : DatabaseConfigProvider,
                           implicit val ec               : ExecutionContext,
                           implicit val esClient         : Client
)
  extends ISlickDbConfig


/**
  * Реализация модели [[ISioUser]], где все future-поля реализованы как lazy val.
  *
  * @param personIdOpt id текущего юзера.
  * @param jsiTgs Список целей js-инициализации.
  * @param msuStatics Статические модели, необходимые для успешной работы ленивых полей инстанса.
  */
case class MSioUserLazy @Inject() (
                                    @Assisted override val personIdOpt  : Option[String],
                                    @Assisted override val jsiTgs       : List[MJsInitTarget],
                                    override val msuStatics             : MsuStatic
)
  extends ISioUserT
{
  // Заворачиваем поля в lazy val для возможности кеширования значений.
  override lazy val personNodeOptFut  = super.personNodeOptFut
  override lazy val contractIdOptFut  = super.contractIdOptFut
  override lazy val mContractOptFut   = super.mContractOptFut
  override lazy val balancesFut       = super.balancesFut
  override lazy val isSuper           = super.isSuper

  override lazy val lkCtxDataFut      = super.lkCtxDataFut
  override lazy val lkMdrCountOptFut  = super.lkMdrCountOptFut
  override lazy val cartItemsCountOptFut = super.cartItemsCountOptFut

  override def toString: String = {
    s"U(${personIdOpt.getOrElse("")}${jsiTgs.mkString(";[", ",", "]")})"
  }
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

  val empty = new MSioUserEmpty

  /**
    * factory-метод с дефолтовыми значениями некоторых аргументов.
    *
    * @param personIdOpt Опциональный id юзера. Экстрактиться из сессии с помощью SessionUtil.
    * @param jsiTgs Список целей js-инициализации [Nil].
    * @return Инстанс какой-то реализации [[ISioUser]].
    */
  def apply(personIdOpt: Option[String], jsiTgs: List[MJsInitTarget] = Nil): ISioUser = {
    // Частые анонимные запросы можно огулять одним общим инстансом ISioUser.
    if (personIdOpt.isEmpty && jsiTgs.isEmpty) {
      empty
    } else {
      factory(
        personIdOpt = personIdOpt,
        jsiTgs      = jsiTgs
      )
    }
  }

}
