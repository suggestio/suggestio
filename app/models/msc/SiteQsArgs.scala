package models.msc

import io.suggest.model.play.qsb.QsbKey1T
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

  val ADN_ID_SUF              = "a"
  val POV_AD_ID_SUF           = "b"
  val VSN_FN                  = "v"

  val empty = SiteQsArgs()

  /** query-string-биндер модели. */
  implicit def qsb(implicit
                   strOptB: QueryStringBindable[Option[String]],
                   apiVsnB: QueryStringBindable[MScApiVsn]
                  ): QueryStringBindable[SiteQsArgs] = {
    new QueryStringBindable[SiteQsArgs] with QsbKey1T {
      /** Маппер из qs. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SiteQsArgs]] = {
        val f = key1F(key)
        for {
          maybeAdnIdOpt     <- strOptB.bind(f(ADN_ID_SUF),    params)
          maybePovAdIdOpt   <- strOptB.bind(f(POV_AD_ID_SUF), params)
          maybeApiVsn       <- apiVsnB.bind(f(VSN_FN),        params)
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
        val f = key1F(key)
        Iterator(
          strOptB.unbind(f(ADN_ID_SUF),     value.adnId),
          strOptB.unbind(f(POV_AD_ID_SUF),  value.povAdId),
          apiVsnB.unbind(f(VSN_FN),         value.apiVsn)
        )
          .filter { !_.isEmpty }
          .mkString("&")
      }
    }
  }

}


/**
 * Экземпляр модели.
 * @param adnId id исходного узла, с которого начинается сайт.
 * @param povAdId Point-of-view id рекламной карточки, с точки зрения которой идёт рендер сайта.
 *                Используется для рендера twitter-meta-тегов и прочего.
 */
case class SiteQsArgs(
  apiVsn  : MScApiVsn      = MScApiVsns.unknownVsn,
  adnId   : Option[String] = None,
  povAdId : Option[String] = None
)
