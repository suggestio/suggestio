package util.adv

import com.google.inject.{Singleton, Inject}
import io.suggest.common.fut.FutureUtil
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.model.n2.edge.MNodeEdges
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.JMXBase
import models._
import models.adv.{MAdvReq, MAdvOk}
import org.elasticsearch.client.Client
import org.joda.time.DateTime
import play.api.Configuration
import play.api.db.Database
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
  mNodeCache              : MNodeCache,
  mmpDailyBilling         : MmpDailyBilling,
  configuration           : Configuration,
  db                      : Database,
  advTownCoverageRcvrs    : AdvTownCoverageRcvrs,
  advFreeGeoParentRcvrs   : AdvFreeGeoParentRcvrs,
  n2NodesUtil             : N2NodesUtil,
  implicit val ec         : ExecutionContext,
  implicit val esClient   : Client,
  implicit val sn         : SioNotifierStaticClientI
)
  extends PlayMacroLogsImpl
{

  import LOGGER._

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
        val updFut = MNode.tryUpdate(mad) { mad1 =>
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
        for (_ <- updFut) yield {
          true
        }

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

}


/** Интерфейс для модулей рассчета extra-ресиверов карточки. */
trait AdvExtraRcvrsCalculator {

  /** Метод для проверки активности модуля. */
  def isEnabled: Boolean

  /**
   * Рассчет доп.ресиверов на основе карты прямых (непосредственных) ресиверов карточки.
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
   * @param adId id карточки.
   */
  def resetReceiversForAd(adId: String): String

  /**
   * Депубликация указанной рекламной карточки отовсюду.
   * @param adId id рекламной карточки.
   */
  def depublishAd(adId: String): String

  /**
   * Депубликация рекламной карточки на указанном узле.
   * @param adId id рекламной карточки.
   * @param rcvrId id ресивера.
   */
  def depublishAdAt(adId: String, rcvrId: String): String
}


/** Реализация MBean'а для прямого взаимодействия с AdvUtil. */
final class AdvUtilJmx @Inject() (
  advUtil                 : AdvUtil,
  implicit val ec         : ExecutionContext,
  implicit val esClient   : Client,
  implicit val sn         : SioNotifierStaticClientI
)
  extends AdvUtilJmxMBean
  with JMXBase
{

  override def jmxName = "io.suggest:type=util,name=" + getClass.getSimpleName.replace("Jmx", "")

  override def resetAllReceivers(): String = {
    val search = new MNodeSearchDfltImpl {
      override def nodeTypes = Seq( MNodeTypes.Ad )
    }
    val cntFut = MNode.updateAll(queryOpt = search.toEsQueryOpt) { mad0 =>
      advUtil.calculateReceiversFor(mad0) map { rcvrs1 =>
        mad0.copy(
          edges = mad0.edges.copy(
            out = mad0.edges.out ++ rcvrs1
          )
        )
      }
    }
    val s = cntFut map { cnt =>
      "Total updated: " + cnt
    }
    awaitString(s)
  }

  override def resetReceiversForAd(adId: String): String = {
    val s = MNode.getById(adId) flatMap {
      case None =>
        Future successful s"Not found ad: $adId"
      case Some(mad) =>
        for {
          rcvrs1 <- advUtil.calculateReceiversFor(mad)
          _      <- {
            MNode.tryUpdate(mad) { mad0 =>
              mad0.copy(
                edges = mad0.edges.copy(
                  out = mad0.edges.out ++ rcvrs1
                )
              )
            }
          }
        } yield {
          "Successfully reset receivers: " + adId + "\n\n" + rcvrs1
        }
    }
    awaitString(s)
  }

  override def depublishAd(adId: String): String = {
    val s = advUtil.removeAdRcvr(adId, rcvrIdOpt = None)
      .map { isOk => "Result: " + isOk }
    awaitString(s)
  }

  override def depublishAdAt(adId: String, rcvrId: String): String = {
    val s = advUtil.removeAdRcvr(adId, rcvrIdOpt = Some(rcvrId))
      .map { isOk => "Result: " + isOk }
    awaitString(s)
  }

}

