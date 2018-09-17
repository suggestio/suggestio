package io.suggest.common.m.sql

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.12.15 16:02
  * Description: Интерфейс для доступа к полю с именем таблицы.
  * Изначально зародился в файле web21:models/SiowebSqlModel.scala в ~2014 году.
  */
trait ITableName {

  /** Название таблицы. Используется при сборке sql-запросов. */
  def TABLE_NAME: String

}
