package io.suggest.ym.model.common

import io.suggest.ym.model.common.AdNetMemberTypes.AdNetMemberType
import io.suggest.model.{EsModel, EsModelStaticT, EsModelT}
import io.suggest.util.SioEsUtil._
import com.fasterxml.jackson.annotation.JsonIgnore
import scala.collection.JavaConversions._
import EsModel._
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import org.elasticsearch.index.query.{FilterBuilders, QueryBuilder, QueryBuilders}
import org.elasticsearch.common.unit.Fuzziness
import org.elasticsearch.index.mapper.internal.AllFieldMapper
import io.suggest.ym.model.{MAdnNode, AdShowLevel, CompanyId_t}
import java.{util => ju}
import io.suggest.event.{AdnNodeOnOffEvent, SioNotifierStaticClientI}
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.04.14 17:03
 * Description: Объект-участнник рекламной сети с произвольной ролью. Это может быть ТЦ, магазин или кто-то ещё.
 * Таким объектом владеют люди, и совершают действия от имени объекта.
 */

object AdNetMember {
  
  /** Название root-object поля, в котором хранятся данные по участию в рекламной сети. */
  val ADN_ESFN = "adn"

  // Имена полей вышеуказнного объекта
  val IS_PRODUCER_ESFN    = "isProd"
  val IS_RECEIVER_ESFN    = "isRcvr"
  val IS_SUPERVISOR_ESFN  = "isSup"
  val SUPERVISOR_ID_ESFN  = "supId"
  val MEMBER_TYPE_ESFN    = "mType"


  val IS_ENABLED_ESFN = "isEnabled"
  val SHOW_LEVELS_ESFN = "showLevels"
  val DISABLE_REASON_ESFN = "disableReason"

  private def fullFN(subFN: String): String = ADN_ESFN + "." + subFN

  // Абсолютные (плоские) имена полей. Используются при поиске.
  val ADN_MI_IS_PRODUCER_ESFN   = fullFN(IS_PRODUCER_ESFN)
  val ADN_MI_IS_RECEIVER_ESFN   = fullFN(IS_RECEIVER_ESFN)
  val ADN_MI_IS_SUPERVISOR_ESFN = fullFN(IS_SUPERVISOR_ESFN)
  val ADN_MI_SUPERVISOR_ID_ESFN = fullFN(SUPERVISOR_ID_ESFN)
  val ADN_MI_MEMBER_TYPE_ESFN   = fullFN(MEMBER_TYPE_ESFN)

  // Полные (flat) имена используемых полей. Используются при составлении поисковых запросов.
  val PS_IS_ENABLED_ESFN = fullFN(IS_ENABLED_ESFN)
  val PS_LEVELS_MAP_ESFN = fullFN(SHOW_LEVELS_ESFN)
  val PS_DISABLE_REASON_ESFN = fullFN(DISABLE_REASON_ESFN)

}

import AdNetMember._


/** Трейт для статической части модели участника рекламной сети. */
trait EMAdNetMemberStatic[T <: EMAdNetMember[T]] extends EsModelStaticT[T] {

  abstract override def generateMappingProps: List[DocField] = {
    import FieldIndexingVariants.not_analyzed
    FieldObject(ADN_ESFN, enabled = true, properties = Seq(
      FieldBoolean(IS_PRODUCER_ESFN, index = not_analyzed, include_in_all = false),
      FieldBoolean(IS_RECEIVER_ESFN, index = not_analyzed, include_in_all = false),
      FieldBoolean(IS_SUPERVISOR_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(SUPERVISOR_ID_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(MEMBER_TYPE_ESFN, index = not_analyzed, include_in_all = false),
      // раньше это лежало в EMAdnMPubSettings, но потом было перемещено сюда, т.к. по сути это разделение было некорректно.
      FieldBoolean(IS_ENABLED_ESFN, index = not_analyzed, include_in_all = false),
      FieldString(DISABLE_REASON_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldObject(SHOW_LEVELS_ESFN, enabled = false, properties = Nil)
    )) :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = super.applyKeyValue(acc) orElse {
    case (ADN_ESFN, value: java.util.Map[_,_]) =>
      if (acc.adn == null)
        acc.adn = new AdNetMemberInfo(null)
      val mi = acc.adn
      value.foreach {
        case (IS_PRODUCER_ESFN, v)    => mi.isProducer = booleanParser(v)
        case (IS_RECEIVER_ESFN, v)    => mi.isReceiver = booleanParser(v)
        case (IS_SUPERVISOR_ESFN, v)  => mi.isSupervisor = booleanParser(v)
        case (SUPERVISOR_ID_ESFN, v)  => mi.supId = Option(stringParser(v))
        case (MEMBER_TYPE_ESFN, v)    => mi.memberType = AdNetMemberTypes.withName(stringParser(v))
        case (SHOW_LEVELS_ESFN, levelsInfoRaw) =>
          mi.showLevelsInfo = AdnMemberShowLevels.deserialize(levelsInfoRaw)
        case (IS_ENABLED_ESFN, isEnabledRaw) =>
          mi.isEnabled = booleanParser(isEnabledRaw)
        case (DISABLE_REASON_ESFN, drRaw) =>
          mi.disableReason = Option(drRaw).map {
            stringParser(_)
          } // TODO Нужно задать через method value, а не через (_). Почему-то не работает использование напрямую
      }
  }

  /** Генератор es-query для указанного member type.
    * @param memberType Необходимый тип участников.
    * @return QueryBuilder.
    */
  def adnMemberTypeQuery(memberType: AdNetMemberType) = {
    QueryBuilders.termQuery(ADN_MI_MEMBER_TYPE_ESFN, memberType.toString())
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
      val isEnabledFilter = FilterBuilders.termFilter(PS_IS_ENABLED_ESFN, true)
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
    prepareSearch
      .setQuery(query)
      .setSize(maxResult)
      .setFrom(offset)
      .execute()
      .map { searchResp2list }
  }

}


/** Трейт для экземпляра модели участника рекламной сети. */
trait EMAdNetMember[T <: EMAdNetMember[T]] extends EsModelT[T] {
  var adn: AdNetMemberInfo

  // Ограничиваем тип объекта-компаньона, чтобы можно было дергать статический setIsEnabled().
  override def companion: EMAdNetMemberStatic[T]

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    ADN_ESFN -> adn.toPlayJson :: super.writeJsonFields(acc)
  }


  /** Быстрый доступ к статическому [[EMAdNetMemberStatic.setIsEnabled( )]].
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
  def getSup(implicit ec: ExecutionContext, client: Client): Future[Option[MAdnNode]] = {
    adn.supId match {
      case Some(supId)  => MAdnNode.getById(supId)
      case None         => Future successful None
    }
  }
}


/** Инфа об участнике рекламной сети. Все параметры его участия свернуты в один объект. */
case class AdNetMemberInfo(
  var memberType: AdNetMemberType,
  var isProducer: Boolean = false,
  var isReceiver: Boolean = false,
  var isSupervisor: Boolean = false,
  var supId: Option[String] = None,
  // перемещено из mpub:
  var showLevelsInfo: AdnMemberShowLevels = AdnMemberShowLevels(),
  var isEnabled: Boolean = false,
  var disableReason: Option[String] = None
) {
  @JsonIgnore
  def toPlayJson: JsObject = {
    var acc0: FieldsJsonAcc = List(
      IS_PRODUCER_ESFN -> JsBoolean(isProducer),
      IS_RECEIVER_ESFN -> JsBoolean(isReceiver),
      MEMBER_TYPE_ESFN -> JsString(memberType.toString()),
      IS_SUPERVISOR_ESFN -> JsBoolean(isSupervisor),
      IS_ENABLED_ESFN -> JsBoolean(isEnabled)
    )
    if (supId.isDefined)
      acc0 ::= SUPERVISOR_ID_ESFN -> JsString(supId.get)
    if (!showLevelsInfo.isEmpty)
      acc0 ::= SHOW_LEVELS_ESFN -> showLevelsInfo.toPlayJson
    if (disableReason.isDefined)
      acc0 ::= DISABLE_REASON_ESFN -> JsString(disableReason.get)
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
          val k = AdShowLevels.withName(aslStr.toString)
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
    if (!levelsMap.isEmpty) {
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
    levelsMap.get(lvl).getOrElse(0)
  }

  /**
   * Определить, возможно ли вообще что-либо постить/принимать на указанном уровне отображения.
   * @param lvl Уровень.
   * @param levelsMap Карта уровней.
   * @return true - значит карта допускает работу на этом уровне.
   */
  def canAtLevel(lvl: AdShowLevel, levelsMap: LvlMap_t): Boolean = {
    levelsMap.get(lvl).exists(_ > 0)
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
  def setMaxOutAtLevel(lvl: AdShowLevel, max: Int) = {
    out = out + (lvl -> max)
    this
  }

  def canInAtLevel(lvl: AdShowLevel) = canAtLevel(lvl, in)
  def maxInAtLevel(lvl: AdShowLevel) = maxAtLevel(lvl, in)
  def setMaxInAtLevel(lvl: AdShowLevel, max: Int) = {
    in = in + (lvl -> max)
    this
  }
}


