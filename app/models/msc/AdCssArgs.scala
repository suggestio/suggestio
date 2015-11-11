package models.msc

import models.blk.SzMult_t
import play.api.mvc.QueryStringBindable
import util.PlayMacroLogsDyn
import util.qsb.QsbSigner
import util.secure.SecretGetter

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 11.11.15 11:52
 * Description: Модель аргументов для рендера ссылки на css для одной рекламной карточки.
 */

object AdCssArgs {

  val SEP_RE = ",".r

  /** Десериализация из строки. */
  def fromString(s: String) = {
    val Array(adId, szMultStr) = SEP_RE.split(s)
    AdCssArgs(adId, szMultStr.toFloat)
  }

  /** Ключ для подписи ссылок. */
  private val SIGN_SECRET: String = {
    val sg = new SecretGetter with PlayMacroLogsDyn {
      import play.api.Play.{current, isProd}
      override val confKey = "ads.css.url.sign.key"
      override def useRandomIfMissing = isProd
    }
    sg()
  }

  /** Подписываемый QSB для списка AdCssArgs. */
  implicit def qsbSeq: QueryStringBindable[Seq[AdCssArgs]] = {
    new QueryStringBindable[Seq[AdCssArgs]] {

      private def getSigner = new QsbSigner(SIGN_SECRET, "sig")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, Seq[AdCssArgs]]] = {
        getSigner.signedOrNone(key, params)
          .flatMap(_.get(key))
          .map { vs =>
            try {
              val parsed = vs.map { v =>
                fromString(v)
              }
              Right(parsed)
            } catch {
              case ex: Exception =>
                Left(ex.getMessage)
            }
          }
      }

      override def unbind(key: String, value: Seq[AdCssArgs]): String = {
        val sb = new StringBuilder(30 * value.size)
        value.foreach { aca =>
          sb.append(key).append('=')
            .append(aca.adId).append(AdCssArgs.SEP_RE).append(aca.szMult)
            .append('&')
        }
        // Убрать финальный & из ссылки
        if (value.nonEmpty)
          sb.setLength(sb.length - 1)
        // Вернуть подписанный результат
        val res = sb.toString()
        getSigner.mkSigned(key, res)
      }
    }
  }

}


/**
 * Для URL css'ки, относящейся к конкретной карточке, нужно передать эти параметры:
 * @param adId id рекламной карточки.
 * @param szMult Мульпликатор размера.
 */
case class AdCssArgs(
  adId    : String,
  szMult  : SzMult_t
) {

  override def toString: String = {
    // TODO Нужно укорачивать szMult: 1.0 -> 1
    adId + AdCssArgs.SEP_RE + szMult
  }

}
