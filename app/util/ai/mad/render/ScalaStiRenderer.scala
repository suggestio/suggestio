package util.ai.mad.render

import io.suggest.ym.model.ad.AOBlock
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
   * @param tplAd Шаблонная карточка.
   * @param args Аргументы рендера.
   * @param targetAd Обновляемая карточка.
   * @return Фьючерс с обновлённой карточкой.
   */
  override def renderTplAd(tplAd: MAd, args: Map[String, ContentHandlerResult], targetAd: MAd): Future[MAd] = {
    Future {
      val tgOffersMap = targetAd
        .offers
        .iterator
        .map(offer2tuple)
        .toMap
      // Отрендерить офферы из шаблонной карточки. В качестве исходных значений попытаться задействовать поля исходной карточки.
      val renderedOffers = tplAd.offers
        .map { tplOffer =>
          val srcOffer = tgOffersMap.getOrElse(tplOffer.n, tplOffer)
          val off2 = srcOffer.copy(
            text1 = renderTextFieldOpt(tplOffer.text1, args, srcOffer.text1),
            text2 = renderTextFieldOpt(tplOffer.text2, args, srcOffer.text2)
          )
          offer2tuple(off2)
        }
        .toMap
      // Накатить отрендеренные офферы на офферы целевой рекламной карточки
      targetAd.copy(
        offers = tgOffersMap
          .++(renderedOffers)
          .valuesIterator
          .toList
      )
    }
  }

  private def offer2tuple(off: AOBlock) = off.n -> off

  /** Рендер одного текстового поля.
    * @param tfOpt Опциональное строковое поле.
    * @param args Аргументы для рендера.
    * @return Новое опциональное строковое поле.
    */
  private def renderTextFieldOpt(tfOpt: Option[AOStringField], args: Map[String, ContentHandlerResult], tgFieldOpt: Option[AOStringField]): Option[AOStringField] = {
    tfOpt.fold(tgFieldOpt) { tf =>
      val st = ST(tf.value, '#', '#')
        .addAttributes(args, raw = true)
      val v2 = st.render(lineWidth = 256)
      val f = tgFieldOpt.getOrElse(tf)
      Some( f.copy(value = v2) )
    }
  }

}
