package util.adv

import com.google.inject.{Singleton, Inject}
import io.suggest.common.fut.FutureUtil
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.n2.edge.MNodeEdges
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.JMXBase
import models._
import models.adv.{MAdvReq, MAdvOk}
import models.mproj.ICommonDi
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import util.PlayMacroLogsImpl
import util.async.AsyncUtil
import util.billing.MmpDailyBilling
import util.n2u.N2NodesUtil

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.14 18:19
 * Description: Утиль для работы с размещениями рекламных карточек.
 */
@Singleton
class AdvUtil @Inject() (
  mmpDailyBilling         : MmpDailyBilling,
  advTownCoverageRcvrs    : AdvTownCoverageRcvrs,
  advFreeGeoParentRcvrs   : AdvFreeGeoParentRcvrs,
  n2NodesUtil             : N2NodesUtil,
  mCommonDi               : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Текущие активные аддоны, участвующие в генерации списка ресиверов. */
  val EXTRA_RCVRS_CALCS: List[AdvExtraRcvrsCalculator] = {
    List(
      advFreeGeoParentRcvrs,
      advTownCoverageRcvrs
    )
    .filter(_.isEnabled)
  }

  /** Причина hard-отказа в размещении со стороны suggest.io, а не узла.
    * Потом надо это заменить на нечто иное: чтобы суперюзер s.io вводил причину. */
  def SIOM_REFUSE_REASON = configuration.getString("sys.m.ad.hard.refuse.reason") getOrElse "Refused by suggest.io."

  
  info(s"Enabled extra-rcvrs generators: " + EXTRA_RCVRS_CALCS.iterator.map(_.getClass.getSimpleName).mkString(", "))

  /**
    * Пересчет текущих размещений рекламной карточки на основе данных других моделей.
    * Данные по саморазмещению мигрируют из исходных данных размещения.
    *
    * @param mad Исходная рекламная карточка или её интерфейс.
    * @param producerOpt Экземпляр продьюсера
    */
  def calculateReceiversFor(mad: MNode, producerOpt: Option[MNode] = None): Future[Receivers_t] = {
    val prodIdOpt = n2NodesUtil.madProducerId(mad)

    val rcvrEdgeOpt = prodIdOpt.flatMap { prodId =>
      mad.edges
        .withPredicateIter(MPredicates.Receiver)
        .find(_.nodeId == prodId)
    }
    val needProducer = rcvrEdgeOpt.isDefined

    // Нам нужен продьюсер для фильтрации копируемых sls продьюсера. Ищем его в фоне.
    val producerFut: Future[MNode] = if (needProducer) {
      val _prodOpt = producerOpt
        // самоконтроль: резать переданного продьюсера, если он не является продьюсером данной карточки.
        .filter { _.id == prodIdOpt }
      FutureUtil.opt2future(_prodOpt) {
        mNodeCache.maybeGetByIdCached(prodIdOpt).map(_.get)
      }
    } else {
      Future successful producerOpt.orNull
    }

    // Считаем непосредственных ресиверов через mmp-billing, т.е. платные размещения по времени.
    val receiversMmpFut = mmpDailyBilling.calcualteReceiversMapForAd(mad.id.get)

    // Чистим саморазмещение и добавляем в карту прямых ресиверов.
    val prodResultFut: Future[Receivers_t] = {
      rcvrEdgeOpt.fold(receiversMmpFut) { rcvrEdge =>
        producerFut.flatMap { producer =>
          // Оставляем только уровни отображения, которые доступны ресиверу.
          val psls2 = rcvrEdge.info.sls
            // Выкинуть sink-уровни продьюсера, которые не соответствуют доступным синкам и
            // уровням отображения, которые записаны в политике исходящих размещений
            .filter { ssl =>
            producer.extras.adn.exists { adn =>
              adn.isReceiver &&
                adn.sinks.contains( ssl.adnSink ) &&
                adn.outSls.get( ssl.sl ).exists(_.limit > 0)
            }
          }
          receiversMmpFut.map { receiversMmp =>
            if (psls2.isEmpty) {
              // Удаляем саморесивер, т.к. уровни пусты.
              receiversMmp
            } else {
              // Добавляем собственный ресивер с обновлёнными уровнями отображениям.
              val edge2 = rcvrEdge.copy(
                info = rcvrEdge.info.copy(
                  sls = psls2
                )
              )
              receiversMmp ++ MNodeEdges.edgesToMapIter(edge2)
            }
          }
        }
      }
    }

    // На чищенную карту ресиверов запускаем поиск экстра-ресиверов на основе списка непосредственных.
    val resultFut: Future[Receivers_t] = prodResultFut
      .flatMap { prodResults =>
        // Запускаем сборку данных по доп.ресиверами. Получающиеся карты ресиверов объединяем асинхронно.
        val extrasFuts = EXTRA_RCVRS_CALCS.map { src =>
          src.calcForDirectRcvrs(prodResults, prodIdOpt)
        }
        Future.reduce(prodResultFut :: extrasFuts) {
          (rm1, rm2) =>
            rm1 ++ rm2
        }
      }

    resultFut
  }


  /**
    * Убрать указанную рекламную карточку из выдачи указанного ресивера или всех ресиверов.
    *
    * @param adId id рекламной карточки.
    * @param rcvrIdOpt id ресивера. Если пусто, то все ресиверы вообще.
    * @return Boolean, который обычно не имеет смысла.
    */
  def removeAdRcvr(adId: String, rcvrIdOpt: Option[String]): Future[Boolean] = {
    lazy val logPrefix = s"removeAdRcvr(ad[$adId]${rcvrIdOpt.fold("")(", rcvr[" + _ + "]")}): "
    val madOptFut = MNode.getByIdType(adId, MNodeTypes.Ad)
    // Радуемся в лог.
    rcvrIdOpt match {
      case Some(rcvrId) =>
        info(logPrefix + "Starting removing for single rcvr...")
      case None =>
        warn(logPrefix + "Starting removing ALL rcvrs...")
    }

    // Надо убрать указанного ресиверов из списка ресиверов
    val isOkFut = madOptFut flatMap {
      case Some(mad) =>
        MNode.tryUpdate(mad) { mad1 =>
          mad1.copy(
            edges = mad1.edges.copy(
              out = {
                val p = MPredicates.Receiver
                val fIter = if (rcvrIdOpt.isEmpty) {
                  mad1.edges.withoutPredicateIter(p)
                } else {
                  mad1.edges.out
                    .valuesIterator
                    .filterNot { e =>
                      e.predicate == p && e.nodeId == rcvrIdOpt.get
                    }
                }
                MNodeEdges.edgesToMap1( fIter )
              }
            )
          )
        }
          .map { _ => true}
      case None =>
        warn(logPrefix + "MAd not found: " + adId)
        Future successful false
    }

    // Надо убрать карточку из текущего размещения на узле, если есть: из advOk и из advReq.
    val dbUpdFut = Future {
      db.withTransaction { implicit c =>
        // Резать как online, так и в очереди на публикацию.
        val sepo = SelectPolicies.UPDATE
        val advsOk = if (rcvrIdOpt.isDefined) {
          MAdvOk.findNotExpiredByAdIdAndRcvr(adId, rcvrId = rcvrIdOpt.get, policy = sepo)
        } else {
          MAdvOk.findNotExpiredByAdId(adId, policy = sepo)
        }
        advsOk
          .foreach { advOk =>
            info(s"${logPrefix}offlining advOk[${advOk.id.get}]")
            advOk.copy(dateEnd = DateTime.now, isOnline = false)
              .saveUpdate
          }
        // Запросы размещения переколбашивать в refused с возвратом бабла.
        val advsReq = if (rcvrIdOpt.isDefined) {
          MAdvReq.findByAdIdAndRcvr(adId, rcvrId = rcvrIdOpt.get, policy = sepo)
        } else {
          MAdvReq.findByAdId(adId, policy = sepo)
        }
        // Расставляем сообщения о депубликации для всех запросов размещения.
        val msg = SIOM_REFUSE_REASON
        advsReq.foreach { madvReq =>
          trace(s"${logPrefix}refusing advReq[${madvReq.id.get}]...")
          // TODO Нужно как-то управлять причиной выпиливания. Этот action работает через POST, поэтому можно замутить форму какую-то.
          mmpDailyBilling.refuseAdvReqTxn(madvReq, madvReq.toRefuse(msg))
        }
      }
    }(AsyncUtil.jdbcExecutionContext)

    dbUpdFut flatMap { _ =>
      isOkFut
    }
  }


  /**
    * Выполнить всеобщий пересчёт карт ресиверов.
    *
    * @return Фьючерс с кол-вом обновлённых карточек.
    */
  def resetAllReceivers(): Future[Int] = {
    val search = new MNodeSearchDfltImpl {
      override def nodeTypes = Seq( MNodeTypes.Ad )
    }
    MNode.updateAll(queryOpt = search.toEsQueryOpt) { mad0 =>
      val newRcvrsFut = calculateReceiversFor(mad0)
      val rcvrsMap = n2NodesUtil.receiversMap(mad0)
      for (newRcvrs <- newRcvrsFut) yield {
        if (isRcvrsMapEquals(rcvrsMap, newRcvrs)) {
          null
        } else {
          updateReceivers(mad0, newRcvrs)
        }
      }
    }
  }

  def isRcvrsMapEquals(map1: Receivers_t, map2: Receivers_t): Boolean = {
    map1 == map2
  }

  // TODO Из-за расхождений между ветками нужно выставить в DEV тип возвращаемого значения в методах ниже.

  /**
    * Удалить ресиверов для узла-карточки.
    *
    * @param mad0 Исходная карточка.
    * @return Новый экземпляр карточки.
    */
  def cleanReceiverFor(mad0: MNode) = {
    MNode.tryUpdate(mad0) { mad =>
      mad.copy(
        edges = mad.edges.copy(
          out = {
            val iter = mad.edges
              .withoutPredicateIter( MPredicates.Receiver )
            MNodeEdges.edgesToMap1(iter)
          }
        )
      )
    }
  }

  /**
    * Вычислить заново ресиверов для узла-карточки.
    *
    * @param mad0
    * @return
    */
  def resetReceiversFor(mad0: MNode) = {
    for {
      // Вычислить ресиверов согласно биллингу и прочему.
      newRcvrs <- calculateReceiversFor(mad0)

      // Запустить обновление ресиверов в карте.
      res      <- {
        MNode.tryUpdate(mad0) { mad =>
          updateReceivers(mad, newRcvrs)
        }
      }

    } yield {
      res
    }
  }

  /** Заменить ресиверов в узле без сохранения. */
  def updateReceivers(mad: MNode, rcvrs1: Receivers_t): MNode = {
    mad.copy(
      edges = mad.edges.copy(
        out = {
          val oldEdgesIter = mad.edges
            .withoutPredicateIter( MPredicates.Receiver )
          val newRcvrEdges = rcvrs1.valuesIterator
          MNodeEdges.edgesToMap1( oldEdgesIter ++ newRcvrEdges )
        }
      )
    )
  }

}


/** Интерфейс для модулей рассчета extra-ресиверов карточки. */
trait AdvExtraRcvrsCalculator {

  /** Метод для проверки активности модуля. */
  def isEnabled: Boolean

  /**
    * Рассчет доп.ресиверов на основе карты прямых (непосредственных) ресиверов карточки.
    *
    * @param allDirectRcvrs Карта непосредственных ресиверов.
    * @param producerIdOpt id продьюсера.
    * @return Фьючерс с картой extra-ресиверов.
    */
  def calcForDirectRcvrs(allDirectRcvrs: Receivers_t, producerIdOpt: Option[String]): Future[Receivers_t]

}



// JMX утиль
/** MBean-интерфейс для доступа к сабжу. */
trait AdvUtilJmxMBean {

  /** Пройтись по карточкам, пересчитать всех ресиверов для каждой карточки и сохранить в хранилище. */
  def resetAllReceivers(): String

  /**
    * Пересчитать ресиверов для карточки и сохранить в карточку.
    *
    * @param adId id карточки.
    */
  def resetReceiversForAd(adId: String): String

  /**
    * Депубликация указанной рекламной карточки отовсюду.
    *
    * @param adId id рекламной карточки.
    */
  def depublishAd(adId: String): String

  /**
    * Депубликация рекламной карточки на указанном узле.
    *
    * @param adId id рекламной карточки.
    * @param rcvrId id ресивера.
    */
  def depublishAdAt(adId: String, rcvrId: String): String
}


/** Реализация MBean'а для прямого взаимодействия с AdvUtil. */
final class AdvUtilJmx @Inject() (
  advUtil                 : AdvUtil,
  mCommonDi               : ICommonDi
)
  extends AdvUtilJmxMBean
  with JMXBase
{

  import mCommonDi._

  override def jmxName = "io.suggest:type=util,name=" + getClass.getSimpleName.replace("Jmx", "")

  override def resetAllReceivers(): String = {
    val countFut = advUtil.resetAllReceivers()
    val fut = for (cnt <- countFut) yield {
      "Total updated: " + cnt
    }
    awaitString(fut)
  }

  override def resetReceiversForAd(adId: String): String = {
    val s = MNode.getById(adId).flatMap {
      case Some(mad) =>
        for (_ <- advUtil.resetReceiversFor(mad)) yield {
          "Successfully reset receivers for " + adId
        }
      case None =>
        val msg = "Not found ad: " + adId
        Future.successful( msg )
    }
    awaitString(s)
  }

  override def depublishAd(adId: String): String = {
    _depublishAd(adId, None)
  }

  override def depublishAdAt(adId: String, rcvrId: String): String = {
    _depublishAd(adId, Some(rcvrId))
  }

  private def _depublishAd(adId: String, rcvrIdOpt: Option[String]): String = {
    val rmFut = advUtil.removeAdRcvr(adId, rcvrIdOpt)
    val fut = for (isOk <- rmFut) yield {
      "Result: " + isOk
    }
    awaitString(fut)
  }

}

