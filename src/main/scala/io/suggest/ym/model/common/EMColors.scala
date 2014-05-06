package io.suggest.ym.model.common

import io.suggest.model.{EsModelT, EsModelStaticT}
import io.suggest.model.EsModel.FieldsJsonAcc
import play.api.libs.json._
import io.suggest.util.SioEsUtil._
import io.suggest.util.JacksonWrapper

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.05.14 17:50
 * Description: Поле для хранения карты цветов. Ключи и значения внутри - строки.
 */
object EMColors {
  val COLORS_ESFN = "colors"
}


import EMColors._


trait EMColorsStatic extends EsModelStaticT {
  override type T <: EMColorsMut

  abstract override def generateMappingProps: List[DocField] = {
    FieldObject(COLORS_ESFN, enabled = false, properties = Nil) :: super.generateMappingProps
  }

  // TODO Надо бы перевести все модели на stackable-трейты и избавится от PartialFunction здесь.
  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (COLORS_ESFN, colorsRaw) =>
        acc.colors = JacksonWrapper.convert [Map[String, String]] (colorsRaw)
    }
  }
}


trait IColors {
  def colors: Map[String, String]
}

trait EMColorsI extends EsModelT with IColors {
  override type T <: EMColorsI
}

trait EMColors extends EMColorsI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (colors.isEmpty) {
      acc0
    } else {
      val jsObjData = colors.foldLeft [List[(String, JsString)]] (Nil) {
        case (jsonAcc, (k, v))  =>  k -> JsString(v) :: jsonAcc
      }
      COLORS_ESFN -> JsObject(jsObjData) :: acc0
    }
  }
}

trait EMColorsMut extends EMColors {
  override type T <: EMColorsMut
  var colors: Map[String, String]
}

