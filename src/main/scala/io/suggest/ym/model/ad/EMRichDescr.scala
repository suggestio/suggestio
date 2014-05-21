package io.suggest.ym.model.ad

import io.suggest.model.{EsModelT, EsModelStaticT}
import io.suggest.model.EsModel.{FieldsJsonAcc, stringParser}
import play.api.libs.json._
import io.suggest.util.SioEsUtil._
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.14 18:45
 * Description: Поле для хранения описания-приложения к рекламной карточке.
 * descr: {
 *   bgColor: String = "FFFFFF"
 *   text: String = "...html..."
 */
object EMRichDescr {

  val RICH_DESCR_ESFN   = "richDescr"
  val BG_COLOR_ESFN     = "bgColor"
  val TEXT_ESFN         = "text"

}


import EMRichDescr._


trait EMRichDescrStatic extends EsModelStaticT {
  override type T <: EMRichDescrMut

  abstract override def generateMappingProps: List[DocField] = {
    val rdoField = FieldObject(RICH_DESCR_ESFN, enabled = true, properties = Seq(
      FieldString(BG_COLOR_ESFN, index = FieldIndexingVariants.no, include_in_all = false),
      FieldString(TEXT_ESFN, index = FieldIndexingVariants.analyzed, include_in_all = false)
    ))
    rdoField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (RICH_DESCR_ESFN, v) =>
        acc.richDescrOpt = RichDescr.deserializeOpt(v)
    }
  }
}


trait EMRichDescrI extends EsModelT {
  override type T <: EMRichDescrI
  def richDescrOpt: Option[RichDescr]
}


trait EMRichDescr extends EMRichDescrI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (richDescrOpt.isDefined) {
      val jsv = richDescrOpt.get.renderPlayJson
      RICH_DESCR_ESFN -> jsv :: acc0
    } else {
      acc0
    }
  }
}


trait EMRichDescrMut extends EMRichDescr {
  override type T <: EMRichDescrMut
  var richDescrOpt: Option[RichDescr]
}


object RichDescr {
  val deserializeOpt: PartialFunction[Any, Option[RichDescr]] = {
    case jm: ju.Map[_,_] =>
      if (jm.isEmpty) {
        None
      } else {
        val rd = RichDescr(
          bgColor = Option(jm.get(BG_COLOR_ESFN)).fold("FFFFFF")(stringParser),
          text = Option(jm.get(TEXT_ESFN)).fold("")(stringParser)
        )
        Some(rd)
      }

    case null =>
      None

    case other =>
      println(s"EMRichDescr.applyKeyValue(): Failed to deserialize rich descr from " + other)
      None
  }
}

case class RichDescr(
  bgColor: String,
  text: String
) {
  def renderPlayJson: JsObject = {
    JsObject(Seq(
      BG_COLOR_ESFN -> JsString(bgColor),
      TEXT_ESFN     -> JsString(text)
    ))
  }
}
