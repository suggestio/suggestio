package util.billing

import com.google.inject.{Inject, Singleton}
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.bill.tariff.daily.{MDailyTf, MDayClause}
import models.MNode
import models.mproj.ICommonDi
import play.api.data._
import Forms._
import io.suggest.bill.MCurrencies
import io.suggest.model.n2.node.MNodes
import io.suggest.util.logs.MacroLogsImpl
import util.FormUtil.{currencyM, doubleM, esIdM}

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.15 15:02
 * Description: Утиль для сборки маппингов форм редактирования тарифов.
 */
@Singleton
class TfDailyUtil @Inject()(
  bill2Util: Bill2Util,
  mNodes   : MNodes,
  mCommonDi: ICommonDi
)
  extends MacroLogsImpl
{

  import mCommonDi._

  def VERY_DEFAULT_WEEKDAY_CLAUSE = MDayClause("Будни", 1.0)

  private def VERY_DEFAULT_FT = {
    val clause = VERY_DEFAULT_WEEKDAY_CLAUSE
    MDailyTf(
      currency      = MCurrencies.default,
      clauses       = Map(
        clause.name -> clause
      ),
      comissionPc   = Some(1.0)
    )
  }

  /** Маппинг инстанса MDailyTf. */
  def tfDailyClauseM: Mapping[MDayClause] = {
    mapping(
      "name"        -> nonEmptyText(minLength = 3, maxLength = 32),
      "amount"      -> doubleM,
      "calId"       -> optional(esIdM)
    )
    { MDayClause.apply }
    { MDayClause.unapply }
  }

  /** Маппинг карты условий тарифа. */
  def tfDailyClausesM = {
    list( optional(tfDailyClauseM) )
      .transform [List[MDayClause]] (
        _.flatten,
        _.map(Some.apply)
      )
      .transform [Map[String, MDayClause]] (
        { MDayClause.clauses2map1 },
        { MDayClause.clausesMap2list }
      )
  }

  /** Маппинг инстанса MDailyTf. */
  def tfDailyM: Mapping[MDailyTf] = {
    mapping(
      "currencyCode"  -> currencyM,
      "clauses"       -> tfDailyClausesM,
      "comission"     -> optional(doubleM)
    )
    { MDailyTf.apply }
    { MDailyTf.unapply }
  }

  /** Маппинг формы создания/редактирования MDailyTf. */
  def tfDailyForm: Form[MDailyTf] = {
    Form(tfDailyM)
  }


  /** Собрать карту тарифов для узлов. */
  def getNodesTfsMap(nodes: TraversableOnce[MNode]): Future[Map[String, MDailyTf]] = {
    for {
      dailyTfsOpts  <- Future.traverse(nodes) { mnode =>
        for (tf <- forcedNodeTf(mnode)) yield {
          mnode.id.get -> tf
        }
      }
    } yield {
      dailyTfsOpts
        .toIterator
        .toMap
    }
  }

  def tfsMap2calIds(tfsMap: Map[String, MDailyTf]): Set[String] = {
    tfsMap
      .valuesIterator
      .flatMap(_.calIdsIter)
      .toSet
  }

  /**
    * Вернуть тариф узла для размещения на узле.
    * Если тариф на узле отсутствует, то будет использован унаследованный сверху или дефолтовый тариф.
    */
  def forcedNodeTf(mnode: MNode): Future[MDailyTf] = {
    // TODO Подниматься по цепочке узлов, чтобы узнать унаследованный тариф.
    FutureUtil.opt2future( mnode.billing.tariffs.daily ) {
      for {
        cbcaNodeOpt <- mNodesCache.getById( bill2Util.CBCA_NODE_ID )
      } yield {
        cbcaNodeOpt
          .flatMap(_.billing.tariffs.daily)
          .getOrElse {
            LOGGER.debug(s"forceNodeTf(): CBCA ${bill2Util.CBCA_NODE_ID} dailyTF is not defined!")
            VERY_DEFAULT_FT
          }
      }
    }
  }


  /**
   * Апдейт тарифа в узле.
   *
   * @param mnode0 Исходный узел.
   * @param newTf Обновлённый тариф или None.
   * @return Фьючерс с обновлённым узлом внутри.
   */
  def updateNodeTf(mnode0: MNode, newTf: Option[MDailyTf]): Future[MNode] = {
    // Запускаем апдейт узла.
    val fut = mNodes.tryUpdate(mnode0) { mnode =>
      mnode.copy(
        billing = mnode.billing.copy(
          tariffs = mnode.billing.tariffs.copy(
            daily = newTf
          )
        )
      )
    }

    // Логгируем в фоне результаты апдейта.
    lazy val logPrefix = s"updateNodeTf(${mnode0.id.get}):"
    fut.onComplete {
      case _: Success[_] =>
        LOGGER.debug(s"$logPrefix Saved new daily tariff[$newTf] for node")
      case Failure(ex) =>
        LOGGER.error(s"$logPrefix Failed to save tariff $newTf into node", ex)
    }

    fut
  }

}


/** Интерфейс для доступа к DI-полю с экземпляром [[TfDailyUtil]]. */
trait ITfDailyUtilDi {
  /** DI-инстанс утили формы для daily-тарифов. */
  def tfDailyUtil: TfDailyUtil
}
