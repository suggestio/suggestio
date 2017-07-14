package models.msc

import io.suggest.model.play.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable

import scala.language.implicitConversions

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 16:33
 * Description: Если какие-то аргументы нужно передавать прямо в sc/site (до фазы showcase),
 * то необходимо делать это через данную модель.
 */

object SiteQsArgs {

  def ADN_ID_FN               = "a"
  def POV_AD_ID_FN            = "b"
  def VSN_FN                  = "v"

  val empty = SiteQsArgs()

  /** query-string-биндер модели. */
  implicit def siteQsArgsQsb(implicit
                             strOptB: QueryStringBindable[Option[String]],
                             apiVsnB: QueryStringBindable[MScApiVsn]
                            ): QueryStringBindable[SiteQsArgs] = {
    new QueryStringBindableImpl[SiteQsArgs] {
      /** Маппер из qs. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SiteQsArgs]] = {
        val f = key1F(key)
        for {
          maybeAdnIdOpt     <- strOptB.bind(f(ADN_ID_FN),    params)
          maybePovAdIdOpt   <- strOptB.bind(f(POV_AD_ID_FN), params)
          maybeApiVsn       <- apiVsnB.bind(f(VSN_FN),       params)
        } yield {
          for {
            adnIdOpt    <- maybeAdnIdOpt.right
            povAdIdOpt  <- maybePovAdIdOpt.right
            apiVsn      <- maybeApiVsn.right
          } yield {
            SiteQsArgs(
              apiVsn  = apiVsn,
              adnId   = adnIdOpt,
              povAdId = povAdIdOpt
            )
          }
        }
      }

      /** Сериализатор. */
      override def unbind(key: String, value: SiteQsArgs): String = {
        _mergeUnbinded {
          val f = key1F(key)
          Iterator(
            strOptB.unbind(f(ADN_ID_FN),     value.adnId),
            strOptB.unbind(f(POV_AD_ID_FN),  value.povAdId),
            apiVsnB.unbind(f(VSN_FN),        value.apiVsn)
          )
        }
      }
    }
  }

}


/**
  * Экземпляр модели.
  * @param adnId опциональный id исходного узла, с которого начинается сайт.
  *              Бывает, что используется в SyncSite, а так же немного при рендере siteTpl.
  * @param povAdId Point-of-view id рекламной карточки, с точки зрения которой идёт рендер сайта.
  *                Используется для рендера twitter-meta-тегов и прочего.
  */
case class SiteQsArgs(
  apiVsn  : MScApiVsn      = MScApiVsns.unknownVsn,
  // TODO Кажется, что adnId параметр не очень-то используется для записи в него. Только на чтение.
  adnId   : Option[String] = None,
  povAdId : Option[String] = None
)
