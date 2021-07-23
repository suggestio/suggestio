package util.adv.build

import javax.inject.Inject
import io.suggest.common.empty.OptionUtil
import io.suggest.geo.{MGeoPoint, MNodeGeoLevels}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.n2.edge._
import io.suggest.n2.node.MNode
import io.suggest.util.logs.MacroLogsImpl
import models.adv.build.{Acc, MCtxOuter}
import models.mproj.ICommonDi
import util.adv.geo.tag.GeoTagsUtil
import util.billing.BillDebugUtil
import io.suggest.mbill2.m.item.MItemJvm.Implicits._
import japgolly.univeq._

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.16 21:35
  * Description: Утиль для AdvBuilder'а.
  */
final class AdvBuilderUtil @Inject() (
                                       mCommonDi        : ICommonDi
                                     )
  extends MacroLogsImpl
{

  import mCommonDi.current.injector

  private lazy val mItems = injector.instanceOf[MItems]
  private lazy val geoTagsUtil = injector.instanceOf[GeoTagsUtil]
  private lazy val billDebugUtil = injector.instanceOf[BillDebugUtil]


  import mCommonDi._
  import slick.profile.api._


  /**
    * Подготовка данных и внешнего контекста для билдера, который будет содержать дополнительные данные,
    * необходимые для работы внутри самого билдера.
    *
    * @param itemsSql Заготовка запроса поиска
    * @return Фьючерс с outer-контекстом для дальнейшей передачи его в билдер.
    */
  def prepareInstallNew(itemsSql: Query[MItems#MItemsTable, MItem, Seq]): Future[MCtxOuter] = {
    geoTagsUtil.prepareInstallNew(itemsSql)
  }


  /**
    * Окончание инсталляции новых item'ов.
    * @param ctxOuterFut Результат prepareInstallNew().
    * @return Фьючер без полезных данных внутри.
    */
  def afterInstallNew(ctxOuterFut: Future[MCtxOuter]): Future[_] = {
    geoTagsUtil.afterInstallNew(ctxOuterFut)
  }


  /**
    * Подготовка outer-контекста к деинсталляции item'ов, требующих дополнительных действий.
    *
    * @param itemsSql Выборка item'ов, которые будут деинсталлированы.
    * @return Фьючерс с готовым outer-контекстом.
    */
  def prepareUnInstall(itemsSql: Query[MItems#MItemsTable, MItem, Seq]): Future[MCtxOuter] = {
    geoTagsUtil.prepareUnInstall(itemsSql)
  }

  def afterUnInstall(ctxOuterFut: Future[MCtxOuter]): Future[_] = {
    geoTagsUtil.afterUnInstall(ctxOuterFut)
  }

  def acc_node_edges_LENS =
    Acc.mnode
      .composeLens( MNode.edges )

  def acc_node_edges_out_LENS =
    acc_node_edges_LENS
      .composeLens( MNodeEdges.out )

  def clearByPredicate(b0: IAdvBuilder, preds: Seq[MPredicate]): IAdvBuilder = {
    // Вычистить теги из эджей карточки
    val acc2Fut = for {
      acc0 <- b0.accFut
    } yield {
      acc_node_edges_LENS
        .modify { edges0 =>
          MNodeEdges.out.set(
            MNodeEdges.edgesToMap1(
              edges0
                // Все теги и геотеги идут через биллинг. Чистка равносильна стиранию всех эджей TaggedBy.
                .withoutPredicateIter( preds: _* )
            )
          )(edges0)
        }(acc0)
    }
    b0.withAcc( acc2Fut )
  }


  /** Извлечение базовых геоточек для гео-шейпов из MItem.
    * Для круга будет извлечена точка центра, для остальных - как пойдёт.
    *
    * @param mitems Итемы биллинга или же итератор оных.
    * @return Итератор из награбленных точек.
    */
  def grabGeoPoints4Stats(mitems: IterableOnce[MItem]): Iterator[MGeoPoint] = {
    for {
      mitem <- mitems.iterator
      gs <- mitem.geoShape
    } yield {
      gs.centerPoint
        // Плевать, если не центральная точка: в работе самой геолокации это не используется, только для всякой статистики.
        .getOrElse( gs.firstPoint )
    }
  }


  /** Дробление списка item'ов на два списка по совпадению с запрашиваемым itype.
    *
    * @param items item'ы.
    * @param itypes Интересующий тип или типы item'ов.
    * @return Подходящие и неподходящие item'ы.
    */
  def partitionItemsByType(items: Iterable[MItem], itypes: MItemType*): (Iterable[MItem], Iterable[MItem]) = {
    items.partition { i =>
      // Интересуют только item'ы с искомым значением в поле itype.
      itypes contains[MItemType] i.iType
      // Тут была проверка на mitem.gsOpt, но это защита от самого себя и она была выпилена.
    }
  }


  def nodeGeoLocTags(mnode: MNode): Set[String] = {
    val b = mnode.meta.basic
    (b.nameOpt ++ b.nameShortOpt)
      .toSet
  }


  /** Код накладывания географических item'ов обычно одинаковый: взяли item'ы, извлекли geo-шейпы,
    * собрали эдж, затолкали эдж в ноду.
    *
    * @param b0 Билдер.
    * @param items Список item'ов.
    * @param predicate Предикат для создаваемых эджей.
    * @param name2tag Заливать ли название узла в индекс поиска тегов?
    *                 Это нужно для поиска узлов, размещённых на карте.
    * @return Обновлённый инстанс AdvBuilder в связке с оставшимися необработанными item'ами.
    */
  def geoInstallNode(b0: IAdvBuilder, items: Iterable[MItem], predicate: MPredicate, name2tag: Boolean): IAdvBuilder = {
    // При сборке эджей считаем, что происходит пересборка эджей с нуля.
    if (items.nonEmpty) {

      // Аккамулируем все item'ы для единого эджа.
      val (geoShapes, _) = items
        .foldLeft( List.empty[MEdgeGeoShape] -> MEdgeGeoShape.SHAPE_ID_START ) {
          case ((acc, counter), mitem) =>
            val meGs = MEdgeGeoShape(
              id      = counter,
              glevel  = MNodeGeoLevels.geoPlace,
              shape   = mitem.geoShape.get
            )
            (meGs :: acc) -> (counter + 1)
        }

      // Надо собрать опорные точки для общей статистики, записав их рядышком.
      // По идее, все шейпы - это PointGs.
      val geoPoints = grabGeoPoints4Stats( items )
        .toSeq

      LOGGER.trace(s"geoInstallNode($predicate): Found ${items.size} items for geo edge-building: ${geoShapes.size} geoshapes, ${geoPoints.size} geo points.")

      // Собрать новую карточку, аккамулятор, билдер...
      b0.withAccUpdated { acc0 =>
        // Индексировать ли имя узла в теги эджа?
        val tags: Set[String] = if (name2tag) {
          nodeGeoLocTags( acc0.mnode )
        } else {
          Set.empty
        }

        // Собираем единый эдж для геолокации карточки в месте на гео.карте.
        val e = MEdge(
          predicate = predicate,
          info = MEdgeInfo(
            geoShapes = geoShapes,
            geoPoints = geoPoints,
            tags      = tags
          )
        )

        // Патчим mnode новым эджем...
        acc_node_edges_out_LENS
          .modify(_ :+ e)(acc0)
      }

    } else {
      b0
    }
  }


  /** Логика для installSql() для поиска и прерывания всех item'ов, аналогичных текущим по типам и nodeIds.
    *
    * @param b0 Текущий инстанс [[IAdvBuilder]].
    * @param items Все item'ы, переданные в installSql().
    * @param itypes Только типы item'ов, котроые нуждаются в проработке.
    * @return Обновлённый инстанс [[IAdvBuilder]].
    */
  def interruptItemsFor(b0: IAdvBuilder, items: Iterable[MItem], itypes: MItemType*): IAdvBuilder = {
    b0.withAccUpdated { acc0 =>
      lazy val logPrefix = s"interruptItemsFor(i##[${items.iterator.flatMap(_.id).mkString(",")}], t=[${itypes.mkString(",")}])[${System.currentTimeMillis()}]:"
      val needInterruptItypes = itypes.toSet -- acc0.interruptedTypes

      if (needInterruptItypes.isEmpty) {
        LOGGER.trace(s"$logPrefix Nothing to do: no more itypes.")
        acc0

      } else {
        LOGGER.trace(s"$logPrefix Only need to interrupt ${needInterruptItypes.size} types: ${needInterruptItypes.mkString(", ")}")
        // Ищем id узлов, которые надо будет обработать.
        val myNodeIds = items
          .iterator
          .filter { i =>
            needInterruptItypes contains i.iType
          }
          .map(_.nodeId)
          .toSet

        if (myNodeIds.nonEmpty) {
          LOGGER.trace(s"$logPrefix Will search for online items for nodes#[${myNodeIds.mkString(", ")}] to interruption...")
          // dbAction: ПЕРЕД выполнением остальных экшенов надо выполнить данную зачистку.
          val dbAction = billDebugUtil.findAndInterruptItemsLike(myNodeIds, itypes: _*)
          (
            Acc.dbActions.modify(dbAction :: _) andThen
            Acc.interruptedTypes.modify(_ ++ needInterruptItypes)
          )(acc0)

        } else {
          LOGGER.trace(s"$logPrefix No related nodes found for itypes [${needInterruptItypes.mkString(", ")}]")
          acc0
        }
      }
    }
  }

  /** Враппер над interruptItemsFor() для adn-items.
    * Обычно надо отменять сразу все adn-размещения пачкой.
    */
  def interruptAdnMapItemsFor(b0: IAdvBuilder, items: Iterable[MItem]): IAdvBuilder = {
    interruptItemsFor(b0, items, MItemTypes.adnMapTypes: _*)
  }


  /** Выбрать наиболее стартующий сейчас item, проигнорив все остальные. */
  def lastStartedItem(items: Iterable[MItem]): Option[MItem] = {
    // Отсеять item'ы без dateStart.
    val itemsIter = items
      .iterator
      .filter(_.dateStartOpt.nonEmpty)
    OptionUtil.maybe( itemsIter.nonEmpty ) {
      // Ищем по dateStart. Фильтрация по наличию dateStart уже сделана выше, поэтому .get безопасен тут.
      itemsIter.maxBy( _.dateStartOpt.get.toInstant.toEpochMilli )
    }
  }


  /** Код SQL-инсталляции для тегов.
    *
    * @param b0 adv-билдер.
    * @param items item'ы, которые билдим.
    * @param next Следующий шаг.
    * @return Обновлённый инстанс [[IAdvBuilder]].
    */
  def tagsInstallSql(b0: IAdvBuilder, items: Iterable[MItem], tagItemType: MItemType, next: Iterable[MItem] => IAdvBuilder): IAdvBuilder = {
    val (ditems, others) = items.partition { i =>
      i.iType ==* tagItemType
    }

    val this2 = next(others)
    lazy val logPrefix = s"tagsInstallSql(${ditems.size}):"

    // Собираем db-экшены для инсталляции
    if (ditems.nonEmpty) {
      LOGGER.trace(s"$logPrefix There are ${ditems.size} geotags for install...")

      b0.withAccUpdatedFut { acc0 =>
        for (outerCtx <- acc0.ctxOuterFut) yield {
          val dbas1 = ditems.foldLeft(acc0.dbActions) { (dbas0, mitem) =>
            val dbAction = {
              val dateStart2 = b0.now
              val dateEnd2 = dateStart2.plus( mitem.dtIntervalOpt.get.toDuration )
              val mitemId = mitem.id.get
              // Определяем заодно id узла-тега. Это облегчит поиск в таблице на этапе перекомпиляции узлов-тегов.
              val tagNodeIdOpt = mitem.tagFaceOpt
                .flatMap(outerCtx.tagNodesMap.get)
                .flatMap(_.id)

              if (tagNodeIdOpt.isEmpty)
                LOGGER.warn(s"$logPrefix NOT found tag node id for tag-face: ${mitem.tagFaceOpt}")

              mItems.query
                .filter { _.id === mitemId }
                .map { i =>
                  (i.status, i.tagNodeIdOpt, i.dateStartOpt, i.dateEndOpt, i.dateStatus)
                }
                .update((MItemStatuses.Online, tagNodeIdOpt, Some(dateStart2), Some(dateEnd2), dateStart2))
                .filter { rowsUpdated =>
                  LOGGER.trace(s"$logPrefix Updated item[$mitemId] '${mitem.tagFaceOpt}': dateEnd => $dateEnd2, tagNodeId => $tagNodeIdOpt")
                  rowsUpdated == 1
                }
            }
            dbAction :: dbas0
          }
          (Acc.dbActions set dbas1)(acc0)
        }
      }
    } else {
      this2
    }
  }


  /** Код обновления MNode под гео-тег (AdvGeoTag, LocationTag).
    *
    * @param builder Инстанс текущего билдера.
    * @param items Все item'ы, как подходящие под item type, так и все остальные.
    * @param itype Текущий тип item type.
    * @param predicate Предикат для создаваемых эджей.
    * @param next Следующий шаг.
    * @return Обновлённое состояние билдера.
    */
  def installNodeGeoTag(
                         builder: IAdvBuilder,
                         items: Iterable[MItem],
                         itype: MItemType,
                         predicate: MPredicate,
                         next: Iterable[MItem] => IAdvBuilder,
                       ): IAdvBuilder = {

    lazy val logPrefix = s"installNodeGeoTag(${System.currentTimeMillis}):"

    val (tagItems, other) = items.partition { i =>
      // Интересуют только item'ы тегов, у которых всё правильно оформлено.
      (i.iType ==* itype) && {
        val r = i.geoShape.isDefined && i.tagFaceOpt.isDefined
        if (!r)
          LOGGER.error(s"$logPrefix Invalid geo-tag item: one or more required fields are empty:\n $i")
        r
      }
    }
    val this2 = next( other )

    // При сборке эджей считаем, что карточка уже была заранее очищена от предыдущих тегов.
    // Это особенность новой архитектуры: всё перенакатывается заново всегда.

    if (tagItems.nonEmpty) {
      LOGGER.debug(s"$logPrefix Found ${tagItems.size} items for adv-geo-tag install: ${tagItems.iterator.flatMap(_.id).mkString(",")}")

      // Теги отработать, группируя по шейпу. Т.е. размещать в эджах карточек ровно как на форме размещения в тегах.
      // Индексировать дубликаты тегов внутри карточки легче для индексов, нежели избыточно индексировать шейпы.
      // Хотя это может вызвать неточности при аггрегации документов по тегам.
      // id узлов-тегов достаём из outer ctx.
      this2.withAccUpdatedFut { acc0 =>
        for {
          ctxOuter <- acc0.ctxOuterFut
        } yield {
          val agtEdgesIter = tagItems
            .iterator
            .toSeq
            .groupBy(_.geoShape.get)
            // Конвертим группы в отдельные эджи.
            .iterator
            .map { case (gs, gsItems) =>
              // Сборка всех tag face'ов.
              val tagFacesSet = gsItems
                .iterator
                .flatMap(_.tagFaceOpt)
                .toSet

              // Надо собрать опорные точки для общей статистики, записав их рядышком.
              val geoPoints = grabGeoPoints4Stats( gsItems )
                .toSet
                .toSeq

              val nodeIdsSet = tagFacesSet
                .iterator
                .flatMap { tagFace =>
                  val tnOpt = ctxOuter.tagNodesMap.get(tagFace)
                  val tnIdOpt = tnOpt.flatMap(_.id)
                  // If tag is missing in state, do crash: logical error here and it need attention.
                  if (tnIdOpt.isEmpty)
                    throw new IllegalStateException(s"$logPrefix No tag-node found for tag-face or _id missing: $tnOpt")
                  tnIdOpt
                }
                .toSet

              MEdge(
                predicate = predicate,
                nodeIds   = nodeIdsSet,
                info      = MEdgeInfo(
                  tags      = tagFacesSet,
                  geoShapes = MEdgeGeoShape(
                    id      = MEdgeGeoShape.SHAPE_ID_START,
                    glevel  = MNodeGeoLevels.geoTag,
                    shape   = gs
                  ) :: Nil,
                  geoPoints = geoPoints
                )
              )
            }

          acc_node_edges_out_LENS
            .modify(_ ++ agtEdgesIter)(acc0)
        }
      }

    } else {
      this2
    }
  }


}

/** Интерфейс для DI-поля, содержащего инстанс [[AdvBuilderUtil]]. */
trait IAdvBuilderUtilDi {
  def advBuilderUtil: AdvBuilderUtil
}
