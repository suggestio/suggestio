package util.n2u

import io.suggest.common.empty.OptionUtil
import io.suggest.es.model.EsModel
import io.suggest.jd.{MJdEdgeFileVldInfo, MJdEdge, MJdEdgeVldInfo}
import io.suggest.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.n2.node.search.MNodeSearch
import io.suggest.n2.node.{MNode, MNodeType, MNodeTypes, MNodes}
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.Inject
import models.im.{MDynImgId, MImg3}
import scalaz.std.iterable._
import scalaz.std.list._
import japgolly.univeq._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 18.04.18 15:40
  * Description: Утиль для валидации данных форм, содержащих edge-линковку.
  */
final class N2VldUtil @Inject()(
                                 esModel                    : EsModel,
                                 mNodes                     : MNodes,
                                 implicit private val ec    : ExecutionContext
                               )
  extends MacroLogsImpl
{

  import esModel.api._

  /** Извлечь данные по картинкам из карты эджей.
    *
    * @param edges Эджи.
    * @return Мапа: id эджа -> nodeId картинки.
    */
  def collectNeededImgIds(edges: IterableOnce[MJdEdge]): Map[EdgeUid_t, MDynImgId] = {
    (for {
      e         <- edges.iterator
      if e.predicate ==>> MPredicates.JdContent.Image
      nodeId    <- e.nodeId
      edgeUid   <- e.edgeDoc.id
    } yield {
      // Без img-формата, т.к. для оригинала он игнорируется, и будет перезаписан в imgsNeededMap()
      edgeUid -> MDynImgId( origNodeId = nodeId )
    })
      .toMap
  }


  /** Собрать ноды, упомянутые в исходном списке эджей. */
  def edgedNodes(edges: IterableOnce[MJdEdge]): Future[Map[String, MNode]] = {
    // Собрать данные по всем упомянутым в запросе узлам, не обрывая связь с исходными эджами.
    val edgeNodeIds = edges
      .iterator
      .flatMap(_.nodeId)
      .toSet

    // Поискать узлы, упомянутые в этих эджах.
    _findEdgeNodes( edgeNodeIds )
  }


  /** Ранняя синхронная поверхностная валидация jd-эджей.
    * Выполняется перед запуском ресурсоёмких проверок.
    */
  def earlyValidateEdges(edges: Iterable[MJdEdge]): StringValidationNel[List[MJdEdge]] = {
    // Ранняя валидация корректности присланных эджей:
    ScalazUtil.validateAll(edges) { jdEdge =>
      MJdEdge
        .validateForStore(jdEdge)
        .map(_ :: Nil)
    }
  }


  private def _findEdgeNodes(nodeIds: Set[String], ofNodeTypes: Seq[MNodeType] = Nil): Future[Map[String, MNode]] = {
    val _count = nodeIds.size
    def logPrefix = s"_findEdgeNodes(${_count}, [${ofNodeTypes.mkString(" ")}]):"

    for {
      allowedIds <- mNodes.dynSearchIds(
        new MNodeSearch {
          override def nodeTypes = ofNodeTypes
          // Собрать id запрашиваемых media-оригиналов:
          override val withIds = nodeIds.toSeq
          // Интересуют только enabled-узлы:
          override val isEnabled = Some(true)
          override def limit = _count
        }
      )
      resMap <- {
        LOGGER.trace(s"$logPrefix Imgs requested, ${allowedIds.size} nodes allowed.")
        mNodes.multiGetMapCache( allowedIds.toSet )
      }
    } yield {
      resMap
    }
  }

  /** Чтение img.media-инстансов из БД.
    *
    * @param edgeImgIds выхлоп collectNeededImgIds().values
    * @return Фьючерс с картой media MNodes.
    */
  private def imgsMedias(edgeImgIds: IterableOnce[MDynImgId]): Future[Map[String, MNode]] = {
    val nodeIdsRequested = edgeImgIds
      .iterator
      .map(_.mediaId)
      .toSet
    _findEdgeNodes( nodeIdsRequested, MNodeTypes.Media.Image :: Nil )
  }


  /** Сборка карты картинок, которые требуются для эджей с учётом форматов и всего такого.
    *
    * @param imgsMediasMap Выхлоп imgsMedias()
    * @param edge2imgId Выхлоп collectNeededImgIds()
    * @return Карта эджей и готовых к использованию данных по картинкам.
    */
  def imgsNeededMap(imgsMediasMap: Map[String, MNode], edge2imgId: IterableOnce[(EdgeUid_t, MDynImgId)]): Map[EdgeUid_t, MImg3] = {
    (for {
      (edgeUid, dynImgId) <- edge2imgId.iterator
      mmedia <- imgsMediasMap.get( dynImgId.mediaId )
      if mmedia.common.ntype ==* MNodeTypes.Media.Image
      fileEdge  <- mmedia.edges.withPredicateIter( MPredicates.Blob.File ).nextOption()
      mediaEdge <- fileEdge.media
      imgFormat2 = mediaEdge.file.imgFormatOpt
      if imgFormat2.nonEmpty
    } yield {
      val dynImgId2 = (MDynImgId.imgFormat set imgFormat2)( dynImgId )
      val mimg = MImg3( dynImgId2 )
      edgeUid -> mimg
    })
      .toMap
  }


  /** Валидация эджей.
    *
    * @param jdEdges Вообще все исходные эджи.
    * @param imgsNeededMap Выхлоп imgsNeededMap()
    * @param nodesMap Выхлоп edgedNodes()
    * @param mediasMap Выхлоп imgsMedias()
    * @return Карта эджей с доп.данными для проверки.
    */
  def validateEdges( jdEdges        : Iterable[MJdEdge],
                     imgsNeededMap  : Map[EdgeUid_t, MImg3],
                     nodesMap       : Map[String, MNode],
                     mediasMap      : Map[String, MNode]
                   ): Map[EdgeUid_t, MJdEdgeVldInfo] = {
    lazy val logPrefix = s"validateEdges()[${System.currentTimeMillis()}]:"

    (for {
      jdEdge <- jdEdges.iterator
      edgeUid <- jdEdge.edgeDoc.id

      fileNodeOpt = for {
        nodeId <- jdEdge.nodeId
        mnode <- nodesMap.get( nodeId )
      } yield mnode

      isJdImage = jdEdge.predicate ==>> MPredicates.JdContent.Image

      // Проверить, что файл-узел существует, когда он требуется по смыслу:
      // Это нужно, чтобы сейчас и позже правильно провалидировать эджи в ситуации, когда картинка есть в imgsNeededMap,
      // но отсутствует в nodesMap или mediasMap. В подобной ситуации, не должно быть никакого MEdgePicInfo(false, None, None),
      // а должен быть общий None - эдж невалиден, отсылки к нему из шаблона не валидны автоматически.
      if {
        if (isJdImage) {
          val r = fileNodeOpt
            .exists( _.common.ntype ==* MNodeTypes.Media.Image )
          if (!r) LOGGER.warn(s"$logPrefix Image edge $jdEdge invalid: Edge related to missing/invalid/unexpected node#${fileNodeOpt.flatMap(_.id).orNull} or type#${fileNodeOpt.map(_.common.ntype).orNull}")
          r
        } else {
          // Не image - связанный файл не обязателен.
          true
        }
      }

      // Если будут другие варианты в будущем, то тут можно объединять различные проверки:
      isImage = isJdImage
    } yield {
      val vldEdge = MJdEdgeVldInfo(
        jdEdge = jdEdge,
        file   = OptionUtil.maybe( isImage ) {
          val edgeMediaOpt = for {
            mimg       <- imgsNeededMap.get( edgeUid )
            fileNode   <- mediasMap.get( mimg.dynImgId.mediaId )
            // Извлечь данные искомого типа из эджа-файла.
            if {
              val ntyp = fileNode.common.ntype
              if (isImage) {
                ntyp ==* MNodeTypes.Media.Image
              } else {
                ntyp eqOrHasParent MNodeTypes.Media
              }
            }
            fileEdge  <- fileNode.edges
              .withPredicateIter( MPredicates.Blob.File )
              .nextOption()
            mediaEdge <- fileEdge.media
          } yield {
            LOGGER.trace(s"$logPrefix edge#$edgeUid => $mediaEdge")
            mediaEdge
          }

          MJdEdgeFileVldInfo(
            isImg = fileNodeOpt
              .nonEmpty,
            imgWh = edgeMediaOpt
              .flatMap( _.picture.whPx ),
            dynFmt = edgeMediaOpt
              .flatMap( _.file.imgFormatOpt ),
          )
        }
      )

      LOGGER.trace(s"$logPrefix Edge#${jdEdge.edgeDoc.id.orNull}, nodeId#${fileNodeOpt.orNull} img=>${vldEdge.file}")
      edgeUid -> vldEdge
    })
      .toMap
  }



  /** Логика работы валидатора эджей, пригодная для повторного использования. */
  case class EdgesValidator( edges    : Iterable[MJdEdge] ) {

    // Собрать данные по всем упомянутым в запросе узлам, не обрывая связь с исходными эджами.
    val edgedNodesMapFut = edgedNodes( edges )

    // Для валидации самого шаблона нужны данные по размерам связанных картинок. Поэтому залезаем в MMedia за оригиналами упомянутых картинок:
    val edge2imgIdMap = collectNeededImgIds( edges )

    val imgsMediasMapFut = imgsMedias( edge2imgIdMap.values )

    // Нужно, используя mmedia оригиналов картинок, собрать MImg3/MDynImgId с правильными форматами внутри:
    val imgsNeededMapFut = for {
      imgsMediasMap <- imgsMediasMapFut
    } yield {
      // Залить данные по форматам в исходную карту imgNeededMap
      imgsNeededMap( imgsMediasMap, edge2imgIdMap )
    }

    // Карта исходных эджей, где каждый эдж дополнен данными для валидации.
    val vldEdgesMapFut = for {
      imgsMediasMap <- imgsMediasMapFut
      edgedNodesMap <- edgedNodesMapFut
      imgsNeededMap <- imgsNeededMapFut
    } yield {
      validateEdges(
        jdEdges       = edges,
        imgsNeededMap = imgsNeededMap,
        nodesMap      = edgedNodesMap,
        mediasMap     = imgsMediasMap
      )
    }

  }

}

