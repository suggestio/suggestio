package io.suggest.model.n2.bill.tariff.daily

import io.suggest.bill.MCurrency
import io.suggest.es.search.DynSearchArgs
import io.suggest.model.n2.node.MNodeFields
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 21.03.17 14:55
  * Description: Поиск по валюте.
  */
trait TfDailyCurrencySearch extends DynSearchArgs {

  /**
    * Поиск/фильтрация узлов по валюте daily-тарифа, сохраненного на узле.
    * @return None - пропустить.
    *         Some([]) - пустой список внутри Some означает, что валюта не важна, главное чтобы она была.
    *         Some(...) - искать узлы, которые имеют только указанные валюты в тарифах.
    */
  def tfDailyCurrencies: Option[Iterable[MCurrency]] = None

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val currsOpt = tfDailyCurrencies

    currsOpt.fold( qbOpt0 ) { currs =>
      // TODO Кажется, тут что-то неисправно: поиск не работает нормально.
      val fn = MNodeFields.Billing.TARIFFS_DAILY_CURRENCY_FN

      val sqb: QueryBuilder = if (currs.isEmpty) {
        QueryBuilders.existsQuery( fn )
      } else {
        val currStrs = currs.iterator
          .map(_.currencyCode)
          .toSeq
        QueryBuilders.termsQuery(fn, currStrs: _*)
      }

      val qb2 = qbOpt0.fold(sqb) { qb0 =>
        QueryBuilders.boolQuery()
          .must( qb0 )
          .filter( sqb )
      }

      Some(qb2)
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val sz0 = super.sbInitSize
    tfDailyCurrencies.fold(sz0) { currs =>
      sz0 + 16 + Math.max(1, currs.size) * 5
    }
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("tfDailyCurrencies", tfDailyCurrencies.iterator.flatten, super.toStringBuilder)
  }

}
