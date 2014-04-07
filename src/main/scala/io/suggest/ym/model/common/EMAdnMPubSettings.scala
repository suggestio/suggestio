package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.{XContentFactory, XContentBuilder}
import io.suggest.ym.model.AdShowLevel
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioEsUtil._
import scala.collection.JavaConversions._
import io.suggest.model.EsModel._
import java.{util => ju}
import scala.concurrent.{Future, ExecutionContext}
import org.elasticsearch.client.Client
import io.suggest.event.{MShopOnOffEvent, SioNotifierStaticClientI}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 11:41
 * Description: Настройки отображения (публикации) контента участника в рекламной сети.
 * Участник может быть отключен, или включен, но его контент ограничен лишь некоторыми уровнями отображения.
 * А на уровнях могут быть ограничители кол-ва отображаемых реклам.
 * Все эти параметры задаются и изменяются супервизорами или администрацией ресурса.
 */

object EMAdnMPubSettings {

  val PUB_SETTINGS_ESFN = "pubSettings"

  val IS_ENABLED_ESFN = "isEnabled"
  val SHOW_LEVELS_ESFN = "showLevels"
  val DISABLE_REASON_ESFN = "disableReason"

  private def fullFN(fn: String) = PUB_SETTINGS_ESFN + "." + fn

  // Полные (flat) имена используемых полей. Используются при составлении поисковых запросов.
  val PS_IS_ENABLED_ESFN = fullFN(IS_ENABLED_ESFN)
  val PS_LEVELS_MAP_ESFN = fullFN(SHOW_LEVELS_ESFN)
  val PS_DISABLE_REASON_ESFN = fullFN(DISABLE_REASON_ESFN)

}

import EMAdnMPubSettings._


trait EMAdnMPubSettingsStatic[T <: EMAdnMPubSettings[T]] extends EsModelStaticT[T] {

  abstract override def generateMappingProps: List[DocField] = {
    // Для сеттингов выставляем enabled = true, но все поля не индексируем. На случай, если в будуем понадобится это изменить.
    FieldObject(PUB_SETTINGS_ESFN, enabled = true, properties = Seq(
      FieldBoolean(IS_ENABLED_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(DISABLE_REASON_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldObject(SHOW_LEVELS_ESFN, enabled = false, properties = Nil)
    )) :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (PUB_SETTINGS_ESFN, pubSettingsJson) =>
        if (acc.pubSettings == null)
          acc.pubSettings = new AdnMPubSettings
        val ps1 = acc.pubSettings
        pubSettingsJson match {
          case ps: ju.Map[_, _] =>
            ps foreach {
              case (SHOW_LEVELS_ESFN, levelsInfoRaw) =>
                ps1.showLevelsInfo = AdnMPubSettingsLevels.deserialize(levelsInfoRaw)

              case (IS_ENABLED_ESFN, isEnabledRaw) =>
                ps1.isEnabled = booleanParser(isEnabledRaw)

              case (DISABLE_REASON_ESFN, drRaw) =>
                ps1.disableReason = Option(drRaw).map {
                  stringParser(_)
                } // TODO Нужно задать через method value, а не через (_). Почему-то не работает использование напрямую
            }
        }
        acc.pubSettings = ps1
    }
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
    val updatedXCB = XContentFactory.jsonBuilder()
      .startObject()
        .field(PS_IS_ENABLED_ESFN, isEnabled)
        .field(PS_DISABLE_REASON_ESFN, reason getOrElse null)
      .endObject()
    val fut = client.prepareUpdate(ES_INDEX_NAME, ES_TYPE_NAME, adnId)
      .setDoc(updatedXCB)
      .execute()
    // Уведомить о переключении состояния магазина
    fut onSuccess { case _ =>
      sn publish MShopOnOffEvent(adnId, isEnabled, reason)
    }
    fut
  }

}

trait EMAdnMPubSettings[T <: EMAdnMPubSettings[T]] extends EsModelT[T] {

  var pubSettings: AdnMPubSettings

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    acc.startObject(PUB_SETTINGS_ESFN)
    pubSettings.writeFields(acc)
    acc.endObject()
  }

}


/**
 * Настройки публикации для участника сети.
 * @param isEnabled Включена ли публикация.
 * @param showLevelsInfo Информация о возможных уровнях отображения.
 * @param disableReason Причина отключения. Обычно выставляется вместе с isEnabled = false
 */
case class AdnMPubSettings(
  var showLevelsInfo: AdnMPubSettingsLevels = AdnMPubSettingsLevels(),
  var isEnabled: Boolean = false,
  var disableReason: Option[String] = None
) {

  @JsonIgnore
  def writeFields(acc: XContentBuilder) {
    acc.field(IS_ENABLED_ESFN, isEnabled)
    if (!showLevelsInfo.isEmpty) {
      acc.startObject(SHOW_LEVELS_ESFN)
      showLevelsInfo.renderFields(acc)
      acc.endObject()
    }
    if (disableReason.isDefined)
      acc.field(DISABLE_REASON_ESFN, disableReason.get)
  }

  /**
   * Выдать карту допустимых out-уровней.
   * @return Если isEnabled = false, то будет пустая карта.
   */
  @JsonIgnore
  def getOutShowLevels: AdnMPubSettingsLevels.LvlMap_t = {
    if (isEnabled)
      showLevelsInfo.out
    else
      Map.empty
  }
}


object AdnMPubSettingsLevels {
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
  def deserialize(raw: Any, acc: AdnMPubSettingsLevels = AdnMPubSettingsLevels()): AdnMPubSettingsLevels = {
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

  /**
   * Отрендерить карту уровней в JSON.
   * @param name Название поле с картой.
   * @param levelsMap Карта уровней.
   * @param acc Аккамулятор, в который идёт запись.
   * @return Аккамулятор.
   */
  def renderLevelsMap(name: String, levelsMap: LvlMap_t, acc: XContentBuilder): XContentBuilder = {
    // Для внутреннего json-объекта-карты, используем Jackson из-за глюков с вложенными объектами в XContentBuilder.
    //val lmSer = JacksonWrapper.serialize(pubSettings.levelsMap).getBytes
    //acc.rawField(LEVELS_MAP_ESFN, lmSer)
    acc.startObject(name)
    levelsMap.foreach { case (sl, max) =>
      acc.field(sl.toString, max)
    }
    acc.endObject()
  }

  def maybeRenderLevelsMap(name: String, levelsMap: LvlMap_t, acc: XContentBuilder): XContentBuilder = {
    if (!levelsMap.isEmpty)
      renderLevelsMap(name, levelsMap, acc)
    else
      acc
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

import AdnMPubSettingsLevels._

/**
 * Данные по допустимым уровням отображения для входящих и исходящих публикаций.
 * @param in Карта уровней для входящих публикаций.
 * @param out Карта уровней для исходящих публикаций.
 */
case class AdnMPubSettingsLevels(
  var in:  AdnMPubSettingsLevels.LvlMap_t = Map.empty,
  var out: AdnMPubSettingsLevels.LvlMap_t = Map.empty
) {

  @JsonIgnore
  def renderFields(acc: XContentBuilder): XContentBuilder = {
    maybeRenderLevelsMap(IN_ESFN, in, acc)
    maybeRenderLevelsMap(OUT_ESFN, out, acc)
  }

  @JsonIgnore
  def isEmpty = in.isEmpty && out.isEmpty

  @JsonIgnore
  def canOutAtLevel(lvl: AdShowLevel) = canAtLevel(lvl, out)
  @JsonIgnore
  def maxOutAtLevel(lvl: AdShowLevel) = maxAtLevel(lvl, out)

  @JsonIgnore
  def canInAtLevel(lvl: AdShowLevel) = canAtLevel(lvl, in)
  @JsonIgnore
  def maxInAtLevel(lvl: AdShowLevel) = maxAtLevel(lvl, in)

}

