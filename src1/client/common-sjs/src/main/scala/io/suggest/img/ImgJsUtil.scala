package io.suggest.img

import io.suggest.common.geom.d2.MSize2di
import org.scalajs.dom
import org.scalajs.dom.Event
import org.scalajs.dom.html.Image

import scala.concurrent.{Future, Promise}
import scala.scalajs.js

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 27.09.17 18:50
  * Description: Утиль для работы с изображениями в JS.
  */
object ImgJsUtil {

  /** Определить ширину и высоту изображения по ссылке.
    *
    * @see [[https://stackoverflow.com/a/626505]]
    * @see [[https://stackoverflow.com/a/623215]]
    *
    * @param imgSrc Любая ссылка на изображение.
    * @return Фьючерс с размерами изображения.
    */
  def getImageWhPx(imgSrc: String): Future[MSize2di] = {
    val imgTag = dom.document.createElement("img").asInstanceOf[Image]
    val p = Promise[MSize2di]()
    imgTag.src = imgSrc
    imgTag.onload = { _: Event =>
      val w = imgTag.naturalWidth
      if ( js.isUndefined(w) || w <= 0 ) {
        p.failure( new IllegalArgumentException(imgSrc) )
      } else {
        val sz = MSize2di(
          width  = w,
          height = imgTag.naturalHeight
        )
        p.success(sz)
      }
    }
    p.future
  }

}
