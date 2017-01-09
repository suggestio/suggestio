package io.suggest.stat.m

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.09.16 17:34
  * Description: Модель кое-каких данных об одном stat-индексе.
  */
case class MStatInxInfo(
  inxName   : String,
  docCount  : Long
)
