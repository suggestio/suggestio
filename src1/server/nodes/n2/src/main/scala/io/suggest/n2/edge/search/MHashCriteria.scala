package io.suggest.n2.edge.search

import io.suggest.common.empty.EmptyProduct
import io.suggest.crypto.hash.MHash
import io.suggest.es.model.{IMust, Must_t}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 25.12.2019 11:37
  * Description: Критерий поиска по hash value.
  *
  * @param hTypes Искомые типы хешей.
  * @param hexValues Искомые значение хешей.
  */
final case class MHashCriteria(
                                hTypes     : Seq[MHash]    = Nil,
                                hexValues  : Seq[String]   = Nil,
                                must       : Must_t        = IMust.MUST
                              )
  extends EmptyProduct
