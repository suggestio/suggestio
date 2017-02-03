package io.suggest.es.search

/**
  * Интерфейс для перечисления названий ES-типов.
  * Используется для поиска в индексе в рамках перечисленных типов.
  */
trait IEsTypes {

  /** Список названий ES-типов. */
  def esTypes: Seq[String]

}
