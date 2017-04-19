package util.acl

import models.req.{MUserInits, MUserInit, ISioUser}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 19.12.15 10:43
 * Description: Некоторые ACL'ки должны запускать в фоне чтение баланса текущего юзера или что-то ещё.
 * Это описывается списком команд инициализации при сборке action builder'а.
 */
object InitUserCmds {

  def maybeInitUser(user: ISioUser, userInits: Seq[MUserInit]): Unit = {
    if (userInits.nonEmpty) {
      MUserInits.initUser(user, userInits)
    }
  }

}


trait InitUserCmds {

  /** Список команд ранней инициализации. */
  def userInits: Seq[MUserInit]

  protected def maybeInitUser(user: ISioUser): Unit = {
    InitUserCmds.maybeInitUser(user, userInits)
  }

}
