package models.msc

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
    new QueryStringBindable[SiteQsArgs] {
      
      private def key1(key: String, fn: String): String = {
        key + "." + fn
      }
      
      /** Маппер из qs. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SiteQsArgs]] = {
        for {
          maybeAdnIdOpt     <- strOptB.bind(key1(key, ADN_ID_SUF), params)
          maybePovAdIdOpt   <- strOptB.bind(key1(key, POV_AD_ID_SUF), params)
          maybeApiVsn       <- apiVsnB.bind(key1(key, VSN_FN), params)
        } yield {
          for {
            adnIdOpt    <- maybeAdnIdOpt.right
            povAdIdOpt  <- maybePovAdIdOpt.right
            apiVsn      <- maybeApiVsn.right
          } yield {
            // Нанооптимизация по RAM: если все аргументы пусты, то вернуть инстанс empty, а не создавать новый.
            if (adnIdOpt.nonEmpty || povAdIdOpt.nonEmpty) {
              SiteQsArgs(
                apiVsn  = apiVsn,
                adnId   = adnIdOpt,
                povAdId = povAdIdOpt
              )
            } else {
              empty
            }
          }
        }
      }

      /** Сериализатор. */
      override def unbind(key: String, value: SiteQsArgs): String = {
        Iterator(
          strOptB.unbind(key1(key, ADN_ID_SUF), value.adnId),
          strOptB.unbind(key1(key, POV_AD_ID_SUF), value.povAdId),
          apiVsnB.unbind(key1(key, VSN_FN), value.apiVsn)
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
