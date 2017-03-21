package util.billing

import com.google.inject.{Inject, Singleton}
import io.suggest.bill.tf.daily._
import io.suggest.bill.{Amount_t, MCurrencies, MPrice}
import io.suggest.cal.m.{MCalType, MCalTypes}
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.bill.tariff.daily.{MDayClause, MTfDaily}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.MNodes
import io.suggest.util.logs.MacroLogsImpl
import models.MNode
import models.mcal.MCalendars
import models.mproj.ICommonDi
import play.api.data.Forms._
import play.api.data._
import util.FormUtil.{currencyM, doubleM, esIdM}

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.15 15:02
 * Description: Утиль для сборки маппингов форм редактирования тарифов.
 */
@Singleton
class TfDailyUtil @Inject()(
  bill2Util   : Bill2Util,
  mNodes      : MNodes,
  mCalendars  : MCalendars,
  mCommonDi   : ICommonDi
)
  extends MacroLogsImpl
{

  import mCommonDi._

  def VERY_DEFAULT_WEEKDAY_CLAUSE = MDayClause("Будни", 1.0)

  /** Комиссия тарифа. 1.0 означает, что 100% улетает в suggest.io. */
  def COMISSION_DFLT = 1.0


  private def VERY_DEFAULT_FT = {
    val clause = VERY_DEFAULT_WEEKDAY_CLAUSE
    MTfDaily(
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
  def tfDailyM: Mapping[MTfDaily] = {
    mapping(
      "currencyCode"  -> currencyM,
      "clauses"       -> tfDailyClausesM,
      "comission"     -> optional(doubleM)
    )
    { MTfDaily.apply }
    { MTfDaily.unapply }
  }

  /** Маппинг формы создания/редактирования MDailyTf. */
  def tfDailyForm: Form[MTfDaily] = {
    Form(tfDailyM)
  }


  /** Собрать карту тарифов для узлов. */
  def getNodesTfsMap(nodes: TraversableOnce[MNode]): Future[Map[String, MTfDaily]] = {
    for {
      // TODO Opt запрашивать тарифы по узлам всей пачкой. Не делать кучи параллельных запросов узлов.
      dailyTfsOpts  <- Future.traverse(nodes) { mnode =>
        for (tf <- nodeTf(mnode)) yield {
          mnode.id.get -> tf
        }
      }
    } yield {
      dailyTfsOpts
        .toIterator
        .toMap
    }
  }

  def tfsMap2calIds(tfsMap: Map[String, MTfDaily]): Set[String] = {
    tfsMap
      .valuesIterator
      .flatMap(_.calIdsIter)
      .toSet
  }


  /** Вернуть fallback-тариф. */
  def fallbackTf(): Future[MTfDaily] = {
    val cbcaNodeId = bill2Util.CBCA_NODE_ID
    val cbcaNodeOptFut = mNodesCache.getById( cbcaNodeId )

    for {
      cbcaNodeOpt <- cbcaNodeOptFut
    } yield {
      cbcaNodeOpt
        .flatMap(_.billing.tariffs.daily)
        .getOrElse {
          LOGGER.debug(s"fallbackTf(): CBCA ${bill2Util.CBCA_NODE_ID} dailyTF is not defined!")
          VERY_DEFAULT_FT
        }
    }
  }

  /**
    * Вернуть тариф узла для размещения на узле.
    * Если тариф на узле отсутствует, то будет использован унаследованный сверху или дефолтовый тариф.
    */
  def nodeTf(mnode: MNode): Future[MTfDaily] = {
    FutureUtil.opt2future( mnode.billing.tariffs.daily ) {
      inheritedNodeTf( mnode :: Nil )
    }
  }

  /**
    * Рекурсивный поиск унаследованого тарифа.
    * Иными словами, подниматься по цепочке узлов, чтобы определить унаследованный тариф.
    *
    * @param mnodes Дочерие узлы. Для них будут определены родительские узлы.
    * @param level Шаг погружения в рекурсию для защиты от StackOverflowError.
    *              Если слишком много уровней, то будет IllegalStateException.
    * @return Фьючерс с найденным тарифом.
    */
  def inheritedNodeTf(mnodes: TraversableOnce[MNode], level: Int = 1): Future[MTfDaily] = {
    // Запретить погружаться слишком глубоко.
    if (level > 8)
      throw new IllegalStateException(s"Too much recursion steps: $level")

    // Собрать id узлов-овнеров, в сторону которых ссылаются текущий список узлов.
    val ownNodeIds = mnodes
      .toIterator
      .flatMap { mnode =>
        mnode.edges
          .withPredicateIterIds(MPredicates.OwnedBy)
      }
      .toSet
      .toSeq

    lazy val logPrefix = s"inheritedNodeTfFrom(${ownNodeIds.size})[${System.currentTimeMillis()}]#$level:"

    if (ownNodeIds.isEmpty) {
      LOGGER.trace(s"$logPrefix No dailyTFs inherited. Fallback...")
      fallbackTf()

    } else {
      val ownNodesFut = mNodesCache.multiGet(ownNodeIds)
      LOGGER.trace(s"$logPrefix Candidate owners-nodes: ${ownNodeIds.mkString(", ")}")

      // Получаем узлы сразу, т.к. при любом раскладе все они будут получены сюда.
      ownNodesFut.flatMap { ownNodes =>
        // Поискать первый тариф среди найденных родительских узлов.
        val ownTfDailyOpt = ownNodes.iterator
          .flatMap( _.billing.tariffs.daily )
          .toStream
          .headOption
        LOGGER.trace(s"$logPrefix ownTfDailyOpt => $ownTfDailyOpt")

        // Если найден тариф, то вернуть его. Если нет, то подняться ещё на уровень выше в этих поисках.
        FutureUtil.opt2future(ownTfDailyOpt) {
          // Нет ни одного тарифа на узлах-владельцах.
          LOGGER.trace(s"$logPrefix Nodes with dailyTF not found in nodes. Looking for parent nodes...")
          mNodesCache
            .multiGet(ownNodeIds)
            .flatMap { mnodes2 =>
              inheritedNodeTf(mnodes2, level + 1)
            }
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
  def updateNodeTf(mnode0: MNode, newTf: Option[MTfDaily]): Future[MNode] = {
    import scala.util.{Failure, Success}

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


  /** Приведение режима тарикации, заданного юзером, к значению поля mnode.billing.tariffs.daily.
    *
    * @param tfMode Режим тарификации, задаваемый юзером.
    * @return Фьючерс с опциональным посуточным тарифом.
    */
  def tfMode2tfDaily(tfMode: ITfDailyMode): Future[Option[MTfDaily]] = {
    FutureUtil.optFut2futOpt( tfMode.manualOpt ) { manTf =>
      for (ftf <- fallbackTf()) yield {
        val tf2 = ftf.withClauses(
          ftf.clauses.mapValues { mdc =>
            mdc.withAmount(
              mdc.amount * manTf.amount
            )
          }
        )
        Some(tf2)
      }
    }
  }

  def getTfInfo(mnode: MNode): Future[MTfDailyInfo] = {
    // Узнать фактический тариф на узле
    val tfFut = nodeTf(mnode)

    // Получить календари
    val calsMapFut = tfFut.flatMap { tf =>
      val calIds = tf.clauses.valuesIterator
        .flatMap(_.calId)
        .toSet
      mCalendars.multiGetMap( calIds )
    }

    val clausesInfoFut = for {
      tf        <- tfFut
      calsMap   <- calsMapFut
    } yield {
      tf.clauses
        .valuesIterator
        .map { mClause =>
          val mcalOpt = mClause.calId
            .flatMap( calsMap.get )
          val calType = mcalOpt.fold [MCalType] {
            if (tf.clauses.size == 1)
              MCalTypes.All
            else
              MCalTypes.WeekDay
          } { _.calType }
          calType -> MPrice( mClause.amount, tf.currency )
        }
        .toMap
    }

    // Перевести ручной тариф узла в режим.
    val tfMode = mnode.billing.tariffs.daily
      .iterator
      .flatMap { tf =>
        for {
          clause <- tf.clauses.valuesIterator
          if clause.calId.isEmpty
        } yield {
          clause.amount
        }
      }
      .toStream
      .headOption
      .fold [ITfDailyMode] (InheritTf) { manAmount =>
        ManualTf( manAmount )
      }

    for {
      tf            <- tfFut
      clausesInfo   <- clausesInfoFut
    } yield {
      MTfDailyInfo(
        mode          = tfMode,
        clauses       = clausesInfo,
        comissionPct  = (tf.comissionPc.getOrElse(COMISSION_DFLT) * 100).toInt
      )
    }
  }

  import com.wix.accord.dsl._
  import com.wix.accord._


  private val tfDailyAmountV = {
    val n = TfDailyConst.Amount
    validator[Amount_t] { amount =>
      amount should be >= n.MIN
      amount should be <= n.MAX
    }
  }

  implicit private val tfModeV = validator[ITfDailyMode] { tfdm =>
    tfdm.amountOpt.each.should(tfDailyAmountV)
  }

  /** Валидация режима тарификации, задаваемого юзером. */
  def validateTfDailyMode(tfdm: ITfDailyMode): Either[Set[Violation], ITfDailyMode] = {
    val tfdm2 = tfdm.manualOpt.fold(tfdm) { manTf =>
      val mprice = MPrice( manTf.amount, MCurrencies.default )
      manTf.withAmount(
        mprice.normalizeAmountByExponent.amount
      )
    }
    validate( tfdm2 ) match {
      case Success =>
        Right(tfdm2)
      case Failure(violations) =>
        Left(violations)
    }
  }

}


/** Интерфейс для доступа к DI-полю с экземпляром [[TfDailyUtil]]. */
trait ITfDailyUtilDi {
  /** DI-инстанс утили формы для daily-тарифов. */
  def tfDailyUtil: TfDailyUtil
}
