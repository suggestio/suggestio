package io.suggest.ym.model.common

import org.elasticsearch.common.xcontent.XContentBuilder
import io.suggest.model.{EsModelStaticT, EsModelT}
import io.suggest.util.SioEsUtil._
import io.suggest.util.JacksonWrapper

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 17:56
 * Description: Поле настроек панели GUI.
 */
object EMAdPanelSettings {
  val PANEL_ESFN = "panel"
  val COLOR_ESFN = "color"
}

import EMAdPanelSettings._


trait EMAdPanelSettingsStatic[T <: EMAdPanelSettingsMut[T]] extends EsModelStaticT[T] {
  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(PANEL_ESFN,  enabled = false,  properties = Nil) :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (PANEL_ESFN, value) =>
        acc.panel = Option(JacksonWrapper.convert[AdPanelSettings](value))
    }
  }
}

trait EMAdPanelSettings[T <: EMAdPanelSettings[T]] extends EsModelT[T] {

  def panel: Option[AdPanelSettings]

  abstract override def writeJsonFields(acc: XContentBuilder) {
    super.writeJsonFields(acc)
    if (panel.isDefined)
      panel.get.render(acc)
  }
}

trait EMAdPanelSettingsMut[T <: EMAdPanelSettingsMut[T]] extends EMAdPanelSettings[T] {
  var panel: Option[AdPanelSettings]
}


case class AdPanelSettings(color: String) {
  def render(acc: XContentBuilder) {
    acc.startObject(PANEL_ESFN)
      .field(COLOR_ESFN, color)
    .endObject()
  }
}
