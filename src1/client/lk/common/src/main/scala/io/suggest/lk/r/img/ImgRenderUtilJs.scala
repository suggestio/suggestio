package io.suggest.lk.r.img

import diode.react.ModelProxy
import io.suggest.common.geom.d2.{ISize2di, MSize2di}
import io.suggest.dev.MSzMult
import io.suggest.err.ErrorConstants
import io.suggest.img.crop.MCrop
import io.suggest.lk.m.SetImgWh
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.msg.ErrorMsgs
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent}
import japgolly.scalajs.react.vdom.TagMod
import japgolly.scalajs.react.vdom.html_<^._
import org.scalajs.dom.html
import io.suggest.react.ReactDiodeUtil.dispatchOnProxyScopeCB
import io.suggest.sjs.common.log.Log

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.04.18 17:08
  * Description: Утиль для рендера кропнутых картинок на клиенте.
  */
class ImgRenderUtilJs extends Log {

  /** Эмуляция опционального кропа. */
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
      htmlImgCropEmuAttrs(
        crop    = crop,
        outerWh = outerWh,
        origWh  = origWh,
        szMult  = szMult
      )
    }
  }


  /** Рендер img-аттрибутов для имитации кропа на клиенте. */
  def htmlImgCropEmuAttrs(crop    : MCrop,
                          outerWh : ISize2di,
                          origWh  : MSize2di,
                          szMult  : MSzMult): TagMod = {
    // Рассчёт коэффициента масштабирования:
    val outer2cropRatio = szMult.toDouble * outerWh.height.toDouble / crop.height.toDouble

    // Проецируем это отношение на натуральные размеры картинки, top и left:
    htmlImgCropEmuAttrs(crop, origWh, outer2cropRatio)
  }

  def htmlImgCropEmuAttrs(crop: MCrop,
                          origWh: MSize2di,
                          outer2cropRatio: Double): TagMod = {
    TagMod(
      ^.width       := (origWh.width  * outer2cropRatio).px,
      ^.height      := (origWh.height * outer2cropRatio).px,
      ^.marginLeft  := (-crop.offX    * outer2cropRatio).px,
      ^.marginTop   := (-crop.offY    * outer2cropRatio).px
    )
  }


  /** В редакторах для нужд кропа требуется вычислять размеры картинки на стороне браузера.
    * Иначе кроп работать не будет.
    * Тут код реакции на react-событие img.onLoad().
    */
  def notifyImageLoaded[P <: ModelProxy[_], S]($: BackendScope[P,S], edgeUid: EdgeUid_t, e: ReactEvent): Callback = {
    // Прочитать natural w/h из экшена.
    try {
      val img = e.target.asInstanceOf[html.Image]
      val sz = MSize2di(
        // IDEA почему-то ругается на deprecated, это ошибка в scala-плагине.
        width  = img.naturalWidth,
        height = img.naturalHeight
      )
      val minWh = 0
      ErrorConstants.assertArg( sz.width > minWh )
      ErrorConstants.assertArg( sz.height > minWh )
      dispatchOnProxyScopeCB( $, SetImgWh(edgeUid, sz) )
    } catch {
      case ex: Throwable =>
        LOG.error( ErrorMsgs.IMG_EXPECTED, ex = ex, msg = (edgeUid, e.target.toString) )
        Callback.empty
    }
  }

}
