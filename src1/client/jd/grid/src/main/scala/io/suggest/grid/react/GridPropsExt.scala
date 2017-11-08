package io.suggest.grid.react

import com.github.dantrain.react.stonecutter.PropsCommon
import io.suggest.dev.MSzMult

import scala.scalajs.js
import scala.language.implicitConversions

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.11.17 16:08
  * Description: Расширенные пропертисы для [[com.github.dantrain.react.stonecutter.PropsCommon]].
  */
trait GridPropsExt extends js.Object {

  /** Инфа о фактическом мультипликаторе размеров. */
  val szMult: MSzMult

}


object GridPropsExt {

  implicit def fromPropsCommon(propsCommon: PropsCommon): GridPropsExt = {
    propsCommon.asInstanceOf[GridPropsExt]
  }

}
