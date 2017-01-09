package io.suggest.mbill2.util

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.12.15 17:22
 * Description: Генератор названий для объектов СУБД.
 * names содержит название таблицы и затем название полей, покрываемых ключом/индексом.
 */
object PgaNamesMaker {

  def SUF_FK          = "fkey"
  def SUF_INX         = "idx"
  def PREFIX_FK_INX   = "fki"
  def KEY             = "key"   // unique
  def SEP             = "_"

  def fkey(names: String*): String = {
    val sep = SEP
    names.mkString("", sep, sep + SUF_FK)
  }

  def fkInx(names: String*): String = {
    val sep = SEP
    names.mkString(PREFIX_FK_INX + sep, sep, sep + SUF_FK)
  }

  def inx(names: String*): String = {
    val sep = SEP
    names.mkString("", sep, sep + SUF_INX)
  }

  def uniq(names: String*): String = {
    val sep = SEP
    names.mkString("", sep, sep + KEY)
  }

}
