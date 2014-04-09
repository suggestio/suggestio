package io.suggest.ym.model.common

import org.elasticsearch.common.xcontent.{XContentBuilder, XContentFactory}
import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties}
import io.suggest.model.EsModel
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 17:31
 * Description: Модель данных и метаданных по картинке.
 */

/** Статическая часть MImgInfo. */
object MImgInfo {

  val ID_ESFN     = "id"
  val META_ESFN   = "meta"

  /**
   * Десериализатор.
   * @param v некое значение.
   * @return
   */
  // TODO Нужно переписать без jackson.
  def convertFrom(v: Any): MImgInfo = {
    v match {
      case m: java.util.Map[_,_] =>
        val id = m.get(ID_ESFN).toString
        val metaOpt = Option(m.get(META_ESFN)).map(MImgInfoMeta.convertFrom)
        MImgInfo(id, metaOpt)
    }
  }
}

import MImgInfo._

/** Объект содержит данные по картинке. Данные не индексируются (по идее), и их схему можно менять на лету. */
@JsonIgnoreProperties(ignoreUnknown = true)
case class MImgInfo(id: String, meta: Option[MImgInfoMeta] = None) {

  @JsonIgnore
  override def hashCode(): Int = id.hashCode()

  @JsonIgnore
  def toJson: String = {
    val acc = XContentFactory.jsonBuilder().startObject()
      .field(ID_ESFN, id)
    if (meta.isDefined) {
      acc.startObject(META_ESFN)
      meta.get.writeFields(acc)
      acc.endObject()
    }
    acc.string()
  }

  @JsonIgnore
  def toPlayJson = {
    var props = List(ID_ESFN -> JsString(id))
    if (meta.isDefined) {
      props ::= META_ESFN -> meta.get.toPlayJson
    }
    JsObject(props)
  }
}


object MImgInfoMeta {
  val WIDTH_ESFN  = "width"
  val HEIGHT_ESFN = "height"

  /** Быстрый десериализатор без jackson. */
  def convertFrom(v: Any): MImgInfoMeta = {
    v match {
      case m: java.util.Map[_,_] =>
        val width  = EsModel.intParser(m.get(WIDTH_ESFN))
        val height = EsModel.intParser(m.get(HEIGHT_ESFN))
        MImgInfoMeta(height=height, width=width)
    }
  }
}

import MImgInfoMeta._

case class MImgInfoMeta(height: Int, width: Int) {
  @JsonIgnore
  def writeFields(acc: XContentBuilder) {
    acc.field(HEIGHT_ESFN, height)
      .field(WIDTH_ESFN, width)
  }

  @JsonIgnore
  def toPlayJson = {
    JsObject(Seq(
      HEIGHT_ESFN -> JsNumber(height),
      WIDTH_ESFN  -> JsNumber(width)
    ))
  }
}

