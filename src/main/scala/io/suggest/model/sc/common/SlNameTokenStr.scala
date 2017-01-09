package io.suggest.model.sc.common


/** Поле name для поиска по полю sls 2014-aug, которое содержит ngram'мы различной длины.
  * Можно искать по sink-name из [[io.suggest.ym.model.common.AdnSinks]],
  * по [[AdShowLevels]] и по полному имени [[SinkShowLevels]]. */
trait SlNameTokenStr {

  /** Название уровня отображения. */
  def name: String

}
