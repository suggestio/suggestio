package models.blk

import io.suggest.img.{MImgFormat, MImgFormats}
import io.suggest.sec.QsbSigner
import io.suggest.sec.m.SecretKeyInit
import io.suggest.xplay.qsb.AbstractQueryStringBindable
import play.api.mvc.QueryStringBindable
import io.suggest.url.bind.QueryStringBindableUtil._

import scala.language.implicitConversions   // конверсий тут по факту нет.

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 25.12.14 16:58
 * Description: Модель qs-аргументов для ссылки запроса рендера одной конкретной карточки.
 */

object OneAdQsArgs extends SecretKeyInit {

  override def CONF_KEY = "only.one.ad.qs.sign.key"
  private var SIGN_SECRET: String = _
  override def setSignSecret(secretKey: String): Unit = {
    SIGN_SECRET = secretKey
  }

  // Суффиксы названий qs-полей.
  def AD_ID_FN     = "a"
  def SZ_MULT_FN   = "m"
  def VSN_FN       = "v"
  def IMG_FMT_FN   = "f"
  def WIDE_FN      = "w"

  /** routes qsb для сериализации/десериализации экземпляра [[OneAdQsArgs]]. */
  implicit def onAdQsArgsQsb(implicit
                             strB     : QueryStringBindable[String],
                             floatB   : QueryStringBindable[SzMult_t],
                             longOptB : QueryStringBindable[Option[Long]],
                             wideOptB : QueryStringBindable[Option[OneAdWideQsArgs]],
                             // compat: Формат в qs опционален, т.к. его не было вообще до 13 марта 2015, а ссылки на картинки уже были в фейсбуке (в тестовых акк-ах).
                             // Потом когда-нибудь наверное можно будет убрать option, окончательно закрепив обязательность формата.
                             // Есть также случаи, когда это обязательное поле не нужно (см. scaladoc для класса-компаньона).
                             imgFmtB  : QueryStringBindable[Option[MImgFormat]]
                            ) : QueryStringBindable[OneAdQsArgs] = {
    new AbstractQueryStringBindable[OneAdQsArgs] {

      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, "sig")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, OneAdQsArgs]] = {
        val k = key1F(key)
        for {
          params1         <- getQsbSigner(key)
            .signedOrNone(k(""), params)
          maybeAdId       <- strB.bind    (k(AD_ID_FN),   params1)
          maybeSzMult     <- floatB.bind  (k(SZ_MULT_FN), params1)
          maybeVsnOpt     <- longOptB.bind(k(VSN_FN),     params1)
          maybeImgFmtOpt  <- imgFmtB.bind (k(IMG_FMT_FN), params1)
          maybeWideOpt    <- wideOptB.bind(k(WIDE_FN),    params1)
        } yield {
          for {
            adId        <- maybeAdId
            szMult      <- maybeSzMult
            vsnOpt      <- maybeVsnOpt
            imgFmtOpt   <- maybeImgFmtOpt
            wideOpt     <- maybeWideOpt
          } yield {
            val imgFmt = imgFmtOpt getOrElse MImgFormats.default
            OneAdQsArgs(
              adId    = adId,
              szMult  = szMult,
              vsnOpt  = vsnOpt,
              imgFmt  = imgFmt,
              wideOpt = wideOpt
            )
          }
        }
      }

      override def unbind(key: String, value: OneAdQsArgs): String = {
        val k = key1F(key)
        val qs = _mergeUnbinded1(
          strB.unbind     (k(AD_ID_FN),   value.adId),
          floatB.unbind   (k(SZ_MULT_FN), value.szMult),
          longOptB.unbind (k(VSN_FN),     value.vsnOpt),
          imgFmtB.unbind  (k(IMG_FMT_FN), Some(value.imgFmt)),
          wideOptB.unbind (k(WIDE_FN),    value.wideOpt)
        )
        getQsbSigner(key)
          .mkSigned(key, qs)
      }
    }
  }

}


/**
 * Параметры запроса рендера только одной карточки.
 * @param adId id запрашиваемой рекламной карточки.
 * @param szMult Мультипликатор размера карточки.
 * @param vsnOpt Версия рекламной карточки.
 *               Используется для подавления кеширования на клиентах при изменении карточки.
 * @param imgFmt 2015.mar.13 Формат выходной картинки. Он не используется, если рендер карточки в картинку не требуется.
 *               Но всё же обязателен для упрощения кода в других местах. Потом, может быть, модели аргументов для
 *               html и img рендера будут разделены, для избежания подобных странностей API.
 * @param wideOpt 2015.mar.05 Контейнер для задания парамеров широкого рендера.
 */
case class OneAdQsArgs(
                        adId    : String,
                        szMult  : SzMult_t,
                        vsnOpt  : Option[Long],
                        imgFmt  : MImgFormat,
                        wideOpt : Option[OneAdWideQsArgs] = None
                      )

