package io.suggest.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 31.08.17 17:52
  * Description: Варианты выхлопа js "typeof x".
  */
object JsTypes {

  def STRING = "string"

  // в т.ч. для null
  def OBJECT = "object"

  def NUMBER = "number"

  def BOOLEAN = "boolean"

  def FUNCTION = "function"

  def UNDEFINED = "undefined"

  def SYMBOL = "symbol"

}
