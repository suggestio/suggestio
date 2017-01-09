package io.suggest.lk.ad.form.model

import io.suggest.sjs.common.model.MaybeFromJsonT

import scala.scalajs.js.{WrappedDictionary, Dictionary, Any, Array}
import io.suggest.ad.form.AdFormConstants.{WS_MSG_DATA_FN, WS_MSG_TYPE_FN, TYPE_COLOR_PALETTE}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.08.15 18:08
 * Description: Модель ws-ответов сервера по поводу цветов картинки.
 *
 * Ответы приходят такие:
 * {"type":"colorPalette", "data":["FCC2DE","2DDCE8","FEDFEB","35C0CE"]}
 */
object MColorPalette extends MaybeFromJsonT {

  override type T = MColorPalette

  override def maybeFromJson(raw: Any): Option[T] = {
    val d: WrappedDictionary[Any] = raw.asInstanceOf[Dictionary[Any]]
    for {
      typRaw <- d.get(WS_MSG_TYPE_FN) if typRaw.asInstanceOf[String] == TYPE_COLOR_PALETTE
      data   <- d.get(WS_MSG_DATA_FN)
    } yield {
      val colors = data.asInstanceOf[Array[String]]
      MColorPalette(colors)
    }
  }

}

case class MColorPalette(
  colors: Seq[String]
)
