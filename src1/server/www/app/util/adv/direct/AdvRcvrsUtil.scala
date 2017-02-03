package util.adv.direct

import java.time.OffsetDateTime

import com.google.inject.{Inject, Singleton}
import io.suggest.common.fut.FutureUtil
import io.suggest.es.model.EsModelUtil
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.n2.edge.MNodeEdges
import io.suggest.model.n2.node.MNodes
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.JMXBase
import io.suggest.util.logs.MacroLogsImpl
import models._
import models.adv.build.{Acc, AdvMNodesTryUpdateBuilderT}
import models.mproj.ICommonDi
import util.adv.build.AdvBuilderFactory
import util.n2u.N2NodesUtil

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.14 18:19
 * Description: Утиль для работы с размещениями рекламных карточек.
 */

// TODO Всё круто, но этот код потерял актуальность с момента его написания, т.к. всё изменилось. Надо заново отладить всё тут.

@Singleton
class AdvRcvrsUtil @Inject()(
  mItems                  : MItems,
  override val mNodes     : MNodes,
  advDirectBilling        : AdvDirectBilling,
  advBuilderFactory       : AdvBuilderFactory,
  n2NodesUtil             : N2NodesUtil,
  mCommonDi               : ICommonDi
)
  extends MacroLogsImpl
  with AdvMNodesTryUpdateBuilderT
{

  import LOGGER._
  import mCommonDi._
  import slick.profile.api._

  /** Причина hard-отказа в размещении со стороны suggest.io, а не узла.
    * Потом надо это заменить на нечто иное: чтобы суперюзер s.io вводил причину. */
  private def SIOM_REFUSE_REASON = configuration.getString("sys.m.ad.hard.refuse.reason") getOrElse "Refused by suggest.io."

  /**
    * Пересчет текущих размещений рекламной карточки на основе данных других моделей.
    * Данные по саморазмещению мигрируют из исходных данных размещения.
    *
    * Сайд-эффекты есть в виде создания недостающих узлов-тегов, но это терпимо.
    * Сама изменившаяся карточка не сохраняется, и транзакций никаких не происходит.
    *
    * @param mad Исходная рекламная карточка или её интерфейс.
    * @param producerOpt Экземпляр продьюсера
    */
  def calculateReceiversFor(mad: MNode, producerOpt: Option[MNode] = None): Future[Acc] = {
    val prodIdOpt = n2NodesUtil.madProducerId(mad)

    val madId = mad.id.get
    lazy val logPrefix = s"calculateReceiversFor(mad[$madId]):"

    val rcvrEdgeOpt = prodIdOpt.flatMap { prodId =>
      mad.edges
        .withPredicateIter(MPredicates.Receiver)
        .find( _.nodeIds.contains(prodId) )
    }
    val needProducer = rcvrEdgeOpt.isDefined

    // Нам нужен продьюсер для фильтрации копируемых sls продьюсера. Ищем его в фоне.
    val producerFut: Future[MNode] = if (needProducer) {
      val _prodOpt = producerOpt
        // самоконтроль: резать переданного продьюсера, если он не является продьюсером данной карточки.
        .filter { _.id == prodIdOpt }
      FutureUtil.opt2future(_prodOpt) {
        mNodesCache.maybeGetByIdCached(prodIdOpt)
          .map(_.get)
      }
    } else {
      Future successful producerOpt.orNull
    }

    // Заготавливаем билдер, нужно это заранее сделать для выборки item'ов только поддерживаемых типов.
    val acc0 = Acc(mad)
    val b0 = advBuilderFactory
      .builder( Future.successful(acc0), OffsetDateTime.now )
      .clearNode(full = true)  // С чистого листа, т.к. у нас полный пересчёт

    // TODO Opt Тут без stream(), т.к. я пока не осилил. А надо бы...
    val adItemsFut = slick.db.run {
      val itypes = b0.supportedItemTypesStrSet
      mItems.query
        .filter { i =>
          (i.nodeId === madId) &&
            (i.statusStr === MItemStatuses.Online.strId) &&
            (i.iTypeStr inSet itypes)
        }
        .result
    }

    // Безопасное (optimistic locking) обновление карточки на стороне ES.
    val bAcc2Fut = for {
      // Дождаться получения непустого объема данных по item'ам.
      adItems <- adItemsFut
      if adItems.nonEmpty

      // Накатить все проплаченные размещения на текущую карточку.
      acc2 <- b0.installNode(adItems).accFut
    } yield {
      acc2
    }

    val bAcc3Fut = bAcc2Fut.recover { case ex: NoSuchElementException =>
      LOGGER.trace(s"$logPrefix No adv items found for $madId")
      acc0
    }

    // Чистим саморазмещение и добавляем в карту прямых ресиверов.
    rcvrEdgeOpt.fold(bAcc3Fut) { rcvrEdge =>
      for {
        producer <- producerFut

        // Оставляем только уровни отображения, которые доступны ресиверу.
        psls2 = rcvrEdge.info
          .sls
          // Выкинуть sink-уровни продьюсера, которые не соответствуют доступным синкам и
          // уровням отображения, которые записаны в политике исходящих размещений
          .filter { ssl =>
            val res = producer.extras.adn.exists { adn =>
              adn.isReceiver &&
                adn.outSls.get( ssl.sl ).exists(_.limit > 0)
            }
            if (!res)
              debug(s"$logPrefix Dropping sink show level[$ssl] because producer[${prodIdOpt.orNull}] has only levels=[${producer.extras.adn.iterator.flatMap(_.out4render).map(_.sl).mkString(",")}]")
            res
          }

        acc3 <- bAcc3Fut

      } yield {
        if (psls2.isEmpty) {
          // Уровни пусты, саморесивер не нужен.
          acc3

        } else {
          // Добавляем собственный ресивер с обновлёнными уровнями отображениям.
          val edge2 = rcvrEdge.copy(
            info = rcvrEdge.info.copy(
              sls = psls2
            )
          )
          acc3.copy(
            mad = acc3.mad.copy(
              edges = acc3.mad.edges.copy(
                out = {
                  acc3.mad.edges.out ++ Seq(edge2)
                }
              )
            )
          )
        }
      }
    }
  }



  /**
    * Убрать указанную рекламную карточку из выдачи указанного ресивера или всех ресиверов.
    *
    * @param adId id рекламной карточки.
    * @param rcvrIdOpt id ресивера. Если пусто, то все ресиверы вообще.
    * @return Boolean, который обычно не имеет смысла.
    */
  def depublishAdOn(adId: String, rcvrIdOpt: Option[String]): Future[MNode] = {
    mNodesCache.getByIdType(adId, MNodeTypes.Ad)
      .map(_.get)
      .flatMap(depublishAdOn(_, rcvrIdOpt.toSet))
  }

  /**
    * Удаление размещений карточки из каких-то ресиверов.
    *
    * @param mad Карточка.
    * @param rcvrIds id удаляемых из карточки ресиверов.
    * @return Фьючерс с обновлённой сохраненной карточкой.
    */
  def depublishAdOn(mad: MNode, rcvrIds: Set[String]): Future[MNode] = {
    val adId = mad.id.get
    lazy val logPrefix = s"removeAdRcvr(ad[$adId],${if (rcvrIds.isEmpty) "*" else "[" + rcvrIds.size + "]"}):"

    // Радуемся в логи
    if (rcvrIds.isEmpty) {
      warn(logPrefix + "Starting removing ALL advs...")
    } else {
      info(logPrefix + "Starting removing advs with rcvrs: " + rcvrIds.mkString(", "))
    }

    val acc0 = Acc(mad)
    val now = OffsetDateTime.now
    val b0 = advBuilderFactory.builder( Future.successful(acc0), now )

    // Собрать db-эшен для получения списка затрагиваемых размещений:
    val onlineItemsAction = mItems.query
      .filter { i =>
        (i.nodeId === adId) &&
          (i.iTypeStr inSet b0.supportedItemTypesStrSet) &&
          (i.statusStr === MItemStatuses.Online.strId)
      }
      .result

    // Собираем логику грядущей транзакции:
    val txnLogic = for {
      // Получить список размещений для обработки. Список может быть пустой.
      // TODO Opt через forUpdate помечаются все ряды, даже те которые обновлять пока не планируется.
      allMitems <- onlineItemsAction.forUpdate

      (forUnInstall, keepOnline) = if (rcvrIds.isEmpty) {
        (allMitems, Nil: Seq[MItem])
      } else {
        allMitems.partition { i =>
          i.rcvrIdOpt.exists( rcvrIds.contains )
        }
      }

      // Деинсталлировать в биллинге всех/некоторых ресиверов для карточки.
      unInstSql <- {
        if (forUnInstall.isEmpty) {
          DBIO.successful(0)
        } else {
          for {
            // Собрать SQL для деинсталляции.
            acc11 <- {
              val reasonOpt = Some(SIOM_REFUSE_REASON)
              val b1 = b0.unInstallSql(forUnInstall, reasonOpt)
              DBIO.from( b1.accFut )
            }
            _     <- DBIO.seq( acc11.dbActions: _* )
          } yield {
            forUnInstall.size
          }
        }
      }

      // Собрать оставшиеся online-итемы, перенакатить их всех на карточку.
      tuData2 <- {
        val tuDataFut = EsModelUtil.tryUpdate[MNode, TryUpdateBuilder](mNodes, TryUpdateBuilder(acc0) ) { tuData0 =>
          val b1 = b0
            .withAcc( Future.successful(tuData0.acc) )
            .clearNode(full = rcvrIds.isEmpty)
            .installNode(keepOnline)

          for (acc2 <- b1.accFut) yield {
            TryUpdateBuilder(acc2)
          }
        }
        DBIO.from( tuDataFut )
      }

    } yield {
      tuData2.acc.mad
    }

    // Запуск транзакции на исполнение
    slick.db.run( txnLogic.transactionally )
  }


  /**
    * Выполнить всеобщий пересчёт карт ресиверов.
    *
    * @return Фьючерс с кол-вом обновлённых карточек.
    */
  def resetAllReceivers(): Future[Int] = {
    lazy val logPrefix = s"resetAllReceivers(${System.currentTimeMillis}):"
    val search = new MNodeSearchDfltImpl {
      override def nodeTypes = Seq( MNodeTypes.Ad )
    }
    mNodes.foldLeftAsync(acc0 = 0, queryOpt = search.toEsQueryOpt) { (counterFut, mnode0) =>
      // Запустить пересчет ресиверов с сохранением.
      val tub2Fut = EsModelUtil.tryUpdate[MNode, TryUpdateBuilder](mNodes, TryUpdateBuilder(Acc(mnode0)) ) { tub0 =>
        for {
          acc1 <- calculateReceiversFor(tub0.acc.mad)
        } yield {
          tub0.copy(acc1)
        }
      }
      for {
        tub2    <- tub2Fut
        counter <- counterFut
      } yield {
        val counter2 = counter + 1
        trace(s"$logPrefix [$counter2] Re-saved ${mnode0.idOrNull}")
        counter2
      }
    }
  }

  private def _orderedRcvrs(rs: Receivers_t) = rs.sortBy(_.toString)
  def isRcvrsMapEquals(map1: Receivers_t, map2: Receivers_t): Boolean = {
    _orderedRcvrs(map1) == _orderedRcvrs(map2)
  }


  /**
    * Удалить ресиверов для узла-карточки.
    *
    * @param mad0 Исходная карточка.
    * @return Новый экземпляр карточки.
    */
  def cleanReceiverFor(mad0: MNode): Future[MNode] = {
    mNodes.tryUpdate(mad0) { mad =>
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
    * @param mad0 Исходная рекламная карточка.
    * @return
    */
  def resetReceiversFor(mad0: MNode): Future[MNode] = {
    val fut = EsModelUtil.tryUpdate[MNode, TryUpdateBuilder](mNodes, TryUpdateBuilder(Acc(mad0)) ) { tub0 =>
      for {
        acc2 <- calculateReceiversFor(tub0.acc.mad)
      } yield {
        tub0.copy(acc2)
      }
    }
    for (tub2 <- fut) yield {
      tub2.acc.mad
    }
  }

  /** Заменить ресиверов в узле без сохранения. */
  def updateReceivers(mad: MNode, rcvrs1: Receivers_t): MNode = {
    mad.copy(
      edges = mad.edges.copy(
        out = {
          val oldEdgesIter = mad.edges
            .withoutPredicateIter( MPredicates.Receiver )
          val newRcvrEdges = rcvrs1.iterator
          MNodeEdges.edgesToMap1( oldEdgesIter ++ newRcvrEdges )
        }
      )
    )
  }

}


// JMX утиль
/** MBean-интерфейс для доступа к сабжу. */
trait AdvRcvrsUtilJmxMBean {

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
final class AdvRcvrsUtilJmx @Inject()(
                                       advRcvrsUtil            : AdvRcvrsUtil,
                                       mCommonDi               : ICommonDi
                                     )
  extends AdvRcvrsUtilJmxMBean
  with JMXBase
{

  import mCommonDi._

  override def jmxName = "io.suggest:type=util,name=" + getClass.getSimpleName.replace("Jmx", "")

  override def resetAllReceivers(): String = {
    val countFut = advRcvrsUtil.resetAllReceivers()
    val fut = for (cnt <- countFut) yield {
      "Total updated: " + cnt
    }
    awaitString(fut)
  }

  override def resetReceiversForAd(adId: String): String = {
    val s = mNodesCache.getById(adId).flatMap {
      case Some(mad) =>
        for (_ <- advRcvrsUtil.resetReceiversFor(mad)) yield {
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
    val rmFut = advRcvrsUtil.depublishAdOn(adId, rcvrIdOpt)
    val fut = for (isOk <- rmFut) yield {
      "Result: " + isOk
    }
    awaitString(fut)
  }

}

