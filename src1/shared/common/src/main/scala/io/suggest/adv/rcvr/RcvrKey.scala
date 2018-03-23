package io.suggest.adv.rcvr

import io.suggest.common.html.HtmlConstants

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.03.18 22:34
  * Description: Статическая утиль для RcvrKey-модели.
  */
object RcvrKey {

  def rcvrKey2urlPath(rcvrKey: RcvrKey): String = {
    rcvrKey.mkString( HtmlConstants.SLASH )
  }

}
