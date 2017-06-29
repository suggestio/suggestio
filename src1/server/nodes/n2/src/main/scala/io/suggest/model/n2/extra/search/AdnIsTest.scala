package io.suggest.model.n2.extra.search

import io.suggest.es.search.{DynSearchArgs, DynSearchArgsWrapper}
import io.suggest.model.n2.node.MNodeFields
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.10.15 22:01
 * Description: Поисковый аддон для поиска/фильтрации по полю adn.test.
 */
trait AdnIsTest extends DynSearchArgs {

  /** Искать/фильтровать по полю extra.adn.testNode. */
  def testNode: Option[Boolean]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qbOpt0 = super.toEsQueryOpt
    val _testNode = testNode
    if (_testNode.isEmpty) {
      qbOpt0
    } else {
      val fn = MNodeFields.Extras.ADN_IS_TEST_FN
      qbOpt0.map { qb =>
        // Отрабатываем флаг testNode.
        _testNode.fold(qb) { tnFlag =>
          var tnf: QueryBuilder = QueryBuilders.termQuery(fn, tnFlag)
          if (!tnFlag) {
            val tmf = QueryBuilders.existsQuery(fn)
            tnf = QueryBuilders.boolQuery()
              // Имитация orQuery, которая стала deprecated во время переезда на es-2.0.
              .should(tnf)
              .mustNot(tmf)
              .minimumShouldMatch(1)
          }
          QueryBuilders.boolQuery()
            .must(qb)
            .filter(tnf)
        }

      }.orElse {
        _testNode.map { tnFlag =>
          // TODO Нужно добавить аналог missing filter для query и как-то объеденить через OR. Или пока так и пересохранять узлы с tn=false.
          QueryBuilders.termQuery(fn, tnFlag)
        }
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val sis = super.sbInitSize
    if (testNode.isDefined) sis + 16 else sis
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("testNode", testNode, super.toStringBuilder)
  }

}


/** Дефолтовая реализация абстрактных полей [[AdnIsTest]]. */
trait AdnIsTestDflt extends AdnIsTest {
  override def testNode: Option[Boolean] = None
}


/** wrap-реализация аддона [[AdnIsTest]]. */
trait AdnIsTestWrap extends AdnIsTest with DynSearchArgsWrapper {
  override type WT <: AdnIsTest
  override def testNode = _dsArgsUnderlying.testNode
}
