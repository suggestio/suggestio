package models.blk

import models.im.{OutImgFmts, OutImgFmt}
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
  private[this] val SIGN_SECRET: String = {
    val sg = new SecretGetter with PlayLazyMacroLogsImpl {
      override val confKey = "only.one.ad.qs.sign.key"
      override def useRandomIfMissing = isProd
    }
    sg()
  }

  // Суффиксы названий qs-полей.
  def AD_ID_SUF     = ".a"
  def SZ_MULT_SUF   = ".m"
  def VSN_SUF       = ".v"
  def IMG_FMT_SUF   = ".f"
  def WIDE_SUF      = ".w"

  /** routes qsb для сериализации/десериализации экземпляра [[OneAdQsArgs]]. */
  implicit def qsb(implicit strB: QueryStringBindable[String],
                   floatB   : QueryStringBindable[SzMult_t],
                   longOptB : QueryStringBindable[Option[Long]],
                   wideOptB : QueryStringBindable[Option[OneAdWideQsArgs]],
                   // compat: Формат в qs опционален, т.к. его не было вообще до 13 марта 2015, а ссылки на картинки уже были в фейсбуке (в тестовых акк-ах).
                   // Потом когда-нибудь наверное можно будет убрать option, окончательно закрепив обязательность формата.
                   // Есть также случаи, когда это обязательное поле не нужно (см. scaladoc для класса-компаньона).
                   imgFmtB  : QueryStringBindable[Option[OutImgFmt]]) : QueryStringBindable[OneAdQsArgs] = {
    new QueryStringBindable[OneAdQsArgs] {

      def getQsbSigner(key: String) = new QsbSigner(SIGN_SECRET, "sig")

      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, OneAdQsArgs]] = {
        val keyDotted = if (!key.isEmpty) s"$key." else key
        for {
          params1         <- getQsbSigner(key).signedOrNone(keyDotted, params)
          maybeAdId       <- strB.bind(key + AD_ID_SUF, params1)
          maybeSzMult     <- floatB.bind(key + SZ_MULT_SUF, params1)
          maybeVsnOpt     <- longOptB.bind(key + VSN_SUF, params1)
          maybeImgFmtOpt  <- imgFmtB.bind(key + IMG_FMT_SUF, params1)
          maybeWideOpt    <- wideOptB.bind(key + WIDE_SUF, params1)
        } yield {
          for {
            adId        <- maybeAdId.right
            szMult      <- maybeSzMult.right
            vsnOpt      <- maybeVsnOpt.right
            imgFmtOpt   <- maybeImgFmtOpt.right
            wideOpt     <- maybeWideOpt.right
          } yield {
            val imgFmt = imgFmtOpt getOrElse OutImgFmts.JPEG
            OneAdQsArgs(adId, szMult, vsnOpt, imgFmt, wideOpt)
          }
        }
      }

      override def unbind(key: String, value: OneAdQsArgs): String = {
        val qss = List(
          strB.unbind(key + AD_ID_SUF,      value.adId),
          floatB.unbind(key + SZ_MULT_SUF,  value.szMult),
          longOptB.unbind(key + VSN_SUF,    value.vsnOpt),
          imgFmtB.unbind(key + IMG_FMT_SUF, Some(value.imgFmt)),
          wideOptB.unbind(key + WIDE_SUF,   value.wideOpt)
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
  imgFmt  : OutImgFmt,
  wideOpt : Option[OneAdWideQsArgs] = None
)



/** Статическая модель для описания данных для широкого рендера. */
object OneAdWideQsArgs {

  def WIDTH_SUF = ".w"

  /** Поддержка биндинга в routes и в qsb. */
  implicit def qsb(implicit intB: QueryStringBindable[Int]): QueryStringBindable[OneAdWideQsArgs] = {
    new QueryStringBindable[OneAdWideQsArgs] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, OneAdWideQsArgs]] = {
        // Оформлено многословно через for{}, т.к. в будущем очень возможно расширения списка аргументов.
        for {
          maybeWidth <- intB.bind(key + WIDTH_SUF, params)
        } yield {
          for {
            width <- maybeWidth.right
          } yield {
            OneAdWideQsArgs(
              width = width
            )
          }
        }
      }

      override def unbind(key: String, value: OneAdWideQsArgs): String = {
        intB.unbind(key + WIDTH_SUF, value.width)
      }
    }
  }
}

/**
 * В случае ссылки wide-карточку, необходима дополнительная инфа.
 * Высота высчитывается через szMult и исходную высоту.
 * @param width Ширина "экрана" и результата.
 */
case class OneAdWideQsArgs(
  width: Int
)
