package util.n2u

import io.suggest.common.empty.OptionUtil
import io.suggest.img.MImgFmts
import io.suggest.jd.{MEdgePicInfo, MJdEdge, MJdEdgeVldInfo}
import io.suggest.model.n2.edge.{EdgeUid_t, MPredicates}
import io.suggest.model.n2.media.{MMedia, MMediasCache}
import io.suggest.model.n2.node.{MNode, MNodeTypes, MNodesCache}
import io.suggest.scalaz.{ScalazUtil, StringValidationNel}
import io.suggest.util.logs.MacroLogsImpl
import javax.inject.{Inject, Singleton}
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
@Singleton
class N2VldUtil @Inject()(
                           mMediasCache               : MMediasCache,
                           mNodesCache                : MNodesCache,
                           implicit private val ec    : ExecutionContext
                         )
  extends MacroLogsImpl
{

  /** Извлечь данные по картинкам из карты эджей.
    *
    * @param edges Эджи.
    * @return Мапа: id эджа -> nodeId картинки.
    */
  def collectNeededImgIds(edges: TraversableOnce[MJdEdge]): Map[EdgeUid_t, MDynImgId] = {
    // Формат дефолтовый, потому что для оригинала он игнорируется, и будет перезаписан в imgsNeededMap()
    val imgFmtDflt = MImgFmts.default
    val needImgsIter = for {
      e <- edges.toIterator
      if e.predicate ==>> MPredicates.JdContent.Image
      fileSrv <- e.fileSrv
    } yield {
      e.id -> MDynImgId(fileSrv.nodeId, dynFormat = imgFmtDflt)
    }
    needImgsIter
      .toMap
  }


  /** Собрать ноды, упомянутые в исходном списке эджей. */
  def edgedNodes(edges: TraversableOnce[MJdEdge]): Future[Map[String, MNode]] = {
    // Собрать данные по всем упомянутым в запросе узлам, не обрывая связь с исходными эджами.
    val edgeNodeIds = (for {
      jdEdge  <- edges.toIterator
      fileSrv <- jdEdge.fileSrv
    } yield {
      fileSrv.nodeId
    })
      .toSet

    // Поискать узлы, упомянутые в этих эджах.
    mNodesCache.multiGetMap( edgeNodeIds )
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


  /** Чтение media-инстансов из БД.
    *
    * @param edgeImgIdsMap выхлоп collectNeededImgIds().values
    * @return Фьючерс с картой MMedia.
    */
  def imgsMedias(edgeImgIdsMap: TraversableOnce[MDynImgId]): Future[Map[String, MMedia]] = {
    mMediasCache.multiGetMap(
      // Собрать id запрашиваемых media-оригиналов.
      edgeImgIdsMap
        .toIterator
        .map(_.mediaId)
        .toSet
    )
  }


  /** Сборка карты картинок, которые требуются для эджей с учётом форматов и всего такого.
    *
    * @param imgsMediasMap Выхлоп imgsMedias()
    * @param edge2imgId Выхлоп collectNeededImgIds()
    * @return Карта эджей и готовых к использованию данных по картинкам.
    */
  def imgsNeededMap(imgsMediasMap: Map[String, MMedia], edge2imgId: TraversableOnce[(EdgeUid_t, MDynImgId)]): Map[EdgeUid_t, MImg3] = {
    val iter2 = for {
      (edgeUid, dynImgId) <- edge2imgId.toIterator
      mmedia <- imgsMediasMap.get( dynImgId.mediaId )
      imgFormat <- mmedia.file.imgFormatOpt
    } yield {
      val dynImgId2 = dynImgId.withDynFormat( imgFormat )
      val mimg = MImg3( dynImgId2 )
      edgeUid -> mimg
    }
    iter2.toMap
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
                     mediasMap      : Map[String, MMedia]
                   ): Map[EdgeUid_t, MJdEdgeVldInfo] = {
    lazy val logPrefix = s"validateEdges()[${System.currentTimeMillis()}]:"

    jdEdges
      .iterator
      .map { jdEdge =>
        val nodeIdOpt = jdEdge.fileSrv.map(_.nodeId)
        val vldEdge = MJdEdgeVldInfo(
          jdEdge = jdEdge,
          img    = OptionUtil.maybe( jdEdge.predicate ==>> MPredicates.JdContent.Image ) {
            val mmediaOpt = imgsNeededMap
              .get( jdEdge.id )
              .flatMap { mimg3 =>
                mediasMap.get( mimg3.dynImgId.mediaId )
              }

            MEdgePicInfo(
              isImg = nodeIdOpt
                .flatMap { nodesMap.get }
                .exists { _.common.ntype ==* MNodeTypes.Media.Image },
              imgWh = mmediaOpt
                .flatMap( _.picture.whPx ),
              dynFmt = mmediaOpt
                .flatMap( _.file.imgFormatOpt )
            )
          }
        )
        LOGGER.trace(s"$logPrefix Edge#${jdEdge.id}, nodeId#${nodeIdOpt.orNull} img=>${vldEdge.img}")
        jdEdge.id -> vldEdge
      }
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

