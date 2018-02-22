package io.suggest.sc.styl

import diode.FastEq
import diode.data.Pot
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.MScreen
import io.suggest.media.IMediaInfo
import io.suggest.model.n2.node.meta.colors.MColors
import io.suggest.sc.index.MWelcomeInfo
import io.suggest.sc.sc3.MSc3IndexResp
import io.suggest.spa.OptFastEq
import japgolly.univeq._
import io.suggest.ueq.UnivEqUtil._

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
        ((a.screen ===* b.screen) || (a.screen ==* b.screen)) &&
        (a.wcBgWh ===* b.wcBgWh) &&
        (a.wcFgWh ===* b.wcFgWh)
    }
  }

  def from(indexResp: Pot[MSc3IndexResp], mscreen: MScreen): MScCssArgs = {
    val indexRespOpt = indexResp.toOption
    val wcOpt = indexRespOpt.flatMap(_.welcome)
    def __wcWhOpt(f: MWelcomeInfo => Option[IMediaInfo]) = wcOpt.flatMap(f).flatMap(_.whPx)

    MScCssArgs(
      customColorsOpt = indexRespOpt.map(_.colors),
      screen          = mscreen,
      wcBgWh          = __wcWhOpt( _.bgImage ),
      wcFgWh          = __wcWhOpt( _.fgImage )
    )
  }

}


case class MScCssArgs(
                       override val customColorsOpt   : Option[MColors],
                       override val screen            : MScreen,
                       override val wcBgWh            : Option[MSize2di],
                       override val wcFgWh            : Option[MSize2di]
                     )
  extends IScCssArgs
