package io.suggest.sc.sjs.vm.util.domvm.create

import io.suggest.primo.TypeT

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 15:52
 * Description: Бытрый доступ к сборке новых ViewModel'ов.
 */
trait CreateVm extends CreateEl with TypeT {

  protected def apply(el: E): T

  def createNew(): T = {
    apply( createNewEl() )
  }

}
