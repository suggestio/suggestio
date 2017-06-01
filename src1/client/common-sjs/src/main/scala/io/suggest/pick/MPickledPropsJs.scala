package io.suggest.pick

import boopickle.Pickler
import io.suggest.bin.ConvCodecs
import io.suggest.sjs.common.bin.Base64JsUtil.SjsBase64JsDecoder

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
object MPickledPropsJs {

  /** Акт опциональной десериализации опциональных пропертисов.
    *
    * @param rawNullUndef Сырые пропертисы, или null, или undefined.
    * @param pickler Поддержка boopickle для класса-результата.
    * @tparam P Тип результата.
    * @return Опциональный результат.
    */
  def applyOpt[P](rawNullUndef: js.UndefOr[js.Any])(implicit pickler: Pickler[P]): Option[P] = {
    for {
      propsOrNull <- rawNullUndef.toOption
      props0      <- Option( propsOrNull )

      base64      <- props0.asInstanceOf[MPickledPropsJs]
        .pickled
        .toOption

      if base64.nonEmpty
    } yield {
      PickleUtil.unpickleConv[String, ConvCodecs.Base64, P](base64)
    }
  }

}


@js.native
trait MPickledPropsJs extends js.Object {

  /** Сериализованные данные. */
  @JSName( PickleUtil.PICKED_FN )
  val pickled: UndefOr[String] = js.native

}