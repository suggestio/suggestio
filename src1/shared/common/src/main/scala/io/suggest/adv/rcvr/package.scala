package io.suggest.adv

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

}
