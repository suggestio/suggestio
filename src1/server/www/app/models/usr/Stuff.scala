package models.usr

import io.suggest.session.Ttl

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 18.02.15 17:03
 * Description: Барахло для моделей.
 */

case class EpwLoginFormBind(
  email       : String,
  password    : String,
  ttl         : Ttl
)


/** Binding формы подтверждения регистрации по email возвращает эти данные. */
case class EmailPwConfirmInfo(
  adnName   : String,
  password  : String
)
