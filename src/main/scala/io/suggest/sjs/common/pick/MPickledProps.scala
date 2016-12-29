package io.suggest.sjs.common.pick

import boopickle.Pickler
import io.suggest.bin.ConvCodecs
import io.suggest.pick.PickleUtil
import io.suggest.sjs.common.bin.EvoBase64JsUtil.EvoBase64JsDecoder

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.scalajs.js.annotation.JSName

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 28.12.16 16:53
  * Description: JSON-фасад модели как-то сериализованных JSON-пропертисов в виде JSON object.
  * Такие пропертисы содержат лишь одно поле, поле с сериализованных данных.
  */
object MPickledProps {

  /** Акт опциональной десериализации опциональных пропертисов. */
  def applyOpt[P](raw: js.UndefOr[js.Any])(implicit pickler: Pickler[P]): Option[P] = {
    for {
      props0 <- raw.toOption
      propsPick = props0.asInstanceOf[MPickledProps]
      base64 <- propsPick.pickled.toOption
      if base64.nonEmpty
    } yield {
      PickleUtil.unpickleConv[String, ConvCodecs.Base64, P](base64)
    }
  }

}


@js.native
trait MPickledProps extends js.Object {

  /** Сериализованные данные. */
  @JSName( PickleUtil.PICKED_FN )
  val pickled: UndefOr[String] = js.native

}
