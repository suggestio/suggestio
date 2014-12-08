package io.suggest.ym.model.common

import io.suggest.ym.model.common.AdNetMemberTypes.AdNetMemberType
import io.suggest.model._
import io.suggest.util.SioEsUtil._
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.collection.JavaConversions._
import EsModel._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{FilterBuilder, FilterBuilders, QueryBuilder, QueryBuilders}
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.index.mapper.internal.AllFieldMapper
import io.suggest.ym.model.{MAdnNode, AdShowLevel}
import java.{util => ju, lang => jl}
import io.suggest.event.{AdnNodeDeletedEvent, AdnNodeOnOffEvent, SioNotifierStaticClientI}
import play.api.libs.json._
import io.suggest.event.subscriber.SnFunSubscriber

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:03
 * Description: Объект-участнник рекламной сети с произвольной ролью. Это может быть ТЦ, магазин или кто-то ещё.
 * Таким объектом владеют люди, и совершают действия от имени объекта.
 */

object AdNetMember {
  import AdnRights.AdnRight

  /** Название root-object поля, в котором хранятся данные по участию в рекламной сети. */
  val ADN_ESFN = "adn"

  // Имена полей вышеуказнного объекта
  val SUPERVISOR_ID_ESFN  = "supId"
  val MEMBER_TYPE_ESFN    = "mType"
  val RIGHTS_ESFN         = "rights"

  /** Option[String] поле, содержит id узла-делегата размещения рекламных карточек.
    * К такому узлу рекламные карточки попадают на модерацию. */
  val ADV_DELEGATE_ESFN = "advDg"

  /** Название поля с флагом тестового узла. */
  val TEST_NODE_ESFN = "tn"

  val IS_ENABLED_ESFN = "isEnabled"
  val SHOW_LEVELS_ESFN = "showLevels"
  val DISABLE_REASON_ESFN = "disableReason"

  /** id отображаемого типа узла. */
  val SHOWN_TYPE_ID_ESFN = "sti"

  /** Поле с sink-флагами, которые описывают возможности узла по рекламным выдачам. */
  val SINKS_ESFN = "sink"


  private def fullFN(subFN: String): String = ADN_ESFN + "." + subFN

  // Абсолютные (плоские) имена полей. Используются при поисковых запросах.
  def ADN_SUPERVISOR_ID_ESFN  = fullFN(SUPERVISOR_ID_ESFN)
  def ADN_MEMBER_TYPE_ESFN    = fullFN(MEMBER_TYPE_ESFN)
  def ADN_ADV_DELEGATE_ESFN   = fullFN(ADV_DELEGATE_ESFN)
  def ADN_RIGHTS_ESFN         = fullFN(RIGHTS_ESFN)
  def ADN_TEST_NODE_ESFN      = fullFN(TEST_NODE_ESFN)
  def ADN_IS_ENABLED_ESFN     = fullFN(IS_ENABLED_ESFN)
  def ADN_SINKS_ESFN          = fullFN(SINKS_ESFN)
  def ADN_SHOWN_TYPE_ID       = fullFN(SHOWN_TYPE_ID_ESFN)


  /** Генератор es-query для указанного member type.
    * @param memberType Необходимый тип участников.
    * @return QueryBuilder.
    */
  def adnMemberTypeQuery(memberType: AdNetMemberType) = {
    QueryBuilders.termQuery(ADN_MEMBER_TYPE_ESFN, memberType.toString())
  }

  def supIdQuery(supId: String): QueryBuilder = {
    QueryBuilders.termQuery(ADN_SUPERVISOR_ID_ESFN, supId)
  }

  /**
   * Генератор es-query для поиска по id делегата размещения.
   * @param adnId id узла-делегата.
   * @return QueryBuilder.
   */
  def advDelegatesQuery(adnId: String): QueryBuilder = {
    QueryBuilders.termQuery(ADN_ADV_DELEGATE_ESFN, adnId)
  }


  /**
   * Собрать es query для поиска по полю adn-прав. Искомый объект обязан обладать всеми перечисленными правами.
   * @param rights Список прав, по которым ищем.
   * @return ES Query.
   */
  def adnRightsAllQuery(rights: Seq[AdnRight]): QueryBuilder = {
    QueryBuilders
      .termsQuery(ADN_RIGHTS_ESFN, rights.map(_.toString()) : _*)
      .minimumMatch(rights.size)
  }

  /** Добавить фильтр, отсеивающий test-узлы. */
  def filterOutTestNodes(qb0: QueryBuilder): QueryBuilder = {
    // Фильтруем инвертированно (tn != true), т.к. изначально у узлов не было флагов TEST_NODE.
    val filter = FilterBuilders.notFilter(
      FilterBuilders.termFilter(ADN_TEST_NODE_ESFN, true))
    QueryBuilders.filteredQuery(qb0, filter)
  }

}


/** Выходы узла для отображения рекламных карточек. */
object AdnSinks extends Enumeration {
  protected case class Val(name: String, longName: String, sioComissionDflt: Float) extends super.Val(name) with SlNameTokenStr
  type AdnSink = Val
  implicit def value2val(x: Value): AdnSink = x.asInstanceOf[AdnSink]

  val SINK_WIFI: AdnSink = Val("w", "wifi", 0.30F)
  val SINK_GEO: AdnSink  = Val("g", "geo", 0.70F)

  def ordered: Seq[AdnSink] = {
    values
      .foldLeft( List.empty[AdnSink] ) { (acc, e) => e :: acc }
      .sortBy(_.longName)
  }

  def default = SINK_WIFI

  def maybeWithName(n: String): Option[AdnSink] = {
    values
      .find(_.name == n)
      .asInstanceOf[Option[AdnSink]]
  }

  def maybeWithLongName(ln: String): Option[AdnSink] = {
    values
      .find(_.longName == ln)
      .asInstanceOf[Option[AdnSink]]
  }
}



/** Положение участника сети и его возможности описываются флагами прав доступа. */
object AdnRights extends Enumeration {
  protected case class Val(name: String, longName: String) extends super.Val(name)
  type AdnRight = Val
  implicit def value2val(x: Value): AdnRight = x.asInstanceOf[AdnRight]

  /** Продьюсер может создавать свою рекламу. */
  val PRODUCER: AdnRight = Val("p", "producer")

  /** Ресивер может отображать в выдаче и просматривать в ЛК рекламу других участников, которые транслируют свою
    * рекламу ему через receivers. Ресивер также может приглашать новых участников. */
  val RECEIVER: AdnRight = Val("r", "receiver")

  /** Супервизор может управлять рекламной сетью и модерировать рекламные карточки. */
  val SUPERVISOR: AdnRight = Val("s", "supervisor")
}


import AdnRights._
import AdnSinks._
import AdNetMember._


/** Трейт для статической части модели участника рекламной сети. */
trait EMAdNetMemberStatic extends EsModelStaticMutAkvT with EsModelStaticT {

  override type T <: EMAdNetMember

  abstract override def generateMappingProps: List[DocField] = {
    import FieldIndexingVariants.not_analyzed
    FieldObject(ADN_ESFN, enabled = true, properties = Seq(
      FieldString(RIGHTS_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(SUPERVISOR_ID_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(MEMBER_TYPE_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(SHOWN_TYPE_ID_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(ADV_DELEGATE_ESFN, index = not_analyzed, include_in_all = false),
      FieldBoolean(TEST_NODE_ESFN, index = not_analyzed, include_in_all = false),
      // раньше это лежало в EMAdnMPubSettings, но потом было перемещено сюда, т.к. по сути это разделение было некорректно.
      FieldBoolean(IS_ENABLED_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(DISABLE_REASON_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldObject(SHOW_LEVELS_ESFN, enabled = false, properties = Nil),
      FieldString(SINKS_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false)
    )) :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = super.applyKeyValue(acc) orElse {
    case (ADN_ESFN, vm: java.util.Map[_,_]) =>
      // Билдим инстанс ANMI как будто он неизменяемый. Потом, возможно, полностью уйдём от var в полях ANMI.
      acc.adn = AdNetMemberInfo(
        memberType = Option(vm get MEMBER_TYPE_ESFN)
          .fold(AdNetMemberTypes.SHOP) { mtRaw => AdNetMemberTypes.withName(stringParser(mtRaw)) : AdNetMemberType },
        rights = Option(vm get RIGHTS_ESFN).fold(Set.empty[AdnRight]) {
          case l: jl.Iterable[_] =>
            l.map { rid => AdnRights.withName(rid.toString) : AdnRight }.toSet
        },
        shownTypeIdOpt = Option(vm get SHOWN_TYPE_ID_ESFN) map stringParser,
        supId = Option(vm get SUPERVISOR_ID_ESFN) map stringParser,
        advDelegate = Option(vm get ADV_DELEGATE_ESFN) map stringParser,
        testNode = Option(vm get TEST_NODE_ESFN)
          .fold(false)(booleanParser),
        showLevelsInfo = Option(vm get SHOW_LEVELS_ESFN)
          .fold(AdnMemberShowLevels()) { AdnMemberShowLevels.deserialize(_) },
        isEnabled = Option(vm get IS_ENABLED_ESFN)
          .fold(true)(booleanParser),
        disableReason = Option(vm get DISABLE_REASON_ESFN) map stringParser,
        sinks = Option(vm get SINKS_ESFN).fold(Set.empty[AdnSink]) {
          case ls: jl.Iterable[_] =>
            ls.map { sinkIdRaw => AdnSinks.withName(stringParser(sinkIdRaw)) : AdnSink }
              .toSet
        }
      )
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
    val req = prepareSearch
      .setQuery(textQuery)
    runSearch(req)
  }

  /**
   * Прочитать поле с id супервизора для указанного элемента.
   * @param id id узла рекламной сети.
   * @return Some(String) если документ найден и у него есть супервизор. Иначе false.
   */
  def getSupIdOf(id: String)(implicit ec: ExecutionContext, client: Client): Future[Option[String]] = {
    prepareGet(id)
      .setFetchSource(false)
      .setFields(ADN_SUPERVISOR_ID_ESFN)
      .execute()
      .map { getResp =>
        // Если not found, то getFields() возвращает пустую карту.
        Option(getResp.getFields.get(ADN_SUPERVISOR_ID_ESFN))
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
                  companyId: Option[String] = None, maxResults: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
                 (implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    var query: QueryBuilder = supIdQuery(supId)
    if (onlyEnabled) {
      val isEnabledFilter = FilterBuilders.termFilter(ADN_IS_ENABLED_ESFN, true)
      query = QueryBuilders.filteredQuery(query, isEnabledFilter)
    }
    if (companyId.isDefined) {
      val ciFilter = FilterBuilders.termFilter(EMCompanyId.COMPANY_ID_ESFN, companyId)
      query = QueryBuilders.filteredQuery(query, ciFilter)
    }
    val req = prepareSearch
      .setQuery(query)
    if (sortField.isDefined)
      req.addSort(sortField.get, isReversed2sortOrder(isReversed))
    req
      .setSize(maxResults)
      .setFrom(offset)
    runSearch(req)
  }


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


  /**
   * Статическое обновление сеттингов isEnabled и disabledReason.
   * @param adnId id изменяемого магазина
   * @param isEnabled Новое значение поля isEnabled.
   * @param reason Причина изменения статуса.
   * @return Фьючерс. Внутри, скорее всего, лежит UpdateResponse.
   */
  def setIsEnabled(adnId: String, isEnabled: Boolean, reason: Option[String])
                  (implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    var jsonFieldsAcc: FieldsJsonAcc = List(
      IS_ENABLED_ESFN -> JsBoolean(isEnabled)
    )
    if (reason.isDefined)
      jsonFieldsAcc ::= DISABLE_REASON_ESFN -> JsString(reason.get)
    val updateJson = JsObject(Seq(
      ADN_ESFN -> JsObject(jsonFieldsAcc)
    ))
    val fut: Future[_] = prepareUpdate(adnId)
      .setDoc(updateJson.toString())
      .execute()
    // Уведомить о переключении состояния магазина
    fut onSuccess { case _ =>
      sn publish AdnNodeOnOffEvent(adnId, isEnabled)
    }
    fut
  }

  /**
   * Выдать по id и желаемому типу.
   * @param id id документа.
   * @param memberType Необходимый adn.memberType.
   * @return Фьючерс с опшеном. Т.е. тоже самое, что и getById().
   */
  def getByIdType(id: String, memberType: AdNetMemberType)(implicit ec: ExecutionContext, client: Client): Future[Option[T]] = {
    // TODO Надо какой-то более эффективный метод работы, чтобы фильтрация была на стороне ES.
    getById(id) map {
      _.filter(_.adn.memberType == memberType)
    }
  }


  /**
   * Найти все документы, но только указанного типа.
   * @param memberType Искомый тип участника рекламной сети.
   * @param maxResult Макс.число результатов.
   * @param offset Сдвиг.
   * @return Последовательность результатов в неопределённом порядке.
   */
  def findAllByType(memberType: AdNetMemberType, maxResult: Int = MAX_RESULTS_DFLT, offset: Int = OFFSET_DFLT)
                   (implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    val query = adnMemberTypeQuery(memberType)
    val req = prepareSearch
      .setQuery(query)
      .setSize(maxResult)
      .setFrom(offset)
    runSearch(req)
  }



  def findByAllAdnRightsBuilder(rights: Seq[AdnRight], withoutTestNodes: Boolean)(implicit client: Client) = {
    var query: QueryBuilder = adnRightsAllQuery(rights)
    if (withoutTestNodes)
      query = filterOutTestNodes(query)
    prepareSearch
      .setQuery(query)
  }

  /**
   * Найти по правам (ролям) узла в сети.
   * @param rights Список прав, по которым происходит поиск.
   * @return Фьючерс со списком результатов в неопределённом порядке.
   */
  def findByAllAdnRights(rights: Seq[AdnRight], withoutTestNodes: Boolean, maxResults: Int = MAX_RESULTS_DFLT)
                        (implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    val req = findByAllAdnRightsBuilder(rights, withoutTestNodes)
      .setSize(maxResults)
    runSearch(req)
  }


  def findIdsByAllAdnRightsBuilder(rights: Seq[AdnRight], withoutTestNodes: Boolean)(implicit client: Client) = {
    findByAllAdnRightsBuilder(rights, withoutTestNodes)
      .setFetchSource(false)
      .setNoFields()
  }

  /**
   * Тоже самое, что и findByAllAdnRights(), но возвращает только список id'шников.
   * @param rights Права.
   * @return Фьючерс со списком id в неопределённом порядке.
   */
  def findIdsByAllAdnRights(rights: Seq[AdnRight], withoutTestNodes: Boolean)(implicit ec: ExecutionContext, client: Client): Future[Seq[String]] = {
    findIdsByAllAdnRightsBuilder(rights, withoutTestNodes)
      .execute()
      .map { searchResp2idsList }
  }


  // Поиски по полю adn.advDelegate

  /**
   * Найти все узлы, которые делегировали свои полномочия размещения рекл.карточек (adv) указанным узлам.
   * @param dgAdnId id узла-делегата.
   * @return Фьючерс со списком результатов в неопределённом порядке.
   */
  def findAdvDelegatedTo(dgAdnId: String, maxResults: Int = MAX_RESULTS_DFLT)(implicit ec: ExecutionContext, client: Client): Future[Seq[T]] = {
    val req = prepareSearch
      .setQuery( advDelegatesQuery(dgAdnId) )
      .setSize( maxResults )
    runSearch(req)
  }

  /**
   * Найти id документов, которые делегировали свои полномочия размещения рекл.карточек (adv) указанным узлам.
   * @param dgAdnId id узла-делегата.
   * @return Список id документов в неопределённом порядке.
   */
  def findIdsAdvDelegatedTo(dgAdnId: String, maxResults: Int = MAX_RESULTS_DFLT)(implicit ec: ExecutionContext, client: Client): Future[Seq[String]] = {
    prepareSearch
      .setQuery( advDelegatesQuery(dgAdnId) )
      .setSize( maxResults )
      .setFetchSource(false)
      .setNoFields()
      .execute()
      .map { searchResp2idsList }
  }
}


/** Трейт для экземпляра модели участника рекламной сети. */
trait EMAdNetMember extends EsModelPlayJsonT with EsModelT {
  override type T <: EMAdNetMember

  var adn: AdNetMemberInfo

  // Ограничиваем тип объекта-компаньона, чтобы можно было дергать статический setIsEnabled().
  override def companion: EMAdNetMemberStatic

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    ADN_ESFN -> adn.toPlayJson :: super.writeJsonFields(acc)
  }


  /** Быстрый доступ к статическому EMAdNetMemberStatic.setIsEnabled().
    * @param isEnabled Новое значение isEnabled.
    * @param reason Причина отлючения.
    * @return Фьючерс для синхронизации.
    */
  def setIsEnabled(isEnabled: Boolean, reason: Option[String])
                  (implicit ec: ExecutionContext, client: Client, sn: SioNotifierStaticClientI): Future[_] = {
    companion.setIsEnabled(id.get, isEnabled, reason)
  }


  /**
   * Прочитать из хранилище супервизора, если он указан в соотв. поле. Считается, что супервизоры
   * хранятся в [[io.suggest.ym.model.MAdnNode]].
   * @return None если не указан или не найден. Иначе Some([[io.suggest.ym.model.MAdnNode]]).
   */
  def getSup(implicit ec: ExecutionContext, client: Client): Future[Option[MAdnNode.T]] = {
    adn.supId match {
      case Some(supId)  => MAdnNode.getById(supId)
      case None         => Future successful None
    }
  }

}


/** Инфа об участнике рекламной сети. Все параметры его участия свернуты в один объект.
  * @param memberType Тип участника. Например, магазин.
  * @param rights Права участника сети.
  * @param shownTypeIdOpt ID отображаемого типа участника сети. Нужно для задания кастомных типов на стороне web21.
  *                       Появилось, когда понадобилось обозначить торговый центр вокзалом/портом, не меняя его свойств.
  * @param supId Опциональный id супер-узла.
  * @param advDelegate Опциональный id узла, который совершает управление размещением рекламных карточек на данном узле.
  * @param testNode Отметка о тестовом характере существования этого узла.
  *                 Он не должен отображаться для обычных участников сети, а только для других тестовых узлов.
  * @param showLevelsInfo Контейнер с инфой об уровнях отображения.
  * @param isEnabled Включен ли узел? Отключение узла приводит к блокировке некоторых функций.
  * @param disableReason Причина отключения узла, если есть.
  */
case class AdNetMemberInfo(
  memberType      : AdNetMemberType,
  rights          : Set[AdnRight],
  shownTypeIdOpt  : Option[String] = None,
  supId           : Option[String] = None,
  advDelegate     : Option[String] = None,
  testNode        : Boolean = false,
  showLevelsInfo  : AdnMemberShowLevels = AdnMemberShowLevels(),
  isEnabled       : Boolean = true,
  disableReason   : Option[String] = None,
  sinks           : Set[AdnSink] = Set.empty
) {

  /** Отображаемый для юзера id типа узла. */
  def shownTypeId: String = shownTypeIdOpt getOrElse memberType.name

  // Быстрый доступ к каталогу adn-прав
  @JsonIgnore
  def isProducer: Boolean = rights contains PRODUCER
  @JsonIgnore
  def isReceiver: Boolean = rights contains RECEIVER
  @JsonIgnore
  def isSupervisor: Boolean = rights contains SUPERVISOR


  /** Быстрая и простая проверка на наличие wifi sink во флагах. */
  @JsonIgnore
  def hasWifiSink = hasSink(AdnSinks.SINK_WIFI)

  /** Быстрая и простая проверка на наличие синка с геолокацией. */
  @JsonIgnore
  def hasGeoSink  = hasSink(AdnSinks.SINK_GEO)

  def hasSink(sink: AdnSink): Boolean = sinks contains sink

  /** Сериализовать в JSON поля текущего экземпляра класса. */
  @JsonIgnore
  def toPlayJson: JsObject = {
    var acc0: FieldsJsonAcc = List(
      MEMBER_TYPE_ESFN   -> JsString(memberType.toString()),
      TEST_NODE_ESFN     -> JsBoolean(testNode),
      IS_ENABLED_ESFN    -> JsBoolean(isEnabled)
    )
    if (rights.nonEmpty) {
      val rightsJson = rights.foldLeft[List[JsString]] (Nil) {
        (acc, e) => JsString(e.toString) :: acc
      }
      acc0 ::= RIGHTS_ESFN -> JsArray(rightsJson)
    }
    if (shownTypeIdOpt.isDefined)
      acc0 ::= SHOWN_TYPE_ID_ESFN -> JsString(shownTypeIdOpt.get)
    if (supId.isDefined)
      acc0 ::= SUPERVISOR_ID_ESFN -> JsString(supId.get)
    if (advDelegate.isDefined)
      acc0 ::= ADV_DELEGATE_ESFN -> JsString(advDelegate.get)
    if (!showLevelsInfo.isEmpty)
      acc0 ::= SHOW_LEVELS_ESFN -> showLevelsInfo.toPlayJson
    if (disableReason.isDefined)
      acc0 ::= DISABLE_REASON_ESFN -> JsString(disableReason.get)
    if (sinks.nonEmpty) {
      val sinkIds = sinks
        .map { sink => JsString(sink.name) }
        .toSeq
      acc0 ::= SINKS_ESFN -> JsArray(sinkIds)
    }
    JsObject(acc0)
  }

  // перемещено из mpub:
  /**
   * Выдать карту допустимых out-уровней. Из-за необходимости доступа к isEnabled находится вне showLevelsInfo.
   * @return Если isEnabled = false, то будет пустая карта.
   */
  @JsonIgnore
  def maybeOutShowLevels: AdnMemberShowLevels.LvlMap_t = {
    if (isEnabled)
      showLevelsInfo.out
    else
      Map.empty
  }

  // Врапперы над соответсвующими фунцкиями showLevelsInfo, которые учитывают флаг isEnabled.
  def canOutAtLevel(sl: AdShowLevel) = isEnabled && showLevelsInfo.canOutAtLevel(sl)
  def canInAtLevel(sl: AdShowLevel)  = isEnabled && showLevelsInfo.canInAtLevel(sl)
  def maxOutAtLevel(sl: AdShowLevel) = if (isEnabled) showLevelsInfo.maxOutAtLevel(sl) else 0
  def maxInAtLevel(sl: AdShowLevel)  = if (isEnabled) showLevelsInfo.maxInAtLevel(sl) else 0

  /**
   * Выдать карту допустимых in-уровней. Если disabled, то будет пустая карта.
   * @return Карта типа LvlMap_t.
   */
  @JsonIgnore
  def maybeInShowLevels: AdnMemberShowLevels.LvlMap_t = {
    if (isEnabled)
      showLevelsInfo.in
    else
      Map.empty
  }

}


object AdnMemberShowLevels {
  type LvlMap_t = Map[AdShowLevel, Int]

  val IN_ESFN = "in"
  val OUT_ESFN = "out"

  /** Функция-десериализатор карты уровней. */
  val deserializeLevelsMap: PartialFunction[Any, LvlMap_t] = {
    case null =>
      Map.empty

    case rawLevelsMap: ju.Map[_,_] =>
      rawLevelsMap.foldLeft[List[(AdShowLevel, Int)]] (Nil) {
        case (mapAcc, (aslStr, count)) =>
          val k: AdShowLevel = AdShowLevels.withName(aslStr.toString)
          val v = count match {
            case n: java.lang.Number => n.intValue()
          }
          k -> v :: mapAcc
      }.toMap
  }

  /**
   * Десериализатор значения.
   * @param raw Выхлоп jackson'a.
   * @param acc Необязательный начальный аккамулятор.
   * @return Десериализованный экземпляр класса.
   */
  def deserialize(raw: Any, acc: AdnMemberShowLevels = AdnMemberShowLevels()): AdnMemberShowLevels = {
    raw match {
      case rawMpsl: ju.Map[_,_] =>
        rawMpsl.foreach { case (k, v) =>
          val lvlMap = deserializeLevelsMap(v)
          k match {
            case IN_ESFN  => acc.in = lvlMap
            case OUT_ESFN => acc.out = lvlMap
          }
        }

      case null => // Do nothing
    }
    acc
  }


  /** Опционально отрендерить карту полей в поле в play.json в аккамулятор. */
  private def maybeRenderLevelsMapPlayJson(name: String, levelsMap: LvlMap_t, acc: FieldsJsonAcc): FieldsJsonAcc = {
    if (levelsMap.nonEmpty) {
      val mapElements = levelsMap.foldLeft[FieldsJsonAcc] (Nil) {
        case (facc, (sl, max))  =>  sl.toString -> JsNumber(max) :: facc
      }
      name -> JsObject(mapElements) :: acc
    } else {
      acc
    }
  }
  
  // Хелперы для класса-компаньона.

  /** Определить макс.число карточек на указанном уровне с помощью указанной карты.
    * @param lvl Уровень.
    * @param levelsMap Карта уровней.
    * @return Неотрицательное целое. Если уровень запрещён, то будет 0.
    */
  def maxAtLevel(lvl: AdShowLevel, levelsMap: LvlMap_t): Int = {
    levelsMap.getOrElse(lvl, 0)
  }

  /**
   * Определить, возможно ли вообще что-либо постить/принимать на указанном уровне отображения.
   * @param lvl Уровень.
   * @param levelsMap Карта уровней.
   * @return true - значит карта допускает работу на этом уровне.
   */
  def canAtLevel(lvl: AdShowLevel, levelsMap: LvlMap_t): Boolean = {
    levelsMap.get(lvl).exists(isPossibleLevel)
  }

  private def isPossibleLevel(max: Int) = max > 0

  private def sls4render(sls: LvlMap_t) = {
    sls.toSeq
      .sortBy(_._1.visualPrio)
      .map {
        case (sl, slMax)  =>  sl -> (isPossibleLevel(slMax), slMax)
      }
  }
}

import AdnMemberShowLevels._

/**
 * Данные по допустимым уровням отображения для входящих и исходящих публикаций.
 * @param in Карта уровней для входящих публикаций.
 * @param out Карта уровней для исходящих публикаций.
 */
case class AdnMemberShowLevels(
  var in:  AdnMemberShowLevels.LvlMap_t = Map.empty,
  var out: AdnMemberShowLevels.LvlMap_t = Map.empty
) {

  @JsonIgnore
  def toPlayJson: JsObject = {
    var acc = maybeRenderLevelsMapPlayJson(IN_ESFN, in, Nil)
    acc = maybeRenderLevelsMapPlayJson(OUT_ESFN, out, acc)
    JsObject(acc)
  }

  @JsonIgnore
  def isEmpty = in.isEmpty && out.isEmpty

  def canOutAtLevel(lvl: AdShowLevel) = canAtLevel(lvl, out)
  def maxOutAtLevel(lvl: AdShowLevel) = maxAtLevel(lvl, out)

  def canInAtLevel(lvl: AdShowLevel) = canAtLevel(lvl, in)
  def maxInAtLevel(lvl: AdShowLevel) = maxAtLevel(lvl, in)

  // Для рендера галочек нужна модифицированная карта.
  def out4render = sls4render(out)
}



// Аддоны для DynSearch-поиска
/** Аддон с поддержкой поиска по полю adn.supId . */
trait AdnSupIdsDsa extends DynSearchArgs {

  /** Искать/фильтровать по id супервизора узла. */
  def adnSupIds: Seq[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      // Отрабатываем adnSupIds:
      if (adnSupIds.isEmpty) {
        qb
      } else {
        val sf = FilterBuilders.termsFilter(ADN_SUPERVISOR_ID_ESFN, adnSupIds : _*)
          .execution("or")
        QueryBuilders.filteredQuery(qb, sf)
      }
    }.orElse[QueryBuilder] {
      if (adnSupIds.nonEmpty) {
        val sq = QueryBuilders.termsQuery(ADN_SUPERVISOR_ID_ESFN, adnSupIds : _*)
          .minimumMatch(1)
        Some(sq)
      } else {
        None
      }
    }
  }

  override def sbInitSize: Int = {
    collStringSize(adnSupIds, super.sbInitSize)
  }

  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("adnSupIds", adnSupIds, super.toStringBuilder)
  }

}

trait AdnSupIdsDsaDflt extends AdnSupIdsDsa {
  override def adnSupIds: Seq[String] = Seq.empty
}

trait AdnSupIdsDsaWrapper extends AdnSupIdsDsa with DynSearchArgsWrapper {
  override type WT <: AdnSupIdsDsa
  override def adnSupIds = _dsArgsUnderlying.adnSupIds
}



/** Аддон с поддержкой поиска по полю advDelegateAdnIds. */
trait AdvDelegateAdnIdsDsa extends DynSearchArgs {

  /** Искать/фильтровать по id узла, которому была делегирована фунция модерации размещения рекламных карточек. */
  def advDelegateAdnIds: Seq[String]

  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      // Отрабатываем id узлов adv-делегатов
      if (advDelegateAdnIds.isEmpty) {
        qb
      } else {
        val af = FilterBuilders.termsFilter(ADN_ADV_DELEGATE_ESFN, advDelegateAdnIds : _*)
          .execution("or")
        QueryBuilders.filteredQuery(qb, af)
      }
    }.orElse[QueryBuilder] {
      if (advDelegateAdnIds.isEmpty) {
        None
      } else {
        val aq = QueryBuilders.termsQuery(ADN_ADV_DELEGATE_ESFN, advDelegateAdnIds: _*)
          .minimumMatch(1)
        Some(aq)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(advDelegateAdnIds, super.sbInitSize)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("advDelegateAdnIds", advDelegateAdnIds, super.toStringBuilder)
  }
}

trait AdvDelegateAdnIdsDsaDflt extends AdvDelegateAdnIdsDsa {
  override def advDelegateAdnIds: Seq[String] = Seq.empty
}

trait AdvDelegateAdnIdsDsaWrapper extends AdvDelegateAdnIdsDsa with DynSearchArgsWrapper {
  override type WT <: AdvDelegateAdnIdsDsa
  override def advDelegateAdnIds = _dsArgsUnderlying.advDelegateAdnIds
}


/** Поддержка dyn-поиска по полю shownTypeIds. */
trait ShownTypeIdsDsa extends DynSearchArgs {

  /** Искать/фильтровать по shownTypeId узла. */
  def shownTypeIds: Seq[String]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      // Поиск/фильтрация по полю shown type id, хранящий id типа узла.
      if (shownTypeIds.isEmpty) {
        qb
      } else {
        val stiFilter = FilterBuilders.termsFilter(ADN_SHOWN_TYPE_ID, shownTypeIds : _*)
        QueryBuilders.filteredQuery(qb, stiFilter)
      }
    }.orElse[QueryBuilder] {
      if (shownTypeIds.isEmpty) {
        None
      } else {
        val stiQuery = QueryBuilders.termsQuery(ADN_SHOWN_TYPE_ID, shownTypeIds : _*)
          .minimumMatch(1)  // может быть только один тип ведь у одного узла.
        Some(stiQuery)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(shownTypeIds, super.sbInitSize)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("shownTypeIds", shownTypeIds, super.toStringBuilder)
  }
}

trait ShownTypeIdsDsaDflt extends ShownTypeIdsDsa {
  override def shownTypeIds: Seq[String] = Seq.empty
}

trait ShownTypeIdsDsaWrapper extends ShownTypeIdsDsa with DynSearchArgsWrapper {
  override type WT <: ShownTypeIdsDsa
  override def shownTypeIds = _dsArgsUnderlying.shownTypeIds
}



/** Аддон для поиска по полю adn.rigths. */
trait AdnRightsDsa extends DynSearchArgs {

  /** Права, которые должны быть у узла. */
  def withAdnRights: Seq[AdnRight]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      if (withAdnRights.isEmpty) {
        qb
      } else {
        val rf = FilterBuilders.termsFilter(ADN_RIGHTS_ESFN, withAdnRights.map(_.name) : _*)
          .execution("and")
        QueryBuilders.filteredQuery(qb, rf)
      }
    }.orElse[QueryBuilder] {
      if (withAdnRights.isEmpty) {
        None
      } else {
        val rq = QueryBuilders.termsQuery(ADN_RIGHTS_ESFN, withAdnRights.map(_.name): _*)
          .minimumMatch(withAdnRights.size)
        Some(rq)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(withAdnRights, super.sbInitSize, addOffset = 10)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("withAdnRights", withAdnRights, super.toStringBuilder)
  }
}

trait AdnRightsDsaDflt extends AdnRightsDsa {
  override def withAdnRights: Seq[AdnRight] = Seq.empty
}

trait AdnRightsDsaWrapper extends AdnRightsDsa with DynSearchArgsWrapper {
  override type WT <: AdnRightsDsa
  override def withAdnRights = _dsArgsUnderlying.withAdnRights
}



/** Аддон для поиска по синкам узла. */
trait AdnSinksDsa extends DynSearchArgs {

  /** Искать/фильтровать по доступным sink'ам. */
  def onlyWithSinks: Seq[AdnSink]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    // Отрабатываем возможный список прав узла.
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      // Ищем/фильтруем по sink-флагам
      if (onlyWithSinks.isEmpty) {
        qb
      } else {
        val sf = FilterBuilders.termsFilter(ADN_SINKS_ESFN, onlyWithSinks.map(_.name) : _*)
        QueryBuilders.filteredQuery(qb, sf)
      }
    }.orElse[QueryBuilder] {
      if (onlyWithSinks.isEmpty) {
        None
      } else {
        val sq = QueryBuilders.termsQuery(ADN_SINKS_ESFN, onlyWithSinks.map(_.name) : _*)
          .minimumMatch(onlyWithSinks.size)
        Some(sq)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    collStringSize(onlyWithSinks, super.sbInitSize, 5)
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("onlyWithSinks", onlyWithSinks, super.toStringBuilder)
  }
}

trait AdnSinksDsaDflt extends AdnSinksDsa {
  override def onlyWithSinks: Seq[AdnSink] = Seq.empty
}

trait AdnSinksDsaWrapper extends AdnSinksDsa with DynSearchArgsWrapper {
  override type WT <: AdnSinksDsa
  override def onlyWithSinks = _dsArgsUnderlying.onlyWithSinks
}



/** Аддон для dyn-search для поиска/фильтрации по флагу тестовости узла. */
trait TestNodeDsa extends DynSearchArgs {

  /** Искать/фильтровать по значению флага тестового узла. */
  def testNode: Option[Boolean]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      // Отрабатываем флаг testNode.
      testNode.fold(qb) { tnFlag =>
        var tnf: FilterBuilder = FilterBuilders.termFilter(AdNetMember.ADN_TEST_NODE_ESFN, tnFlag)
        if (!tnFlag) {
          val tmf = FilterBuilders.missingFilter(AdNetMember.ADN_TEST_NODE_ESFN)
          tnf = FilterBuilders.orFilter(tnf, tmf)
        }
        QueryBuilders.filteredQuery(qb, tnf)
      }
    }.orElse[QueryBuilder] {
     testNode.map { tnFlag =>
        // TODO Нужно добавить аналог missing filter для query и как-то объеденить через OR. Или пока так и пересохранять узлы с tn=false.
        QueryBuilders.termQuery(AdNetMember.ADN_TEST_NODE_ESFN, tnFlag)
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

trait TestNodeDsaDflt extends TestNodeDsa {
  override def testNode: Option[Boolean] = None
}

trait TestNodeDsaWrapper extends TestNodeDsa with DynSearchArgsWrapper {
  override type WT <: TestNodeDsa
  override def testNode = _dsArgsUnderlying.testNode
}



/** Аддон для поиска/фильтрации по значению поля adn.isEnabled. */
trait NodeIsEnabledDsa extends DynSearchArgs {

  /** Искать/фильтровать по галочки активности узла. */
  def isEnabled: Option[Boolean]

  /** Сборка EsQuery сверху вниз. */
  override def toEsQueryOpt: Option[QueryBuilder] = {
    super.toEsQueryOpt.map[QueryBuilder] { qb =>
      // Ищем/фильтруем по флагу включённости узла.
      isEnabled.fold(qb) { isEnabled =>
        val ief = FilterBuilders.termFilter(ADN_IS_ENABLED_ESFN, isEnabled)
        QueryBuilders.filteredQuery(qb, ief)
      }
    }.orElse[QueryBuilder] {
      isEnabled.map { isEnabled =>
        QueryBuilders.termQuery(ADN_IS_ENABLED_ESFN, isEnabled)
      }
    }
  }

  /** Базовый размер StringBuilder'а. */
  override def sbInitSize: Int = {
    val sis = super.sbInitSize
    if (isEnabled.isDefined) sis + 16 else sis
  }

  /** Построение выхлопа метода toString(). */
  override def toStringBuilder: StringBuilder = {
    fmtColl2sb("isEnabled", isEnabled, super.toStringBuilder)
  }
}

trait NodeIsEnabledDsaDflt extends NodeIsEnabledDsa {
  override def isEnabled: Option[Boolean] = None
}

trait NodeIsEnabledDsaWrapper extends NodeIsEnabledDsa with DynSearchArgsWrapper {
  override type WT <: NodeIsEnabledDsa
  override def isEnabled = _dsArgsUnderlying.isEnabled
}
