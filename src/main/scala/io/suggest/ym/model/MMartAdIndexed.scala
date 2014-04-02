package io.suggest.ym.model

import io.suggest.util.{JacksonWrapper, MacroLogsImpl}
import io.suggest.model.inx2.MMartInx
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.action.search.SearchResponse
import org.elasticsearch.index.query.{QueryBuilder, FilterBuilders, QueryBuilders}
import io.suggest.util.SioEsUtil._
import scala.collection.JavaConversions._
import io.suggest.event.SioNotifierStaticClientI
import io.suggest.ym.model.MShop.ShopId_t
import org.elasticsearch.common.xcontent.XContentBuilder
import MMartAd.{SHOW_LEVELS_ESFN, USER_CAT_ID_ESFN}
import io.suggest.model.EsModel.SHOP_ID_ESFN
import io.suggest.util.SioConstants._
import io.suggest.util.text.TextQueryV2Util
import io.suggest.ym.model.common.AdReceiverInfo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.03.14 18:42
 * Description: MMartAdIndexed - экспорт-модель для MMartAd. Нужна для сохранения MMartAd при индексации.
 */

object MMartAdIndexed extends MacroLogsImpl {
  import LOGGER._

  val USER_CAT_STR_ESFN = "userCat.str"

  def generateMappingStaticFields = List(
    FieldAll(enabled = true, index_analyzer = EDGE_NGRAM_AN_1, search_analyzer = DFLT_AN),
    FieldSource(enabled = true)
  )

  def generateMappingProps: List[DocField] = {
    FieldString(USER_CAT_STR_ESFN, include_in_all = true, boost = Some(0.5F), index = FieldIndexingVariants.no) ::
    MMartAd.generateMappingProps
  }

  private def dummy(id: String, inx2: MMartInx) = MMartAdIndexed(
    mmartAd = MMartAd.dummy(id),
    userCatStr = null,
    showLevels1 = null,
    inx2 = inx2
  )

  /**
   * Десериализация одного элементам модели.
   * @param id id документа.
   * @param m Карта, распарсенное json-тело документа.
   * @return Экземпляр модели.
   */
  private def deserializeOne(id: String, m: collection.Map[String, AnyRef], inx2: MMartInx): MMartAdIndexed = {
    val acc = dummy(id, inx2)
    m foreach applyKeyValue(acc)
    acc
  }

  def applyKeyValue(acc: MMartAdIndexed): PartialFunction[(String, AnyRef), Unit] = {
    // Собираем partial-функцию, которая будет всё делать как надо. Чтобы типы аккамуляторов и врапперов были совместимы, тут небольшой велосипед:
    val fm = MMartAd.applyKeyValue(acc.mmartAd)
    val pf: PartialFunction[(String, AnyRef), Unit] = {
      case (USER_CAT_STR_ESFN, value)  => acc.userCatStr = JacksonWrapper.convert[List[String]](value)
      case other => fm(other)
    }
    pf
  }

  /**
   * Выбрать ряд из таблицы по id.
   * @param id Ключ магазина.
   * @return Экземпляр сабжа, если такой существует.
   */
  def getById(id: String, inx2: MMartInx)(implicit ec:ExecutionContext, client: Client): Future[Option[MMartAdIndexed]] = {
    val req = client.prepareGet(inx2.targetEsInxName, inx2.esType, id)
    req.execute()
      .map { getResp =>
        if (getResp.isExists) {
          val result = deserializeOne(getResp.getId, getResp.getSourceAsMap, inx2)
          Some(result)
        } else {
          None
        }
      }
  }

  /** Список результатов с source внутри перегнать в распарсенный список. */
  protected def searchResp2list(searchResp: SearchResponse, inx2: MMartInx): Seq[MMartAdIndexed] = {
    searchResp.getHits.getHits.toSeq.map { hit =>
      deserializeOne(hit.getId, hit.getSource, inx2)
    }
  }


  /**
   * Выдать все магазины. Метод подходит только для административных задач.
   * @return Список магазинов в порядке их создания.
   */
  def getAll(inx2: MMartInx)(implicit ec:ExecutionContext, client: Client): Future[Seq[MMartAdIndexed]] = {
    client.prepareSearch(inx2.targetEsInxName)
      .setTypes(inx2.esType)
      .setQuery(QueryBuilders.matchAllQuery())
      .execute()
      .map { searchResp2list(_, inx2) }
  }


  /**
   * Удалить документ по id.
   * @param id id документа.
   * @return true, если документ найден и удалён. Если не найден, то false
   */
  def deleteById(id: String, inx2: MMartInx)(implicit ec:ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    client.prepareDelete(inx2.targetEsInxName, inx2.esType, id)
      .execute()
      .map { _.isFound }
  }

  /**
   * Удалить все рекламные карточки указанного магазина.
   * @param shopId id магазина.
   * @param inx2 Метаданные об индексе.
   * @return Кол-во удалённых рядов.
   */
  def deleteByShop(shopId: ShopId_t, inx2: MMartInx)(implicit ec: ExecutionContext, client: Client): Future[Int] = {
    client.prepareDeleteByQuery(inx2.targetEsInxName)
      .setTypes(inx2.esType)
      .setQuery(MMartAd.shopSearchQuery(shopId))
      .execute()
      .map { _.iterator().size }
  }


  private def levelQuery(level: AdShowLevel)  = QueryBuilders.termQuery(SHOW_LEVELS_ESFN, level.toString)
  private def levelFilter(level: AdShowLevel) = FilterBuilders.termFilter(SHOW_LEVELS_ESFN, level.toString)

  /**
   * Поиск карточек в ТЦ по критериям.
   * @param inx2 Метаданные об индексе ТЦ.
   * @return Список рекламных карточек, подходящих под требования.
   */
  def find(inx2: MMartInx, adSearch: AdsSearchT)(implicit ec:ExecutionContext, client: Client): Future[Seq[MMartAdIndexed]] = {
    import adSearch._
    // Собираем запрос в функциональном стиле, иначе получается многовато вложенных if-else.
    val query: QueryBuilder = adSearch.qOpt.flatMap { q =>
      TextQueryV2Util.queryStr2QueryMarket(q)
    } map { qstrQB =>
      // Если shopId задан, то навешиваем фильтр.
      shopIdOpt.fold(qstrQB) { shopId =>
        val shopIdFilter = FilterBuilders.termFilter(SHOP_ID_ESFN, shopId)
        QueryBuilders.filteredQuery(qstrQB, shopIdFilter)
      }
    } orElse {
      // Не удалось собрать текстовый запрос. Если задан shopId, то собираем query по магазину.
      shopIdOpt.map { shopId =>
        QueryBuilders.termQuery(SHOP_ID_ESFN, shopId)
      }
    } map { qb =>
      // Если есть q или shopId и указана catId, то добавляем catId-фильтр.
      catIdOpt.fold(qb) { catId =>
        val catIdFilter = FilterBuilders.termFilter(USER_CAT_ID_ESFN, catId)
        QueryBuilders.filteredQuery(qb, catIdFilter)
      }
    } orElse {
      // Запроса всё ещё нет, т.е. собрать запрос по shopId тоже не удалось. Пробуем собрать запрос с catIdOpt...
      catIdOpt.map { catId =>
        QueryBuilders.termQuery(USER_CAT_ID_ESFN, catId)
      }
    } map { qb =>
      // Добавить фильтрацию по уровню, если он указан.
      levelOpt.fold(qb) { level =>
        QueryBuilders.filteredQuery(qb, levelFilter(level))
      }
    } orElse {
      // Сборка запроса по catId тоже не удалась. Просто ищем по уровню:
      levelOpt map levelQuery
    } getOrElse {
      // Сборка реквеста не удалась: все параметры не заданы. Просто возвращаем все объявы в рамках индекса.
      QueryBuilders.matchAllQuery()
    }
    // Запускаем собранный запрос.
    inx2.prepareSearchRequest(client.prepareSearch())
      .setQuery(query)
      .setSize(maxResults)
      .setFrom(offset)
      .execute()
      .map { searchResp2list(_, inx2) }
  }

}

import MMartAdIndexed.USER_CAT_STR_ESFN

/**
 * Экземпляр хорошо индексируемого [[MMartAd]]. Обладает полями, содержащими данные об индексе и индексируемом
 * названии категории.
 * @param mmartAd Исходный [[MMartAd]].
 * @param userCatStr Строки, собранная из названий индексируемых категорий. Используются для индексации.
 * @param showLevels1 Индексируемые уровни отображения этой карточки. Формируются на основе исходных уровней.
 * @param inx2 Данные об используемом индексе. НЕ сохраняются в БД.
 */
case class MMartAdIndexed(
  mmartAd          : MMartAd,
  var userCatStr   : List[String],
  var showLevels1  : Set[AdShowLevel],
  inx2             : MMartInx
) extends MMartAdWrapperT[MMartAd] {

  override def isFieldsValid: Boolean = super.isFieldsValid && inx2 != null
  override def showLevels = showLevels1

  override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (!userCatStr.isEmpty)
      acc.array(USER_CAT_STR_ESFN, userCatStr : _*)
  }

  override protected def esIndexName: String = inx2.targetEsInxName
  override protected def esTypeName: String  = inx2.esType

  /**
   * Удалить текущий документ из хранилища. Если ключ не выставлен, то сразу будет экзепшен.
   * @return true - всё ок, false - документ не найден.
   */
  override def delete(implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[Boolean] = {
    id match {
      case Some(_id) => MMartAdIndexed.deleteById(id.get, inx2)
      case None => ???
    }
  }
}


/** Интерфейс для передачи параметров поиска объявлений в индексе/типе. */
trait AdsSearchT {
  /** Необязательный id магазина. */
  def shopIdOpt: Option[ShopId_t]

  /** Необязательный id категории */
  def catIdOpt: Option[String]

  /** Какого уровня требуются карточки. */
  def levelOpt: Option[AdShowLevel]

  /** Произвольный текстовый запрос, если есть. */
  def qOpt: Option[String]

  /** Макс.кол-во результатов. */
  def maxResults: Int

  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  def offset: Int
}

