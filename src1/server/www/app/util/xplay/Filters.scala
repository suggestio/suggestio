package util.xplay

import com.google.inject.Inject
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import util.HtmlCompressFilter
import util.cdn.{CorsFilter, DumpXffHeaders}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.10.15 22:50
 * Description: Инжектор фильтров.
 */
class Filters @Inject() (
  htmlCompress: HtmlCompressFilter,
  dumpXffHdrs : DumpXffHeaders,
  secHeaders  : SecHeadersFilter,
  cors        : CorsFilter
)
  extends HttpFilters
{

  override val filters: Seq[EssentialFilter] = {
    // TODO Выкинуть неактивные фильтры из списка. Для этого нужно isEnabled засунуть в них же.
    Seq(htmlCompress, dumpXffHdrs, secHeaders, cors)
  }

}
