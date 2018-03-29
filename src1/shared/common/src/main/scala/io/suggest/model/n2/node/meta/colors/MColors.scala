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
    (__ \ MColorKeys.Bg.value).formatNullable[MColorData] and
    (__ \ MColorKeys.Fg.value).formatNullable[MColorData] and
    (__ \ MColorKeys.Pattern.value).formatNullable[MColorData]
  )(MColors.apply, unlift(MColors.unapply))

  implicit def univEq: UnivEq[MColors] = UnivEq.derive

}


case class MColors(
  bg        : Option[MColorData]    = None,
  fg        : Option[MColorData]    = None,
  pattern   : Option[MColorData]    = None
)
  extends EmptyProduct
{

  /** Цвет паттерна. */
  def withPattern(pattern: Option[MColorData] = None) = copy(pattern = pattern)

}
