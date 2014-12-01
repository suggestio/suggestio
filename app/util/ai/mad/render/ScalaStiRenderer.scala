package util.ai.mad.render

import models.{AOStringField, MAd}
import models.ai.ContentHandlerResult
import org.clapper.scalasti.ST
import play.api.libs.concurrent.Execution.Implicits.defaultContext

import scala.concurrent.Future

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.14 19:29
 * Description: Статический рендерер динамических карточек, использующий scala-sti рендерер.
 */
object ScalaStiRenderer extends MadAiRenderedT {

  /**
   * Компиляция текстовых шаблонов в карточке.
   * @param tplAd Исходная карточка.
   * @param args Аргументы рендера.
   * @return Фьючерс с новой карточкой.
   */
  override def renderTplAd(tplAd: MAd, args: Map[String, ContentHandlerResult]): Future[MAd] = {
    Future {
      tplAd.copy(
        offers = tplAd.offers.map { offer =>
          offer.copy(
            text1 = renderTextFieldOpt(offer.text1, args),
            text2 = renderTextFieldOpt(offer.text2, args)
          )
        },
        id = None,
        versionOpt = None,
        alienRsc = true
      )
    }
  }


  /** Рендер одного текстового поля.
    * @param tfOpt Опциональное строковое поле.
    * @param args Аргументы для рендера.
    * @return Новое опциональное строковое поле.
    */
  private def renderTextFieldOpt(tfOpt: Option[AOStringField], args: Map[String, ContentHandlerResult]): Option[AOStringField] = {
    tfOpt.map { tf =>
      val st = ST(tf.value, '#', '#')
        .addAttributes(args, raw = true)
      val v2 = st.render(lineWidth = 256)
      tf.copy(value = v2)
    }
  }

}
