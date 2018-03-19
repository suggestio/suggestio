package util.adv.geo.tag

import javax.inject.Inject

import akka.stream.scaladsl.{Keep, Sink}
import io.suggest.es.util.SioEsUtil
import io.suggest.geo.MNodeGeoLevels
import io.suggest.mbill2.m.item.{MItem, MItems}
import io.suggest.mbill2.m.item.status.MItemStatuses
import io.suggest.mbill2.m.item.typ.MItemTypes
import io.suggest.model.n2.edge._
import io.suggest.model.n2.edge.search.{Criteria, ICriteria, TagCriteria}
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodes, MNodesCache}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.primo.id.OptId
import io.suggest.streams.StreamsUtil
import io.suggest.util.JMXBase
import io.suggest.util.logs.{MacroLogsImpl, MacroLogsImplLazy}
import models.adv.build.MCtxOuter
import io.suggest.enum2.EnumeratumUtil.ValueEnumEntriesOps
import models.mproj.ICommonDi

import scala.concurrent.{ExecutionContext, Future}
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
                              mNodes        : MNodes,
                              mItems        : MItems,
                              streamsUtil   : StreamsUtil,
                              mCommonDi     : ICommonDi
                            )
  extends MacroLogsImpl
{

  import LOGGER._
  import mCommonDi._
  import slick.profile.api._
  import streamsUtil.Implicits._

  /** Предикат эджей, используемых в рамках этого модуля. */
  private def _PRED = MPredicates.TaggedBy.Self

  private def TAG_ITEM_TYPES = {
    // Поддержка разных тегов: географических и "прямых".
    MItemTypes
      .tagTypes
      .onlyIds
      .toTraversable
  }

  /**
    * Создание тегов из указанного множества с сбор id узлов.
    * Если тег уже существует, то вернуть его id.
    *
    * @param tags Исходное множество тегов.
    * @return Карта с названиями исходных тегов и id узлов n2.
    */
  def ensureTags(tags: Set[String]): Future[Map[String, MNode]] = {
    if (tags.isEmpty) {
      Future.successful( Map.empty )

    } else {
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

    val msearch = new MNodeSearchDfltImpl {
      override def outEdges: Seq[ICriteria] = {
        val tcr = TagCriteria(
          face      = tagFace,
          isPrefix  = false,
          exact     = true
        )
        val cr = Criteria(
          predicates  = _PRED :: Nil,
          tags        = tcr :: Nil
        )
        cr :: Nil
      }

      override def nodeTypes = MNodeTypes.Tag :: Nil

      // Берём поиск с запасом. Возможно появление дублирующихся результатов поиска, и на это надо будет реагировать.
      // Когда всё будет отлажено, тут можно ставить limit=1, и искать через dynSearchOne() и подобных.
      override def limit = 2
    }

    for (tagNodes <- mNodes.dynSearch(msearch)) yield {
      if (tagNodes.lengthCompare(1) > 0)
        warn(s"$logPrefix Too many tag-nodes found for single-tag request: ${tagNodes.mkString(", ")}, ...")
      // TODO Нужно запускать тут мерж tag-узлов при выявлении коллизии: 2+ узлов относяться к одному и тому же тегу.
      tagNodes.headOption
    }
  }


  /**
    * Убедиться, что узел для указанного тега существует и вернуть id узла.
    *
    * @param tagFace tag face.
    * @return Фьючерс с id узла-тега.
    */
  def ensureTag(tagFace: String): Future[MNode] = {
    val findTagFut = findTagNode(tagFace)
    lazy val logPrefix = s"ensureTag($tagFace):"

    findTagFut
      .map(_.get)
      .recoverWith { case _: NoSuchElementException =>

        val e = MEdge(
          predicate = _PRED,
          info = MEdgeInfo(
            tags = Set(tagFace)
          )
        )

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
            out = MNodeEdges.edgesToMap(e)
          )
        )

        trace(s"$logPrefix Tag not exists, creating new one: $tagNode0")

        // Запустить сохранение нового узла.
        val saveFut = mNodes.save(tagNode0)

        saveFut.onComplete {
          case Success(nodeId) => info(s"$logPrefix Created NEW node[$nodeId] for tag")
          case Failure(ex)     => error(s"$logPrefix Unable to create tag-node", ex)
        }

        for (tagId <- saveFut) yield {
          tagNode0.copy(
            id          = Some(tagId),
            versionOpt  = Some( SioEsUtil.DOC_VSN_0 )
          )
        }
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
    val startTs = System.currentTimeMillis
    lazy val logPrefix = s"prepareInstallNew($startTs):"

    for {
      // Найти все теги, которые затрагиваются грядующим инсталлом.
      tagFacesOpts <- slick.db.run {
        itemsSql
          .filter(_.iTypeStr inSet TAG_ITEM_TYPES)
          .map(_.tagFaceOpt)
          .distinct
          .result
      }

      // Создать множество недублирующихся тегов.
      tagFaces = {
        val r = OptId.optIds2ids(tagFacesOpts).toSet

        // Залоггировать результат, если он есть.
        val rSize = r.size
        if (rSize > 0)
          trace(s"$logPrefix Found $rSize tag faces. First tagFace = ${r.head}")

        r
      }

      // Собрать карту узлов-тегов, создав при необходимости какие-то новые узлы-теги.
      gtMap <- ensureTags(tagFaces)

    } yield {

      val mSize = gtMap.size
      if (mSize > 0)
        debug(s"$logPrefix Have Map[tagFace,node] with $mSize keys. Took ${System.currentTimeMillis - startTs}ms.")

      // Собрать и вернуть результат.
      MCtxOuter(
        tagNodesMap = gtMap
      )
    }
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
    */
  def rebuildTag(mnode: MNode): Future[MNode] = {
    val mnodeId = mnode.id.get

    val startTs = System.currentTimeMillis()
    lazy val logPrefix = s"rebuildTag(#${mnode.guessDisplayName.orNull} $mnodeId)#$startTs:"

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
          .map(_.geoShapeOpt)
          .distinct
          .result
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
      mnode2 <- {
        LOGGER.trace(s"$logPrefix Found ${directTagRcvrs.size} direct-tag rcvrs: [${directTagRcvrs.mkString(", ")}]")

        val p = _PRED
        val someShapesCount = Some(tagShapesCount)

        mNodes.tryUpdate(mnode) { mnode0 =>
          // Собрать единый эдж self-тега для всех геошейпов.
          val e0 = mnode0.edges
            .withPredicateIter(p)
            .toStream
            .head

          val e1 = e0.copy(
            order = someShapesCount,
            info = e0.info.copy(
              geoShapes = tagShapes
            ),
            nodeIds = directTagRcvrs ++ e0.nodeIds
          )

          mnode0.copy(
            edges = mnode0.edges.copy(
              out = {
                val iter = mnode0.edges
                  .withoutPredicateIter(p)
                  .++( e1 :: Nil )
                MNodeEdges.edgesToMap1( iter )
              }
            )
          )
        }
      }

    } yield {
      trace(s"$logPrefix Tag rebuilded, took ${System.currentTimeMillis - startTs}ms.")
      mnode2
    }
  }


  /** Запуск параллельного ребилда пачки узлов-тегов.
    *
    * @param tagNodes Узлы-теги перед ребилдом.
    * @return Фьючерс со списком отребилденных тегов.
    */
  def rebuildTags(tagNodes: Iterable[MNode]): Future[Iterable[MNode]] = {
    if (tagNodes.isEmpty) {
      Future.successful(tagNodes)
    } else {
      Future.traverse(tagNodes)(rebuildTag)
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
    val startTs = System.currentTimeMillis

    for {
      // Найти все теги, которые затрагиваются грядующим инсталлом.
      tagIdsOpts <- slick.db.run {
        itemsSql
          .filter(_.iTypeStr inSet TAG_ITEM_TYPES)
          .map(_.tagNodeIdOpt)
          .distinct
          .result
      }

      // Нормализовать множество id узлов-тегов.
      tagIds = OptId.optIds2ids(tagIdsOpts).toSet

      // Получить узлы через кеш
      tagNodesMap <- mNodesCache.multiGetMap(tagIds)

    } yield {
      val tnMapSize = tagNodesMap.size
      if (tnMapSize > 0)
        trace(s"prepareUnInstall(): Found $tnMapSize nodes for ${tagIds.size} tagIds. Took ${System.currentTimeMillis - startTs}ms.")

      MCtxOuter(
        tagNodesMap = tagNodesMap
      )
    }
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
        info(s"$logPrefix Starting tags rebuild, $tmapSize tags to go.")
        val rebuildTagsFut = rebuildTags(tmap.values)
        for (ex <- rebuildTagsFut.failed) {
          error(s"$logPrefix Failed to rebuild the tags", ex)
        }
        rebuildTagsFut
      }
    } yield {
      info(s"$logPrefix Rebuilt $tmapSize tags in ${System.currentTimeMillis - startTs}ms.")
    }

    // Подавляем оптимизацию if tmap.nonEmpty, приводящую к экзепшену.
    fut.recover {
      case _: NoSuchElementException =>
        // Nothing to do
    }
  }


  /**
    * Удаление всех узлов-тегов.
    *
    * @return Кол-во удаленных узлов.
    */
  def deleteAllTagNodes(): Future[Int] = {
    val msearch = new MNodeSearchDfltImpl {
      override def nodeTypes = MNodeTypes.Tag :: Nil
    }
    val scroller = mNodes.startScroll( msearch.toEsQueryOpt )
    mNodes.deleteByQuery(scroller)
  }

}



trait GeoTagsUtilJmxMBean {

  def deleteAllTagNodes(): String

  def rebuildTagByNodeId(nodeId: String): String

  def ensureTag(tags: String): String

}

class GeoTagsUtilJmx @Inject() (
                                 geoTagsUtil               : GeoTagsUtil,
                                 mNodesCache               : MNodesCache,
                                 override implicit val ec  : ExecutionContext
                               )
  extends JMXBase
  with GeoTagsUtilJmxMBean
  with MacroLogsImplLazy
{

  override def jmxName = "io.suggest:type=util,name=" + getClass.getSimpleName.replace("Jmx", "")

  override def deleteAllTagNodes(): String = {
    val fut = for (countDeleted <- geoTagsUtil.deleteAllTagNodes()) yield {
      s"Deleted $countDeleted tag nodes."
    }
    awaitString(fut)
  }

  override def rebuildTagByNodeId(nodeId: String): String = {
    val fut = for {
      mnodeOpt <- mNodesCache.getByIdType(nodeId, MNodeTypes.Tag)
      mnode = mnodeOpt.get
      mnode2 <- geoTagsUtil.rebuildTag( mnode )
    } yield {
      s"Done.\n\n\n$mnode2"
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

}

