package io.suggest.bill.cart.c

import diode.ModelRO
import io.suggest.bill.cart.MCartConf

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 20.09.18 16:41
  * Description: API для взаимодействия с сервером по вопросам биллинга/корзины.
  */
trait ILkCartApi {

}


/** Реализация Bill-Cart API поверх Http XHR. */
class LkCartApiXhrImpl( confRO: ModelRO[MCartConf] ) extends ILkCartApi {

}
