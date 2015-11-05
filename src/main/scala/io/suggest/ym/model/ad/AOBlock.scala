package io.suggest.ym.model.ad

import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.model.es.{IGenEsMappingProps, EsModelUtil}
import io.suggest.model.es.EsModelUtil.FieldsJsonAcc
import io.suggest.model.n2.ad.ent.{MEntity, Coords2d}
import io.suggest.model.n2.ad.ent.text.{TextAligns, EntFont, ValueEnt, TextEnt}
import io.suggest.util.SioEsUtil.{FieldIndexingVariants, FieldString, FieldObject, DocField}
import play.api.libs.json.{JsNumber, JsObject, JsString}
import java.{util => ju}

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

  // Тут спагетти из сериализаторов и десериализаторов офферов.
  // Т.к. это всё всё равно скоро удалять, оно всё свалено тут, а не раскидано по очищенным портированным моделям.

  /** Десериализация тела блочного оффера. */
  def deserializeBody(jsObject: Any, n: Int) = {
    jsObject match {
      case m: java.util.Map[_,_] =>
        AOBlock(
          n = n,
          text1 = Option(m get TEXT1_ESFN)
            .flatMap(deserializeOptTextEnt),
          href = Option(m get HREF_ESFN)
            .map(EsModelUtil.stringParser)
        )
    }
  }


  val deserializeOptTextEnt: PartialFunction[Any, Option[TextEnt]] = {
    case null =>
      None
    case jm: ju.Map[_,_] =>
      if (jm.isEmpty) {
        None
      } else {
        val result = TextEnt(
          value  = getAndDeserializeValue(jm),
          font   = getAndDeserializeFont(jm),
          coords = getAndDeserializeCoords(jm)
        )
        Some(result)
      }
  }


  def getAndDeserializeCoords(jm: ju.Map[_,_]): Option[Coords2d] = {
    Option( jm.get(ValueEnt.COORDS_ESFN) )
      .map(deserializeCoords)
  }

  val deserializeCoords: PartialFunction[Any, Coords2d] = {
    case jm: ju.Map[_,_] =>
      Coords2d(
        x = getAndDeserializeCoord(Coords2d.X_FN, jm),
        y = getAndDeserializeCoord(Coords2d.Y_FN, jm)
      )
  }

  def getAndDeserializeCoord(fn: String, jm: ju.Map[_,_]): Int = {
    Option(jm.get(fn)).fold(0)(EsModelUtil.intParser)
  }

  def getAndDeserializeFont(jm: ju.Map[_,_]): EntFont = {
    Option( jm.get(ValueEnt.FONT_ESFN) )
      .fold(EntFont())(deserializeFont)
  }


  // TODO Выпилить, когда свершиться переезд на N2-архитектуру.
  val deserializeFont: PartialFunction[Any, EntFont] = {
    case jm: ju.Map[_,_] =>
      import EntFont._
      EntFont(
        color  = Option(jm.get(COLOR_FN))
          .fold(FONT_COLOR_DFLT)(EsModelUtil.stringParser),
        size   = Option(jm.get(SIZE_FN))
          .map(EsModelUtil.intParser),
        align  = Option(jm.get(ALIGN_FN))
          .map(EsModelUtil.stringParser)
          .flatMap(TextAligns.maybeWithName),
        family = Option(jm.get(FAMILY_FN))
          .map(EsModelUtil.stringParser)
      )
  }


  def getAndDeserializeValue(jm: ju.Map[_,_]): String = {
    Option( jm.get(ValueEnt.VALUE_ESFN) )
      .fold("")(EsModelUtil.stringParser)
  }

  @JsonIgnore
  def renderPlayJsonTextEnt(te: TextEnt): JsObject = {
    var acc: FieldsJsonAcc = List(
      ValueEnt.FONT_ESFN -> renderPlayJsonFieldsFont(te.font)
    )
    acc = renderPlayJsonFieldsTextEnt(te, acc)
    if (te.coords.isDefined)
      acc ::= ValueEnt.COORDS_ESFN -> renderPlayJsonFieldsCoords(te.coords.get)
    JsObject(acc)
  }

  def renderPlayJsonFieldsFont(f: EntFont): JsObject = {
    import EntFont._
    var fieldsAcc: FieldsJsonAcc = List(
      COLOR_FN -> JsString(f.color)
    )
    if (f.family.isDefined)
      fieldsAcc ::= FAMILY_FN -> JsString(f.family.get)
    if (f.align.isDefined)
      fieldsAcc ::= ALIGN_FN -> JsString(f.align.get.toString())
    if (f.size.isDefined)
      fieldsAcc ::= SIZE_FN -> JsNumber(f.size.get)
    JsObject(fieldsAcc)
  }

  def renderPlayJsonFieldsTextEnt(te: TextEnt, acc0: FieldsJsonAcc): FieldsJsonAcc = {
    (ValueEnt.VALUE_ESFN, JsString(te.value)) :: acc0
  }
  
  
  // TODO Удалить после переключения на N2 с play.json.
  def renderPlayJsonFieldsCoords(c: Coords2d): JsObject = {
    JsObject(Seq(
      Coords2d.X_FN -> JsNumber(c.x),
      Coords2d.Y_FN -> JsNumber(c.y)
    ))
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
      acc ::= TEXT1_ESFN -> AOBlock.renderPlayJsonTextEnt(text1.get)
    if (href.isDefined)
      acc ::= HREF_ESFN -> JsString(href.get)
    acc
  }


  def toMEntity: MEntity = {
    MEntity(n, text1)
  }

}
