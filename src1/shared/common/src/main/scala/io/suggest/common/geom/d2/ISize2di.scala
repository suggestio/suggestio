package io.suggest.common.geom.d2

import io.suggest.math.{IBinaryMathOp, IntMathModifiers}
import io.suggest.media.MediaConst
import japgolly.univeq.UnivEq


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
    s"${sz2d.width}x${sz2d.height}"
  }

  def whRatio(sz2d: ISize2di): Double = {
    sz2d.width.toDouble / sz2d.height.toDouble
  }

  implicit def univEq: UnivEq[ISize2di] = UnivEq.force

}


/** Интерфейс моделей двумерных целочисленных размеров. */
trait ISize2di extends IWidth with IHeight {

  override def toString = ISize2di.toString(this)

  // 2017.sep.28: Нельзя тут определять всякие equals, hashCode и прочее.

}


/** Интерфейс для доступа к ширине. Т.е. одномерная проекция горизонтального размера. */
trait IWidth {
  /** Ширина. */
  def width: Int
  override def toString = "W(" + width + ")"
}

/** Интерфейс для доступа к высоте. Т.е. одномерная проекция вертикального размера. */
trait IHeight {
  /** Высота. */
  def height: Int
  override def toString = "H(" + height + ")"
}


object MSize2di {

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

  implicit def univEq: UnivEq[MSize2di] = UnivEq.derive

}

/** Дефолтовая реализация [[ISize2di]]. */
final case class MSize2di(
                          override val width  : Int,
                          override val height : Int
                        )
  extends ISize2di
  with IntMathModifiers[MSize2di]
{

  /** Модифицировать ширину и длину одной математической операцией. */
  override protected[this] def applyMathOp(op: IBinaryMathOp[Int], arg2: Int): MSize2di = {
    copy(
      width   = op(width, arg2),
      height  = op(height, arg2)
    )
  }

  def withWidth(width: Int)   = copy( width = width )
  def withHeight(height: Int) = copy( height = height )

}


/** Именованая версия [[ISize2di]]. Полезно для enum'ов.
  * Позволяет задать допустимый размер строковым алиасом. */
trait INamedSize2di extends ISize2di {

  /** Алиас (название) размера. */
  def szAlias: String

  override def toString = "Sz2D(" + szAlias + ",w=" + width + ";h=" + height + ")"
}


/** Враппер для модели [[ISize2di]]. */
trait ISize2diWrap extends ISize2di {

  def _sz2dUnderlying: ISize2di

  override def height = _sz2dUnderlying.height
  override def width  = _sz2dUnderlying.width

}