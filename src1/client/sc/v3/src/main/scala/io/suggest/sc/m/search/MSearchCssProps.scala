package io.suggest.sc.m.search

import diode.FastEq
import io.suggest.dev.MScreenInfo
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 03.08.18 14:32
  * Description: Модель аргументов для css-шаблона доп.данных гео-панели.
  */
object MSearchCssProps {

  implicit object MSearchCssPropsFastEq extends FastEq[MSearchCssProps] {
    override def eqv(a: MSearchCssProps, b: MSearchCssProps): Boolean = {
      (a.screenInfo ===* b.screenInfo) &&
      // ==, потому что инстанс всегда новый, а внутри - примитивный тип данных.
      (a.nodesFoundShownCount ==* b.nodesFoundShownCount)
    }
  }


  implicit def univEq: UnivEq[MSearchCssProps] = UnivEq.derive

}


case class MSearchCssProps(
                            screenInfo              : MScreenInfo,
                            nodesFoundShownCount    : Option[Int]     = None
                          ) {

  def withScreenInfo(screenInfo: MScreenInfo) = copy(screenInfo = screenInfo)

}
