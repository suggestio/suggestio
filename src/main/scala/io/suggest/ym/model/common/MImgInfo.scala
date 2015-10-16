package io.suggest.ym.model.common

import com.fasterxml.jackson.annotation.{JsonIgnore, JsonIgnoreProperties}
import io.suggest.common.geom.d2.ISize2di
import io.suggest.model.es.EsModelUtil
import play.api.libs.json._
import EsModelUtil.FieldsJsonAcc
import java.{util => ju}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.04.14 17:31
 * Description: Модель данных и метаданных по картинке.
 */

/** Статическая часть MImgInfo. */
object MImgInfo {

  val FILENAME_ESFN = "id"
  val META_ESFN     = "meta"

  /**
   * Десериализатор.
   * @param v некое значение.
   * @return
   */
  def convertFrom(v: Any): MImgInfo = {
    v match {
      case m: ju.Map[_,_] =>
        val id = m.get(FILENAME_ESFN).toString
        val metaOpt = Option(m.get(META_ESFN)).map(MImgInfoMeta.convertFrom)
        MImgInfo(id, metaOpt)
    }
  }

}

import MImgInfo._

trait MImgInfoT {
  def filename: String
  def meta: Option[MImgInfoMeta]

  @JsonIgnore
  def toPlayJson = {
    var props: FieldsJsonAcc = List(FILENAME_ESFN -> JsString(filename))
    if (meta.isDefined) {
      props ::= META_ESFN -> meta.get.toPlayJson
    }
    JsObject(props)
  }
}

/** Объект содержит данные по картинке. Данные не индексируются (по идее), и их схему можно менять на лету. */
@JsonIgnoreProperties(ignoreUnknown = true)
case class MImgInfo(filename: String, meta: Option[MImgInfoMeta] = None) extends MImgInfoT {

  @JsonIgnore
  override def hashCode(): Int = filename.hashCode()
}


object MImgInfoMeta {
  val WIDTH_ESFN  = "width"
  val HEIGHT_ESFN = "height"

  /** Быстрый десериализатор без jackson. */
  def convertFrom(v: Any): MImgInfoMeta = {
    v match {
      case m: java.util.Map[_,_] =>
        val width  = EsModelUtil.intParser(m.get(WIDTH_ESFN))
        val height = EsModelUtil.intParser(m.get(HEIGHT_ESFN))
        MImgInfoMeta(height=height, width=width)
    }
  }

  def apply(sz2d: ISize2di): MImgInfoMeta = {
    MImgInfoMeta(
      height = sz2d.height,
      width  = sz2d.width
    )
  }

}

import MImgInfoMeta._

/** Интерфейс для класса, который будет хранить размер картинки. */
trait MImgSizeT extends ISize2di {

  def isSmallerThan(sz: MImgSizeT): Boolean = {
    height < sz.height  &&  width < sz.width
  }

  def isIncudesSz(sz: MImgSizeT): Boolean = {
    height >= sz.height  &&  width >= sz.width
  }

  def isLargerThan(sz: MImgSizeT): Boolean = {
    height > sz.height  &&  width > sz.width
  }

  def isVertical = height > width
  def isHorizontal = width > height

  override def toString: String = s"${width}x$height"
}


case class MImgInfoMeta(height: Int, width: Int) extends MImgSizeT {

  @JsonIgnore
  def toPlayJson = {
    JsObject(Seq(
      HEIGHT_ESFN -> JsNumber(height),
      WIDTH_ESFN  -> JsNumber(width)
    ))
  }

}

