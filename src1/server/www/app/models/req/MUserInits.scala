package models.req

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.12.15 11:03
 * Description: Модель команд инициализации модели [[ISioUser]].
 * Команды перечисляются в конце вызовов к ActionBuilder'у:
 * {{{
 *    def action = CanDoSomething(arg1, MUserInits.One...) { implicit request =>
 *      request.user.mBalancesFut ...
 *    }
 * }}}
 */
object MUserInits {

  /** Запуск чтения баланса юзера. */
  object Balance extends MUserInit {
    override def initUser(user: ISioUser): Unit = {
      user.balancesFut
    }
  }

  /** Запуск чтения контракта юзера. */
  object Contract extends MUserInit {
    override def initUser(user: ISioUser): Unit = user.mContractOptFut
  }

  /** Запуск чтения MNode юзера. */
  object PersonNode extends MUserInit {
    override def initUser(user: ISioUser): Unit = user.personNodeOptFut
  }

  /** Запуск чтения id контракта. */
  object ContractId extends MUserInit {
    override def initUser(user: ISioUser): Unit = user.contractIdOptFut
  }

  /** Инициализация обыденного личного кабинета: баланс, счетчик новых событий, возможно ещё что-то.
    * @see [[models.req.ISioUserT.lkCtxDataFut]] */
  object Lk extends MUserInit {
    override def initUser(user: ISioUser): Unit = {
      user.lkCtxDataFut
    }
  }


  /** Накатить список команд инициализации на экземпляр [[ISioUser]]. */
  def initUser(user: ISioUser, cmds: Iterable[MUserInit]): Unit = {
    for (cmd <- cmds)
      cmd.initUser(user)
  }

}


sealed trait MUserInit {
  /** Исполнить одну команду инициализации. */
  def initUser(user: ISioUser): Unit
}

