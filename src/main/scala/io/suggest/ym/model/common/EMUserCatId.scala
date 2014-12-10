package io.suggest.ym.model.common

import io.suggest.model.{EsModel, EsModelStaticMutAkvT, EsModelPlayJsonT}
import io.suggest.util.SioEsUtil._
import io.suggest.model.EsModel.FieldsJsonAcc
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilders, QueryBuilder}
import play.api.libs.json.{JsArray, JsString}
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.search.aggregations.AggregationBuilders
import org.elasticsearch.search.aggregations.bucket.terms.Terms
import scala.collection.JavaConversions._
import java.{lang => jl}
import EsModel.{stringParser, iteratorParser}

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


/** Аддон для статической части модели с поддержкой поля категорий. */
trait EMUserCatIdStatic extends EsModelStaticMutAkvT {
  override type T <: EMUserCatIdMut

  abstract override def generateMappingProps: List[DocField] = {
    FieldString(USER_CAT_ID_ESFN, include_in_all = false, index = FieldIndexingVariants.not_analyzed) ::
    super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (USER_CAT_ID_ESFN, value) =>
        val v1 = value match {
          case ids: jl.Iterable[_] =>
            iteratorParser(ids)
              .map { stringParser }
              .toSet
          case id =>
            Set( stringParser(id) )
        }
        acc.userCatId = v1
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


/** Аддон-интерфейс экземпляра модели. */
trait EMUserCatIdI extends EsModelPlayJsonT {
  override type T <: EMUserCatIdI
  def userCatId: Set[String]
}


/** Аддон к экземпляру модели. */
trait EMUserCatId extends EMUserCatIdI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (userCatId.isEmpty) {
      acc0
    } else {
      val arr = userCatId.iterator
        .map(JsString.apply)
        .toSeq
      USER_CAT_ID_ESFN -> JsArray(arr) :: acc0
    }
  }
}


trait EMUserCatIdMut extends EMUserCatId {
  override type T <: EMUserCatIdMut
  var userCatId: Set[String]
}


/** Аддон для dyn-search для поиска по полю userCatId. */
trait UserCatIdDsa extends DynSearchArgs {

  /** Необязательный id категории */
  def catIds: Seq[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map { qb =>
      // Если есть q или shopId и указана catId, то добавляем catId-фильтр.
      if (catIds.isEmpty) {
        qb
      } else {
        val catIdFilter = FilterBuilders.termsFilter(USER_CAT_ID_ESFN, catIds : _*)
        QueryBuilders.filteredQuery(qb, catIdFilter)
      }
    }.orElse[QueryBuilder] {
      // Запроса всё ещё нет, т.е. собрать запрос по shopId тоже не удалось. Пробуем собрать запрос с catIdOpt...
      if (catIds.isEmpty) {
        None
      } else {
        val qb = QueryBuilders.termsQuery(USER_CAT_ID_ESFN, catIds : _*)
        Some(qb)
      }
    }
  }

}
trait UserCatIdDsaDflt extends UserCatIdDsa {
  override def catIds: Seq[String] = Seq.empty
}
trait UserCatIdDsaWrapper extends UserCatIdDsa with DynSearchArgsWrapper {
  override type WT <: UserCatIdDsa
  override def catIds = _dsArgsUnderlying.catIds
}

