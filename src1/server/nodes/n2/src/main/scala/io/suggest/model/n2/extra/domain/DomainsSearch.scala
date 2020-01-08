package io.suggest.model.n2.extra.domain

import io.suggest.common.empty.EmptyProduct
import io.suggest.enum2.EnumeratumUtil
import io.suggest.es.search.DynSearchArgs
import io.suggest.model.n2.node.MNodeFields
import org.apache.lucene.search.join.ScoreMode
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 06.09.16 16:29
  * Description: Поиск доменных ключей.
  * TODO Надо перенести домены в эджи. Modes - в флаги, dkeys - в теги.
  */
trait DomainsSearch extends DynSearchArgs {

  /** Данные для поиска узла по домену и каким-то доменным характеристикам.
    * Списки с несколькими значениями объединяются через ИЛИ. */
  def domains: Seq[DomainCriteria] = Nil

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qOpt0 = super.toEsQueryOpt
    val _domainsIter = domains
      .iterator
      .filter(_.nonEmpty)
    if (_domainsIter.isEmpty) {
      qOpt0
    } else {
      // Собрать nested query, которая найдёт узлы, подходящие под указанные домены.
      val inNestedQ = QueryBuilders.boolQuery()
        .minimumShouldMatch(1)

      // Пробежаться по заданным критериям...
      for (cr <- _domainsIter) {
        val innerQ = QueryBuilders.boolQuery()

        // Отработать сначала доменные ключики.
        if (cr.dkeys.nonEmpty) {
          val qd = QueryBuilders.termsQuery( MNodeFields.Extras.DOMAIN_DKEY_FN, cr.dkeys: _* )
          innerQ.must(qd)
        }

        // Затем отработать режимы, если они заданы.
        if (cr.modes.nonEmpty) {
          val qm = QueryBuilders.termsQuery( MNodeFields.Extras.DOMAIN_MODE_FN, cr.modes.map(EnumeratumUtil.valueF): _* )
          innerQ.must(qm)
        }

        inNestedQ.should(innerQ)
      }

      // У нас ведь nested object. Поэтому, надо аккамулятор завернуть в nested query перед объединением с qOpt0.
      val nestedQ = QueryBuilders.nestedQuery( MNodeFields.Extras.DOMAIN_FN, inNestedQ, ScoreMode.Max )

      qOpt0.map { q0 =>
        QueryBuilders.boolQuery()
          .must(q0)
          .must(nestedQ)
      }.orElse {
        Some(nestedQ)
      }
    }
  }

}


/** Модель критериев поиска узла по данным домена. */
case class DomainCriteria(
  dkeys: Seq[String]      = Nil,
  modes: Seq[MDomainMode] = Nil
)
  extends EmptyProduct
