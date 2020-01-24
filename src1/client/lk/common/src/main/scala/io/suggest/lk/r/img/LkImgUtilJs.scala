package io.suggest.lk.r.img

import io.suggest.common.geom.d2.MSize2di
import io.suggest.err.ErrorConstants
import io.suggest.img.crop.MCrop
import io.suggest.lk.m.SetImgWh
import io.suggest.lk.m.img.MPictureCropPopup
import io.suggest.n2.edge.EdgeUid_t
import io.suggest.msg.ErrorMsgs
import io.suggest.react.Props2ModelProxy
import io.suggest.react.ReactDiodeUtil
import io.suggest.sjs.common.log.Log
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent}
import org.scalajs.dom.html

import scala.scalajs.js
import scala.scalajs.js.UndefOr

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.04.18 17:08
  * Description: Утиль для рендера кропнутых картинок на клиенте.
  */
object LkImgUtilJs extends Log {

  /** В редакторах для нужд кропа требуется вычислять размеры картинки на стороне браузера.
    * Иначе кроп работать не будет.
    * Тут код реакции на react-событие img.onLoad().
    */
  def notifyImageLoaded[P: Props2ModelProxy, S]($: BackendScope[P,S], edgeUid: EdgeUid_t)(e: ReactEvent): Callback = {
    // Прочитать natural w/h из экшена.
    try {
      val img = e.target.asInstanceOf[html.Image]
      val sz = MSize2di(
        width  = img.naturalWidth,
        height = img.naturalHeight
      )
      val minWh = 0
      ErrorConstants.assertArg( sz.width > minWh )
      ErrorConstants.assertArg( sz.height > minWh )
      ReactDiodeUtil.dispatchOnProxyScopeCB( $, SetImgWh(edgeUid, sz) )
    } catch {
      case ex: Throwable =>
        LOG.error( ErrorMsgs.IMG_EXPECTED, ex = ex, msg = (edgeUid, e.target.toString) )
        Callback.empty
    }
  }


  // int mult с процентами.
  private def _imult3(a: Int, b: UndefOr[Double], dflt: Int): Int =
    b.fold(dflt)(x => (a * x / 100).toInt)
  private def _imult2(a: Int, b: UndefOr[Double]): Int =
    _imult3(a, b, a)
  private def _orZero(a: js.UndefOr[Int]): Int =
    a.getOrElse(0)

  /** Вычисляем MCrop в пикселях на основе данных состояния и wh изображения. */
  def cropPopupS2mcrop(cropPopup: MPictureCropPopup, origWh: MSize2di): MCrop = {
    cropPopup.pixelCrop.fold {
      val pcCrop = cropPopup.percentCrop
      // Попытаться перемножить percent crop и image wh
      MCrop(
        width  = _imult2(origWh.width,  pcCrop.width),
        height = _imult2(origWh.height, pcCrop.height),
        offX   = _imult3(origWh.width,  pcCrop.x, 0),
        offY   = _imult3(origWh.height, pcCrop.y, 0)
      )
    } { pxCrop =>
      MCrop(
        width  = pxCrop.width.getOrElse( origWh.width ),
        height = pxCrop.height.getOrElse( origWh.height ),
        offX   = _orZero( pxCrop.x ),
        offY   = _orZero( pxCrop.y )
      )
    }
  }

}
