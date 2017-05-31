package io.suggest.model.n2.node.meta.colors

import io.suggest.common.empty.{EmptyProduct, IEmpty}

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
