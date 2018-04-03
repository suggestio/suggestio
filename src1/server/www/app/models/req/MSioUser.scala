package models.req

import javax.inject.{Inject, Singleton}

import com.google.inject.ImplementedBy
import com.google.inject.assistedinject.Assisted
import io.suggest.bill.{MCurrencies, MPrice}
import io.suggest.common.fut.FutureUtil
import io.suggest.ctx.CtxData
import io.suggest.di.ISlickDbConfig
import io.suggest.init.routed.MJsiTg
import io.suggest.mbill2.m.balance.{MBalance, MBalances}
import io.suggest.mbill2.m.contract.{MContract, MContracts}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodesCache}
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import models.usr.MSuperUsers
import org.elasticsearch.client.Client
import play.api.db.slick.DatabaseConfigProvider

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
  def contractIdOptFut: Future[Option[Long]]

  /** Контракт юзера, если есть. */
  def mContractOptFut: Future[Option[MContract]]

  /**
   * Остатки по балансов текущего юзера.
   * Реализуется через lazy val, val или что-то подобное.
   * Часто используется в личном кабинете, поэтому живёт прямо здесь.
    *
    * @return Фьючерс со списком остатков на балансах в различных валютах.
   */
  def mBalancesFut: Future[Seq[MBalance]]

  /** Дополнительные цели js-инициализации по мнению ActionBuilder'а. */
  def jsiTgs: List[MJsiTg]

  /** Частый экземпяр CtxData для нужд ЛК. */
  def lkCtxDataFut: Future[CtxData]

  override def toString: String = s"U(${personIdOpt.getOrElse("")})"

}


/** Пустая инфа по юзеру. Инстанс кешируется в factory. Полезно для анонимусов без js-init-таргетов. */
class MSioUserEmpty extends ISioUser {
  private def _futOptOk[T] = Future.successful(Option.empty[T])

  override def personIdOpt          = None
  override def mContractOptFut      = _futOptOk[MContract]
  override def personNodeOptFut     = _futOptOk[MNode]
  override def isSuper              = false
  override def contractIdOptFut     = _futOptOk[Long]
  override def isAuth               = false
  override def jsiTgs               = Nil
  override def mBalancesFut         = Future.successful(Nil)
  override def personNodeFut: Future[MNode] = {
    Future.failed( new NoSuchElementException("personIdOpt is empty") )
  }

  override def lkCtxDataFut = Future.successful(CtxData.empty)
}


/** Частичная реализация [[ISioUser]] для дальнейших ижектируемых реализаций. */
@ImplementedBy( classOf[MSioUserLazy] )
sealed trait ISioUserT extends ISioUser with MacroLogsDyn {

  // DI-инжектируемые контейнер со статическими моделями.
  protected val msuStatics: MsuStatic
  import msuStatics._


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

  override def contractIdOptFut: Future[Option[Long]] = {
    for (mnodeOpt <- personNodeOptFut) yield {
      mnodeOpt.flatMap(_.billing.contractId)
    }
  }

  override def mContractOptFut: Future[Option[MContract]] = {
    contractIdOptFut.flatMap { contractIdOpt =>
      FutureUtil.optFut2futOpt( contractIdOpt ) { contractId =>
        val action = mContracts.getById(contractId)
        slick.db.run(action)
      }
    }
  }

  override def mBalancesFut: Future[Seq[MBalance]] = {
    // Мысленный эксперимент показал, что кеш здесь практически НЕ нужен. Работаем без кеша, заодно и проблем меньше.
    val fut = contractIdOptFut.flatMap { contractIdOpt =>
      contractIdOpt.fold [Future[Seq[MBalance]]] (Future.successful(Nil)) { contractId =>
        val action = mBalances.findByContractId(contractId)
        slick.db.run(action)
      }
    }
    // Если баланса не найдено, то надо его сочинить в уме. Реальный баланс будет создан во время фактической оплаты.
    for (balances <- fut) yield {
      if (balances.nonEmpty) {
        balances
      } else {
        // Вернуть эфемерный баланс, пригодный для отображения в шаблонах.
        val mb = MBalance(-1L, MPrice(0.0, MCurrencies.default))
        Seq(mb)
      }
    }
  }

  override def lkCtxDataFut: Future[CtxData] = {
    for {
      mBalances <- mBalancesFut
    } yield {
      CtxData(
        mUsrBalances  = mBalances
      )
    }
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
            jsiTgs: List[MJsiTg]): MSioUserLazy
}

/** Контейнер со статическими моделями для инстансов [[MSioUserLazy]]. */
@Singleton
class MsuStatic @Inject()(
                           val mSuperUsers               : MSuperUsers,
                           val mContracts                : MContracts,
                           val mBalances                 : MBalances,
                           // Не следует тут юзать MCommonDi, т.к. тут живёт слишком фундаментальный для проекта компонент.
                           val mNodeCache                : MNodesCache,
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
  @Assisted override val jsiTgs       : List[MJsiTg],
  override val msuStatics             : MsuStatic
)
  extends ISioUserT
{
  // Заворачиваем поля в lazy val для возможности кеширования значений.
  override lazy val personNodeOptFut  = super.personNodeOptFut
  override lazy val contractIdOptFut  = super.contractIdOptFut
  override lazy val mContractOptFut   = super.mContractOptFut
  override lazy val mBalancesFut      = super.mBalancesFut
  override lazy val isSuper           = super.isSuper

  override lazy val lkCtxDataFut      = super.lkCtxDataFut

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
  def apply(personIdOpt: Option[String], jsiTgs: List[MJsiTg] = Nil): ISioUser = {
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
