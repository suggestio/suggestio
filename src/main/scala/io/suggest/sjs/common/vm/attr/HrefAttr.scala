package io.suggest.sjs.common.vm.attr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.09.16 15:10
  * Description: У разных тегов бывает аттрибут href, который хочется пробросить как API.
  */
trait HrefAttr extends AttrVmT {

  /** Значение атрибута href, если оно есть. */
  def href = getAttribute("href")

}
