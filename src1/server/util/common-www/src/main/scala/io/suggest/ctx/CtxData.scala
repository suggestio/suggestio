package io.suggest.ctx

import io.suggest.init.routed.MJsiTg
import io.suggest.mbill2.m.balance.MBalance

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.12.15 8:26
 * Description: Модель дополнительных произвольных данных, передаваемых по шаблонам внутри Context.
 */


/**
 * Модель для произвольных данных, закидываемых в контекст.
 * @param jsiTgs Какие-то доп.цели инициализации, выставляемые на уровне экшена
 * @param mUsrBalances Остатки на счетах юзера, обычно приходят из request.user.balancesFut в контроллер.
 */
case class CtxData(
                    jsiTgs           : List[MJsiTg]    = Nil,
                    mUsrBalances     : Seq[MBalance]   = Nil
                  ) {

  def withJsiTgs(jsiTgs2: List[MJsiTg]) = copy(jsiTgs = jsiTgs2)

  /**
   * builder-метод, враппер над copy. Приписывает новые цели js-инициализации перед текущими.
   * @param jsiTgss Списки новых целей js-инициализации.
   * @return Экземпляр [[CtxData]], этот либо обновлённый.
   */
  def prependJsiTgs(jsiTgss: List[MJsiTg]*): CtxData = {
    if (jsiTgss.exists(_.nonEmpty)) {
      copy(
        jsiTgs = jsiTgss.iterator.flatten.toList
      )
    } else {
      this
    }
  }

}


object CtxData {

  /** Часто-используемый пустой инстанс [[CtxData]]. */
  val empty = CtxData()
}
