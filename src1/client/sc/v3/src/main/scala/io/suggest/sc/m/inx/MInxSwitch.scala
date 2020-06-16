package io.suggest.sc.m.inx

import diode.FastEq
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 10.06.2020 17:06
  * Description: Неопциональная обёртка вокруг [[MInxSwitchAskS]], чтобы была возможность хранить
  * какие-либо неопциональные данные состояния.
  */
object MInxSwitch {

  val empty = apply()

  implicit object MInxSwitchFeq extends FastEq[MInxSwitch] {
    override def eqv(a: MInxSwitch, b: MInxSwitch): Boolean = {
      (a.ask ===* b.ask)
    }
  }


  def ask = GenLens[MInxSwitch]( _.ask )

  @inline implicit def univEq: UnivEq[MInxSwitch] = UnivEq.derive

}

case class MInxSwitch(
                       ask      : Option[MInxSwitchAskS]      = None,
                     ) {

  lazy val searchCssOpt = ask.map(_.searchCss)

  lazy val nodesFoundOpt = ask.map(_.searchCss.args.nodesFound)

}
