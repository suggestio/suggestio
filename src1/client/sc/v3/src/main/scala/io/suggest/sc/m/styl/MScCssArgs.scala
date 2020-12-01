package io.suggest.sc.m.styl

import diode.FastEq
import diode.data.Pot
import io.suggest.color.MColors
import io.suggest.common.empty.OptionUtil.BoolOptOps
import io.suggest.common.geom.d2.MSize2di
import io.suggest.dev.MScreenInfo
import io.suggest.img.MImgFormats
import io.suggest.sc.index.MSc3IndexResp
import io.suggest.spa.OptFastEq
import io.suggest.ueq.UnivEqUtil._
import japgolly.univeq._

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
      // Screen сравнивать референсно или по значению. TODO Актуально ли ещё полное сравнение? Может сравнивания в контроллере ScreenAh достаточно?
      // customColors -- референсно внутри Option, т.к. внешний Option пересобирается каждый раз.
      // Остальное -- чисто референсно.
      OptFastEq.Plain.eqv(a.customColorsOpt, b.customColorsOpt) &&
        ((a.screenInfo ===* b.screenInfo) || (a.screenInfo ==* b.screenInfo)) &&
        (a.wcBgWh ===* b.wcBgWh) &&
        (a.wcFgWh ===* b.wcFgWh) &&
        (a.wcFgForceEnlarge ==* b.wcFgForceEnlarge)
    }
  }

  def from(indexResp: Pot[MSc3IndexResp], screenInfo: MScreenInfo): MScCssArgs = {
    val indexRespOpt = indexResp.toOption
    val wcOpt = indexRespOpt.flatMap(_.welcome)

    val fgOpt = wcOpt.flatMap(_.fgImage)

    MScCssArgs(
      customColorsOpt = indexRespOpt.map(_.colors),
      screenInfo      = screenInfo,
      wcBgWh          = wcOpt.flatMap(_.bgImage).flatMap(_.whPx),
      wcFgWh          = fgOpt.flatMap(_.whPx),
      wcFgForceEnlarge = (for {
        wc <- wcOpt
        fg <- wc.fgImage
        imgFmt <- MImgFormats.withMime( fg.contentType )
        isVector = imgFmt.isVector
        // Разрешить растягивать векторные картинки:
        if isVector &&
           // Запретить растягивать служебные страницы. Нужно для пустого эфемерного suggest.io-узла в произвольной точке.
           indexRespOpt.exists(_.nodeId.nonEmpty)
      } yield {
        isVector
      })
        .getOrElseFalse
    )
  }

  @inline implicit def univEq: UnivEq[MScCssArgs] = UnivEq.derive

}


final case class MScCssArgs(
                             customColorsOpt   : Option[MColors],
                             screenInfo        : MScreenInfo,
                             wcBgWh            : Option[MSize2di],
                             wcFgWh            : Option[MSize2di],
                             wcFgForceEnlarge  : Boolean,
                           )
