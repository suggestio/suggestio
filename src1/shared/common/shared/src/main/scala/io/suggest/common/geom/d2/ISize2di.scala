package io.suggest.common.geom.d2

import enumeratum.values.StringEnumEntry
import io.suggest.es.{IEsMappingProps, MappingDsl}
import io.suggest.math.SimpleArithmetics
import io.suggest.media.MediaConst
import japgolly.univeq.UnivEq
import monocle.macros.GenLens


/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.03.15 15:58
 * Description: Модель размеров двумерный целочисленный.
 */

object ISize2di {

  def isVertical(sz: ISize2di): Boolean = {
    sz.height > sz.width
  }

  def isHorizontal(sz: ISize2di): Boolean = {
    sz.width > sz.height
  }

  def isSmallerThan(outer: ISize2di, than: ISize2di): Boolean = {
    outer.height < than.height  &&  outer.width < than.width
  }

  def isIncudesSz(outer: ISize2di, than: ISize2di): Boolean = {
    outer.height >= than.height  &&  outer.width >= than.width
  }

  def isLargerThan(outer: ISize2di, than: ISize2di): Boolean = {
    outer.height > than.height  &&  outer.width > than.width
  }

  /** Отрендерить в строку вида WxH. */
  def toString(sz2d: ISize2di): String = {
    wxh(sz2d)
  }

  def wxh(sz2d: ISize2di): String = {
    sz2d.width.toString + "x" + sz2d.height
  }

  def whRatio(sz2d: ISize2di): Double = {
    sz2d.width.toDouble / sz2d.height.toDouble
  }

  @inline implicit def univEq: UnivEq[ISize2di] = UnivEq.force

}


/** Интерфейс моделей двумерных целочисленных размеров. */
trait ISize2di extends IWidth with IHeight {

  override def toString = ISize2di.toString(this)

  // 2017.sep.28: Нельзя тут определять всякие equals, hashCode и прочее.

}


object IWidth {
  def f = { x: IWidth => x.width }
}
/** Интерфейс для доступа к ширине. Т.е. одномерная проекция горизонтального размера. */
trait IWidth {
  /** Ширина. */
  def width: Int
}

object IHeight {
  def f = { y: IHeight => y.height}
}
/** Интерфейс для доступа к высоте. Т.е. одномерная проекция вертикального размера. */
trait IHeight {
  /** Высота. */
  def height: Int
}


object MSize2di extends IEsMappingProps {

  import play.api.libs.json._
  import play.api.libs.functional.syntax._

  implicit val MSIZE2DI_FORMAT: OFormat[MSize2di] = {
    val C = MediaConst.NamesShort
    (
      (__ \ C.WIDTH_FN).format[Int] and
      (__ \ C.HEIGHT_FN).format[Int]
    )(apply, unlift(unapply))
  }


  import boopickle.Default._

  /** Поддержка boopickle для инстансов [[MSize2di]]. */
  implicit val size2diPickler: Pickler[MSize2di] = {
    generatePickler[MSize2di]
  }

  /** Собрать инстанс [[MSize2di]] из данных, записанных в [[ISize2di]] */
  def apply(sz2d: ISize2di): MSize2di = {
    apply(
      width  = sz2d.width,
      height = sz2d.height
    )
  }

  /** Вернуть переданный либо собрать новый инстанс [[MSize2di]]. */
  def applyOrThis(sz2d: ISize2di): MSize2di = {
    sz2d match {
      case msz2d: MSize2di =>
        msz2d
      case other =>
        apply(other)
    }
  }

  @inline implicit def univEq: UnivEq[MSize2di] = UnivEq.derive

  def square(sidePx: Int) = apply(width = sidePx, height = sidePx)


  implicit object MSize2diSimpleArithmeticHelper extends SimpleArithmetics[MSize2di, Int] {
    override def applyMathOp(v: MSize2di)(op: Int => Int): MSize2di = {
      v.copy(
        width   = op(v.width),
        height  = op(v.height)
      )
    }
  }


  val width = GenLens[MSize2di](_.width)
  val height = GenLens[MSize2di](_.height)


  override def esMappingProps(implicit dsl: MappingDsl): JsObject = {
    import dsl.{DocFieldTypes => Dft, _}
    val numField = Json.toJsObject(
      FNumber(
        typ   = Dft.Integer,
        index = someTrue,
      )
    )
    val F = MediaConst.NamesShort
    Json.obj(
      F.WIDTH_FN -> numField,
      F.HEIGHT_FN -> numField,
    )
  }

}

/** Дефолтовая реализация [[ISize2di]]. */
final case class MSize2di(
                          override val width  : Int,
                          override val height : Int
                        )
  extends ISize2di


/** Интерфейс для именованной обёртки над [[MSize2di]]. Полезно для enum'ов.
  * Позволяет задать допустимый размер строковым алиасом. */
trait INamedSize2di extends StringEnumEntry {

  def whPx: MSize2di

}
