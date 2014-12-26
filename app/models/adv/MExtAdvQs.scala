package models.adv

import play.api.mvc.QueryStringBindable
import util.PlayLazyMacroLogsImpl
import util.qsb.QsbSigner
import util.secure.SecretGetter
import play.api.Play._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 15:06
 * Description: signed-модель для хранения перевадаемых параметров размещения рекламной карточки
 * на внешних сервисах.
 */

object MExtAdvQs {

  /** Статический секретный ключ для подписывания запросов. */
  private val SIGN_SECRET: String = {
    val sg = new SecretGetter with PlayLazyMacroLogsImpl {
      override val confKey = "ext.adv.qs.sign.key"
      override def useRandomIfMissing = isProd
    }
    sg()
  }

  def AD_ID_SUF             = ".a"
  def BEST_BEFORE_SEC_SUF   = ".b"
  def TARGET_ID_SUF         = ".t"
  def WS_ID_SUF             = ".w"

  /** Разделитель id'шников с поле targetIds. */
  def TARGET_IDS_DELIM = ","

  implicit def qsb(implicit longB: QueryStringBindable[Long], strB: QueryStringBindable[String]) = {
    new QueryStringBindable[MExtAdvQs] {

      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, "sig")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MExtAdvQs]] = {
        for {
          params1           <- getQsbSigner(key).signedOrNone(key, params)
          maybeAdId         <- strB.bind(key + AD_ID_SUF, params1)
          maybeCreatedAt    <- longB.bind(key + BEST_BEFORE_SEC_SUF, params1)
          maybeTargetIdsStr <- strB.bind(key + TARGET_ID_SUF, params1)
          maybeWsId         <- strB.bind(key + WS_ID_SUF, params1)
        } yield {
          for {
            createdAt     <- maybeCreatedAt.right
            targetIdsStr  <- maybeTargetIdsStr.right
            adId          <- maybeAdId.right
            wsId          <- maybeWsId.right
          } yield {
            val targetIds = targetIdsStr.split(TARGET_IDS_DELIM)
              .toList
            MExtAdvQs(
              adId            = adId,
              bestBeforeSec   = createdAt,
              targetIds       = targetIds,
              wsId            = wsId
            )
          }
        }
      }

      override def unbind(key: String, value: MExtAdvQs): String = {
        val targetIdsStr = value.targetIds.mkString(TARGET_IDS_DELIM)
        val unsigned = Seq(
          strB.unbind(key + AD_ID_SUF, value.adId),
          longB.unbind(key + BEST_BEFORE_SEC_SUF, value.bestBeforeSec),
          strB.unbind(key + TARGET_ID_SUF, targetIdsStr),
          strB.unbind(key + WS_ID_SUF, value.wsId)
        )
        .mkString("&")
        getQsbSigner(key).mkSigned(key, unsigned)
      }
    }
  }

}


case class MExtAdvQs(
  adId          : String,
  targetIds     : List[String],
  bestBeforeSec : Long,
  wsId          : String
)
