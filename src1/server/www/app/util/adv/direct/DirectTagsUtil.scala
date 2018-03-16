package util.adv.direct

import javax.inject.Inject

import io.suggest.model.n2.edge.MPredicates
import io.suggest.model.n2.edge.search.Criteria
import io.suggest.model.n2.node.{MNodeFields, MNodeTypes, MNodes}
import io.suggest.model.n2.node.search.MNodeSearchDfltImpl
import org.elasticsearch.search.aggregations.AggregationBuilders
import io.suggest.es.util.SioEsUtil.laFuture2sFuture
import io.suggest.util.JMXBase
import io.suggest.util.logs.{MacroLogsDyn, MacroLogsImpl}
import org.elasticsearch.search.aggregations.bucket.filter.Filter
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import org.elasticsearch.search.aggregations.bucket.terms.Terms

import scala.collection.JavaConverters._
import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 16.03.18 14:17
  * Description: Утиль, связанная с тегами, размещёнными прямо на узлах.
  */
class DirectTagsUtil @Inject()(
                                mNodes: MNodes,
                                implicit private val ec: ExecutionContext
                              )
  extends MacroLogsImpl
{

  /** Собрать все tag face, размещённые на указанном узле.
    *
    * @param nodeIds id-целевого узла.
    * @return Фьючерс с множеством tag face'ов, которые размещены на узле.
    */
  // TODO XXX Не работает, почему-то elasticsearch ошибкается эджем и возвращает предшествующий эдж вместо искомого. Код не используется.
  def findForNodes(nodeIds: String*): Future[Stream[(String, Long)]] = {
    // Поиск карточек, имеющих какие-либо direct-tags-размещения:
    val edgeCr = Criteria(
      nodeIds     = nodeIds,
      predicates  = MPredicates.TaggedBy.DirectTag :: Nil
    )
    val edgeCrs = edgeCr :: Nil

    val msearch = new MNodeSearchDfltImpl {
      override def outEdges = edgeCrs
      override def nodeTypes = MNodeTypes.Ad :: Nil
      override def limit = 0
    }

    val aggNestEdgesName = "edgeNest"
    val aggNodeIdsFilter = "nodeIds"
    val aggTagFaceName = "tagFaces"

    // Чтобы выделить из списка эджей нужный, надо дополнительно запрос передать в аггрегатор.
    val filterQuery = new MNodeSearchDfltImpl {
      override def outEdges = edgeCrs
    }

    val request = mNodes
      .prepareSearch(msearch)
      .setSize( 0 )
      .addAggregation(
        AggregationBuilders
          .nested( aggNestEdgesName, MNodeFields.Edges.E_OUT_FN )
          .subAggregation(
            AggregationBuilders
              .filter(aggNodeIdsFilter, filterQuery.toEsQuery)
              .subAggregation(
                AggregationBuilders.terms( aggTagFaceName )
                  .field( MNodeFields.Edges.E_OUT_INFO_TAGS_RAW_FN )
              )
          )
      )
    val respFut = request.execute()

    lazy val logPrefix = s"findForNodes(${nodeIds.mkString(", ")})#${System.currentTimeMillis()}:"
    LOGGER.trace(s"$logPrefix Final request:\n${request.toString}")

    for {
      resp <- respFut
    } yield {
      val nestedAgg = resp
        .getAggregations
        .get[Nested](aggNestEdgesName)

      val filteredAgg = nestedAgg
        .getAggregations
        .get[Filter](aggNodeIdsFilter)

      val tagFacesAgg = filteredAgg
        .getAggregations
        .get[Terms](aggTagFaceName)

      LOGGER.trace(s"$logPrefix\n Nested ${nestedAgg.getDocCount} docs\n Filtered ${filteredAgg.getDocCount}\n Tag faces ok=${tagFacesAgg.getSumOfOtherDocCounts} err=${tagFacesAgg.getDocCountError}")

      tagFacesAgg
        .getBuckets
        .asScala
        .toStream
        .map { bucket =>
          val tagFace = bucket.getKey.asInstanceOf[String]
          val count = bucket.getDocCount
          (tagFace, count)
        }
    }
  }

}


trait DirectTagsUtilJmxMBean {

  def findForNode(nodeId: String): String

}

class DirectTagsUtilJmx @Inject() (
                                    directTagsUtil            : DirectTagsUtil,
                                    override implicit val ec  : ExecutionContext
                                  )
  extends JMXBase
  with DirectTagsUtilJmxMBean
  with MacroLogsDyn
{

  override def jmxName = "io.suggest:type=util,name=" + getClass.getSimpleName.replace("Jmx", "")

  override def findForNode(nodeId: String): String = {
    val fut = for {
      tagFacesAgg <- directTagsUtil.findForNodes(nodeId)
    } yield {
      tagFacesAgg.mkString(",\n")
    }
    awaitString( fut )
  }

}
