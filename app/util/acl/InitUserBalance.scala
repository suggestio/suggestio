package util.acl

import models.req.ISioUser

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.12.15 10:43
 * Description: Некоторые ACL'ки должны запускать в фоне чтение баланса текущего юзера.
 */
trait InitUserBalance {

  /** Нужен ли доступ к кошельку узла и другие функции sioReqMd? */
  def initUserBalance: Boolean

  protected def maybeInitUserBalance(user: ISioUser): Unit = {
    if (initUserBalance)
      user.mBalancesFut
  }

}
