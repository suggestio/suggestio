package io.suggest.ym.model.common

import io.suggest.model.{EsModelStaticMutAkvT, EsModelPlayJsonT}
import io.suggest.util.SioEsUtil._
import io.suggest.util.JacksonWrapper
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json._

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


trait EMAdPanelSettingsStatic extends EsModelStaticMutAkvT {
  override type T <: EMAdPanelSettingsMut

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

trait EMAdPanelSettings extends EsModelPlayJsonT {
  override type T <: EMAdPanelSettings
  def panel: Option[AdPanelSettings]

  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (panel.isDefined)
      PANEL_ESFN -> panel.get.renderPlayJson  ::  acc0
    else
      acc0
  }
}

trait EMAdPanelSettingsMut extends EMAdPanelSettings {
  override type T <: EMAdPanelSettingsMut
  var panel: Option[AdPanelSettings]
}


/** Класс с настройками отображения панельки. Изначально требовался только цвет. */
case class AdPanelSettings(color: String) {

  def renderPlayJson: JsObject = {
    JsObject(Seq(
      COLOR_ESFN -> JsString(color)
    ))
  }

}
