package io.suggest.adv

import io.suggest.common.html.HtmlConstants
import play.api.libs.json.KeyWrites

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.01.17 15:56
  */
package object rcvr {

  /**
    * Раньше это был класс.
    * Теперь просто цепочка id узлов в прямом порядке: от id родителя к подчиненном узлу.
    */
  type RcvrKey = List[String]

  def KEY_DELIM = HtmlConstants.SPACE

  /** Тип не указан, чтобы был доступ к полю DELIM. */
  implicit def rcvrKeyKeyWrites: KeyWrites[RcvrKey] = {
    new KeyWrites[RcvrKey] {
      override def writeKey(key: RcvrKey): String = key.mkString( KEY_DELIM )
    }
  }

}
