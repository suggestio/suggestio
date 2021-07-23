package util.adv.geo.tag

import javax.inject.{Inject, Singleton}
import akka.stream.scaladsl.{Keep, Sink}
import io.suggest.geo.MNodeGeoLevels
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.n2.edge._
import io.suggest.n2.edge.search.{Criteria, TagCriteria}
import io.suggest.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.n2.node.{MNode, MNodeTypes, MNodes}
import io.suggest.n2.node.common.MNodeCommon
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.streams.StreamsUtil
import io.suggest.util.JmxBase
import io.suggest.util.logs.MacroLogsImpl
import models.adv.build.MCtxOuter
import io.suggest.enum2.EnumeratumUtil.ValueEnumEntriesOps
import io.suggest.es.model.{EsModel, MEsNestedSearch}
import models.mcron.MCronTask
import models.mproj.ICommonDi
import play.api.inject.Injector
import util.cron.ICronTasksProvider

import scala.concurrent.{ExecutionContext, Future}
import scala.concurrent.duration._
import scala.util.{Failure, Success}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.03.16 17:06
  * Description: Для работы системы поиска тегов/геотегов требуется актуальный индекс узлов n2,
  * содержащий все необходимые узлы-теги.
  *
  * В виду особенностей архитектуры тегов, теги жиреют из-за сброса туда горы географических данных.
  * Поэтому API предпочитает работать с индексами и id, по возможности без обращения к _source.
  * Сброс хлама в теги необходим для поиска тегов.
  */
class GeoTagsUtil @Inject() (
                              mCommonDi     : ICommonDi,
                            )
  extends MacroLogsImpl
{

  import mCommonDi.current.injector

  private lazy val esModel = injector.instanceOf[EsModel]
  private lazy val mNodes = injector.instanceOf[MNodes]
  private lazy val mItems = injector.instanceOf[MItems]
  private lazy val streamsUtil = injector.instanceOf[StreamsUtil]

  import mCommonDi._
  import slick.profile.api._
  import mNodes.Implicits.elSourcingHelper
  import esModel.api._


  /** Предикат эджей, используемых в рамках этого модуля. */
  private def _PRED = MPredicates.TaggedBy.Self

  private def TAG_ITEM_TYPES = {
    // Поддержка разных тегов: географических и "прямых".
    MItemTypes
      .tagTypes
      .onlyIds
      .toList
  }

  /**
    * Создание тегов из указанного множества с сбор id узлов.
    * Если тег уже существует, то вернуть его id.
    *
    * @param tags Исходное множество тегов.
    * @return Карта с названиями исходных тегов и id узлов n2.
    */
  def ensureTags(tags: Set[String]): Future[Map[String, MNode]] = {
    lazy val logPrefix = s"ensureTags( [${tags.size}]=[${tags.mkString(" | ")}] )"
    if (tags.isEmpty) {
      LOGGER.trace(s"$logPrefix Nothing to do: ${tags.size} tags requested")
      Future.successful( Map.empty )

    } else {
      LOGGER.trace(s"$logPrefix Start ensuring...")
      for {
        tNodes <- {
          Future.traverse(tags) { tagFace =>
            for (tagNode <- ensureTag(tagFace)) yield {
              tagFace -> tagNode
            }
          }
        }
      } yield {
        tNodes.toMap
      }
    }
  }


  /**
    * Поиск id узла-тега в по точному имени тега.
    *
    * @param tagFace Название тега.
    * @return Фьючерс с опциоальным id тега-узла.
    */
  def findTagNode(tagFace: String): Future[Option[MNode]] = {
    lazy val logPrefix = s"findTagNode($tagFace):"
    LOGGER.trace(s"$logPrefix Looking for node of tag#[[$tagFace]]")

    val msearch = new MNodeSearch {
      override val outEdges: MEsNestedSearch[Criteria] = {
        val tcr = TagCriteria(
          face      = tagFace,
          isPrefix  = false,
          exact     = true
        )
        val cr = Criteria(
          predicates  = _PRED :: Nil,
          tags        = tcr :: Nil
        )
        MEsNestedSearch.plain( cr )
      }

      override val nodeTypes = MNodeTypes.Tag :: Nil

      // Берём поиск с запасом. Возможно появление дублирующихся результатов поиска, и на это надо будет реагировать.
      // Когда всё будет отлажено, тут можно ставить limit=1, и искать через dynSearchOne() и подобных.
      override def limit = 2
    }

    for {
      tagNodes <- mNodes.dynSearch(msearch)
    } yield {
      if (tagNodes.lengthCompare(1) > 0) {
        LOGGER.warn(s"$logPrefix Too many tag-nodes found for single-tag request:\n ${tagNodes.iterator.map(n => n.idOrNull -> n.guessDisplayName.orNull).mkString(", ")}")
      }
      // TODO Нужно запускать тут мерж tag-узлов при выявлении коллизии: 2+ узлов относяться к одному и тому же тегу.
      val r = tagNodes.headOption
      LOGGER.trace(s"$logPrefix Found ${tagNodes.size} nodes, returning node#${r.flatMap(_.id)}")
      r
    }
  }


  /**
    * Убедиться, что узел для указанного тега существует и вернуть id узла.
    *
    * @param tagFace tag face.
    * @return Фьючерс с id узла-тега.
    */
  def ensureTag(tagFace: String): Future[MNode] = {
    lazy val logPrefix = s"ensureTag($tagFace):"
    LOGGER.trace(s"$logPrefix Ensuring tag [[$tagFace]]...")

    findTagNode(tagFace)
      .map(_.get)
      .recoverWith { case _: NoSuchElementException =>
        // Собрать заготовку узла.
        val tagNode0 = MNode(
          common = MNodeCommon(
            ntype       = MNodeTypes.Tag,
            isDependent = true
          ),
          meta = MMeta(
            basic = MBasicMeta()
          ),
          edges = MNodeEdges(
            out = MNodeEdges.edgesToMap(
              MEdge(
                predicate = _PRED,
                info = MEdgeInfo(
                  tags = Set.empty + tagFace,
                )
              )
            )
          )
        )

        LOGGER.trace(s"$logPrefix Tag not exists, creating new one...")

        // Запустить сохранение нового узла.
        mNodes
          .saveReturning(tagNode0)
          .andThen {
            case Success(tagNode) => LOGGER.info(s"$logPrefix Created NEW node#${tagNode.idOrNull} for tag#${tagFace}")
            case Failure(ex)     => LOGGER.error(s"$logPrefix Unable to create tag-node", ex)
          }
      }
  }


  /** Common code between prepareInstall() and prepareUninstall().
    * Generate content.
    * @param itemsSql Select query for collecting items to be processed.
    * @param getTagIdOrFace Rep for get tag_node_id or tag_face column.
    * @param tagsToNodes Getter for tag strings.
    * @return Future with tags nodes map (wrapped into context by now).
    */
  private def _prepareTagsCtx(
                               itemsSql: Query[MItems#MItemsTable, MItem, Seq],
                               getTagIdOrFace: MItems#MItemsTable => Rep[Option[String]],
                               tagsToNodes: Set[String] => Future[Map[String, MNode]],
                             ): Future[MCtxOuter] = {
    val startTs = System.currentTimeMillis
    lazy val logPrefix = s"prepareTagsCtx($startTs):"

    for {
      // Найти все теги, которые затрагиваются грядующим инсталлом.
      tagFacesOpts <- slick.db.run {
        // We need ALL tag faces (online and offline) related to all nodes to be processed.
        // So, using node ids set, we collecting all active tag names.
        // TODO Opt: Maybe, also to collect tag_node_id here, and pass it into ensureTags (via tagToNodes)?
        mItems.query
          .filter { item =>
            (item.nodeId in itemsSql.map(_.nodeId)) &&
            (item.iTypeStr inSet TAG_ITEM_TYPES) &&
            (item.statusStr inSet MItemStatuses.advBusyApprovedIds.toSet)
          }
          .map(getTagIdOrFace)
          .distinct
          .result
      }

      // Создать множество недублирующихся тегов.
      tagFaces = {
        val r = tagFacesOpts
          .iterator
          .flatten
          .toSet

        // Залоггировать результат, если он есть.
        val rSize = r.size
        if (rSize > 0)
          LOGGER.trace(s"$logPrefix Found $rSize tag faces. First tagFace = ${r.head}")

        r
      }

      // Собрать карту узлов-тегов, создав при необходимости какие-то новые узлы-теги.
      tagNodesMap <- tagsToNodes(tagFaces)

    } yield {
      // Do not render periodical garbage, if nothing to do here:
      def haveMapLogMsg = s"$logPrefix Have Map[tagFace,node] with ${tagNodesMap.size} keys. Took ${System.currentTimeMillis - startTs}ms."
      if (tagNodesMap.isEmpty) LOGGER.trace(haveMapLogMsg)
      else LOGGER.debug(haveMapLogMsg)

      // If trace, render full map into logs:
      if (tagNodesMap.nonEmpty)
        LOGGER.trace(s"$logPrefix Tag nodes map contains:\n ${tagNodesMap.view.mapValues(_.id.orNull).mkString("\n ")}")

      // Собрать и вернуть результат.
      MCtxOuter(
        tagNodesMap = tagNodesMap,
      )
    }
  }


  /**
    * Подготовка данных и внешнего контекста для билдера, который будет содержать дополнительные данные,
    * необходимые для работы внутри самого билдера.
    *
    * @param itemsSql Заготовка запроса поиска
    * @return Фьючерс с outer-контекстом для дальнейшей передачи его в билдер.
    *         Карта tag-нод в outer-контексте имеет tagFace-ключи.
    */
  def prepareInstallNew(itemsSql: Query[MItems#MItemsTable, MItem, Seq]): Future[MCtxOuter] = {
    LOGGER.trace("prepareInstallNew():")
    _prepareTagsCtx( itemsSql, _.tagFaceOpt, ensureTags )
  }


  /**
    * Окончание инсталляции новых item'ов.
    * Нужно отребилдить теги, затронутые сделанными изменениями.
    *
    * @param ctxOuterFut Результат prepareInstallNew().
    * @return Фьючер без полезных данных внутри.
    */
  def afterInstallNew(ctxOuterFut: Future[MCtxOuter]): Future[_] = {
    _after(ctxOuterFut, "afterInstallNew")
  }


  /**
    * Ребилдинг одного тега.
    *
    * @param mnode Исходный инстанс тега.
    * @return Фьючерс с результатом ребилда тега-узла.
    *         Left(None) = tag node deleted.
    *         Right(None) = not changed
    *         Right(Some()) = tag node updated.
    */
  def rebuildTag(mnode: MNode): Future[Either[None.type, Option[MNode]]] = {
    import streamsUtil.Implicits._

    val mnodeId = mnode.id.get

    val startTs = System.currentTimeMillis()
    lazy val logPrefix = s"rebuildTag(#${mnode.guessDisplayName.orNull} $mnodeId)#$startTs:"
    LOGGER.trace(s"$logPrefix Starting")

    // Для наиболее оптимального сбора данных тега, параллельно и реактивно собираем данные сразу с нескольких колонок.
    // Общий код SQL-запроса здесь:
    val basicQuery = mItems.query
      .filter { i =>
        (i.tagNodeIdOpt === mnodeId) &&
        (i.statusStr === MItemStatuses.Online.value) &&
        (i.iTypeStr inSet TAG_ITEM_TYPES)
      }

    // Запуск сбора шейпов для geo-тегов.
    val (shapesCountFut, shapesFut) = slick.db
      .stream {
        basicQuery
          .filter(_.geoShapeStrOpt.nonEmpty)
          // https://github.com/slick/slick/issues/2246#issuecomment-853378559
          .distinctOn( _.geoShapeStrOpt )
          .map(_.geoShapeOpt)
          .result
          .forPgStreaming(10)
      }
      .toSource
      .mapConcat(_.toList)
      .zipWithIndex
      .alsoToMat( streamsUtil.Sinks.count )(Keep.right)
      .map { case (s, i) =>
        MEdgeGeoShape(
          id      = i.toInt + MEdgeGeoShape.SHAPE_ID_START,
          glevel  = MNodeGeoLevels.geoTag,
          shape   = s
        )
      }
      .toMat( Sink.collection[MEdgeGeoShape, List[MEdgeGeoShape]] )(Keep.both)
      .run()

    // Запуск сбора id ресиверов для direct-тегов
    val directTagRcvrsFut = slick.db
      .stream {
        basicQuery
          .filter(_.rcvrIdOpt.nonEmpty)
          .map(_.rcvrIdOpt)
          .distinct
          .result
          .forPgStreaming(30)
      }
      .toSource
      .mapConcat(_.toList)
      .toMat( Sink.collection[String, Set[String]] )(Keep.right)
      .run()

    for {
      // Дождаться окончания поиска шейпов для тега.
      tagShapes       <- shapesFut
      tagShapesCount  <- shapesCountFut

      directTagRcvrs <- {
        LOGGER.trace(s"$logPrefix $tagShapesCount different shapes found:\n ${tagShapes.mkString("\n ")}")
        directTagRcvrsFut
      }

      // Залить собранные шейпы в узел тега.
      mnodeOpt2 <- {
        LOGGER.trace(s"$logPrefix Found ${directTagRcvrs.size} direct-tag rcvrs: [${directTagRcvrs.mkString(", ")}]")

        val p = _PRED
        val someShapesCount = Some(tagShapesCount)

        if (directTagRcvrs.isEmpty && tagShapes.isEmpty) {
          // Нет данных для self-тега. Удаляем его сразу:
          for {
            isDeleted <- mNodes.deleteById( mnode.id.get )
          } yield {
            LOGGER.info(s"$logPrefix Empty tag node deleted ok?$isDeleted")
            Left( None )
          }

        } else {
          mNodes
            .tryUpdate(mnode) { mnode0 =>
              val e0Opt = mnode0
                .edges
                .withPredicateIter(p)
                .nextOption()

              // Собрать единый эдж self-тега для всех геошейпов.
              val e1 = e0Opt.fold {
                // Should never happen.
                LOGGER.warn(s"$logPrefix Tag-node with missing self-tag edge. Repairing...")
                MEdge(
                  predicate = p,
                  order = someShapesCount,
                  info = MEdgeInfo(
                    geoShapes = tagShapes
                  ),
                  nodeIds = directTagRcvrs
                )
              } { e0 =>
                e0.copy(
                  order = someShapesCount,
                  info = e0.info.copy(
                    geoShapes = tagShapes
                  ),
                  nodeIds = directTagRcvrs
                )
              }
              LOGGER.trace(s"$logPrefix Updated self-tag edge: $e1")

              if (e0Opt contains[MEdge] e1) {
                // Новый и старый эджи эквивалентны. Значит, обновлять этот tag-node не требуется.
                LOGGER.trace(s"$logPrefix Self-tag edge not modified. Do not update")
                null
              } else {
                // Есть изменения. Заливаем в инстанс MNode и сохраняем:
                MNode.edges.modify { edges0 =>
                  MNodeEdges.out.set(
                    MNodeEdges.edgesToMap1(
                      edges0
                        .withoutPredicateIter(p)
                        .++( e1 :: Nil )
                    )
                  )(edges0)
                }(mnode0)
              }
            }
            // Надо помнить, что tryUpdate() тут может вернуть null.
            .map { mnodeOrNull =>
              // If tryUpdate() returns null, it means update has been ignored.
              Right( Option( mnodeOrNull ) )
            }
        }
      }

    } yield {
      LOGGER.trace(s"$logPrefix Tag rebuilded, took ${System.currentTimeMillis - startTs}ms. savedOrDeleted?${mnodeOpt2.isRight}")
      mnodeOpt2
    }
  }


  /** Запуск параллельного ребилда пачки узлов-тегов.
    *
    * @param tagNodes Узлы-теги перед ребилдом.
    * @return Фьючерс со списком отребилденных тегов.
    */
  def rebuildTags(tagNodes: Iterable[MNode]): Future[Seq[MNode]] = {
    lazy val logPrefix = s"rebuildTag([${tagNodes.size}]):"
    if (tagNodes.isEmpty) {
      LOGGER.trace(s"$logPrefix Nothing to do")
      Future.successful( Nil )
    } else {
      LOGGER.trace(s"$logPrefix Rebuild tag nodes:\n [${tagNodes.map(_.idOrNull).mkString(" | ")}]")
      for {
        res <- Future.traverse(tagNodes)(rebuildTag)
      } yield {
        // Явно ленивый список обновлённых тегов. Результат этого метода игнорируется (кроме logger.trace).
        val r = res
          .iterator
          .flatMap(_.toOption)
          .flatten
          .to( LazyList )
        LOGGER.trace(s"$logPrefix Done, ${r.size} tag ids returned:\n [${r.iterator.map(_.orNull).mkString(" | ")}]")
        r
      }
    }
  }


  /** Пересобрать все существующие узлы-теги.
    * Пустые теги будут удалены.
    *
    * @return Кол-во обновлённый и кол-во удалённых тегов-узлов.
    */
  def rebuildAllTagNodes(): Future[(Int, Int)] = {
    val logPrefix = "rebuildAllTagNodes():"
    LOGGER.trace(s"$logPrefix Starting")
    mNodes
      .source( (new AllTagNodesSearch).toEsQuery )
      // Пересобираем все теги одновременно.
      .mapAsyncUnordered(10) { mnode =>
        rebuildTag(mnode)
      }
      .runFold( (0,0) ) { case (acc0 @ (countUpdated, countDeleted), mnodeOptE) =>
        LOGGER.trace(s"$logPrefix Folding... (updated=$countUpdated deleted=${countDeleted}) node#${mnodeOptE.toOption.flatten.flatMap(_.id).orNull}")
        mnodeOptE.fold(
          _ => (countUpdated, countDeleted + 1),
          _.fold( acc0 )( _ => (countUpdated + 1, countDeleted) )
        )
      }
  }


  /**
    * Подготовка outer-контекста к деинсталляции тегов, связанных с item'ами.
    *
    * @param itemsSql Выборка item'ов, которые будут деинсталлированы.
    * @return Фьючерс с готовым outer-контекстом.
    *         Карта узлов-тегов содержит nodeId в качестве ключей.
    */
  def prepareUnInstall(itemsSql: Query[MItems#MItemsTable, MItem, Seq]): Future[MCtxOuter] = {
    LOGGER.trace("prepareUnInstall():")
    _prepareTagsCtx( itemsSql, _.tagNodeIdOpt, mNodes.multiGetMapCache )
  }


  /**
    * Выполнить пост-деинсталляционные действа: отребилдить теги, затронутые общей вакханалией.
    *
    * @param ctxOuterFut outer-контекст билдера.
    * @return Фьючерс без полезной нагрузки.
    */
  def afterUnInstall(ctxOuterFut: Future[MCtxOuter]): Future[_] = {
    _after(ctxOuterFut, "afterUnInstall")
  }

  /**
    * Код afterInstallNew() и afterUnInstall() чрезвычайно похож, поэтому он вынесен в отдельный метод.
    *
    * @param ctxOuterFut Фьючерс outer-контекста билдера.
    * @param logPrefixPrefix Название текущего метода (для логгирования).
    * @return Фьючерс без какой-либо полезной нагрузки.
    */
  private def _after(ctxOuterFut: Future[MCtxOuter], logPrefixPrefix: String): Future[_] = {
    // Необходимо перекомпилить теги, которые были затронуты всем этим действом
    val fut = for {
      ctxOut    <- ctxOuterFut
      tmap      = ctxOut.tagNodesMap
      if tmap.nonEmpty
      tmapSize  = tmap.size
      startTs   = System.currentTimeMillis()
      logPrefix = s"$logPrefixPrefix($tmapSize $startTs):"
      _         <- {
        LOGGER.info(s"$logPrefix Starting tags rebuild, $tmapSize tags to go.")
        val rebuildTagsFut = rebuildTags(tmap.values)
        for (ex <- rebuildTagsFut.failed) {
          LOGGER.error(s"$logPrefix Failed to rebuild the tags", ex)
        }
        rebuildTagsFut
      }
    } yield {
      LOGGER.info(s"$logPrefix Rebuilt $tmapSize tags in ${System.currentTimeMillis - startTs}ms.")
    }

    // Подавляем оптимизацию if tmap.nonEmpty, приводящую к экзепшену.
    fut.recover {
      case _: NoSuchElementException =>
        // Nothing to do
    }
  }


  private class AllTagNodesSearch extends MNodeSearch {
    override def nodeTypes = MNodeTypes.Tag :: Nil
  }


  /**
    * Удаление всех узлов-тегов.
    *
    * @return Кол-во удаленных узлов.
    */
  def deleteAllTagNodes(): Future[Int] = {
    val msearch = new AllTagNodesSearch
    val scroller = mNodes.startScroll( msearch.toEsQueryOpt )
    mNodes.deleteByQuery(scroller)
  }

}

trait IGeoTagsUtilDi {
  def geoTagsUtil: GeoTagsUtil
}



trait GeoTagsUtilJmxMBean {

  def deleteAllTagNodes(): String

  def rebuildTagByNodeId(nodeId: String): String

  def ensureTag(tags: String): String

  def rebuildAllTagNodes(): String

}


@Singleton
final class GeoTagsUtilJmx @Inject() (
                                       injector: Injector,
                                     )
  extends JmxBase
  with GeoTagsUtilJmxMBean
  with ICronTasksProvider
{

  private def esModel = injector.instanceOf[EsModel]
  private def geoTagsUtil = injector.instanceOf[GeoTagsUtil]
  private def mNodes = injector.instanceOf[MNodes]
  implicit private def ec = injector.instanceOf[ExecutionContext]

  import JmxBase._

  override def _jmxType = Types.UTIL

  override def deleteAllTagNodes(): String = {
    val fut = for {
      countDeleted <- geoTagsUtil.deleteAllTagNodes()
    } yield {
      s"Deleted $countDeleted tag nodes."
    }
    awaitString(fut)
  }

  override def rebuildTagByNodeId(nodeId: String): String = {
    val _esModel = esModel
    import _esModel.api._

    val fut = for {
      mnodeOpt <- mNodes
        .getByIdCache(nodeId)
        .withNodeType(MNodeTypes.Tag)
      mnode = mnodeOpt.get
      mnodeOpt2 <- geoTagsUtil.rebuildTag( mnode )
    } yield {
      s"Done.\n\n\n$mnodeOpt2"
    }
    awaitString( fut )
  }

  override def ensureTag(tag: String): String = {
    val fut = for {
      mnode <- geoTagsUtil.ensureTag( tag )
    } yield {
      s"ok, nodeId = ${mnode.id.orNull}\n\n\n$mnode"
    }
    awaitString( fut )
  }

  override def rebuildAllTagNodes(): String = {
    val fut = for {
      (countUpdated, countDeleted) <- geoTagsUtil.rebuildAllTagNodes()
    } yield {
      s"Updated = $countUpdated, Deleted = $countDeleted"
    }
    awaitString( fut )
  }

  /** Cron task for periodical rebuilding of all current tags.
    * Placed into JMX, because GeoTagsUtil is not a singleton by-design.
    * Current JMX class is a singleton.
    *
    * This cron-related stuff here due to previous problems with tags.
    * TODO Possibly, it is not actual anymore and may be removed or make not so frequent.
    */
  override def cronTasks(): Iterable[MCronTask] = {
    val rebuildAllCron = MCronTask(
      startDelay  = 1.minutes,
      every       = 15.minutes,
      displayName = "rebuildAllTags",
    )( geoTagsUtil.rebuildAllTagNodes )

    rebuildAllCron :: Nil
  }

}
