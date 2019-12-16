package io.suggest.ctx

import io.suggest.init.routed.MJsInitTarget
import io.suggest.mbill2.m.balance.MBalance
import monocle.macros.GenLens

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.12.15 8:26
 * Description: Модель дополнительных произвольных данных, передаваемых по шаблонам внутри Context.
 */
object CtxData {

  /** Часто-используемый пустой инстанс [[CtxData]]. */
  val empty = apply()

  val jsInitTargets = GenLens[CtxData](_.jsInitTargets)

  def jsInitTargetsAppendOne(v: MJsInitTarget) =
    jsInitTargets.modify(v :: _)


  implicit class CtxDataOpsExt( val ctxData0: CtxData ) extends AnyVal {

    /**
      * Заменить jsInitTargets на список списков.
      * @param jsInitTarget2 Списки новых целей js-инициализации.
      * @return this, либо обновлённый экземпляр.
      */
    def appendJsInitTargetsAll(jsInitTarget2: List[MJsInitTarget]*): CtxData = {
      if (jsInitTarget2.exists(_.nonEmpty)) {
        CtxData.jsInitTargets.modify { jsInitTargets0 =>
          (jsInitTargets0.iterator ++ jsInitTarget2.iterator.flatten)
            .toList
        }(ctxData0)
      } else {
        ctxData0
      }
    }

  }

}


/**
 * Модель для произвольных данных, закидываемых в контекст.
 * @param jsInitTargets Какие-то доп.цели инициализации, выставляемые на уровне экшена
 * @param mUsrBalances Остатки на счетах юзера, обычно приходят из request.user.balancesFut в контроллер.
 * @param mdrNodesCount Кол-во узлов в очереди на модерацию.
 * @param cartItemsCount Кол-во элементов в заказе-корзине.
 */
case class CtxData(
                    jsInitTargets    : List[MJsInitTarget]    = Nil,
                    mUsrBalances     : Seq[MBalance]          = Nil,
                    mdrNodesCount    : Option[Int]            = None,
                    cartItemsCount   : Option[Int]            = None,
                  )
