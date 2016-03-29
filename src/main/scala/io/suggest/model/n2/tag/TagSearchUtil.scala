package io.suggest.model.n2.tag

import javax.inject.Inject

import com.google.inject.Singleton
import io.suggest.model.n2.node.search.MNodeSearch
import io.suggest.model.n2.node.{MNode, MNodeFields}
import io.suggest.util.SioEsUtil.laFuture2sFuture
import org.elasticsearch.client.Client
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.nested.Nested
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import scala.collection.JavaConversions._

import scala.concurrent.{ExecutionContext, Future}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 29.03.16 16:15
  * Description: Утиль для взаимодействия с тегами, живующими внутри эджей.
  */
@Singleton
class TagSearchUtil @Inject() (implicit ec: ExecutionContext, client: Client) {

  /**
    * Полнотекстовый поиск тегов и аггрегация статистики по подходящим документам.
    *
    * @param search Поисковый запрос.
    * @return Фьючерс с результатами.
    */
  def searchAgg(search: MNodeSearch): Future[TagsAggResult] = {

    val tagsAggName = "tags"
    val tagg = AggregationBuilders.terms(tagsAggName)
      .field( MNodeFields.Edges.E_OUT_INFO_TAGS_RAW_FN )

    val nestedAggName = "edges"
    val nagg = AggregationBuilders.nested(nestedAggName)
      .path( MNodeFields.Edges.E_OUT_FN )
      .subAggregation( tagg )

    // Запустить аггрегацию...
    for {
      resp <- {
        MNode.dynSearchReqBuilder(search)
          .addAggregation(nagg)
          .setSize(0)
          .execute()
      }

    } yield {

      val naggRes = resp.getAggregations
        .get[Nested](nestedAggName)

      // Собрать данные по тегам.
      val tags = {
        val iter = for {
          bucket <- naggRes.getAggregations
            .get[Terms](tagsAggName)
            .getBuckets
            .iterator()
        } yield {
          TagAggInfo(
            face    = bucket.getKey,
            count   = bucket.getDocCount
          )
        }
        iter.toSeq
      }

      // Собрать результат.
      TagsAggResult(
        tags  = tags,
        count = naggRes.getDocCount
      )
    }
  }

}

trait ITagSearchUtilDi {
  def tagSearchUtil: TagSearchUtil
}
