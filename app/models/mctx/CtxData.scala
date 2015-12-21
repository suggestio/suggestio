package models.mctx

import io.suggest.mbill2.m.balance.MBalance
import models.jsm.init.MTarget

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.12.15 8:26
 * Description: Модель дополнительных произвольных данных, передаваемых по шаблонам внутри [[Context]].
 */

trait ICtxData {

  def jsiTgs          : Seq[MTarget]
  def mUsrBalances    : Seq[MBalance]
  def evtsCount       : Option[Int]

}


/**
 * Модель для произвольных данных, закидываемых в контекст.
 * @param jsiTgs Какие-то доп.цели инициализации, выставляемые на уровне экшена
 * @param mUsrBalances Остатки на счетах юзера, обычно приходят из request.user.balancesFut в контроллер.
 * @param evtsCount Отображаемое кол-во непрочитанных событий.
 */
case class CtxData(
  override val jsiTgs           : Seq[MTarget]    = Nil,
  override val mUsrBalances     : Seq[MBalance]   = Nil,
  override val evtsCount        : Option[Int]     = None
)
  extends ICtxData
{

  /**
   * builder-метод, враппер над copy. Приписывает новые цели js-инициализации перед текущими.
   * @param jsiTgss Списки новых целей js-инициализации.
   * @return Экземпляр [[CtxData]], этот либо обновлённый.
   */
  def prependJsiTgs(jsiTgss: Seq[MTarget]*): CtxData = {
    if (jsiTgss.exists(_.nonEmpty)) {
      copy(
        jsiTgs = (jsiTgs.iterator ++ jsiTgss.iterator.flatten).toSeq
      )
    } else {
      this
    }
  }

}


object CtxData {

  /** Часто-используемый пустой инстанс [[ICtxData]]. */
  val empty = CtxData()
}
