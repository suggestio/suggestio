package util.xplay

import com.google.inject.Inject
import io.suggest.www.util.sec.ExpireSessionFilter
import play.api.http.HttpFilters
import play.api.mvc.EssentialFilter
import util.HtmlCompressFilter
import util.cdn.{CorsFilter, DumpXffHeaders}

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
  expireSessionFilter     : ExpireSessionFilter,
  htmlCompress            : HtmlCompressFilter,
  dumpXffHdrs             : DumpXffHeaders,
  secHeaders              : SecHeadersFilter,
  cors                    : CorsFilter
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
