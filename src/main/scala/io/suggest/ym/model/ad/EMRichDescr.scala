package io.suggest.ym.model.ad

import io.suggest.model.es.{EsModelPlayJsonT, EsModelStaticMutAkvT, EsModelUtil}
import EsModelUtil.{FieldsJsonAcc, stringParser}
import io.suggest.model.n2.ad.rd.RichDescr
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
 * }
 */
object EMRichDescr {

  val RICH_DESCR_ESFN   = "richDescr"

}


import EMRichDescr._


trait EMRichDescrStatic extends EsModelStaticMutAkvT {
  override type T <: EMRichDescrMut

  abstract override def generateMappingProps: List[DocField] = {
    val rdoField = FieldObject(RICH_DESCR_ESFN, enabled = true, properties = RichDescr.generateMappingProps)
    rdoField :: super.generateMappingProps
  }

  abstract override def applyKeyValue(acc: T): PartialFunction[(String, AnyRef), Unit] = {
    super.applyKeyValue(acc) orElse {
      case (RICH_DESCR_ESFN, v) =>
        acc.richDescrOpt = RichDescrUtil.deserializeOpt(v)
    }
  }
}


trait EMRichDescrI extends EsModelPlayJsonT {
  override type T <: EMRichDescrI
  def richDescrOpt: Option[RichDescr]
}


trait EMRichDescr extends EMRichDescrI {
  abstract override def writeJsonFields(acc: FieldsJsonAcc): FieldsJsonAcc = {
    val acc0 = super.writeJsonFields(acc)
    if (richDescrOpt.isDefined) {
      val jsv = RichDescrUtil.renderPlayJson( richDescrOpt.get )
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


object RichDescrUtil {

  import RichDescr._

  val deserializeOpt: PartialFunction[Any, Option[RichDescr]] = {
    case jm: ju.Map[_,_] =>
      if (jm.isEmpty) {
        None
      } else {
        val rd = RichDescr(
          bgColor = Option(jm.get(BG_COLOR_ESFN))
            .fold("FFFFFF")(stringParser),
          text = Option(jm.get(TEXT_ESFN))
            .fold("")(stringParser)
        )
        Some(rd)
      }

    case null =>
      None

    case other =>
      println(s"EMRichDescr.applyKeyValue(): Failed to deserialize rich descr from " + other)
      None
  }

  def renderPlayJson(rd: RichDescr): JsObject = {
    import RichDescr._
    JsObject(Seq(
      BG_COLOR_ESFN -> JsString(rd.bgColor),
      TEXT_ESFN     -> JsString(rd.text)
    ))
  }

}

