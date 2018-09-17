package io.suggest.msg

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.08.15 17:32
 * Description: Общая утиль для Msgs-объектов.
 */
trait MsgsStaticT {

  protected def _PREFIX: String

  protected def E(i: Int): ErrorMsg_t = {
    _PREFIX + i
  }

}
