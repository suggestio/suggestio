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

  val empty = SiteQsArgs()

  /** query-string-биндер модели. */
  implicit def qsb(implicit strOptB: QueryStringBindable[Option[String]]): QueryStringBindable[SiteQsArgs] = {
    new QueryStringBindable[SiteQsArgs] {
      /** Маппер из qs. */
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, SiteQsArgs]] = {
        for {
          maybeAdnIdOpt     <- strOptB.bind(s"$key.$ADN_ID_SUF", params)
          maybePovAdIdOpt   <- strOptB.bind(s"$key.$POV_AD_ID_SUF", params)
        } yield {
          for {
            adnIdOpt    <- maybeAdnIdOpt.right
            povAdIdOpt  <- maybePovAdIdOpt.right
          } yield {
            // Нанооптимизация по RAM: если все аргументы пусты, то вернуть инстанс empty, а не создавать новый.
            if (adnIdOpt.nonEmpty || povAdIdOpt.nonEmpty) {
              SiteQsArgs(
                adnId = adnIdOpt,
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
          strOptB.unbind(s"$key.$ADN_ID_SUF", value.adnId),
          strOptB.unbind(s"$key.$POV_AD_ID_SUF", value.povAdId)
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
  adnId   : Option[String] = None,
  povAdId : Option[String] = None
)
