package util.xplay

import javax.inject.Inject
import io.suggest.sec.ExpireSessionFilter
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import play.filters.gzip.GzipFilter
import util.app.CdvFetchHttpCrunchFilter
import util.cdn.{CorsFilter, DumpXffHeaders}
import util.tpl.HtmlCompressFilter

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 23.10.15 22:50
  * Description: Инжектор фильтров.
  *
  * 2017.feb.28: Фильтры добавляются путём закидывания их в список аргументов конструктора.
  * Внутренний список фильтров упразднён.
  */
final case class Filters @Inject() (
  // gzip должен идти выше htmlCompress. TODO Кажется, что фильтры применяются в обратном порядке. Надо разобаться, возможно val filters требует reverse?
  gzipFilter              : GzipFilter,
  expireSessionFilter     : ExpireSessionFilter,
  htmlCompress            : HtmlCompressFilter,
  dumpXffHdrs             : DumpXffHeaders,
  secHeaders              : SecHeadersFilter,
  cors                    : CorsFilter,
  //cdvFetchHttpCrunchFilter: CdvFetchHttpCrunchFilter,
)
  extends HttpFilters
{

  override val filters: Seq[EssentialFilter] = {
    // Извлечь из конструктора только фильтры.
    productIterator
      .collect {
        case f: EssentialFilter => f
      }
      // TODO Выкидывать неактивные фильтры из списка. Для этого нужно isEnabled засунуть в них же, или config как-то читать...
      // toList, т.к. List ест меньше памяти, чем Stream.
      .toList
  }

}
