package models

import models.im.DevScreen
import play.api.mvc.QueryStringBindable
import play.api.Play.{current, configuration}
import io.suggest.ym.model.ad.{AdsSearchArgsWrapper, AdsSearchArgsDflt}
import util.qsb.QsbKey1T
import util.qsb.QsbUtil._
import io.suggest.ad.search.AdSearchConstants._
import scala.language.implicitConversions

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
                                 devScreenB: QueryStringBindable[Option[DevScreen]]
                                ): QueryStringBindable[AdSearch] = {
    new QueryStringBindable[AdSearch] with QsbKey1T {

      def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AdSearch]] = {
        def f = key1F(key)
        for {
          maybeProdIdOpt <- strOptBinder.bind (f(PRODUSER_ID_FN),    params)
          maybeCatIdOpt  <- strOptBinder.bind (f(CAT_ID_FN),         params)
          maybeLevelOpt  <- strOptBinder.bind (f(LEVEL_ID_FN),       params)
          maybeQOpt      <- strOptBinder.bind (f(FTS_QUERY_FN),      params)
          maybeSizeOpt   <- intOptB.bind      (f(RESULTS_LIMIT_FN),  params)
          maybeOffsetOpt <- intOptB.bind      (f(RESULTS_OFFSET_FN), params)
          maybeRcvrIdOpt <- strOptBinder.bind (f(RECEIVER_ID_FN),    params)
          maybeFirstId   <- strOptBinder.bind (f(FIRST_AD_ID_FN),    params)
          maybeGen       <- longOptB.bind     (f(GENERATION_FN),     params)
          maybeGeo       <- geoModeB.bind     (f(GEO_MODE_FN),       params)
          maybeDevScreen <- devScreenB.bind   (f(SCREEN_INFO_FN),    params)

        } yield {
          val res = new AdSearch {
            override def receiverIds    = maybeRcvrIdOpt
            override def producerIds    = maybeProdIdOpt
            override def catIds         = maybeCatIdOpt
            override def levels         = eitherOpt2list(maybeLevelOpt).flatMap(AdShowLevels.maybeWithName)
            override def qOpt           = maybeQOpt
            override def maxResultsOpt: Option[Int] = {
              eitherOpt2option(maybeSizeOpt) map { size =>
                Math.max(1,  Math.min(size, MAX_RESULTS_PER_RESPONSE))
              }
            }
            override def offsetOpt: Option[Int] = {
              eitherOpt2option(maybeOffsetOpt) map { offset =>
                Math.max(0, Math.min(offset, MAX_OFFSET))
              }
            }
            override def forceFirstIds  = maybeFirstId
            override def generationOpt  = maybeGen
            override def geo            = maybeGeo
            override def screen         = maybeDevScreen
          }
          Right(res)
        }
      }

      def unbind(key: String, value: AdSearch): String = {
        val f = key1F(key)
        Iterator(
          strOptBinder.unbind (f(RECEIVER_ID_FN),    value.receiverIds.headOption),  // TODO Разбиндивать на весь список receivers сразу надо
          strOptBinder.unbind (f(PRODUSER_ID_FN),    value.producerIds.headOption),  // TODO Разбиндивать на весь список producers сразу надо.
          strOptBinder.unbind (f(CAT_ID_FN),         value.catIds.headOption),       // TODO Разбиндивать на весь список catIds надо бы
          strOptBinder.unbind (f(LEVEL_ID_FN),       value.levels.headOption.map(_.toString)),
          strOptBinder.unbind (f(FTS_QUERY_FN),      value.qOpt),
          intOptB.unbind      (f(RESULTS_LIMIT_FN),  value.maxResultsOpt),
          intOptB.unbind      (f(RESULTS_OFFSET_FN), value.offsetOpt),
          strOptBinder.unbind (f(FIRST_AD_ID_FN),    value.forceFirstIds.headOption),
          longOptB.unbind     (f(GENERATION_FN),     value.generationOpt),
          strOptBinder.unbind (f(GEO_MODE_FN),       value.geo.toQsStringOpt),
          devScreenB.unbind   (f(SCREEN_INFO_FN),    value.screen)
        )
          .filter(!_.isEmpty)
          .mkString("&")
      }
    }
  }

}


trait AdSearch extends AdsSearchArgsDflt { that =>

  /** Опциональное значение обязательного maxResults. Удобно при query-string. */
  def maxResultsOpt : Option[Int] = None

  /** Опциональное значение обязательного сдвига в результатах. */
  def offsetOpt     : Option[Int] = None

  /** Географическая информация, присланная клиентом. */
  def geo           : GeoMode = GeoNone

  /** Данные по экрану, присланные клиентом. */
  def screen        : Option[DevScreen] = None

  /** Принудительно должен быть эти карточки первыми в списке.
    * На уровне ES это дело не прижилось, поэтому тут параметр, который отрабатывается в контроллере. */
  def forceFirstIds : Seq[String] = Seq.empty

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
    override def _dsArgsUnderlying = that
    override def offsetOpt: Option[Int] = None
  }

  /** Вычесть указанное число элементов из offset'а, отфильтровать неположительные значения. */
  def minusOffset(count: Int = maxResults): AdSearch = new AdSearchWrapper {
    override def _dsArgsUnderlying = that
    override def offsetOpt = super.offsetOpt.map(_ - count).filter(_ > 0)
  }

  /** Инкрементить offset на указанное кол-во элементов. */
  def plusOffset(count: Int = maxResults): AdSearch = new AdSearchWrapper {
    override def _dsArgsUnderlying = that
    override def offsetOpt: Option[Int] = Some( super.offsetOpt.fold(count)(_ + count) )
  }

}

/** Враппер для AdSearch-контейнера, своим существованием имитирует case class copy(). */
trait AdSearchWrapper extends AdSearch with AdsSearchArgsWrapper {
  override type WT = AdSearch

  /** Значение, которое скрывает этот враппер. */
  override def _dsArgsUnderlying: AdSearch

  override def maxResultsOpt  = _dsArgsUnderlying.maxResultsOpt
  override def offsetOpt      = _dsArgsUnderlying.offsetOpt
  override def geo            = _dsArgsUnderlying.geo
  override def screen         = _dsArgsUnderlying.screen
}

