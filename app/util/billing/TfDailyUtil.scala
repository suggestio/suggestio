package util.billing

import com.google.inject.{Inject, Singleton}
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.bill.tariff.daily.{MDayClause, MDailyTf}
import models.{MNode, CurrencyCodeDflt}
import models.mproj.ICommonDi
import play.api.data._, Forms._
import util.FormUtil.{doubleM, esIdM, currencyCodeM}
import util.PlayMacroLogsImpl

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
  mCommonDi: ICommonDi,
  bill2Util: Bill2Util
)
  extends PlayMacroLogsImpl
{

  import mCommonDi._

  def VERY_DEFAULT = {
    val clause = MDayClause("Будни", 1.0)
    MDailyTf(
      currencyCode  = CurrencyCodeDflt.currencyCode,
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
        _.flatMap(identity(_)),
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
      "currencyCode"  -> currencyCodeM,
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


  def nodeTf(mnode: MNode): Future[MDailyTf] = {
    nodeTf( mnode.billing.tariffs.daily )
  }
  /**
   * Узнать тариф для узла. Если у узла нет тарифа, то будет взят тариф cbca.
   * @param nodeTfOpt Тариф узла, если есть.
   * @return Фьючерс с тарифом.
   */
  def nodeTf(nodeTfOpt: Option[MDailyTf]): Future[MDailyTf] = {
    // Вычисляем текущий реальный тариф узла.
    FutureUtil.opt2future(nodeTfOpt) {
      for (cbcaNodeOpt <- mNodeCache.getById( bill2Util.CBCA_NODE_ID )) yield {
        cbcaNodeOpt
          .flatMap(_.billing.tariffs.daily)
          .getOrElse( VERY_DEFAULT )
      }
    }
  }

  /**
   * Апдейт тарифа в узле.
   * @param mnode0 Исходный узел.
   * @param newTf Обновлённый тариф или None.
   * @return Фьючерс с обновлённым узлом внутри.
   */
  def updateNodeTf(mnode0: MNode, newTf: Option[MDailyTf]): Future[MNode] = {
    // Запускаем апдейт узла.
    val fut = MNode.tryUpdate(mnode0) { mnode =>
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
