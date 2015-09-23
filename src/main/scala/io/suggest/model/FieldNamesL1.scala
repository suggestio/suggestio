package io.suggest.model

import io.suggest.common.menum.EnumValue2Val

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.09.15 17:25
 * Description: Модель коротких имён полей первого уровня ES-моделей.
 * Создана чтобы не было путаницы в полях, имена которых раскиданы по разным аддонам.
 */
object FieldNamesL1 extends EnumValue2Val {

  /** Экземпляр модели, т.е. инфа по полю. */
  protected[this] sealed class Val(val name: String)
    extends super.Val

  override type T = Val

  /** Поле вершины графа с проекцией на тег. */
  val TagVertex                   : T = new Val("tv")

  /** Поле с общими данными всех узлов N2. */
  val Common                      : T = new Val("c")

}
