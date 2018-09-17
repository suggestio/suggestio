package io.suggest.mbill2.m.order

import io.suggest.mbill2.m.item.MItem

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 05.02.16 13:17
  * Description: Виртуальная модель-контейнер для ордера с item'ами.
  */
case class MOrderWithItems(
                            morder: MOrder,
                            mitems: Seq[MItem]
                          )
