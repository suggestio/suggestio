package util.sqlm

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.11.15 18:28
  * Description: Есть SQL-модели, которые генерируют динамический SQL для своей работы.
  */


/** Поддержка генерации кусков SQL для INSERT'ов. */
trait SqlInsertFieldsGenerators {

  def INSERT_FIELDS: TraversableOnce[String]

  /**
    * Строка со списком полей для вставки в
    * {{{
    *   INSERT INTO table(СЮДА) ...
    * }}}
    */
  def INSERT_FNS: String = {
    INSERT_FIELDS.mkString(",")
  }

  /** Строка со списком anorm-полей. */
  def INSERT_VALUES_FNS: String = {
    INSERT_FIELDS.mkString("{", "},{", "}")
  }

}


/** Поддержка генерации кусков SQL для UPDATE'ов. */
trait SqlUpdateFieldsGenerators {

  def UPDATE_FIELDS: TraversableOnce[String]

  /**
    * Строка для UPDATE SET:
    * {{{
    *   UPDATE ... SET x=y, z=a, ....
    * }}}
    */
  def UPDATE_FNS: String = {
    UPDATE_FIELDS
      .toIterator
      .map { v => s"$v = {$v}" }
      .mkString(",")
  }

}


/** Поддержка генерации кусков SQL для INSERT/UPDATE на основе единого списка. */
trait SqlFieldsGenerators
  extends SqlInsertFieldsGenerators
  with SqlUpdateFieldsGenerators
{

  /** Метод, возвращающий список полей, который можно пройти хотя бы один раз. */
  def FIELDS: TraversableOnce[String]

  override def INSERT_FIELDS = FIELDS
  override def UPDATE_FIELDS = FIELDS
}

