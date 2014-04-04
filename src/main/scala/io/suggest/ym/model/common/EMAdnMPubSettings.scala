package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.JacksonWrapper
import io.suggest.ym.model.AdShowLevel
import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.util.SioEsUtil._

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
}

import EMAdnMPubSettings._


trait EMAdnMPubSettingsStatic[T <: EMAdnMPubSettings[T]] extends EsModelStaticT[T] {

  def generateMappingProps: List[DocField] = {
    // Для сеттингов выставляем enabled = true, но все поля не индексируем. На случай, если в будуем понадобится это изменить.
    FieldObject(PUB_SETTINGS_ESFN, enabled = true, properties = Seq(
      ???
    ))
    :: super.generateMappingProps
  }
}

trait EMAdnMPubSettings[T <: EMAdnMPubSettings[T]] extends EsModelT[T] {

  var pubSettings: AdnMPubSettings

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    val psSer = JacksonWrapper.serialize(pubSettings)
    acc.rawField(PUB_SETTINGS_ESFN, psSer.getBytes)
  }
}


/**
 * Настройки публикации для участника сети.
 * @param isEnabled Включена ли публикация.
 * @param levelsMap Карта уровней.
 * @param disableReason Причина отключения. Обычно выставляется вместе с isEnabled = false
 */
case class AdnMPubSettings(
  var levelsMap: Map[AdShowLevel, Int],
  var isEnabled: Boolean,
  var disableReason: Option[String] = None
) {

  @JsonIgnore
  def hasLevel(asl: AdShowLevel): Boolean = levelsMap.get(asl).exists(_ > 0)

  @JsonIgnore
  def maxOnLevel(asl: AdShowLevel): Int = levelsMap.get(asl) getOrElse 0

}

