package util.ai.mad.render

import javax.inject.Inject

import io.suggest.ad.blk.ent.TextEnt
import io.suggest.model.n2.ad.EntMap_t
import models.ai.ContentHandlerResult
import models.MNode
import org.clapper.scalasti.ST

import scala.concurrent.{ExecutionContext, Future}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 01.12.14 19:29
 * Description: Статический рендерер динамических карточек, использующий scala-sti рендерер.
 */
class ScalaStiRenderer @Inject() (
                                  implicit private val ec: ExecutionContext
                                 )
  extends MadAiRenderedT
{

  /**
   * Компиляция текстовых шаблонов в карточке.
   * @param tplAd Шаблонная карточка.
   * @param args Аргументы рендера.
   * @param targetAd Обновляемая карточка.
   * @return Фьючерс с обновлённой карточкой.
   */
  override def renderTplAd(tplAd: MNode, args: Map[String, ContentHandlerResult], targetAd: MNode): Future[MNode] = {
    Future {
      // Отрендерить офферы из шаблонной карточки.
      // В качестве исходных значений попытаться задействовать поля исходной карточки.
      val renderedOffers: EntMap_t = {
        tplAd
          .ad.entities
          .mapValues { tplOffer =>
            val srcOffer = targetAd.ad.entities
              .getOrElse(tplOffer.id, tplOffer)
            srcOffer.copy(
              text = renderTextFieldOpt(tplOffer.text, args, srcOffer.text)
            )
          }
      }
      // Накатить отрендеренные офферы на офферы целевой рекламной карточки
      targetAd.copy(
        ad = targetAd.ad.copy(
          entities = targetAd.ad.entities ++ renderedOffers
        )
      )
    }
  }


  /** Рендер одного текстового поля.
    * @param tfOpt Опциональное строковое поле.
    * @param args Аргументы для рендера.
    * @return Новое опциональное строковое поле.
    */
  private def renderTextFieldOpt(tfOpt: Option[TextEnt], args: Map[String, ContentHandlerResult], tgFieldOpt: Option[TextEnt]): Option[TextEnt] = {
    tfOpt.fold(tgFieldOpt) { tf =>
      val st = ST(tf.value, '#', '#')
        .addAttributes(args, raw = true)
      val v2 = st.render(lineWidth = 256)
      val f = tgFieldOpt.getOrElse(tf)
      Some( f.copy(value = v2) )
    }
  }

}
