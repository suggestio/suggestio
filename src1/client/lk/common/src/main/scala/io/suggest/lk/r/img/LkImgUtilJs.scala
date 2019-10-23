package io.suggest.lk.r.img

import io.suggest.common.geom.d2.MSize2di
import io.suggest.err.ErrorConstants
import io.suggest.model.n2.edge.EdgeUid_t
import io.suggest.msg.ErrorMsgs
import io.suggest.react.Props2ModelProxy
import io.suggest.react.ReactDiodeUtil
import io.suggest.sjs.common.log.Log
import io.suggest.spa.DAction
import japgolly.scalajs.react.{BackendScope, Callback, ReactEvent}
import org.scalajs.dom.html

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.04.18 17:08
  * Description: Утиль для рендера кропнутых картинок на клиенте.
  */
class LkImgUtilJs extends Log {


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

}

/** Уведомить систему о ширине и длине загруженной картинки. */
case class SetImgWh(edgeUid: EdgeUid_t, wh: MSize2di) extends DAction

