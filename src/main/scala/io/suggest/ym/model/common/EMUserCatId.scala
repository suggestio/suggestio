package io.suggest.ym.model.common

import io.suggest.model.{EsModel, EsModelStaticT, EsModelT}
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json.JsString
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import scala.collection.JavaConversions._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 18:48
 * Description: Поддержка поля userCatId, содержащего id категории.
 */
object EMUserCatId {
  val USER_CAT_ID_ESFN = "userCatId"
}

import EMUserCatId._


trait EMUserCatIdStatic extends EsModelStaticT {
  override type T <: EMUserCatIdMut

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(USER_CAT_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (USER_CAT_ID_ESFN, value) =>
        acc.userCatId = Option(EsModel.stringParser(value))
    }
  }

  /**
   * Сбор статистики по категориям в рамках произвольного реквеста.
   * @param reqBuilder Билдер поискового реквеста.
   * @return Фьючерс со списком id_категории -> кол-во документов.
   */
  def stats4UserCats(reqBuilder: SearchRequestBuilder)(implicit ec: ExecutionContext): Future[List[(String, Long)]] = {
    val agg = AggregationBuilders
      .terms(USER_CAT_ID_ESFN)
      .field(USER_CAT_ID_ESFN)
    reqBuilder
      .addAggregation(agg)
      .execute()
      .map { searchResp =>
        searchResp.getAggregations
          .get[Terms](USER_CAT_ID_ESFN)
          .getBuckets
          .iterator()
          .map { bkt => bkt.getKey -> bkt.getDocCount }
          .toList
      }
  }
}


trait EMUserCatIdI extends EsModelT {
  override type T <: EMUserCatIdI
  def userCatId: Option[String]
}


trait EMUserCatId extends EMUserCatIdI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (userCatId.isDefined)
      USER_CAT_ID_ESFN -> JsString(userCatId.get) :: acc0
    else
      acc0
  }
}


trait EMUserCatIdMut extends EMUserCatId {
  override type T <: EMUserCatIdMut
  var userCatId: Option[String]
}
