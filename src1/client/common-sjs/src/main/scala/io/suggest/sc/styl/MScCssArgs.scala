package io.suggest.sc.styl

import diode.FastEq
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.MScreen
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.sjs.common.spa.OptFastEq

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 24.07.17 10:56
  * Description: Реализация common-модели IScCssArgs, которая описывает аргументы для
  * рендера css-шаблона с динамическими стилями выдачи.
  */

object MScCssArgs {

  /** Поддержка FastEq для инстансов [[MScCssArgs]]. */
  implicit object MScCssArgsFastEq extends FastEq[MScCssArgs] {
    override def eqv(a: MScCssArgs, b: MScCssArgs): Boolean = {
      // Screen сравнивать референсно или по значению.
      // customColors -- референсно внутри Option, т.к. внешний Option пересобирается каждый раз.
      // Остальное -- чисто референсно.
      OptFastEq.Plain.eqv(a.customColorsOpt, b.customColorsOpt) &&
        ((a.screen eq b.screen) || (a.screen == b.screen)) &&
        (a.wcBgWh eq b.wcBgWh) &&
        (a.wcFgWh eq b.wcFgWh)
    }
  }

}

case class MScCssArgs(
                       override val customColorsOpt   : Option[MColors],
                       override val screen            : MScreen,
                       override val wcBgWh            : Option[MSize2di],
                       override val wcFgWh            : Option[MSize2di]
                     )
  extends IScCssArgs
