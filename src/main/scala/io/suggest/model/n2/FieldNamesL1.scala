package io.suggest.model.n2

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
  @deprecated("use Extras.tag instead", "2015.oct.2")
  val TagVertex                   : T = new Val("tv")

  /** Поле с общими данными всех узлов N2. */
  val Common                      : T = new Val("c")

  /** Имя поля метаданных. Название унаследовано из MAdnNode. */
  val Meta                        : T = new Val("meta")

  /** Имя поля встроенных эджей. */
  val Edges                       : T = new Val("e")

  /** Имя поля-контейнера специальных моделей. */
  val Extras                      : T = new Val("x")

  /** Имя поля-контейнера геоданных. */
  val Geo                         : T = new Val("g")

}
