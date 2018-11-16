package io.suggest.sc.styl

import diode.FastEq
import diode.data.Pot
import io.suggest.color.MColors
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.MScreenInfo
import io.suggest.media.IMediaInfo
import io.suggest.sc.index.{MSc3IndexResp, MWelcomeInfo}
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
  implicit object MScCssArgsFastEq extends FastEq[IScCssArgs] {
    override def eqv(a: IScCssArgs, b: IScCssArgs): Boolean = {
      // Screen сравнивать референсно или по значению. TODO Актуально ли ещё полное сравнение? Может сравнивания в контроллере ScreenAh достаточно?
      // customColors -- референсно внутри Option, т.к. внешний Option пересобирается каждый раз.
      // Остальное -- чисто референсно.
      OptFastEq.Plain.eqv(a.customColorsOpt, b.customColorsOpt) &&
        ((a.screenInfo ===* b.screenInfo) || (a.screenInfo ==* b.screenInfo)) &&
        (a.wcBgWh ===* b.wcBgWh) &&
        (a.wcFgWh ===* b.wcFgWh)
    }
  }

  def from(indexResp: Pot[MSc3IndexResp], screenInfo: MScreenInfo): MScCssArgs = {
    val indexRespOpt = indexResp.toOption
    val wcOpt = indexRespOpt.flatMap(_.welcome)
    def __wcWhOpt(f: MWelcomeInfo => Option[IMediaInfo]) = wcOpt.flatMap(f).flatMap(_.whPx)

    MScCssArgs(
      customColorsOpt = indexRespOpt.map(_.colors),
      screenInfo      = screenInfo,
      wcBgWh          = __wcWhOpt( _.bgImage ),
      wcFgWh          = __wcWhOpt( _.fgImage )
    )
  }

}


case class MScCssArgs(
                       override val customColorsOpt   : Option[MColors],
                       override val screenInfo        : MScreenInfo,
                       override val wcBgWh            : Option[MSize2di],
                       override val wcFgWh            : Option[MSize2di]
                     )
  extends IScCssArgs
