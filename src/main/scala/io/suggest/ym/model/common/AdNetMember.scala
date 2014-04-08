package io.suggest.ym.model.common

import io.suggest.ym.model.common.AdNetMemberTypes.AdNetMemberType
import io.suggest.model.{EsModel, EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.SioEsUtil._
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.collection.JavaConversions._
import EsModel._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.index.mapper.internal.AllFieldMapper
import io.suggest.ym.model.CompanyId_t

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:03
 * Description: Объект-участнник рекламной сети с произвольной ролью. Это может быть ТЦ, магазин или кто-то ещё.
 * Таким объектом владеют люди, и совершают действия от имени объекта.
 */

object AdNetMember {
  
  /** Название root-object поля, в котором хранятся данные по участию в рекламной сети. */
  val ADN_MEMBER_INFO_ESFN = "adnMemberInfo"

  // Имена полей вышеуказнного объекта
  val IS_PRODUCER_ESFN    = "isProd"
  val IS_RECEIVER_ESFN    = "isRcvr"
  val IS_SUPERVISOR_ESFN  = "isSup"
  val SUPERVISOR_ID_ESFN  = "supId"
  val MEMBER_TYPE_ESFN    = "mType"

  private def fullFN(subFN: String): String = ADN_MEMBER_INFO_ESFN + "." + subFN

  // Абсолютные (плоские) имена полей. Используются при поиске.
  val ADN_MI_IS_PRODUCER_ESFN   = fullFN(IS_PRODUCER_ESFN)
  val ADN_MI_IS_RECEIVER_ESFN   = fullFN(IS_RECEIVER_ESFN)
  val ADN_MI_IS_SUPERVISOR_ESFN = fullFN(IS_SUPERVISOR_ESFN)
  val ADN_MI_SUPERVISOR_ID_ESFN = fullFN(SUPERVISOR_ID_ESFN)
  val ADN_MI_MEMBER_TYPE_ESFN   = fullFN(MEMBER_TYPE_ESFN)

}

import AdNetMember._


/** Трейт для статической части модели участника рекламной сети. */
trait EMAdNetMemberStatic[T <: EMAdNetMember[T]] extends EsModelStaticT[T] {

  abstract override def generateMappingProps: List[DocField] = {
    import FieldIndexingVariants.not_analyzed
    FieldObject(ADN_MEMBER_INFO_ESFN, enabled = true, properties = Seq(
      FieldBoolean(IS_PRODUCER_ESFN, index = not_analyzed, include_in_all = false),
      FieldBoolean(IS_RECEIVER_ESFN, index = not_analyzed, include_in_all = false),
      FieldBoolean(IS_SUPERVISOR_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(SUPERVISOR_ID_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(MEMBER_TYPE_ESFN, index = not_analyzed, include_in_all = false)
    )) :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = super.applyKeyValue(acc) orElse {
    case (ADN_MEMBER_INFO_ESFN, value: java.util.Map[_,_]) =>
      if (acc.adnMemberInfo == null)
        acc.adnMemberInfo = new AdNetMemberInfo(AdNetMemberTypes.NOBODY)
      val mi = acc.adnMemberInfo
      value.foreach {
        case (IS_PRODUCER_ESFN, v)    => mi.isProducer = booleanParser(v)
        case (IS_RECEIVER_ESFN, v)    => mi.isReceiver = booleanParser(v)
        case (IS_SUPERVISOR_ESFN, v)  => mi.isSupervisor = booleanParser(v)
        case (SUPERVISOR_ID_ESFN, v)  => mi.supId = Option(stringParser(v))
        case (MEMBER_TYPE_ESFN, v)    => mi.memberType = AdNetMemberTypes.withName(stringParser(v))
      }
  }


  /**
   * Поиск по указанному запросу. Обычно используется для полнотекстового поиска, и исходный запрос генерится в
   * соответствующем генераторе текстовых запросов на основе строки, введённой пользователем.
   * @param searchQuery Поисковый запрос.
   * @param supId id супервизора. Можно указать при поиске подчиненных конкретному узлу сети.
   * @return Список результатов в порядке релевантности.
   */
  def searchAll(searchQuery: String, supId: Option[String] = None)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    var textQuery: QueryBuilder = QueryBuilders.fuzzyQuery(AllFieldMapper.NAME, searchQuery)
      .fuzziness(Fuzziness.AUTO)
      .prefixLength(2)
      .maxExpansions(20)
    if (supId.isDefined) {
      val martIdFilter = FilterBuilders.termFilter(SUPERVISOR_ID_ESFN, supId.get)
      textQuery = QueryBuilders.filteredQuery(textQuery, martIdFilter)
    }
    prepareSearch
      .setQuery(textQuery)
      .execute()
      .map { searchResp2list }
  }

  /**
   * Прочитать поле с id супервизора для указанного элемента.
   * @param id id узла рекламной сети.
   * @return Some(String) если документ найден и у него есть супервизор. Иначе false.
   */
  def getSupIdOf(id: String)(implicit ec: ExecutionContext, client: Client): Future[Option[String]] = {
    prepareGet(id)
      .setFetchSource(false)
      .setFields(ADN_MI_SUPERVISOR_ID_ESFN)
      .execute()
      .map { getResp =>
        // Если not found, то getFields() возвращает пустую карту.
        Option(getResp.getFields.get(ADN_MI_SUPERVISOR_ID_ESFN))
          .flatMap { field =>
            Option(stringParser(field.getValue))
          }
      }
  }

  /**
   * Найти все магазины, относящиеся к указанному ТЦ.
   * @param supId id непосредственного управляющего звена.
   * @param sortField Название поля, по которому надо сортировать результаты.
   * @param isReversed Если true, то сортировать будем в обратном порядке.
   *                   Игнорируется, если sortField не задано.
   * @param onlyEnabled Если true, то будет фильтр settings.supIsEnabled = true.
   * @param companyId Можно искать только в рамках указанной компании.
   * @return Список MShop в неопределённом порядке.
   */
  def findBySupId(supId: String, sortField: Option[String] = None, isReversed:Boolean = false, onlyEnabled: Boolean = false,
                  companyId: Option[CompanyId_t] = None)(implicit ec:ExecutionContext, client: Client): Future[Seq[T]] = {
    var query: QueryBuilder = supIdQuery(supId)
    if (onlyEnabled) {
      val isEnabledFilter = FilterBuilders.termFilter(EMAdnMPubSettings.PS_IS_ENABLED_ESFN, true)
      query = QueryBuilders.filteredQuery(query, isEnabledFilter)
    }
    if (companyId.isDefined) {
      val ciFilter = FilterBuilders.termFilter(COMPANY_ID_ESFN, companyId)
      query = QueryBuilders.filteredQuery(query, ciFilter)
    }
    val req = prepareSearch
      .setQuery(query)
    if (sortField.isDefined)
      req.addSort(sortField.get, isReversed2sortOrder(isReversed))
    req.execute()
      .map { searchResp2list }
  }

  def supIdQuery(supId: String) = QueryBuilders.termQuery(ADN_MI_SUPERVISOR_ID_ESFN, supId)

  /**
   * Подсчет узлов, принадлежащих указанному супервизору.
   * @param supId id супервизора.
   * @return Неотрицательное кол-во найденных элементов.
   */
  def countBySupId(supId: String)(implicit ec:ExecutionContext, client: Client): Future[Long] = {
    prepareCount
      .setQuery(supIdQuery(supId))
      .execute()
      .map { _.getCount }
  }

}


/** Трейт для экземпляра модели участника рекламной сети. */
trait EMAdNetMember[T <: EMAdNetMember[T]] extends EsModelT[T] {
  var adnMemberInfo: AdNetMemberInfo

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    // Не используем jackson для ускорения и из-за присутствия полей с enum-типами.
    acc.startObject(ADN_MEMBER_INFO_ESFN)
    adnMemberInfo.writeFields(acc)
    acc.endObject()
  }
}


/** Инфа об участнике рекламной сети. Все параметры его участия свернуты в один объект. */
case class AdNetMemberInfo(
  var memberType: AdNetMemberType,
  var isProducer: Boolean = false,
  var isReceiver: Boolean = false,
  var isSupervisor: Boolean = false,
  var supId: Option[String] = None
) {
  @JsonIgnore
  def writeFields(acc: XContentBuilder) {
    acc.field(IS_PRODUCER_ESFN, isProducer)
      .field(IS_RECEIVER_ESFN, isReceiver)
      .field(MEMBER_TYPE_ESFN, memberType.toString)
      .field(IS_SUPERVISOR_ESFN, isSupervisor)
    if (supId.isDefined)
      acc.field(SUPERVISOR_ID_ESFN, supId.get)
  }
}

