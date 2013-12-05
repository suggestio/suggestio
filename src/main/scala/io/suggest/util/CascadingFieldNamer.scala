package io.suggest.util

import com.scaleunlimited.cascading.BaseDatum

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 04.10.13 17:40
 * Description: Быстрое добавление метода fieldName(String) в объект.
 * Методы fieldName() везде одинаковые, статические и везде зависят от getClass, поэтому сам б-г велел вынести его в trait.
 */
trait CascadingFieldNamer {

  /**
   * Генератор названий полей, порождаемых в текущем классе.
   * @param fn Корень имени поля.
   * @return Стабильное и какбы уникальное имя поля, пригодное для flow.
   */
  def fieldName(fn: String) = BaseDatum.fieldName(getClass, fn).replace("$", "")
}