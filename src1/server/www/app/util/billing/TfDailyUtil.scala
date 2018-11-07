package util.billing

import java.util.concurrent.atomic.AtomicInteger

import javax.inject.{Inject, Singleton}
import io.suggest.bill.tf.daily._
import io.suggest.bill.{Amount_t, MCurrencies, MPrice}
import io.suggest.cal.m.{MCalType, MCalTypes}
import io.suggest.common.fut.FutureUtil
import io.suggest.model.n2.bill.tariff.daily.{MDayClause, MTfDaily}
import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.node.{MNode, MNodes}
import io.suggest.scalaz.ScalazUtil
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import models.mcal.MCalendars
import models.mctx.Context
import models.mproj.ICommonDi
import play.api.data.Forms._
import play.api.data._
import util.FormUtil.{currencyM, doubleM, esIdM}
import util.TplDataFormatUtil
import MPrice.HellImplicits.AmountMonoid
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.JMXBase
import scalaz._
import scalaz.syntax.apply._
import scalaz.std.option._

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.12.15 15:02
 * Description: Утиль для сборки маппингов форм редактирования тарифов.
 */
@Singleton
class TfDailyUtil @Inject()(
                             bill2Conf   : Bill2Conf,
                             mNodes      : MNodes,
                             mCalendars  : MCalendars,
                             mCommonDi   : ICommonDi
                           )
  extends MacroLogsImpl
{

  import mCommonDi._

  def VERY_DEFAULT_WEEKDAY_CLAUSE = MDayClause("Будни", MCurrencies.default.centsInUnit)

  /** Комиссия тарифа. 1.0 означает, что 100% улетает в suggest.io. */
  def COMISSION_FULL = 1.0
  def COMISSION_DFLT = COMISSION_FULL


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


  // 2018-10-11 Тут mappings-велосипед, в связи с несовременной формой на фоне необходимости конвертить значения
  // в Amount_t и обратно на основе валюты, которая задаётся на верхнем уровне маппингов.

  private case class MDayClauseRealAmount(
                                           name         : String,
                                           realAmount   : Double,
                                           calId        : Option[String] = None
                                         )

  /** Маппинг инстанса MDailyTf. */
  private def tfDailyClauseM: Mapping[MDayClauseRealAmount] = {
    mapping(
      "name"        -> nonEmptyText(minLength = 3, maxLength = 32),
      // TODO Надо копейки к рублям приводить, центы к баксам и т.д. (и наоборот тоже).
      "amount"      -> doubleM,
      "calId"       -> optional(esIdM)
    )
    { MDayClauseRealAmount.apply }
    { MDayClauseRealAmount.unapply }
  }

  /** Маппинг карты условий тарифа. */
  private def tfDailyClausesM = {
    list( optional(tfDailyClauseM) )
      .transform [List[MDayClauseRealAmount]] (
        _.flatten,
        _.map(Some.apply)
      )
      /*
      .transform [Map[String, MDayClause]] (
        { MDayClause.clauses2map1 },
        { MDayClause.clausesMap2list }
      )
      */
  }

  /** Маппинг инстанса MDailyTf. */
  def tfDailyM: Mapping[MTfDaily] = {
    mapping(
      "currencyCode"  -> currencyM,
      "clauses"       -> tfDailyClausesM,
      "comission"     -> optional(doubleM)
    )
    { (currency, clausesRA, comission) =>
      val clauses = MDayClause.clauses2map1(
        clausesRA
          .iterator
          .map { mdcRA =>
            MDayClause(
              name   = mdcRA.name,
              amount = MPrice.realAmountToAmount( mdcRA.realAmount, currency ),
              calId  = mdcRA.calId
            )
          }
      )
      MTfDaily( currency, clauses, comission )
    }
    {tfDaily =>
      for {
        (currency, clauses, comission) <- MTfDaily.unapply( tfDaily )
      } yield {
        val clausesRA: List[MDayClauseRealAmount] = clauses
          .valuesIterator
          .map { mdc =>
            MDayClauseRealAmount(
              name        = mdc.name,
              realAmount  = MPrice.amountToReal(mdc.amount, currency),
              calId       = mdc.calId
            )
          }
          .toList
        (currency, clausesRA, comission)
      }
    }
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
    for {
      cbcaNodeOpt <- mNodesCache.getById( bill2Conf.CBCA_NODE_ID )
    } yield {
      cbcaNodeOpt
        .flatMap(_.billing.tariffs.daily)
        .getOrElse {
          LOGGER.debug(s"fallbackTf(): CBCA ${bill2Conf.CBCA_NODE_ID} dailyTF is not defined!")
          VERY_DEFAULT_FT
        }
    }
  }

  /**
    * Вернуть тариф узла для размещения на узле.
    * Если тариф на узле отсутствует, то будет использован унаследованный сверху или дефолтовый тариф.
    */
  def nodeTf(mnode: MNode): Future[MTfDaily] = {
    val tfDailyFut0 = FutureUtil.opt2future( mnode.billing.tariffs.daily ) {
      _inheritedNodeTf( mnode :: Nil )
    }
    val fallbackTfFut = fallbackTf()

    // Размер комиссии нужно брать из тарифа узла CBCA.
    for {
      tfDaily0   <- tfDailyFut0
      tfDaily2   <- {
        if (tfDaily0.comissionPc.isEmpty) {
          // Комиссия не задана. Брать её из cbca.
          for (fallbackTf <- fallbackTfFut) yield
            tfDaily0.withComission( fallbackTf.comissionPc )
        } else {
          // Задана какая-то комиссия. CBCA не интересен.
          Future.successful( tfDaily0 )
        }
      }

    } yield {
      tfDaily2
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
  private def _inheritedNodeTf(mnodes: TraversableOnce[MNode], level: Int = 1): Future[MTfDaily] = {
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
              _inheritedNodeTf(mnodes2, level + 1)
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


  /** Приведение режима тарификации, заданного юзером, к значению поля mnode.billing.tariffs.daily.
    *
    * @param tfMode Режим тарификации, задаваемый юзером.
    * @param nodeTfOpt0 Текущий тариф узла, если есть.
    * @return Фьючерс с опциональным посуточным тарифом.
    */
  def tfMode2tfDaily(tfMode: ITfDailyMode, nodeTfOpt0: Option[MTfDaily]): Future[Option[MTfDaily]] = {
    FutureUtil.optFut2futOpt( tfMode.manualOpt ) { manTf =>
      for (ftf <- fallbackTf()) yield {
        val minClauseAmount = ftf.defaultClause
        val tf2 = ftf.copy(
          clauses = ftf.clauses.mapValues { mdc =>
            mdc.withAmount(
              mdc.amount * manTf.amount / minClauseAmount.amount
            )
          },
          // Переносим размер данные о размере комиссии в обновлённый тариф.
          // Размер комиссии может быть задан вручную администрацией s.io, но не юзером.
          comissionPc = nodeTfOpt0.flatMap(_.comissionPc)
        )
        Some(tf2)
      }
    }
  }

  def getTfInfo(mnode: MNode)(implicit ctx: Context): Future[MTfDailyInfo] = {
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
          val mprice0 = MPrice( mClause.amount, tf.currency )
          val mprice1 = TplDataFormatUtil.setFormatPrice(mprice0)
          calType -> mprice1
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
        comissionPct  = (tf.comissionPc.getOrElse(COMISSION_DFLT) * 100).toInt,
        currency      = tf.currency
      )
    }
  }



  def tfDailyAmountV(amount: Amount_t): ValidationNel[String, Amount_t] = {
    val n = TfDailyConst.Amount
    (
      Validation.liftNel(amount)(_ < n.MIN, "e.amount.too.small") |@|
      Validation.liftNel(amount)(_ > n.MAX, "e.amount.too.big")
    )( (_, _) => amount )
  }

  private def tfModeAmountOptV(tfdm: ITfDailyMode): ValidationNel[String, ITfDailyMode] = {
    ScalazUtil.validateAll(tfdm.amountOpt)(tfDailyAmountV)
      .map { _ => tfdm }
  }

  /** Валидация режима тарификации, задаваемого юзером. */
  def validateTfDailyMode(tfdm: ITfDailyMode): ValidationNel[String, ITfDailyMode] = {
    val tfdm2 = tfdm.manualOpt.fold(tfdm) { manTf =>
      val mprice = MPrice( manTf.amount, MCurrencies.default )
      manTf.withAmount(
        mprice.amount
      )
    }
    tfModeAmountOptV(tfdm2)
  }


  /** Сброс всех комиссионных всех узлов.
    *
    * @return Фьючерс с данными по пройденным узлам.
    */
  def resetAllTfDailyComissions(): Future[_] = {
    // поле MTfDaily.comissionPc(double) не индексируется, поэтому ищем просто по наличию тарифа и дофильтровываем тут.
    import mNodes.Implicits._

    val logPrefix = s"resetAllTfDailyComissions()#${System.currentTimeMillis()}:"
    LOGGER.warn(s"$logPrefix Starting")

    val bp = mNodes.bulkProcessor(
      listener = new mNodes.BulkProcessorListener( logPrefix )
    )
    val counter = new AtomicInteger(0)

    mNodes
      .source[MNode](
        searchQuery = new MNodeSearchDfltImpl {
          override val withoutIds = bill2Conf.CBCA_NODE_ID :: Nil
          // TODO Не работает фильтрация по валюте тарифа, надо разобраться.
          //override def tfDailyCurrencies = Some( Nil )
        }.toEsQuery
      )
      .runForeach { mnode =>
        if (mnode.billing.tariffs.daily.exists(_.comissionPc.nonEmpty)) {
          LOGGER.trace(s"$logPrefix Will update ${mnode.idOrNull}, tf0=${mnode.billing.tariffs.daily.orNull}")
          // Есть тариф с заданной комиссей. Надо сбросить значение поля.
          val mnode2 = mnode.withBilling(
            mnode.billing.withTariffs(
              mnode.billing.tariffs.copy(
                daily = mnode.billing.tariffs.daily.map { tfDaily0 =>
                  tfDaily0.withComission( None )
                }
              )
            )
          )
          bp.add( mNodes.prepareIndex(mnode2).request() )
          counter.incrementAndGet()
        } else {
          LOGGER.trace(s"$logPrefix Skip node#${mnode.idOrNull}.")
        }
      }
      .map { _ =>
        bp.close()
        val totalUpdated = counter.get
        LOGGER.info(s"$logPrefix Updated $totalUpdated nodes")
        totalUpdated
      }
  }

}


trait TfDailyUtilJmxMBean {
  def resetAllTfDailyComissions(): String
}

final class TfDailyUtilJmx @Inject()(
                                      tfDailyUtil: TfDailyUtil,
                                      override implicit val ec: ExecutionContext
                                    )
  extends JMXBase
  with TfDailyUtilJmxMBean
  with MacroLogsDyn
{

  override def jmxName = "io.suggest:type=bill,name=" + classOf[TfDailyUtil].getSimpleName

  override def resetAllTfDailyComissions(): String = {
    val resFut = for {
      res <- tfDailyUtil.resetAllTfDailyComissions()
    } yield {
      val str = s"Done, res => $res"
      LOGGER.info(str)
      str
    }
    awaitString(resFut)
  }

}
