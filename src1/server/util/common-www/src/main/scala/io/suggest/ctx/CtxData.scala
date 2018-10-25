package io.suggest.ctx

import io.suggest.init.routed.MJsInitTarget
import io.suggest.mbill2.m.balance.MBalance

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.12.15 8:26
 * Description: Модель дополнительных произвольных данных, передаваемых по шаблонам внутри Context.
 */


/**
 * Модель для произвольных данных, закидываемых в контекст.
 * @param jsInitTargets Какие-то доп.цели инициализации, выставляемые на уровне экшена
 * @param mUsrBalances Остатки на счетах юзера, обычно приходят из request.user.balancesFut в контроллер.
 * @param mdrNodesCount Кол-во узлов в очереди на модерацию.
 */
case class CtxData(
                    jsInitTargets    : List[MJsInitTarget]    = Nil,
                    mUsrBalances     : Seq[MBalance]          = Nil,
                    mdrNodesCount    : Option[Int]            = None,
                  ) {

  def withJsInitTargets(jsInitTargets: List[MJsInitTarget]) = copy(jsInitTargets = jsInitTargets)

  /**
   * Заменить jsInitTargets на список списков.
   * @param jsInitTarget2 Списки новых целей js-инициализации.
   * @return this, либо обновлённый экземпляр.
   */
  def withJsInitTargetsAll(jsInitTarget2: List[MJsInitTarget]*): CtxData = {
    if (jsInitTarget2.exists(_.nonEmpty)) {
      copy(
        jsInitTargets = jsInitTarget2.iterator.flatten.toList
      )
    } else {
      this
    }
  }

}


object CtxData {

  /** Часто-используемый пустой инстанс [[CtxData]]. */
  val empty = apply()

}
