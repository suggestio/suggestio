package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.ym.model.{AdShowLevels, AdShowLevel}
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioEsUtil._
import scala.collection.JavaConversions._
import io.suggest.model.EsModel._

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
  val LEVELS_MAP_ESFN = "levelsMap"
  val DISABLE_REASON_ESFN = "disableReason"

  private def fullFN(fn: String) = PUB_SETTINGS_ESFN + "." + fn

  // Полные (flat) имена используемых полей. Используются при составлении поисковых запросов.
  val PS_IS_ENABLED_ESFN = fullFN(IS_ENABLED_ESFN)
  val PS_LEVELS_MAP_ESFN = fullFN(LEVELS_MAP_ESFN)
  val PS_DISABLE_REASON_ESFN = fullFN(DISABLE_REASON_ESFN)

}

import EMAdnMPubSettings._


trait EMAdnMPubSettingsStatic[T <: EMAdnMPubSettings[T]] extends EsModelStaticT[T] {

  def generateMappingProps: List[DocField] = {
    // Для сеттингов выставляем enabled = true, но все поля не индексируем. На случай, если в будуем понадобится это изменить.
    FieldObject(PUB_SETTINGS_ESFN, enabled = true, properties = Seq(
      FieldBoolean(IS_ENABLED_ESFN, index = FieldIndexingVariants.not_analyzed, include_in_all = false),
      FieldString(DISABLE_REASON_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldObject(LEVELS_MAP_ESFN, enabled = false, properties = Nil)
    )) :: super.generateMappingProps
  }

  def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    case (PUB_SETTINGS_ESFN, pubSettings) =>
      if (acc.pubSettings == null)
        acc.pubSettings = new AdnMPubSettings
      val ps1 = acc.pubSettings
      pubSettings match {
        case ps: java.util.Map[_,_] =>
          ps foreach {
            case (LEVELS_MAP_ESFN, levelsRawMap: java.util.Map[_, _]) =>
              val levelsMap = levelsRawMap.foldLeft[List[(AdShowLevel, Int)]] (Nil) { case (mapAcc, (aslStr, count)) =>
                val k = AdShowLevels.withName(aslStr.toString)
                val v = count match {
                  case n: java.lang.Number => n.intValue()
                }
                k -> v :: mapAcc
              }.toMap
              ps1.levelsMap = levelsMap

            case (IS_ENABLED_ESFN, isEnabledRaw) =>
              ps1.isEnabled = booleanParser(isEnabledRaw)

            case (DISABLE_REASON_ESFN, drRaw) =>
              ps1.disableReason = Option(drRaw).map { stringParser(_) }  // TODO Нужно задать через method value, а не через (_). Почему-то не работает использование напрямую
          }
      }
      acc.pubSettings = ps1
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
 * @param levelsMap Карта уровней.
 * @param disableReason Причина отключения. Обычно выставляется вместе с isEnabled = false
 */
case class AdnMPubSettings(
  var levelsMap: Map[AdShowLevel, Int] = Map.empty,
  var isEnabled: Boolean = false,
  var disableReason: Option[String] = None
) {

  @JsonIgnore
  def hasLevel(asl: AdShowLevel): Boolean = levelsMap.get(asl).exists(_ > 0)

  @JsonIgnore
  def maxOnLevel(asl: AdShowLevel): Int = levelsMap.get(asl) getOrElse 0

  @JsonIgnore
  def writeFields(acc: XContentBuilder) {
    acc.field(IS_ENABLED_ESFN, isEnabled)
    if (!levelsMap.isEmpty) {
      // Для внутреннего json-объекта-карты, используем Jackson из-за глюков с вложенными объектами в XContentBuilder.
      //val lmSer = JacksonWrapper.serialize(pubSettings.levelsMap).getBytes
      //acc.rawField(LEVELS_MAP_ESFN, lmSer)
      // Если не будет глюков, то можно использовать без-reflection методику для записи карты (генерит тоже самое):
      acc.startObject(LEVELS_MAP_ESFN)
      levelsMap.foreach { case (sl, max) =>
        acc.field(sl.toString, max)
      }
      acc.endObject()
    }
    if (disableReason.isDefined)
      acc.field(DISABLE_REASON_ESFN, disableReason.get)
  }
}

