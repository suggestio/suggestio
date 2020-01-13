package models.adv.js.ctx

import _root_.util.{FormUtil, TplDataFormatUtil}
import io.suggest.adv.ext.model.ctx.MAdContentCtx._
import io.suggest.n2.node.MNode
import play.api.libs.functional.syntax._
import play.api.libs.json._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 11:30
 * Description: Представление данных одной рекламной карточки для SioPR.
 */

object MAdContentCtx {

  /** Желаемая длина дескрипшена, отправляемого в контекст. */
  def FROM_AD_DESCR_LEN = 192

  /** Делаем из инстанса рекламной карточки класс, пригодный для js-контекста.
    * @param mad Рекламная карточка.
    * @param producer Узел-продьюсер. Нужен для генерации title карточки.
    * @return Экземпляр MAdContentCtx.
    */
  def fromAd(mad: MNode, producer: MNode): MAdContentCtx = {
    MAdContentCtx(
      // Поля карточки -- это именно поля.
      fields = mad.ad.entities
        .valuesIterator
        .flatMap { offer => offer.text }
        .map { field => MAdContentField(
          text = FormUtil.strTrimSanitizeF(field.value)
        )}
        .toSeq,
      // Заголовок карточки -- это заголовок узла.
      title = {
        val sb = new StringBuilder(128, producer.meta.basic.name)
        for (town <- producer.meta.address.town) {
          sb.append(" | ")
            .append(town)
        }
        Some(sb.toString())
      },
      // Дескрипшен карточки мы берём из начала long-дескрипшена карточки.
      descr = mad.ad.richDescr.map { rd =>
        // Выкинуть html-теги и пустоты по бокам.
        val allPlain = FormUtil.strTrimSanitizeF(rd.text)
        // Укоротить текст до скольки-то символов. Нужно найти точку срезания, чтобы не обрывать слово.
        TplDataFormatUtil.strLimitLenNoTrailingWordPart(allPlain, FROM_AD_DESCR_LEN)
      }
    )
  }

  /** mapping */
  implicit def reads: Reads[MAdContentCtx] = (
    (__ \ FIELDS_FN).readNullable[Seq[MAdContentField]].map(_ getOrElse Seq.empty) and
    (__ \ TITLE_FN).readNullable[String] and
    (__ \ DESCR_FN).readNullable[String]
  )(apply _)

  /** unmapping */
  implicit def writes: Writes[MAdContentCtx] = (
    (__ \ FIELDS_FN).write[Seq[MAdContentField]] and
    (__ \ TITLE_FN).writeNullable[String] and
    (__ \ DESCR_FN).writeNullable[String]
  )(unlift(unapply))

}

/** Сырой контент отной рекламной карточки. */
case class MAdContentCtx(
  fields  : Seq[MAdContentField],
  title   : Option[String],
  descr   : Option[String]
)
