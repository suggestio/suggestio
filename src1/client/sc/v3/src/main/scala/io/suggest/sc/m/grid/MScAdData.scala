package io.suggest.sc.m.grid

import diode.FastEq
import diode.data.Pot
import io.suggest.common.empty.OptionUtil
import io.suggest.jd.render.m.MJdDataJs
import io.suggest.jd.tags.MJdTagNames
import io.suggest.primo.id.OptStrId
import io.suggest.ueq.JsUnivEqUtil._
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 15.11.17 19:07
  * Description: Модель рантаймовой инфы по одному блоку рекламной карточки в выдаче.
  * Модель собирается на базе созвучной MJdAdData, более простой и абстрактной по сравнению с этой.
  */
object MScAdData {

  /** Поддержка FastEq для инстансов [[MScAdData]]. */
  implicit object MScAdDataFastEq extends FastEq[MScAdData] {
    override def eqv(a: MScAdData, b: MScAdData): Boolean = {
      (a.main ===* b.main) &&
      (a.focused ===* b.focused)
    }
  }

  @inline implicit def univEq: UnivEq[MScAdData] = UnivEq.derive

  val main    = GenLens[MScAdData](_.main)
  val focused = GenLens[MScAdData](_.focused)

}


/** Класс-контейнер данных по одному блоку плитки.
  *
  * @param main Рендеренный главный блок для плитки разных карточек.
  * @param focused Плитка одной открытой карточки.
  *                Приходит после открытия карточки, представленной main-блоком.
  */
final case class MScAdData(
                            main        : MJdDataJs,
                            focused     : Pot[MJdDataJs]    = Pot.empty,
                          )
  extends OptStrId
{

  def isFocused = focused.isReady

  def focOrMain: MJdDataJs =
    focused getOrElse main

  /** Всегда раскрытая карточка в данной плитке. */
  def isAlwaysOpened: Boolean = {
    main.doc.template.rootLabel.name ==* MJdTagNames.DOCUMENT
  }

  // 2019-11-18 Может быть ситуация, что уже развёрнутая карточка прямо в плитке, и вместо одного блока тут целый DOCUMENT.
  def alwaysOpened: Option[MJdDataJs] =
    OptionUtil.maybe( isAlwaysOpened )(main)

  def focOrAlwaysOpened: Option[MJdDataJs] = {
    focused
      .toOption
      .orElse( alwaysOpened )
  }


  def nodeId = main.doc.jdId.nodeId
  override def id = nodeId

}
