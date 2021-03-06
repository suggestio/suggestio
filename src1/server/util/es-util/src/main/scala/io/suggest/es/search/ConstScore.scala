package io.suggest.es.search

import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.10.16 17:03
  * Description: Поддержка выставления постоянного кастомного скора для результатов поиска dyn search.
  */
trait ConstScore extends DynSearchArgs {

  /** Выставлять этот скор для всех результатов. */
  def constScore: Option[Float] = None

  override def toEsQuery: QueryBuilder = {
    val q0 = super.toEsQuery
    constScore.fold(q0) { boost =>
      QueryBuilders.constantScoreQuery(q0)
        .boost( boost )
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val sz0 = super.sbInitSize
    val cs = constScore
    if (cs.isEmpty) sz0 else sz0 + 20
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("constScore", constScore, super.toStringBuilder)
  }

}
