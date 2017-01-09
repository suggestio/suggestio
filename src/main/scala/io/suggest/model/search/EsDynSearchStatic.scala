package io.suggest.model.search

import com.sksamuel.elastic4s.{IndexesAndTypes, SearchDefinition}
import io.suggest.model.common.OptId
import io.suggest.model.es.{EsModelStaticT, ISearchResp}
import io.suggest.util.MacroLogsI
import io.suggest.util.SioEsUtil.laFuture2sFuture
import org.elasticsearch.action.search.{SearchRequestBuilder, SearchResponse}
import org.elasticsearch.index.query.{QueryBuilder, QueryBuilders}

import scala.concurrent.Future
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

trait EsDynSearchStatic[A <: DynSearchArgs] extends EsModelStaticT with MacroLogsI {

  import mCommonDi._


  // DynSearch v2: Для всего (кроме Rt) используется ровно один метод с параметризованным типом результата.

  /** Отрефакторенный вариант dynSearch, где логика, касающаяся возвращаемого типа, вынесена в helper typeclass.
    *
    * @param args Аргументы поиска.
    * @param helper typeclass dyn-search хелпер'а. Занимается финальным допиливание search-реквеста и маппингом результатов.
    * @tparam R Тип результата.
    * @return Фьючерс с результатом типа R.
    */
  // TODO Может быть вынести [A] в implicit convertions, а тут выставить SearchRequestBuilder?
  def search[R](args: A)(implicit helper: IEsSearchHelper[R]): Future[R] = {
    // Логика очень варьируется от типа возвращаемого значения, поэтому сразу передаём управление в helper.
    helper.run(args)
  }


  // TODO С prepareSearch() пока какой-то говнокод. args.prepareSearchRequest следовало бы вынести за пределы модели DynSearchArgs куда-то сюда.
  /** Сборка билдера поискового запроса. */
  def prepareSearch(args: A): SearchRequestBuilder = {
    prepareSearch(args, prepareSearch())
  }
  def prepareSearch(args: A, srb0: SearchRequestBuilder): SearchRequestBuilder = {
    args.prepareSearchRequest(srb0)
  }

  /**
    * Самый абстрактный интерфейс для всех хелперов DynSearch.
    * Вынести за пределы модели нельзя, т.к. трейт зависит от [A].
    */
  trait IEsSearchHelper[R] {
    def run(args: A): Future[R]
  }

  /** Трейт для сборки DynSearchHelper, возвращающего Future-результат. */
  // abstract class для оптимизации, но можно завернуть назад в трейт.
  abstract class EsSearchFutHelper[R] extends IEsSearchHelper[R] {

    /** Подготовка исходного реквеста к поиску. */
    def prepareSearchRequest(args: A): SearchRequestBuilder = {
      prepareSearch(args)
    }

    /** Парсинг и обработка сырого результата в некий результат. */
    def mapSearchResp(searchResp: SearchResponse): Future[R]

    override def run(args: A): Future[R] = {
      // Запускаем собранный запрос.
      val srb = prepareSearchRequest(args)

      val fut = srb
        .execute()
        .flatMap( mapSearchResp )

      // Логгируем всё вместе с es-индексом и типом, чтобы облегчить curl-отладку на основе залоггированного.
      LOGGER.trace(s"dynSearch2.run($args): Will search on $ES_INDEX_NAME/$ES_TYPE_NAME\n Compiled request = \n${srb.toString}")

      fut
    }
  }

  /** Если поисковый запрос подразумевает только получение id'шников, то использовать этот трейт. */
  abstract class EsSearchIdsFutHelper[R] extends EsSearchFutHelper[R] {
    override def prepareSearchRequest(args: A): SearchRequestBuilder = {
      super.prepareSearchRequest(args)
        .setFetchSource(false)
        .setNoFields()
    }
  }


  // Реализации typeclass'ов:

  /** search-маппер, просто возвращающий сырой ответ. */
  class RawSearchRespMapper extends EsSearchFutHelper[SearchResponse] {
    /** Парсинг и обработка сырого результата в некий результат. */
    override def mapSearchResp(searchResp: SearchResponse): Future[SearchResponse] = {
      Future.successful( searchResp )
    }
  }

  /** typeclass: возвращает результаты в виде инстансом моделей. */
  class SeqTMapper extends EsSearchFutHelper[Seq[T]] {
    override def mapSearchResp(searchResp: SearchResponse): Future[Seq[T]] = {
      val result = searchResp2list(searchResp)
      Future.successful(result)
    }
  }

  /** typeclass: Маппер ответа в id'шники ответов. */
  class IdsMapper extends EsSearchIdsFutHelper[ ISearchResp[String] ] {
    override def mapSearchResp(searchResp: SearchResponse): Future[ISearchResp[String]] = {
      val result = searchResp2idsList(searchResp)
      Future.successful(result)
    }
  }

  /** typeclass: подсчёт кол-ва результатов без самих результатов. */
  class CountMapper extends IEsSearchHelper[Long] {
    override def run(args: A): Future[Long] = {
      countByQuery(args.toEsQuery)
    }
  }

  /** typeclass для возврата максимум одного результата и в виде Option'а. */
  class OptionTMapper extends EsSearchFutHelper[Option[T]] {
    /** Парсинг и обработка сырого результата в некий результат. */
    override def mapSearchResp(searchResp: SearchResponse): Future[Option[T]] = {
      val r = searchResp2list(searchResp)
        .headOption
      Future.successful(r)
    }
  }

  /** typeclass маппинга в карту по id. */
  class MapByIdMapper extends EsSearchFutHelper[Map[String, T]] {
    override def mapSearchResp(searchResp: SearchResponse): Future[Map[String, T]] = {
      val r = OptId.els2idMap[String, T] {
        searchRespMap(searchResp)(deserializeSearchHit)
      }
      Future.successful(r)
    }
  }

  /** Вызываемая вручную сборка multigetter'а для найденных результатов.
    * Это typeclass, передаваемый вручную в dynSearch2() при редкой необходимости такого действия.
    */
  class SeqRtMapper extends EsSearchIdsFutHelper[Seq[T]] {
    override def mapSearchResp(searchResp: SearchResponse): Future[Seq[T]] = {
      // TODO Когда старый поиск уйдёт в историю, можно будет заинлайнить здесь searchResp2RtMultiget().
      searchResp2RtMultiget(searchResp)
    }
  }
  def seqRtMapper: EsSearchFutHelper[Seq[T]] = {
    new SeqRtMapper
  }


  /** Дополнение implicit API модели новыми конвертациями и typeclass'ами. */
  class Implicits extends super.Implicits {

    implicit def rawRespMapper: EsSearchFutHelper[SearchResponse] = {
      new RawSearchRespMapper
    }

    implicit def seqMapper: EsSearchFutHelper[Seq[T]] = {
      new SeqTMapper
    }

    implicit def idsMapper: EsSearchFutHelper[ISearchResp[String]] = {
      new IdsMapper
    }

    implicit def countMapper: IEsSearchHelper[Long] = {
      new CountMapper
    }

    implicit def optionMapper: EsSearchFutHelper[Option[T]] = {
      new OptionTMapper
    }

    implicit def mapByIdMapper: EsSearchFutHelper[Map[String, T]] = {
      new MapByIdMapper
    }


    /** Приведение инстанса DynSearchArgs к врапперам elastic4s.
      *
      * @param args Аргументы поиска dyn-search для текущей модели.
      * @return Инстанс SearchDefinition, пригодный для огуливания в elastic4s.
      */
    implicit def dynSearchArgs2es4sDefinition(args: A): SearchDefinition = {
      // TODO Говнокод из-за особенностей внутреннего устройства системы es4s, API-методы которого потребляют case class'ы на вход.
      //      По-хорошему, надо pull-request организовать, который заменяет в elastic4s API всякие классы на их интерфейсы.
      val iAndT = IndexesAndTypes(
        indexes = Seq(ES_INDEX_NAME),
        types   = Seq(ES_TYPE_NAME)
      )

      // Пред-инстанс для доступа к инстансу билдера, т.к. super._builder() недоступен из-за val.
      val sd0 = SearchDefinition(iAndT)

      // Компилим и собираем итоговое добро. Выносим компиляцию за пределы инстанса для улучшения сборки мусора.
      val srb = prepareSearch(args, sd0._builder)
      new SearchDefinition(iAndT) {
        override val _builder = srb
      }
    }

  }

  override val Implicits = new Implicits


  // DynSearch v1 реализован через v2. TODO Спилить весь старый поиск (все dyn*() из v1 API).

  import Implicits._

  /**
   * Поиск карточек в ТЦ по критериям.
   *
   * @return Список рекламных карточек, подходящих под требования.
   */
  def dynSearch(dsa: A): Future[Seq[T]] = {
    search [Seq[T]] (dsa)
  }

  /**
   * Поиск и сборка карты результатов в id в качестве ключа.
   *
   * @param dsa Поисковые критерии.
   * @return Карта с найденными элементами в неопределённом порядке.
   */
  def dynSearchMap(dsa: A): Future[Map[String, T]] = {
    search [Map[String,T]] (dsa)
  }

  /**
   * Разновидность dynSearch для максимум одного результата. Вместо коллекции возвращается Option[T].
   *
   * @param dsa Аргументы поиска.
   * @return Фьючерс с Option[T] внутри.
   */
  def dynSearchOne(dsa: A): Future[Option[T]] = {
    search [Option[T]] (dsa)
  }

  /**
   * Аналог dynSearch, но возвращаются только id документов.
   *
   * @param dsa Поисковый запрос.
   * @return Список id, подходящих под запрос, в неопределённом порядке.
   */
  def dynSearchIds(dsa: A): Future[ISearchResp[String]] = {
    search [ISearchResp[String]] (dsa)
  }

  /**
   * Посчитать кол-во рекламных карточек, подходящих под запрос.
   *
   * @param dsa Экземпляр, описывающий критерии поискового запроса.
   * @return Фьючерс с кол-вом совпадений.
   */
  def dynCount(dsa: A): Future[Long] = {
    search[Long](dsa)
  }


  /** Поиск id, подходящих под запрос и последующий multiget. Используется для реалтаймого получения
    * изменчивых результатов, например поиск сразу после сохранения. */
  def dynSearchRt(dsa: A): Future[Seq[T]] = {
    search(dsa)( seqRtMapper )
  }

}


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
