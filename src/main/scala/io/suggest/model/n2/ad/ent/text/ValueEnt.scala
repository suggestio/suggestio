package io.suggest.model.n2.ad.ent.text

import java.{util => ju}

import com.fasterxml.jackson.annotation.JsonIgnore
import io.suggest.model.es.EsModelUtil.FieldsJsonAcc
import io.suggest.model.es.IGenEsMappingProps
import io.suggest.model.n2.ad.ent.Coords2d
import io.suggest.util.SioEsUtil.{DocField, FieldObject}
import play.api.libs.json.JsObject

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 30.10.15 16:21
 * Description: Абстрактная модель для entity-значений.
 *
 * Скорее всего её можно будет замёржить внутрь [[TextEnt]]-модель,
 * т.к. необходимость в существовании этой абстрактной value-модели отпала вместе с отказом
 * от float-полей (Price, Discount).
 */

object ValueEnt extends IGenEsMappingProps {

  val FONT_ESFN         = "font"
  val COORDS_ESFN       = "coords"
  val VALUE_ESFN        = "value"

  def getAndDeserializeFont(jm: ju.Map[_,_]): EntFont = {
    Option(jm.get(FONT_ESFN))
      .fold(EntFont())(EntFont.deserialize)
  }

  def getAndDeserializeCoords(jm: ju.Map[_,_]): Option[Coords2d] = {
    Option(jm.get(COORDS_ESFN))
      .map(Coords2d.deserialize)
  }

  override def generateMappingProps: List[DocField] = {
    List(
      FieldObject(FONT_ESFN, enabled = false, properties = Nil),
      FieldObject(COORDS_ESFN, enabled = false, properties = Nil)
    )
  }

}


import io.suggest.model.n2.ad.ent.text.ValueEnt._


trait ValueEnt {

  def font: EntFont
  def coords: Option[Coords2d]

  @JsonIgnore
  def renderPlayJson = {
    var acc: FieldsJsonAcc = List(
      FONT_ESFN -> font.renderPlayJsonFields()
    )
    acc = renderPlayJsonFields(acc)
    if (coords.isDefined)
      acc ::= COORDS_ESFN -> coords.get.renderPlayJsonFields()
    JsObject(acc)
  }

  def renderPlayJsonFields(acc0: FieldsJsonAcc): FieldsJsonAcc

}
