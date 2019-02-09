package io.suggest.dev

import diode.FastEq
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._
import monocle.macros.GenLens

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 26.06.18 17:46
  * Description: Контейнер моделей, описывающих данные по экрану устройства для выдачи.
  *
  * MScreen описывает только базовую геометрию для контента,
  * а бывают экраны с более сложной геометрией (iphone10+ с вырезом сверху посередине).
  *
  * Сюда можно складывать разные кросс-платформенные модели, описывающие экран.
  */
object MScreenInfo {

  implicit object MScreenInfoFastEq extends FastEq[MScreenInfo] {
    override def eqv(a: MScreenInfo, b: MScreenInfo): Boolean = {
      (a.screen ===* b.screen) &&
      (a.unsafeOffsets ===* b.unsafeOffsets)
    }
  }


  @inline implicit def univEq: UnivEq[MScreenInfo] = UnivEq.derive

  val screen = GenLens[MScreenInfo](_.screen)
  val unsafeOffsets = GenLens[MScreenInfo](_.unsafeOffsets)

}


case class MScreenInfo(
                        screen            : MScreen,
                        unsafeOffsets     : MTlbr       = MTlbr.empty
                      ) {

  def withScreen(screen: MScreen) = copy(screen = screen)
  def withSafeArea(safeArea: MTlbr) = copy(unsafeOffsets = safeArea)

  /** Геометрия экрана, пригодная для отображения контента (переднего плана). */
  def safeScreen: MScreen = {
    if (unsafeOffsets.isEmpty)
      screen
    else
      screen.withWh(
        width   = screen.width - unsafeOffsets.width,
        height  = screen.height - unsafeOffsets.height
      )
  }


  /** Следует ли разворачивать диалоги на весь экран? */
  def isDialogWndFullScreen: Boolean =
    screen.width < 800

}
