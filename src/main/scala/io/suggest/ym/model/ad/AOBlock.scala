package io.suggest.ym.model.ad

import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.model.es.{IGenEsMappingProps, EsModelUtil}
import io.suggest.model.es.EsModelUtil.FieldsJsonAcc
import io.suggest.model.n2.ad.ent.text.TextEnt
import io.suggest.util.SioEsUtil.{FieldIndexingVariants, FieldString, FieldObject, DocField}
import play.api.libs.json.JsString

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 17:17
 * Description: Модель одного текстового блока, т.е. оффера, который в теле имеет текстовые данные.
 */

object AOBlock extends IGenEsMappingProps {

  val TEXT1_ESFN        = "text1"
  val HREF_ESFN         = "href"

  override def generateMappingProps: List[DocField] = {
    List(
      FieldObject(TEXT1_ESFN, properties = TextEnt.generateMappingProps),
      FieldString(HREF_ESFN, index = FieldIndexingVariants.no, include_in_all = false)
    )
  }

  /** Десериализация тела блочного оффера. */
  def deserializeBody(jsObject: Any, n: Int) = {
    jsObject match {
      case m: java.util.Map[_,_] =>
        AOBlock(
          n = n,
          text1 = Option(m get TEXT1_ESFN)
            .flatMap(TextEnt.deserializeOpt),
          href = Option(m get HREF_ESFN)
            .map(EsModelUtil.stringParser)
        )
    }
  }

}


import AOBlock._


// Поле n наверное остаётся нужен для упорядочивания. Хотя и это тоже не обязательно.
case class AOBlock(
  n         : Int,
  text1     : Option[TextEnt] = None,
  href      : Option[String] = None
)
  extends AdOfferT
{

  @JsonIgnore
  override def renderPlayJsonBody: FieldsJsonAcc = {
    var acc: FieldsJsonAcc = Nil
    if (text1.isDefined)
      acc ::= TEXT1_ESFN -> text1.get.renderPlayJson
    if (href.isDefined)
      acc ::= HREF_ESFN -> JsString(href.get)
    acc
  }
}
