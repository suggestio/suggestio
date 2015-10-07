package models

import models.im.DevScreen
import models.msc.{MScApiVsns, MScApiVsn}
import play.api.mvc.QueryStringBindable
import play.api.Play.{current, configuration}
import io.suggest.ym.model.ad.{AdsSearchArgsDfltImpl, AdsSearchArgsWrapper, AdsSearchArgsDflt}
import util.qsb.{CommaDelimitedStringSeq, QsbKey1T}
import util.qsb.QsbUtil._
import io.suggest.ad.search.AdSearchConstants._
import views.js.stuff.m.adSearchJsUnbindTpl
import scala.language.implicitConversions
import io.suggest.sc.ScConstants.ReqArgs.VSN

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 02.07.14 16:05
 * Description: Модель представления поискового запроса.
 */

object AdSearch extends CommaDelimitedStringSeq {

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
  implicit def qsb(implicit
                   strOptB      : QueryStringBindable[Option[String]],
                   intOptB      : QueryStringBindable[Option[Int]],
                   longOptB     : QueryStringBindable[Option[Long]],
                   geoModeB     : QueryStringBindable[GeoMode],
                   devScreenB   : QueryStringBindable[Option[DevScreen]],
                   apiVsnB      : QueryStringBindable[MScApiVsn]
                  ): QueryStringBindable[AdSearch] = {
    val strSeqB = cdssQsb
    new QueryStringBindable[AdSearch] with QsbKey1T {
      def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AdSearch]] = {
        val f = key1F(key)
        for {
          maybeApiVsn    <- apiVsnB.bind      (f(VSN),               params)
          maybeProdIdOpt <- strOptB.bind      (f(PRODUCER_ID_FN),    params)
          maybeCatIdOpt  <- strOptB.bind      (f(CAT_ID_FN),         params)
          maybeLevelOpt  <- strOptB.bind      (f(LEVEL_ID_FN),       params)
          maybeQOpt      <- strOptB.bind      (f(FTS_QUERY_FN),      params)
          maybeSizeOpt   <- intOptB.bind      (f(RESULTS_LIMIT_FN),  params)
          maybeOffsetOpt <- intOptB.bind      (f(RESULTS_OFFSET_FN), params)
          maybeRcvrIdOpt <- strOptB.bind      (f(RECEIVER_ID_FN),    params)
          maybeFirstIds  <- strSeqB.bind      (f(FIRST_AD_ID_FN),    params)
          maybeGen       <- {
            // Нужно игнорить возможный generation sort seed, если происходит полнотекстовый поиск.
            if (maybeQOpt.right.exists(_.nonEmpty))
              None
            else
              longOptB.bind(f(GENERATION_FN),     params)
          }
          maybeGeo       <- geoModeB.bind     (f(GEO_MODE_FN),       params)
          maybeDevScreen <- devScreenB.bind   (f(SCREEN_INFO_FN),    params)
          maybeInxOpAdId <- strOptB.bind      (f(OPEN_INDEX_AD_ID_FN), params)

        } yield {
          for {
            _apiVsn     <- maybeApiVsn.right
            _firstIds   <- maybeFirstIds.right
          } yield {
            new AdSearchImpl {
              override def apiVsn         = _apiVsn
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
              override def firstIds       = _firstIds
              override def randomSortSeed = maybeGen
              override def geo            = maybeGeo
              override def screen         = maybeDevScreen
              override def openIndexAdId  = maybeInxOpAdId
            }
          }
        }
      }

      def unbind(key: String, value: AdSearch): String = {
        val f = key1F(key)
        Iterator(
          apiVsnB.unbind      (f(VSN),               value.apiVsn),
          strOptB.unbind      (f(RECEIVER_ID_FN),    value.receiverIds.headOption),  // TODO Разбиндивать на весь список receivers сразу надо
          strOptB.unbind      (f(PRODUCER_ID_FN),    value.producerIds.headOption),  // TODO Разбиндивать на весь список producers сразу надо.
          strOptB.unbind      (f(CAT_ID_FN),         value.catIds.headOption),       // TODO Разбиндивать на весь список catIds надо бы
          strOptB.unbind      (f(LEVEL_ID_FN),       value.levels.headOption.map(_.toString)),
          strOptB.unbind      (f(FTS_QUERY_FN),      value.qOpt),
          intOptB.unbind      (f(RESULTS_LIMIT_FN),  value.maxResultsOpt),
          intOptB.unbind      (f(RESULTS_OFFSET_FN), value.offsetOpt),
          strSeqB.unbind      (f(FIRST_AD_ID_FN),    value.firstIds),
          longOptB.unbind     (f(GENERATION_FN),     value.randomSortSeed),
          strOptB.unbind      (f(GEO_MODE_FN),       value.geo.toQsStringOpt),
          devScreenB.unbind   (f(SCREEN_INFO_FN),    value.screen),
          strOptB.unbind      (f(OPEN_INDEX_AD_ID_FN), value.openIndexAdId)
        )
          .filter(!_.isEmpty)
          .mkString("&")
      }

      /** Для js-роутера нужна поддержка через JSON. */
      override def javascriptUnbind: String = {
        adSearchJsUnbindTpl(KEY_DELIM).body
      }
    }
  }

}


trait AdSearch extends AdsSearchArgsDflt { that =>

  /** Версия API выдачи, используемая для взаимодействия. */
  def apiVsn: MScApiVsn = MScApiVsns.unknownVsn

  /** Опциональное значение обязательного maxResults. Удобно при query-string. */
  def maxResultsOpt : Option[Int] = None

  /** Опциональное значение обязательного сдвига в результатах. */
  def offsetOpt     : Option[Int] = None

  /** Географическая информация, присланная клиентом. */
  def geo           : GeoMode = GeoNone

  /** Данные по экрану, присланные клиентом. */
  def screen        : Option[DevScreen] = None

  /**
   * Принудительно должен быть эти карточки первыми в списке.
   * На уровне ES это дело не прижилось, поэтому тут параметр, который отрабатывается в контроллере.
   * Следует помнить, что sc v1 и v2 имеют различный смысл этого аргумента.
   */
  def firstIds      : Seq[String] = Nil

  /** id карточки, для которой допускается вернуть index её продьюсера. */
  def openIndexAdId : Option[String] = None


  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  override def offset: Int = {
    if (offsetOpt.isDefined) offsetOpt.get else super.offset
  }

  /** Макс.кол-во результатов. */
  override def limit: Int = {
    maxResultsOpt getOrElse AdSearch.MAX_RESULTS_DFLT
  }

  // Утиль для удобства работы в шаблонах, привыкших к вызову copy().
  /** Без оффсета */
  def withoutOffset: AdSearch = new AdSearchWrapper {
    override def _dsArgsUnderlying = that
    override def offsetOpt: Option[Int] = None
  }

  /** Вычесть указанное число элементов из offset'а, отфильтровать неположительные значения. */
  def minusOffset(count: Int = limit): AdSearch = new AdSearchWrapper {
    override def _dsArgsUnderlying = that
    override def offsetOpt = super.offsetOpt.map(_ - count).filter(_ > 0)
  }

  /** Инкрементить offset на указанное кол-во элементов. */
  def plusOffset(count: Int = limit): AdSearch = new AdSearchWrapper {
    override def _dsArgsUnderlying = that
    override def offsetOpt: Option[Int] = {
      super.offsetOpt
        .map(_ + count)
        .orElse( Some(count) )
    }
  }

}

/** Дефолтовая реализация [[AdSearch]], очень облегчающая жизнь компилятору и кодогенератору.
  * Анонимные реализации new [[AdSearch]]{...} дают от 14кб+ на выходе,
  * а данный new AdSearchImpl{...} -- от 1,5кб всего лишь. */
class AdSearchImpl
  extends AdsSearchArgsDfltImpl
  with AdSearch


/** Враппер для [[AdSearch]] с абстрактным типом. Пригоден для дальнейшего расширения в иных моделях. */
trait AdSearchWrapper_ extends AdSearch with AdsSearchArgsWrapper {
  override type WT <: AdSearch

  override def apiVsn         = _dsArgsUnderlying.apiVsn
  override def maxResultsOpt  = _dsArgsUnderlying.maxResultsOpt
  override def offsetOpt      = _dsArgsUnderlying.offsetOpt
  override def geo            = _dsArgsUnderlying.geo
  override def screen         = _dsArgsUnderlying.screen
  override def firstIds       = _dsArgsUnderlying.firstIds
  override def openIndexAdId  = _dsArgsUnderlying.openIndexAdId

}

/** Враппер для AdSearch-контейнера, своим существованием имитирует case class copy(). */
trait AdSearchWrapper extends AdSearchWrapper_ {
  override type WT = AdSearch
}

