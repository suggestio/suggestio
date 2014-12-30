package models.blk

import play.api.Play._
import play.api.mvc.QueryStringBindable
import util.PlayLazyMacroLogsImpl
import util.qsb.QsbSigner
import util.secure.SecretGetter

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.14 16:58
 * Description: Модель qs-аргументов для ссылки запроса рендера одной конкретной карточки.
 */

object OneAdQsArgs {

  /** Статический секретный ключ для подписывания запросов. */
  private[models] val SIGN_SECRET: String = {
    val sg = new SecretGetter with PlayLazyMacroLogsImpl {
      override val confKey = "only.one.ad.qs.sign.key"
      override def useRandomIfMissing = isProd
    }
    sg()
  }

  // Суффиксы названий qs-полей.
  def AD_ID_SUF     = ".a"
  def SZ_MULT_SUF   = ".m"


  /** routes qsb для сериализации/десериализации экземпляра [[OneAdQsArgs]]. */
  implicit def qsb(implicit strB: QueryStringBindable[String], floatB: QueryStringBindable[Float]) = {
    new QueryStringBindable[OneAdQsArgs] {

      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, "sig")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, OneAdQsArgs]] = {
        val keyDotted = if (!key.isEmpty) s"$key." else key
        for {
          params1       <- getQsbSigner(key).signedOrNone(keyDotted, params)
          maybeImgId    <- strB.bind(key + AD_ID_SUF, params1)
          maybeSzMult   <- floatB.bind(key + SZ_MULT_SUF, params1)
        } yield {
          maybeImgId.right.flatMap { adId =>
            maybeSzMult.right.map { szMult =>
              new OneAdQsArgs(adId, szMult)
            }
          }
        }
      }

      override def unbind(key: String, value: OneAdQsArgs): String = {
        val qss = List(
          strB.unbind(key + AD_ID_SUF, value.adId),
          floatB.unbind(key + SZ_MULT_SUF, value.szMult)
        )
        val qs = qss.mkString("&")
        getQsbSigner(key).mkSigned(key, qs)
      }
    }
  }

}


/**
 * Параметры запроса рендера только одной карточки.
 * @param adId id запрашиваемой рекламной карточки.
 * @param szMult Мультипликатор размера карточки.
 */
case class OneAdQsArgs(
  adId    : String,
  szMult  : SzMult_t
)

