package util.adv.geo.tag

import com.google.inject.Inject
import io.suggest.model.n2.edge.{MEdgeInfo, MEdge, MNodeEdges, MPredicates}
import io.suggest.model.n2.edge.search.{TagCriteria, Criteria, ICriteria}
import io.suggest.model.n2.node.meta.{MBasicMeta, MMeta}
import io.suggest.model.n2.node.{MNodeTypes, MNode}
import io.suggest.model.n2.node.common.MNodeCommon
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import io.suggest.util.SioEsUtil
import models.mproj.ICommonDi
import util.PlayMacroLogsImpl

import scala.concurrent.Future
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
  mCommonDi     : ICommonDi
)
  extends PlayMacroLogsImpl
{

  import LOGGER._
  import mCommonDi._

  /** Предикат эджей, используемых в рамках этого модуля. */
  private def _PRED = MPredicates.TaggedBy.Self


  /**
    * Создание тегов из указанного множества с сбор id узлов.
    * Если тег уже существует, то вернуть его id.
    *
    * @param tags Исходное множество тегов.
    * @return Карта с названиями исходных тегов и id узлов n2.
    */
  def ensureTags(tags: Set[String]): Future[Map[String, MNode]] = {
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


  /**
    * Поиск id узла-тега в по точному имени тега.
    *
    * @param tag Название тега.
    * @return Фьючерс с опциоальным id тега-узла.
    */
  def findTagNode(tag: String): Future[Option[MNode]] = {
    lazy val logPrefix = s"findTagNodeId($tag):"

    val msearch = new MNodeSearchDfltImpl {
      override def outEdges: Seq[ICriteria] = {
        val tcr = TagCriteria(
          face      = tag,
          isPrefix  = false,
          exact     = true
        )
        val cr = Criteria(
          predicates  = Seq( _PRED ),
          tags        = Seq(tcr)
        )
        Seq(cr)
      }

      override def nodeTypes = Seq( MNodeTypes.Tag )

      // Берём поиск с запасом. Возможно появление дублирующихся результатов поиска, и на это надо будет реагировать.
      // Когда всё будет отлажено, тут можно ставить limit=1, и искать через dynSearchOne() и подобных.
      override def limit = 2
    }

    for (tagNodes <- MNode.dynSearch(msearch)) yield {
      if (tagNodes.size > 1)
        warn(s"$logPrefix Too many tag-nodes found for single-tag request: ${tagNodes.mkString(", ")}, ...")
      // TODO Нужно запускать тут мерж tag-узлов при выявлении коллизии: 2+ узлов относяться к одному и тому же тегу.
      tagNodes.headOption
    }
  }


  /**
    * Убедиться, что узел для указанного тега существует и вернуть id узла.
    *
    * @param tag tag face.
    * @return Фьючерс с id узла-тега.
    */
  def ensureTag(tag: String): Future[MNode] = {
    val findTagFut = findTagNode(tag)
    lazy val logPrefix = s"ensureTag($tag):"

    findTagFut
      .map(_.get)
      .recoverWith { case _: NoSuchElementException =>
        trace(s"$logPrefix Tag not exists, creating new one.")

        val e = MEdge(
          predicate = _PRED,
          info = MEdgeInfo(
            tags = Set(tag)
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

        // Запустить сохранение нового узла.
        val fut = tagNode0.save

        fut.onComplete {
          case Success(nodeId) => info(s"$logPrefix Created NEW node[$nodeId] for tag")
          case Failure(ex)     => error(s"$logPrefix Unable to create tag-node", ex)
        }

        for (tagId <- fut) yield {
          tagNode0.copy(
            id          = Some(tagId),
            versionOpt  = Some( SioEsUtil.DOC_VSN_0 )
          )
        }
      }
  }




}
