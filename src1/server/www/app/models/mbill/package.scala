package models

import play.api.data.Form

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.03.19 17:39
  */
package object mbill {

  type ContractForm_t       = Form[io.suggest.mbill2.m.contract.MContract]

}
