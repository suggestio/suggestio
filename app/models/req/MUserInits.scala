package models.req

import io.suggest.primo.TypeT

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
object MUserInits extends TypeT {

  sealed trait ValT {
    /** Исполнить одну команду инициализации. */
    def initUser(user: ISioUser): Unit
  }

  sealed trait ValTDummy extends ValT {
    override def initUser(user: ISioUser): Unit = {}
  }

  override type T = ValT


  /** Запуск чтения баланса юзера. */
  sealed trait BalanceT extends ValTDummy {
    override def initUser(user: ISioUser): Unit = {
      super.initUser(user)
      user.mBalancesFut
    }
  }
  object Balance extends BalanceT

  /** Запуск чтения контракта юзера. */
  object Contract extends ValT {
    override def initUser(user: ISioUser): Unit = user.mContractOptFut
  }

  /** Запуск чтения MNode юзера. */
  object PersonNode extends ValT {
    override def initUser(user: ISioUser): Unit = user.personNodeOptFut
  }

  /** Запуск чтения id контракта. */
  object ContractId extends ValT {
    override def initUser(user: ISioUser): Unit = user.contractIdOptFut
  }

  /** Запуск узнавания значения счетчика событий. */
  sealed trait EvtsCountT extends ValTDummy {
    override def initUser(user: ISioUser): Unit = {
      super.initUser(user)
      user.evtsCountFut
    }
  }
  object EvtsCount extends EvtsCountT

  /** Инициализация обыденного личного кабинета: баланс, счетчик новых событий, возможно ещё что-то. */
  object Lk extends ValT {
    override def initUser(user: ISioUser): Unit = {
      user.lkCtxData
    }
  }


  /** Накатить список команд инициализации на экземпляр [[ISioUser]]. */
  def initUser(user: ISioUser, cmds: TraversableOnce[ValT]): Unit = {
    for (cmd <- cmds) {
      cmd.initUser(user)
    }
  }

}
