package io.suggest.ym.model.common

import io.suggest.model.{EsModel, EsModelStaticT, EsModelT}
import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.util.JacksonWrapper
import io.suggest.util.SioEsUtil._
import EsModel._
import io.suggest.ym.model.MImgInfo

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 11:20
 * Description: Визуальные параметры отображения участника рекламной сети.
 */

object EMAdnMVisual {

  val VISUAL_ESFN = "visual"

  val COLOR_ESFN = "color"
  val WELCOME_AD_ID = "welcomeAdId"

}

import EMAdnMVisual._


trait EMAdnMVisualStatic[T <: EMAdnMVisual[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(VISUAL_ESFN, enabled = true, properties = Seq(
      FieldString(COLOR_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      EMLogoImg.esMappingField,
      FieldString(WELCOME_AD_ID, index = FieldIndexingVariants.no, include_in_all = false)
    )) :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (VISUAL_ESFN, value) =>
        acc.visual = JacksonWrapper.convert[AdnVisual](value)
    }
  }

}

trait EMAdnMVisual[T <: EMAdnMVisual[T]] extends EsModelT[T] {

  var visual: AdnVisual

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    val visSer = JacksonWrapper.serialize(visual)
    acc.rawField(VISUAL_ESFN, visSer.getBytes)
  }
}


/**
 * Параметры визуального отображения участника сети.
 * @param color Предпочтительный цвет оформления.
 * @param logoImg Логотип.
 * @param welcomeAdId id карточки приветствия в [[io.suggest.ym.model.MWelcomeAd]].
 */
case class AdnVisual(
  var color         : Option[String] = None,
  var logoImg       : Option[MImgInfo] = None,
  var welcomeAdId   : Option[String] = None
)
