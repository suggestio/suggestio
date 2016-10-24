package io.suggest.model.search

import io.suggest.model.es._
import org.elasticsearch.index.query.QueryBuilder

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.10.16 13:59
  * Description: Поддержка вложенных запросов в DynSearch.
  * По задумке, нижележащие запросы объединяются через bool query (хотя и необязательно)
  * в единую QueryBuilder, которая становится основой для сборки поиска на текущем уровне.
  *
  * Нижележащий запрос (если есть) тоже катенируется в общий bool query,
  * но очень желательно, чтобы нижележащего запроса не было, дабы не нарушать здравый смысл.
  * Поэтому желательно, чтобы этот трейт был самым-самым первым в списке extends/with.
  */
trait SubSearches extends DynSearchArgs {

  /** Подчиненные query. */
  def subSearches: Iterable[MSubSearch]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    val qOpt0 = super.toEsQueryOpt
    val _subArgs = subSearches

    if (_subArgs.isEmpty) {
      qOpt0

    } else {
      // Запрещаем использовать подзапросы, если трейт ошибочно скомпилен/вызван НЕ_первым в MNodeSearch.
      assert(
        qOpt0.isEmpty,
        "Subquery non-empty. SubSearches trait must be called first, before other traits: " + qOpt0
      )

      // Закинуть подзапросы в общую кучу...
      val subQueries = _subArgs.iterator
        .map { subArg =>
          MWrapClause(
            must = subArg.must,
            // Тут нельзя дергать toEsQueryOpt, т.к. это приведёт к тихому отключению score-base сортировок (RandomSort, ConstScore).
            queryBuilder = subArg.search.toEsQuery
          )
        }
        .toTraversable

      val q1 = QueryUtil.maybeWrapToBool( subQueries )

      Some(q1)
    }
  }

  override def sbInitSize: Int = {
    val sis = super.sbInitSize
    val _subSearches = subSearches
    if (_subSearches.nonEmpty) {
      sis  +  128 * _subSearches.size
    } else {
      sis
    }
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("subSearches", subSearches, super.toStringBuilder)
  }

}

/** Дефолтовая реализация [[SubSearches]]. */
trait SubSearchesDflt extends SubSearches {
  override def subSearches: Iterable[MSubSearch] = Nil
}

/** Wrap-реализация [[SubSearches]]. */
trait SubSearchesWrap extends SubSearches with DynSearchArgsWrapper {
  override type WT <: SubSearches
  override def subSearches = _dsArgsUnderlying.subSearches
}


/** Контейнер данных по sub-аргументам. */
case class MSubSearch(
  search    : DynSearchArgs,
  must      : Must_t          = IMust.SHOULD
)
  extends IMust
