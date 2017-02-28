package io.suggest.www.util.acl

import play.api.mvc.ActionBuilder

import scala.language.higherKinds

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.02.17 14:49
  * Description: Трейт для добавления простой поддержки сборки ActionBuilder'ов под новые нужды.
  * В ходе упрощения и рефакторинга ActionBuilder'ы стали анонимными реализациями внутри apply().
  */
abstract class SioActionBuilder[R[_]] extends ActionBuilder[R]


class SioActionBuilderOuter { outer =>

  /** Для снижения кодогенерации компилятором, используем классы вместо трейтов. */
  abstract class SioActionBuilderImpl[R[_]] extends SioActionBuilder[R] {

    /** Для анонимной реализация это вполне удобно: использовать название от внешнего класса. */
    override def toString = outer.toString

  }


  override def toString: String = {
    try {
      getClass.getSimpleName
    } catch {
      case _: Throwable =>
        super.toString
    }
  }

}
