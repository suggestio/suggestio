package io.suggest.img

import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.dev.MSzMult
import io.suggest.img.crop.MCrop
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.10.2019 22:27
  * Description: Утиль для рендера картинок в реакте.
  */
object ImgUtilRJs {

  /** Эмуляция опционального кропа.
    * Рендер img-аттрибутов для имитации кропа на клиенте.
    */
  def htmlImgCropEmuAttrsOpt(
                              cropOpt    : Option[MCrop],
                              outerWhOpt : Option[ISize2di],
                              origWhOpt  : Option[MSize2di],
                              szMult     : MSzMult
                            ): Option[TagMod] = {
    for {
      crop    <- cropOpt
      outerWh <- outerWhOpt
      origWh  <- origWhOpt
    } yield {
      // Рассчёт коэффициента масштабирования:
      val outer2cropRatio = szMult.toDouble * outerWh.height.toDouble / crop.height.toDouble

      // Проецируем это отношение на натуральные размеры картинки, top и left:
      TagMod(
        ^.width       := (origWh.width  * outer2cropRatio).px,
        ^.height      := (origWh.height * outer2cropRatio).px,
        ^.marginLeft  := (-crop.offX    * outer2cropRatio).px,
        ^.marginTop   := (-crop.offY    * outer2cropRatio).px,
      )
    }
  }

}
