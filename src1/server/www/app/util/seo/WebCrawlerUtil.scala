package util.seo

import io.suggest.util.logs.MacroLogsImpl
import play.api.mvc.QueryStringBindable
import play.core.parsers.FormUrlEncodedParser

import javax.inject.Inject

/** Utility functions to interact with web-crawlers. */
final class WebCrawlerUtil @Inject()(
                                    )
  extends MacroLogsImpl
{

  /** Parse AJAX escaped fragment from request queryString.
    *
    * @param queryString URL QueryString map.
    * @tparam T Type to parse into.
    * @return Parsed and mapped optional result.
    */
  def ajaxJsScState[T: QueryStringBindable]( queryString: Map[String, Seq[String]] ): Option[T] = {
    for {
      escapedFragments <- queryString.get("_escaped_fragment_")
      escapedFragment <- escapedFragments.headOption
      escFragQsMap <- try {
        val r = FormUrlEncodedParser.parseNotPreservingOrder( escapedFragment )
        Some(r)
      } catch {
        case ex: Exception =>
          LOGGER.debug("Failed to parse ajax-escaped fragment.", ex)
          None
      }
      resultE <- implicitly[QueryStringBindable[T]]
        .bind( "", escFragQsMap )
      result <- {
        for (errorMsg <- resultE.left)
          LOGGER.warn(s"_geoSiteResult(): Failed to bind ajax escaped_fragment '$escapedFragment' => ERROR: $errorMsg")

        resultE.toOption
      }
    } yield {
      result
    }
  }

}
