package io.suggest.model.search

import io.suggest.model.EsModelStaticT
import io.suggest.util.MacroLogsI
import io.suggest.util.SioEsUtil.laFuture2sFuture
import org.elasticsearch.action.search.SearchRequestBuilder
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.08.14 16:16
 * Description: Общий код для статических моделей и их запускалок динамически-генерируемых поисковых запросов.
 * Подразумевается, что Es Query генерятся на основе экземпляра, реализующего [[DynSearchArgs]].
 */

trait EsDynSearchStatic[A <: DynSearchArgs] extends EsModelStaticT with MacroLogsI {

  /** Билдер поискового запроса. */
  def dynSearchReqBuilder(dsa: A)(implicit client: Client): SearchRequestBuilder = {
    // Запускаем собранный запрос.
    val result = dsa.prepareSearchRequest(prepareSearch)
    LOGGER.trace(s"dynSearchReqBuilder($dsa): Compiled request = \n${result.toString}")
    result
  }

  /**
   * Поиск карточек в ТЦ по критериям.
   * @return Список рекламных карточек, подходящих под требования.
   */
  def dynSearch(dsa: A)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    dynSearchReqBuilder(dsa)
      .execute()
      .map { searchResp2list }
  }

  /**
   * Разновидность dynSearch для максимум одного результата. Вместо коллекции возвращается Option[T].
   * @param dsa Аргументы поиска.
   * @return Фьючерс с Option[T] внутри.
   */
  def dynSearchOne(dsa: A)(implicit ec: ExecutionContext, client: Client): Future[Option[T]] = {
    dynSearch(dsa)
      .map { _.headOption }
  }

  /**
   * Аналог dynSearch, но возвращаются только id документов.
   * @param dsa Поисковый запрос.
   * @return Список id, подходящих под запрос, в неопределённом порядке.
   */
  def dynSearchIds(dsa: A)(implicit ec: ExecutionContext, client: Client): Future[Seq[String]] = {
    dynSearchReqBuilder(dsa)
      .setFetchSource(false)
      .setNoFields()
      .execute()
      .map { searchResp2idsList }
  }

  /**
   * Посчитать кол-во рекламных карточек, подходящих под запрос.
   * @param dsa Экземпляр, описывающий критерии поискового запроса.
   * @return Фьючерс с кол-вом совпадений.
   */
  def dynCount(dsa: A)(implicit ec: ExecutionContext, client: Client): Future[Long] = {
    // Необходимо выкинуть из запроса ненужные части.
    countByQuery(dsa.toEsQuery)
  }


  /** Поиск id, подходящих под запрос и последующий multiget. Используется для реалтаймого получения
    * изменчивых результатов, например поиск сразу после сохранения. */
  def dynSearchRt(dsa: A)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    dynSearchReqBuilder(dsa)
      .setFetchSource(false)
      .setNoFields()
      .execute()
      .flatMap { searchResp2RtMultiget(_) }
  }

}


/** Базовый интерфейс для аргументов-критериев поиска. */
trait DynSearchArgs {

  /** Собрать экземпляр ES QueryBuilder на основе имеющихся в экземпляре данных.
    * Здесь можно навешивать дополнительные фильтры, выполнять post-процессинг запроса. */
  def toEsQuery: QueryBuilder = {
    toEsQueryOpt getOrElse defaultEsQuery
  }

  /** Генератор самого дефолтового запроса, когда toEsQueryOpt не смог ничего предложить. */
  def defaultEsQuery: QueryBuilder = QueryBuilders.matchAllQuery()

  /** Сборка EsQuery сверху вниз. */
  def toEsQueryOpt: Option[QueryBuilder] = None

  /**
   * Сборка search-реквеста. Можно переопределить чтобы добавить в реквест какие-то дополнительные вещи,
   * кастомную сортировку например.
   * @param srb Поисковый реквест, пришедший из модели.
   * @return SearchRequestBuilder, наполненный данными по поисковому запросу.
   */
  def prepareSearchRequest(srb: SearchRequestBuilder): SearchRequestBuilder = {
    srb.setQuery(toEsQuery)
  }

  /** toString() выводит экземпляр этого класса списком. Но ей нужно знать какое-то название модуля,
    * которое конкретные реализации могут переопределять. */
  def mySimpleName = getClass.getSimpleName

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
    new StringBuilder(sbInitSize, mySimpleName).append('{')
  }

  override def toString: String = {
    toStringBuilder
      .append('\n')
      .append('}')
      .toString()
  }

  /** Вспомогательное форматирование аргумента-коллекции строкой внутрь StringBuilder'а. */
  final protected def fmtColl2sb(name: String, coll: TraversableOnce[_], sb: StringBuilder): StringBuilder = {
    if (coll.nonEmpty)
      sb.append("\n  ").append(name).append(" = ").append(coll.mkString(", "))
    sb
  }

}


/** Интерфейс для врапперов search-результатов контейнера аргументов dyn-поиска. */
trait DynSearchArgsWrapper extends DynSearchArgs {
  type WT <: DynSearchArgs
  def _dsArgsUnderlying: WT
}
