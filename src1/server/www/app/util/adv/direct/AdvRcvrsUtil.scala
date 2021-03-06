package util.adv.direct

import java.time.OffsetDateTime
import io.suggest.adn.MAdnRights

import javax.inject.Inject
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.SlickHolder
import io.suggest.n2.edge.search.Criteria
import io.suggest.n2.edge.{MEdge, MNodeEdges, MPredicates}
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsImpl
import models.adv.build.{Acc, MCtxOuter, TryUpdateBuilder}
import org.elasticsearch.search.sort.SortOrder
import util.adv.build.{AdvBuilderFactory, AdvBuilderUtil}
import util.n2u.N2NodesUtil
import japgolly.univeq._
import play.api.Configuration
import play.api.inject.Injector

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.14 18:19
 * Description: Утиль для работы с размещениями рекламных карточек.
 */

// TODO Всё круто, но этот код потерял актуальность с момента его написания, т.к. всё изменилось. Надо заново отладить всё тут.

class AdvRcvrsUtil @Inject()(
                              injector: Injector,
                            )
  extends MacroLogsImpl
{

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mItems = injector.instanceOf[MItems]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val advBuilderFactory = injector.instanceOf[AdvBuilderFactory]
  private lazy val n2NodesUtil = injector.instanceOf[N2NodesUtil]
  private lazy val advBuilderUtil = injector.instanceOf[AdvBuilderUtil]
  private lazy val configuration = injector.instanceOf[Configuration]
  private lazy val slickHolder = injector.instanceOf[SlickHolder]
  implicit private lazy val ec = injector.instanceOf[ExecutionContext]

  import slickHolder.slick

  /** Причина hard-отказа в размещении со стороны suggest.io, а не узла.
    * Потом надо это заменить на нечто иное: чтобы суперюзер s.io вводил причину. */
  private lazy val SIOM_REFUSE_REASON = configuration.getOptional[String]("sys.m.ad.hard.refuse.reason")
    .getOrElse("Refused by Suggest.io.")

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
        .withNodePred(prodId, MPredicates.Receiver)
        .nextOption()
    }
    val needProducer = rcvrEdgeOpt.isDefined

    // Заготавливаем билдер, нужно это заранее сделать для выборки item'ов только поддерживаемых типов.
    val acc0 = _mkBuilderAcc(mad)
    val b0 = advBuilderFactory
      .builder( Future.successful(acc0), OffsetDateTime.now )
      .clearNode(full = true)  // С чистого листа, т.к. у нас полный пересчёт

    // TODO Opt Тут без stream(), т.к. я пока не осилил. А надо бы...
    import slick.profile.api._
    val adItemsFut = slick.db.run {
      val itypes = b0.supportedItemTypesStrSet
      mItems.query
        .filter { i =>
          (i.nodeId === madId) &&
          (i.statusStr === MItemStatuses.Online.value) &&
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

    val bAcc3Fut = bAcc2Fut.recover { case _: NoSuchElementException =>
      LOGGER.trace(s"$logPrefix No adv items found for $madId")
      acc0
    }

    // Чистим саморазмещение и добавляем в карту прямых ресиверов.
    rcvrEdgeOpt.fold(bAcc3Fut) { rcvrEdge =>
      for {
        acc3     <- bAcc3Fut
      } yield {

        if (!needProducer) {
          // Уровни пусты, саморесивер не нужен.
          acc3

        } else {
          // Добавляем собственный ресивер с обновлёнными уровнями отображениям.
          advBuilderUtil
            .acc_node_edges_out_LENS
            .modify(_ :+ rcvrEdge)(acc3)
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
  def advUnInstallNodeOn(adId: String, rcvrIdOpt: Option[String]): Future[MNode] = {
    import esModel.api._
    for {
      mnodeOpt <- mNodes.getByIdCache(adId)
      mnode = mnodeOpt.get
      mnode2 <- advUnInstallNodeOn(mnode, rcvrIdOpt.toSet)
    } yield {
      mnode2
    }
  }

  /**
    * Удаление размещений карточки из каких-то ресиверов.
    *
    * @param mad Карточка.
    * @param rcvrIds id удаляемых из карточки ресиверов.
    * @return Фьючерс с обновлённой сохраненной карточкой.
    */
  def advUnInstallNodeOn(mad: MNode, rcvrIds: Set[String]): Future[MNode] = {
    val adId = mad.id.get
    lazy val logPrefix = s"removeAdRcvr(ad[$adId],${if (rcvrIds.isEmpty) "*" else "[" + rcvrIds.size + "]"}):"

    // Радуемся в логи
    if (rcvrIds.isEmpty) {
      LOGGER.warn(logPrefix + "Starting removing ALL advs...")
    } else {
      LOGGER.info(logPrefix + "Starting removing advs with rcvrs: " + rcvrIds.mkString(", "))
    }

    val acc0 = _mkBuilderAcc(mad)
    val now = OffsetDateTime.now
    val b0 = advBuilderFactory.builder( Future.successful(acc0), now )

    // Собрать db-эшен для получения списка затрагиваемых размещений:
    import slick.profile.api._
    val onlineItemsAction = mItems.query
      .filter { i =>
        (i.nodeId === adId) &&
        (i.iTypeStr inSet b0.supportedItemTypesStrSet) &&
        (i.statusStr === MItemStatuses.Online.value)
      }
      .result

    // Собираем логику грядущей транзакции:
    val txnLogic = (for {
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
      _ <- {
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
        val tuDataFut = esModel.tryUpdateM[MNode, TryUpdateBuilder](mNodes, TryUpdateBuilder(acc0) ) { tuData0 =>
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
      tuData2.acc.mnode
    })
      .transactionally

    // Запуск транзакции на исполнение
    slick.db.run( txnLogic )
  }

  private def _mkBuilderAcc(mnode: MNode) = {
    LOGGER.warn(s"_mkBuilderAcc(${mnode.id.orNull}): Not fully implemented: tagNodesMap is empty")
    Acc(mnode, ctxOuterFut = Future.successful(MCtxOuter(
      // TODO Missing tags map may break tags (re)building.
      tagNodesMap = Map.empty,
    )))
  }

  /**
    * Выполнить всеобщий пересчёт карт ресиверов.
    *
    * @return Фьючерс с кол-вом обновлённых карточек.
    */
  final def resetAllReceivers(): Future[Int] = {
    lazy val logPrefix = s"resetAllReceivers(${System.currentTimeMillis}):"
    val search = new MNodeSearch {
      override def nodeTypes = MNodeTypes.Ad :: Nil
    }
    import esModel.api._
    mNodes.foldLeftAsync(acc0 = 0, queryOpt = search.toEsQueryOpt) { (counterFut, mnode0) =>
      // Запустить пересчет ресиверов с сохранением.
      val tub2Fut = esModel.tryUpdateM[MNode, TryUpdateBuilder](mNodes, TryUpdateBuilder(_mkBuilderAcc(mnode0)) ) { tub0 =>
        for {
          acc1 <- calculateReceiversFor(tub0.acc.mnode)
        } yield {
          tub0.copy(acc1)
        }
      }
      for {
        _       <- tub2Fut
        counter <- counterFut
      } yield {
        val counter2 = counter + 1
        LOGGER.trace(s"$logPrefix [$counter2] Re-saved ${mnode0.idOrNull}")
        counter2
      }
    }
  }

  private def _orderedRcvrs(rs: Seq[MEdge]) = rs.sortBy(_.toString)
  def isRcvrsMapEquals(map1: Seq[MEdge], map2: Seq[MEdge]): Boolean = {
    _orderedRcvrs(map1) ==* _orderedRcvrs(map2)
  }


  /**
    * Удалить ресиверов для узла-карточки.
    *
    * @param mad0 Исходная карточка.
    * @return Новый экземпляр карточки.
    */
  def cleanReceiverFor(mad0: MNode): Future[MNode] = {
    import esModel.api._
    mNodes.tryUpdate(mad0)(
      MNode.edges.modify { edges0 =>
        MNodeEdges.out.replace(
          MNodeEdges.edgesToMap1(
            edges0
              .withoutPredicateIter( MPredicates.Receiver )
          )
        )(edges0)
      }
    )
  }

  /**
    * Вычислить заново ресиверов для узла-карточки.
    *
    * @param mad0 Исходная рекламная карточка.
    * @return
    */
  def resetReceiversFor(mad0: MNode): Future[MNode] = {
    val fut = esModel.tryUpdateM[MNode, TryUpdateBuilder](mNodes, TryUpdateBuilder(_mkBuilderAcc(mad0)) ) { tub0 =>
      for {
        acc2 <- calculateReceiversFor(tub0.acc.mnode)
      } yield {
        tub0.copy(acc2)
      }
    }
    for (tub2 <- fut) yield {
      tub2.acc.mnode
    }
  }


  /** Поиск под-ресиверов по отношению к указанным ресиверам. */
  def subRcvrsSearch(
                      parentIds: Seq[String],
                      onlyWithIds: Seq[String] = Nil,
                      limit1: Int = 100,
                    ): MNodeSearch = {
    new MNodeSearch {
      override val isEnabled = Some(true)
      override val outEdges: MEsNestedSearch[Criteria] = {
        val cr = Criteria(
          nodeIds    = parentIds,
          predicates = MPredicates.OwnedBy :: Nil,
        )
        MEsNestedSearch.plain( cr )
      }
      override val withAdnRights  = MAdnRights.RECEIVER :: Nil
      override val withNameSort   = Some( SortOrder.ASC )
      override def limit          = limit1
      override def withIds        = onlyWithIds
    }
  }

  /** Поиск под-ресиверов у указанного ресивера. */
  def findSubRcvrsOf(parentIds: String*): Future[Seq[MNode]] = {
    import esModel.api._
    val search = subRcvrsSearch(parentIds = parentIds)
    mNodes.dynSearch(search)
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
  def advUnInstallNode(adId: String): String

  /**
    * Депубликация рекламной карточки на указанном узле.
    *
    * @param adId id рекламной карточки.
    * @param rcvrId id ресивера.
    */
  def advUnInstallNodeOn(adId: String, rcvrId: String): String
}


/** Реализация MBean'а для прямого взаимодействия с AdvUtil. */
final class AdvRcvrsUtilJmx @Inject()(
                                       injector: Injector,
                                     )
  extends AdvRcvrsUtilJmxMBean
  with JmxBase
{

  import io.suggest.util.JmxBase._

  private lazy val esModel = injector.instanceOf[EsModel]
  private def advRcvrsUtil = injector.instanceOf[AdvRcvrsUtil]
  private def mNodes = injector.instanceOf[MNodes]
  implicit private def ec = injector.instanceOf[ExecutionContext]


  override def _jmxType = Types.UTIL

  override def resetAllReceivers(): String = {
    val countFut = advRcvrsUtil.resetAllReceivers()
    val fut = for (cnt <- countFut) yield {
      "Total updated: " + cnt
    }
    awaitString(fut)
  }

  override def resetReceiversForAd(adId: String): String = {
    import esModel.api._

    val s = mNodes.getByIdCache(adId).flatMap {
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

  override def advUnInstallNode(adId: String): String = {
    _advUnInstallNodeOn(adId, None)
  }

  override def advUnInstallNodeOn(adId: String, rcvrId: String): String = {
    _advUnInstallNodeOn(adId, Some(rcvrId))
  }

  private def _advUnInstallNodeOn(adId: String, rcvrIdOpt: Option[String]): String = {
    val rmFut = advRcvrsUtil.advUnInstallNodeOn(adId, rcvrIdOpt)
    val fut = for (isOk <- rmFut) yield {
      "Result: " + isOk
    }
    awaitString(fut)
  }

}

