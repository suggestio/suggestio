package models.adv.js.ctx

import _root_.util.{TplDataFormatUtil, FormUtil}
import models.{MAdnNode, MAdT}
import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.Play.{current, configuration}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 22.01.15 11:30
 * Description: 
 */

object MAdContentCtx {

  val FIELDS_FN       = "fields"
  val TITLE_FN        = "title"
  val DESCR_FN        = "descr"

  /** Желаемая длина дескрипшена, отправляемого в контекст. */
  val FROM_AD_DESCR_LEN = configuration.getInt("adv.ext.ad.content.descr.len") getOrElse 192

  /** Делаем из инстанса рекламной карточки класс, пригодный для js-контекста.
    * @param mad Рекламная карточка.
    * @param producer Узел-продьюсер. Нужен для генерации title карточки.
    * @return Экземпляр MAdContentCtx.
    */
  def fromAd(mad: MAdT, producer: MAdnNode): MAdContentCtx = {
    MAdContentCtx(
      // Поля карточки -- это именно поля.
      fields = mad.offers
        .iterator
        .flatMap { offer => offer.text1.iterator }
        .map { field => MAdContentField(
          text = FormUtil.strTrimSanitizeF(field.value)
        )}
        .toSeq,
      // Заголовок карточки -- это заголовок узла.
      title = {
        val sb = new StringBuilder(128, producer.meta.name)
        if (producer.meta.town.isDefined) {
          sb.append(" / ")
            .append(producer.meta.town.get)
        }
        Some(sb.toString())
      },
      // Дескрипшен карточки мы берём из начала long-дескрипшена карточки.
      descr = mad.richDescrOpt.map { rd =>
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
case class MAdContentCtx(fields: Seq[MAdContentField], title: Option[String], descr: Option[String])



object MAdContentField {
  val TEXT_FN = "text"

  implicit def reads: Reads[MAdContentField] = {
    (__ \ TEXT_FN)
      .read[String]
      .map(apply)
  }

  implicit def writes: Writes[MAdContentField] = {
    (__ \ TEXT_FN)
      .write[String]
      .contramap(_.text)
  }

}

case class MAdContentField(text: String)

