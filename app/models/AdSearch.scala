package models

import models.im.DevScreen
import play.api.mvc.QueryStringBindable
import play.api.Play.{current, configuration}
import io.suggest.ym.model.ad.{AdsSearchArgsWrapper, AdsSearchArgsDflt}
import util.qsb.QsbUtil._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 16:05
 * Description: Модель представления поискового запроса.
 */

object AdSearch {

  /** Максимальное число результатов в ответе на запрос (макс. результатов на странице). */
  val MAX_RESULTS_PER_RESPONSE = configuration.getInt("market.search.ad.results.max") getOrElse 50

  /** Кол-во результатов на страницу по дефолту. */
  val MAX_RESULTS_DFLT = configuration.getInt("market.search.ad.results.count.dflt") getOrElse 20

  /** Макс.кол-во сдвигов в страницах. */
  def MAX_PAGE_OFFSET = configuration.getInt("market.search.ad.results.offset.max") getOrElse 20

  /** Максимальный абсолютный сдвиг в выдаче. */
  val MAX_OFFSET: Int = MAX_PAGE_OFFSET * MAX_RESULTS_PER_RESPONSE



  private implicit def eitherOpt2list[T](e: Either[_, Option[T]]): List[T] = {
    e match {
      case Left(_)  => Nil
      case Right(b) => b.toList
    }
  }

  /** QSB для экземпляра сабжа. Неявно дергается из routes. */
  implicit def queryStringBinder(implicit strOptBinder: QueryStringBindable[Option[String]],
                                 intOptB: QueryStringBindable[Option[Int]],
                                 longOptB: QueryStringBindable[Option[Long]],
                                 geoModeB: QueryStringBindable[GeoMode],
                                 devScreenB: QueryStringBindable[Option[DevScreen]] ) = {
    new QueryStringBindable[AdSearch] {

      def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AdSearch]] = {
        for {
          maybeProdIdOpt <- strOptBinder.bind(key + ".shopId", params)
          maybeCatIdOpt  <- strOptBinder.bind(key + ".catId", params)
          maybeLevelOpt  <- strOptBinder.bind(key + ".level", params)
          maybeQOpt      <- strOptBinder.bind(key + ".q", params)
          maybeSizeOpt   <- intOptB.bind(key + ".size", params)
          maybeOffsetOpt <- intOptB.bind(key + ".offset", params)
          maybeRcvrIdOpt <- strOptBinder.bind(key + ".rcvr", params)
          maybeFirstId   <- strOptBinder.bind(key + ".firstAdId", params)
          maybeGen       <- longOptB.bind(key + ".gen", params)
          maybeGeo       <- geoModeB.bind(key + ".geo", params)
          maybeDevScreen <- devScreenB.bind(key + ".screen", params)

        } yield {
          val _maxResultsOpt = eitherOpt2option(maybeSizeOpt) map { size =>
            Math.max(1,  Math.min(size, MAX_RESULTS_PER_RESPONSE))
          }
          Right(
            new AdSearch {
              override def receiverIds = maybeRcvrIdOpt
              override def producerIds = maybeProdIdOpt
              override def catIds = maybeCatIdOpt
              override def levels = eitherOpt2list(maybeLevelOpt).flatMap(AdShowLevels.maybeWithName)
              override def qOpt = maybeQOpt
              override def maxResultsOpt = _maxResultsOpt
              override lazy val offsetOpt = eitherOpt2option(maybeOffsetOpt) map { offset =>
                Math.max(0, Math.min(offset, MAX_OFFSET))
              }
              override def forceFirstIds = maybeFirstId
              override def generationOpt = maybeGen
              override def geo = maybeGeo
              override def screen = maybeDevScreen
            }
          )
        }
      }

      def unbind(key: String, value: AdSearch): String = {
        List(
          strOptBinder.unbind(key + ".rcvr", value.receiverIds.headOption),   // TODO Разбиндивать на весь список receivers сразу надо
          strOptBinder.unbind(key + ".shopId", value.producerIds.headOption), // TODO Разбиндивать на весь список producers сразу надо.
          strOptBinder.unbind(key + ".catId", value.catIds.headOption),       // TODO Разбиндивать на весь список catIds надо бы
          strOptBinder.unbind(key + ".level", value.levels.headOption.map(_.toString)),
          strOptBinder.unbind(key + ".q", value.qOpt),
          intOptB.unbind(key + ".size", value.maxResultsOpt),
          intOptB.unbind(key + ".offset", value.offsetOpt),
          strOptBinder.unbind(key + ".firstAdId", value.forceFirstIds.headOption),
          longOptB.unbind(key + ".gen", value.generationOpt),
          strOptBinder.unbind(key + ".geo", value.geo.toQsStringOpt),
          devScreenB.unbind(key + ".screen", value.screen)
        ) .filter(!_.isEmpty)
          .mkString("&")
      }
    }
  }

}


trait AdSearch extends AdsSearchArgsDflt {

  /** Опциональное значение обязательного maxResults. Удобно при query-string. */
  def maxResultsOpt : Option[Int] = None

  /** Опциональное значение обязательного сдвига в результатах. */
  def offsetOpt     : Option[Int] = None

  /** Географическая информация, присланная клиентом. */
  def geo           : GeoMode = GeoNone

  /** Данные по экрану, присланные клиентом. */
  def screen        : Option[DevScreen] = None


  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  override def offset: Int = {
    if (offsetOpt.isDefined) offsetOpt.get else super.offset
  }

  /** Макс.кол-во результатов. */
  override def maxResults: Int = {
    maxResultsOpt getOrElse AdSearch.MAX_RESULTS_DFLT
  }

  // Утиль для удобства работы в шаблонах, привыкших к вызову copy().
  /** Без оффсета */
  def withoutOffset: AdSearch = new AdSearchWrapper {
    override def _adsSearchArgsUnderlying = this
    override def offsetOpt: Option[Int] = None
  }

  /** Вычесть указанное число элементов из offset'а, отфильтровать неположительные значения. */
  def minusOffset(count: Int = maxResults): AdSearch = new AdSearchWrapper {
    override def _adsSearchArgsUnderlying = this
    override def offsetOpt = super.offsetOpt.map(_ - count).filter(_ > 0)
  }

  /** Инкрементить offset на указанное кол-во элементов. */
  def plusOffset(count: Int = maxResults): AdSearch = new AdSearchWrapper {
    override def _adsSearchArgsUnderlying = this
    override def offsetOpt: Option[Int] = Some( super.offsetOpt.fold(count)(_ + count) )
  }

}

/** Враппер для AdSearch-контейнера, своим существованием имитирует case class copy(). */
trait AdSearchWrapper extends AdSearch with AdsSearchArgsWrapper {
  /** Значение, которое скрывает этот враппер. */
  override def _adsSearchArgsUnderlying: AdSearch

  override def maxResultsOpt  = _adsSearchArgsUnderlying.maxResultsOpt
  override def offsetOpt      = _adsSearchArgsUnderlying.offsetOpt
  override def geo            = _adsSearchArgsUnderlying.geo
  override def screen         = _adsSearchArgsUnderlying.screen
}

