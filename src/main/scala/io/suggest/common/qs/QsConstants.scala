package io.suggest.common.qs

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.09.16 12:00
  * Description: Разные важные common-константы, не подходящие в другие категории.
  */
object QsConstants {

  /**
    * Разделитель между частями ключа qs.
    * URL: ?k.subk=...&k.subk2=...
    */
  def KEY_PARTS_DELIM_STR = "."

  /**
    * Имя js-функции/2 в js-роутере, которая занимается сериализацией значений, объектов в первую очередь.
    * {{{
    *  var @JSRR_OBJ_TO_QS_F = function(k, v) { return "a.b=1&a.c=666" }
    * }}}
    * См. шаблон jsRevRouterBase.scala.js в web21.
    */
  def JSRR_OBJ_TO_QS_F = "_o2qs"

}
