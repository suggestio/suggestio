package models

import play.api.data.Form

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 01.03.16 12:10
  */
package object mdr {

  type MRefuseMode  = MRefuseModes.T

  type RefuseForm_t = Form[MRefuseFormRes]

}
