package io.suggest.qr

/** To isolate react-qrcode component in sc3-sjs,
  * this class is used as render function args container,
  * and as DI marker-type for auto. compile-time injection in template. */
case class QrCodeRenderArgs(
                             data     : String,
                             sizePx   : Int,
                           )
