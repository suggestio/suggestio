package models.adv

import io.suggest.sec.QsbSigner
import io.suggest.sec.m.SecretKeyInit
import io.suggest.xplay.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 26.12.14 15:06
 * Description: signed-модель для хранения перевадаемых параметров размещения рекламной карточки
 * на внешних сервисах.
 */

object MExtAdvQs extends SecretKeyInit {

  private var SIGN_SECRET: String = _

  override def setSignSecret(secretKey: String): Unit = {
    SIGN_SECRET = secretKey
  }

  override def CONF_KEY = "ext.adv.qs.sign.key"

  def AD_ID_FN             = "a"
  def BEST_BEFORE_SEC_FN   = "b"
  def TARGET_ID_FN         = "t"
  def WS_ID_FN             = "w"

  /** Разделитель id'шников с поле targetIds. */
  def TARGET_IDS_DELIM = ","

  implicit def mExtAdvQsQsb(implicit
                            longB   : QueryStringBindable[Long],
                            strB    : QueryStringBindable[String],
                            infosB  : QueryStringBindable[List[MExtTargetInfo]]
                           ): QueryStringBindable[MExtAdvQs] = {
    new QueryStringBindableImpl[MExtAdvQs] {

      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, "sig")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, MExtAdvQs]] = {
        val k = key1F(key)
        for {
          params1             <- getQsbSigner(key)
            .signedOrNone(k(""), params)
          maybeAdId           <- strB.bind   (k(AD_ID_FN),            params1)
          maybeCreatedAt      <- longB.bind  (k(BEST_BEFORE_SEC_FN),  params1)
          maybeTargetInfos    <- infosB.bind (k(TARGET_ID_FN),        params1)
          maybeWsId           <- strB.bind   (k(WS_ID_FN),            params1)
        } yield {
          for {
            createdAt         <- maybeCreatedAt
            targetInfos       <- maybeTargetInfos
            adId              <- maybeAdId
            wsId              <- maybeWsId
          } yield {
            MExtAdvQs(
              adId            = adId,
              bestBeforeSec   = createdAt,
              targets         = targetInfos,
              wsId            = wsId
            )
          }
        }
      }

      override def unbind(key: String, value: MExtAdvQs): String = {
        val k = key1F(key)
        val unsigned = Seq(
          strB  .unbind(k(AD_ID_FN),            value.adId),
          longB .unbind(k(BEST_BEFORE_SEC_FN),  value.bestBeforeSec),
          infosB.unbind(k(TARGET_ID_FN),        value.targets),
          strB  .unbind(k(WS_ID_FN),            value.wsId)
        )
          .mkString("&")
        getQsbSigner(key)
          .mkSigned(key, unsigned)
      }
    }
  }

}


case class MExtAdvQs(
  adId          : String,
  targets       : List[MExtTargetInfo],
  bestBeforeSec : Long,
  wsId          : String
)

