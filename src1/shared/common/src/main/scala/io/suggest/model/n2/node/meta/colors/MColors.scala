package io.suggest.model.n2.node.meta.colors

import boopickle.Default._
import io.suggest.color.MColorData
import io.suggest.common.empty.{EmptyProduct, IEmpty}
import japgolly.univeq.UnivEq
import play.api.libs.json._
import play.api.libs.functional.syntax._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.05.17 15:15
  * Description: Кросс-платформенная модель описания цветовой триады узла.
  *
  * Она имеет вид мапы, но в виде класса, для удобства доступа по ключам.
  * Есть мнение, что это неправильно, и класс нужно заменить на коллекцию.
  */
object MColors extends IEmpty {

  override type T = MColors

  override val empty: MColors = {
    new MColors() {
      override def nonEmpty = false
    }
  }

  /** Поддержка boopickle. */
  implicit val mColorsPickler: Pickler[MColors] = {
    implicit val mColorsDataP = MColorData.mColorDataPickler
    generatePickler[MColors]
  }

  /** Поддержка JSON. */
  implicit val MCOLORS_FORMAT: OFormat[MColors] = (
    (__ \ MColorTypes.Bg.value).formatNullable[MColorData] and
    (__ \ MColorTypes.Fg.value).formatNullable[MColorData]
  )(apply, unlift(unapply))

  implicit def univEq: UnivEq[MColors] = UnivEq.derive

  def bgF = { cs: MColors => cs.bg }
  def fgF = { cs: MColors => cs.fg }

}


// TODO Вероятно, надо заменить эту модель (или её поля) чем-то типа Map[MColorType, MColorData].

case class MColors(
  bg        : Option[MColorData]    = None,
  fg        : Option[MColorData]    = None
)
  extends EmptyProduct
{

  /** Вернуть все перечисленные цвета. */
  def allColorsIter: Iterator[MColorData] = {
    Iterator( bg, fg )
      .flatten
  }


  def ofType(mct: MColorType): Option[MColorData] = {
    mct match {
      case MColorTypes.Bg => bg
      case MColorTypes.Fg => fg
    }
  }


  def withColorOfType(mct: MColorType, mcdOpt: Option[MColorData]): MColors = {
    mct match {
      case MColorTypes.Bg => withBg(mcdOpt)
      case MColorTypes.Fg => withFg(mcdOpt)
    }
  }

  def withBg(bg: Option[MColorData]) = copy(bg = bg)
  def withFg(fg: Option[MColorData]) = copy(fg = fg)

}
