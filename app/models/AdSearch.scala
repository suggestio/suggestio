package models

import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.n2.node.search._
import models.im.DevScreen
import models.msc.{MScApiVsns, MScApiVsn}
import play.api.mvc.QueryStringBindable
import util.qsb.{CommaDelimitedStringSeq, QsbKey1T}
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
  def MAX_RESULTS_PER_RESPONSE    = 50

  /** Кол-во результатов на страницу по дефолту. */
  def MAX_RESULTS_DFLT            = 20

  /** Макс.кол-во сдвигов в страницах. */
  def MAX_PAGE_OFFSET             = 20

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
      /** Десериализация URL qs в экземпляр [[AdSearch]]. */
      def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, AdSearch]] = {
        val f = key1F(key)
        for {
          apiVsnEith        <- apiVsnB.bind      (f(VSN),                   params)
          prodIdOptEith     <- strOptB.bind      (f(PRODUCER_ID_FN),        params)
          rcvrSlOptEith     <- strOptB.bind      (f(LEVEL_ID_FN),           params)
          qOptEith          <- strOptB.bind      (f(FTS_QUERY_FN),          params)
          limitOptEith      <- intOptB.bind      (f(RESULTS_LIMIT_FN),      params)
          offsetOptEith     <- intOptB.bind      (f(RESULTS_OFFSET_FN),     params)
          rcvrIdOptEith     <- strOptB.bind      (f(RECEIVER_ID_FN),        params)
          firstIdsEith      <- strSeqB.bind      (f(FIRST_AD_ID_FN),        params)
          rndSeedEith       <- {
            // Нужно игнорить возможный generation sort seed, если происходит полнотекстовый поиск.
            if (qOptEith.right.exists(_.nonEmpty))
              None
            else
              longOptB.bind(f(GENERATION_FN),     params)
          }
          geoEith           <- geoModeB.bind     (f(GEO_MODE_FN),           params)
          screenEith        <- devScreenB.bind   (f(SCREEN_INFO_FN),        params)
          openInxAdIdEith   <- strOptB.bind      (f(OPEN_INDEX_AD_ID_FN),   params)

        } yield {
          for {
            _apiVsn         <- apiVsnEith.right
            _firstIds       <- firstIdsEith.right
            _prodIdOpt      <- prodIdOptEith.right
            _rcvrIdOpt      <- rcvrIdOptEith.right
            _rcvrSlOpt      <- rcvrSlOptEith.right
            _geo            <- geoEith.right
            _screen         <- screenEith.right
            _rndSeed        <- rndSeedEith.right
            _qOpt           <- qOptEith.right
            _openInxAdId    <- openInxAdIdEith.right
            _limitOpt       <- limitOptEith.right
            _offsetOpt      <- offsetOptEith.right
          } yield {
            // Расчитываем значение эджей.
            val _outEdges: Seq[ICriteria] = {
              val someTrue = Some(true)
              val prodCrOpt = for (prodId <- _prodIdOpt) yield {
                Criteria(
                  nodeIds     = Seq( prodId ),
                  predicates  = Seq( MPredicates.OwnedBy ),
                  must        = someTrue
                )
              }
              val rcvrCrOpt = for (rcvrId <- _rcvrIdOpt) yield {
                Criteria(
                  nodeIds     = Seq( rcvrId ),
                  predicates  = Seq( MPredicates.Receiver ),
                  sls         = _rcvrSlOpt.flatMap(AdShowLevels.maybeWithName).toSeq,
                  must        = someTrue
                )
              }
              (prodCrOpt ++ rcvrCrOpt).toSeq
            }
            // Собираем результат.
            new AdSearchImpl {
              override def apiVsn         = _apiVsn
              override def outEdges       = _outEdges
              override def qOpt           = _qOpt
              override def limitOpt: Option[Int] = {
                for (_limit <- _limitOpt) yield {
                  Math.max(1,  Math.min(_limit, MAX_RESULTS_PER_RESPONSE))
                }
              }
              override def offsetOpt: Option[Int] = {
                for (_offset <- _offsetOpt) yield {
                  Math.max(0, Math.min(_offset, MAX_OFFSET))
                }
              }
              override def firstIds       = _firstIds
              override def randomSortSeed = _rndSeed
              override def geo            = _geo
              override def screen         = _screen
              override def openIndexAdId  = _openInxAdId
            }
          }
        }
      }

      /** Сериализация экземпляра [[AdSearch]] в URL query string. */
      def unbind(key: String, value: AdSearch): String = {
        val f = key1F(key)
        // Вычисляем id продьюсера.
        val _prodIdOpt = value.outEdges
            .iterator
            .filter { _.predicates.contains( MPredicates.OwnedBy ) }
            .flatMap { _.nodeIds }
            .toStream
            // TODO Разбиндивать на весь список producers сразу надо?
            .headOption
        // Вычисляем данные по ресиверу.
        val (_rcvrIdOpt, _rcvrSlOpt) = {
          val v = value.outEdges
            .iterator
            .filter { _.predicates.contains( MPredicates.Receiver ) }
            .toStream
            // TODO Разбиндивать на весь список receivers сразу надо?
            .headOption
          (v.flatMap(_.nodeIds.headOption),
            v.flatMap(_.sls.headOption).map(_.name))
        }
        // Собираем аргументы для сборки query string.
        Iterator(
          apiVsnB.unbind      (f(VSN),               value.apiVsn),
          strOptB.unbind      (f(RECEIVER_ID_FN),    _rcvrIdOpt),
          strOptB.unbind      (f(PRODUCER_ID_FN),    _prodIdOpt),
          strOptB.unbind      (f(LEVEL_ID_FN),       _rcvrSlOpt),
          strOptB.unbind      (f(FTS_QUERY_FN),      value.qOpt),
          intOptB.unbind      (f(RESULTS_LIMIT_FN),  value.limitOpt),
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


  /** Быстрая сборка экземпляра AdSearch для поиска по producerId без лишней конкретики.
    * Используется шаблонами для краткой сборки экземпляров. */
  def byProducerId(producerIds: String*): AdSearch = {
    _adSearchEdge(MPredicates.OwnedBy, producerIds)
  }

  /** Быстрая сборка экземпляра AdSearch для поиска по producerId без лишней конкретики.
    * Используется шаблонами для краткой сборки экземпляров. */
  def byRcvrId(rcvrIds: String*): AdSearch = {
    _adSearchEdge(MPredicates.Receiver, rcvrIds)
  }

  private def _adSearchEdge(_pred: MPredicate, _nodeIds: Seq[String]): AdSearch = {
    new AdSearchImpl {
      override def outEdges: Seq[ICriteria] = {
        val cr = Criteria(
          nodeIds     = _nodeIds,
          predicates  = Seq(_pred)
        )
        Seq(cr)
      }
    }
  }

}


/** Этот трейт является базовой единицей модели.
  * Он описывает итоговую модель распарсенных аргументов поиска карточек. */
trait AdSearch extends MNodeSearchDflt { that =>

  /** Изначально, этот поиск был заточен только под поиск карточек. */
  override def nodeTypes: Seq[MNodeType] = {
    Seq( MNodeTypes.Ad )
  }

  /** Версия API выдачи, используемая для взаимодействия. */
  def apiVsn: MScApiVsn = MScApiVsns.unknownVsn


  /** Опциональное значение обязательного maxResults. Удобно при query-string. */
  def limitOpt : Option[Int] = None

  /** Макс.кол-во результатов. */
  override def limit: Int = {
    limitOpt getOrElse AdSearch.MAX_RESULTS_DFLT
  }


  /** Опциональное значение обязательного сдвига в результатах. */
  def offsetOpt     : Option[Int] = None

  /** Абсолютный сдвиг в результатах (постраничный вывод). */
  override def offset: Int = {
    if (offsetOpt.isDefined) offsetOpt.get else super.offset
  }


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


  def withOffset(offset1: Int): AdSearch = {
    new AdSearchWrapper {
      override def _dsArgsUnderlying = that
      override def offsetOpt = Some(offset1)
    }
  }

}

/** Дефолтовая реализация [[AdSearch]], очень облегчающая жизнь компилятору и кодогенератору.
  * Анонимные реализации new [[AdSearch]]{...} дают от 14кб+ на выходе,
  * а данный new AdSearchImpl{...} -- от 1,5кб всего лишь. */
class AdSearchImpl
  extends MNodeSearchDfltImpl
  with AdSearch


trait AdSearchWrap_ extends AdSearch with MNodeSearchWrap {

  override type WT <: AdSearch

  override def apiVsn         = _dsArgsUnderlying.apiVsn
  override def limitOpt       = _dsArgsUnderlying.limitOpt
  override def offsetOpt      = _dsArgsUnderlying.offsetOpt
  override def geo            = _dsArgsUnderlying.geo
  override def screen         = _dsArgsUnderlying.screen
  override def firstIds       = _dsArgsUnderlying.firstIds
  override def openIndexAdId  = _dsArgsUnderlying.openIndexAdId

}

trait AdSearchWrap extends AdSearchWrap_ {
  override type WT = AdSearch
}


/** Враппер для [[AdSearch]] с абстрактным типом. Пригоден для дальнейшего расширения в иных моделях. */
abstract class AdSearchWrapper_
  extends MNodeSearchWrapImpl_
  with AdSearchWrap_


/** Враппер для AdSearch-контейнера, своим существованием имитирует case class copy(). */
abstract class AdSearchWrapper
  extends AdSearchWrapper_
  with AdSearchWrap

