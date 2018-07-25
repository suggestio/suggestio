package util.adv.build

import javax.inject.{Inject, Singleton}

import io.suggest.common.empty.OptionUtil
import io.suggest.geo.{MGeoPoint, MNodeGeoLevels}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.{MItemType, MItemTypes}
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.model.n2.edge.{MEdge, MEdgeGeoShape, MEdgeInfo, MNodeEdges}
import io.suggest.util.logs.MacroLogsImpl
import models.MPredicate
import models.adv.build.MCtxOuter
import models.mproj.ICommonDi
import util.adv.geo.tag.GeoTagsUtil
import util.billing.BillDebugUtil

import scala.concurrent.Future

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.16 21:35
  * Description: Утиль для AdvBuilder'а.
  */
@Singleton
class AdvBuilderUtil @Inject() (
                                 mItems           : MItems,
                                 geoTagsUtil      : GeoTagsUtil,
                                 billDebugUtil    : BillDebugUtil,
                                 mCommonDi        : ICommonDi
                               )
  extends MacroLogsImpl
{

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


  def clearByPredicate(b0: IAdvBuilder, preds: Seq[MPredicate]): IAdvBuilder = {
    // Вычистить теги из эджей карточки
    val acc2Fut = for {
      acc0 <- b0.accFut
    } yield {
      val mad2 = acc0.mnode.withEdges(
        acc0.mnode.edges.copy(
          out = {
            val iter = acc0.mnode
              .edges
              // Все теги и геотеги идут через биллинг. Чистка равносильна стиранию всех эджей TaggedBy.
              .withoutPredicateIter( preds: _* )
            MNodeEdges.edgesToMap1( iter )
          }
        )
      )
      // Сохранить почищенную карточку в возвращаемый акк.
      acc0.withMnode( mad2 )
    }
    b0.withAcc( acc2Fut )
  }


  /** Извлечение геоточек из MItems для нужд статистики.
    *
    * @param mitems Итемы биллинга или же итератор оных.
    * @return Итератор из награбленных точек.
    */
  def grabGeoPoints4Stats(mitems: TraversableOnce[MItem]): Iterator[MGeoPoint] = {
    mitems
      .toIterator
      .flatMap(_.geoShape)
      .map { gs =>
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
      itypes.contains( i.iType )
      // Тут была проверка на mitem.gsOpt, но это защита от самого себя и она была выпилена.
    }
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
          val b = acc0.mnode.meta.basic
          (b.nameOpt ++ b.nameShortOpt)
            .toSet
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
        acc0.withMnode(
          mnode = acc0.mnode.withEdges(
            acc0.mnode.edges.copy(
              out = {
                val iter = acc0.mnode.edges.iterator ++ (e :: Nil)
                MNodeEdges.edgesToMap1(iter)
              }
            )
          )
        )
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
            needInterruptItypes.contains( i.iType )
          }
          .map(_.nodeId)
          .toSet

        if (myNodeIds.nonEmpty) {
          LOGGER.trace(s"$logPrefix Will search for online items for nodes#[${myNodeIds.mkString(", ")}] to interruption...")
          val dbAction = billDebugUtil.findAndInterruptItemsLike(myNodeIds, itypes: _*)
          acc0
            .withDbActions(
              // ПЕРЕД выполнением остальных экшенов надо выполнить данную зачистку.
              dbActions = dbAction :: acc0.dbActions
            )
            .withInterruptedTypes(
              acc0.interruptedTypes ++ needInterruptItypes
            )
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
    * @param ditems item'ы, которые билдим.
    * @return Обновлённый инстанс [[IAdvBuilder]].
    */
  def tagsInstallSql(b0: IAdvBuilder, ditems: Iterable[MItem]): IAdvBuilder = {
    lazy val logPrefix = s"AGT.installSql(${ditems.size}):"
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
          acc0.withDbActions(
            dbActions = dbas1
          )
        }
      }
  }

}

/** Интерфейс для DI-поля, содержащего инстанс [[AdvBuilderUtil]]. */
trait IAdvBuilderUtilDi {
  def advBuilderUtil: AdvBuilderUtil
}
