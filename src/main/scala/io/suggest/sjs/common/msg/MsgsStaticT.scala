package io.suggest.sjs.common.msg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.08.15 17:32
 * Description: Общая утиль для Msgs-объектов.
 */
trait MsgsStaticT {

  protected def _PREFIX: String

  protected def E(i: Int): String = {
    _PREFIX + i
  }

}
