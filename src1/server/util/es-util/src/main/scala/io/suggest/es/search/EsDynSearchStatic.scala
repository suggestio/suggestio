package io.suggest.es.search

import io.suggest.es.model.EsModelStaticT
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.08.14 16:16
  * Description: Общий код для статических моделей и их запускалок динамически-генерируемых поисковых запросов.
  * Подразумевается, что Es Query генерятся на основе экземпляра, реализующего [[DynSearchArgs]].
  *
  * v1 API:
  * - пачка похожих методов dyn*() => Future[...]
  *
  * v2 API:
  * - typeclass'ы и search[X](args: A)(implicit typeclass: Helper[X]) => Future[X]
  */

trait EsDynSearchStatic[A <: DynSearchArgs] extends EsModelStaticT


/** Базовый интерфейс для аргументов-критериев поиска. */
trait DynSearchArgs {

  /** Собрать экземпляр ES QueryBuilder на основе имеющихся в экземпляре данных.
    * Здесь можно навешивать дополнительные фильтры, выполнять post-процессинг запроса. */
  def toEsQuery: QueryBuilder = {
    toEsQueryOpt
      .getOrElse( defaultEsQuery )
  }

  /** Генератор самого дефолтового запроса, когда toEsQueryOpt не смог ничего предложить. */
  // private: не используется в иных местах.
  private def defaultEsQuery: QueryBuilder = {
    QueryBuilders.matchAllQuery()
  }

  /** Сборка EsQuery сверху вниз. */
  def toEsQueryOpt: Option[QueryBuilder] = None

  /**
   * Сборка search-реквеста. Можно переопределить чтобы добавить в реквест какие-то дополнительные вещи,
   * кастомную сортировку например.
   *
   * @param srb Поисковый реквест, пришедший из модели.
   * @return SearchRequestBuilder, наполненный данными по поисковому запросу.
   */
  def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    srb.setQuery(toEsQuery)
  }

  /** Базовый размер StringBuilder'а. */
  def sbInitSize = 32

  /** Вспомогательный подсчет размер коллекции для ускорения работы toStringBuilder. */
  final protected def collStringSize(coll: Iterable[_], sis: Int, addOffset: Int = 0): Int = {
    if (coll.isEmpty)
      sis
    else
      sis + coll.size * (coll.head.toString.length + 1) + 10 + addOffset
  }

  /** Построение выхлопа метода toString(). */
  def toStringBuilder: StringBuilder = {
    new StringBuilder(sbInitSize, getClass.getSimpleName)
      .append('{')
  }

  override def toString: String = {
    toStringBuilder
      .append('\n')
      .append('}')
      .toString()
  }

  /** Вспомогательное форматирование аргумента-коллекции строкой внутрь StringBuilder'а. */
  final protected def fmtColl2sb(name: String, coll: IterableOnce[_], sb: StringBuilder): StringBuilder = {
    val iter = coll.iterator
    if (iter.nonEmpty)
      sb.append("\n  ")
        .append(name)
        .append(" = ")
        .append(iter.mkString(", "))

    sb
  }

}


/** Интерфейс для врапперов search-результатов контейнера аргументов dyn-поиска. */
trait DynSearchArgsWrapper extends DynSearchArgs {
  type WT <: DynSearchArgs
  def _dsArgsUnderlying: WT
}
