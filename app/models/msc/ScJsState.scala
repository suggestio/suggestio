package models.msc

import io.suggest.model.n2.edge.search.{Criteria, ICriteria}
import io.suggest.model.play.qsb.QueryStringBindableImpl
import models._
import models.mlu.MLookupModes
import play.api.mvc.QueryStringBindable
import play.twirl.api.Html
import util.qsb.QSBs.NglsStateMap_t
import util.qsb.QsbUtil._

import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:26
 * Description: Контейнер js-состояния выдачи (какой узел сейчас, какие панели открыты и т.д.).
 */

object ScJsState {

  import io.suggest.sc.ScConstants.ScJsState._

  def generationDflt: Option[Long] = {
    val l = new Random().nextLong()
    Some(l)
  }

  def qsbStandalone: QueryStringBindable[ScJsState] = {
    import QueryStringBindable._
    import util.qsb.QSBs._
    qsb
  }

  private def noFalse(boolOpt: Option[Boolean]) = boolOpt.filter(identity)
  private def strNonEmpty(strOpt: Option[String]) = strOpt.filter(!_.isEmpty)

  implicit def qsb(implicit
                   strOptB  : QueryStringBindable[Option[String]],
                   boolOptB : QueryStringBindable[Option[Boolean]],
                   longOptB : QueryStringBindable[Option[Long]],
                   intOptB  : QueryStringBindable[Option[Int]],
                   nglsMapB : QueryStringBindable[Option[NglsStateMap_t]]
                  ): QueryStringBindable[ScJsState] = {
    new QueryStringBindableImpl[ScJsState] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ScJsState]] = {
        for {
          maybeAdnId            <- strOptB.bind (ADN_ID_FN,             params)
          maybeCatScreenOpened  <- boolOptB.bind(CAT_SCR_OPENED_FN,     params)
          maybeGeoScreenOpened  <- boolOptB.bind(GEO_SCR_OPENED_FN,     params)
          maybeGeneration       <- longOptB.bind(GENERATION_FN,         params)
          maybeFadsOpened       <- strOptB.bind (FADS_CURRENT_AD_ID_FN, params)
          maybeFadsOffset       <- intOptB.bind (FADS_OFFSET_FN,        params)
          maybeSearchTab        <- boolOptB.bind(SEARCH_TAB_FN,         params)
          maybeProducerAdnId    <- strOptB.bind (PRODUCER_ADN_ID_FN,    params)
          maybeTileCatId        <- strOptB.bind (TILES_CAT_ID_FN,       params)
          maybeNglsMap          <- nglsMapB.bind(NAV_NGLS_STATE_MAP_FN, params)
        } yield {
          val res = ScJsState(
            adnId               = strNonEmpty( maybeAdnId ),
            searchScrOpenedOpt  = noFalse( maybeCatScreenOpened ),
            navScrOpenedOpt     = noFalse( maybeGeoScreenOpened ),
            generationOpt       = maybeGeneration orElse generationDflt,
            fadOpenedIdOpt      = strNonEmpty( maybeFadsOpened ),
            fadsOffsetOpt       = maybeFadsOffset,
            searchTabListOpt    = noFalse( maybeSearchTab ),
            fadsProdIdOpt       = strNonEmpty( maybeProducerAdnId ),
            tilesCatIdOpt       = strNonEmpty( maybeTileCatId ),
            navNglsMap          = maybeNglsMap getOrElse Map.empty
          )
          Right(res)
        }
      }

      override def unbind(key: String, value: ScJsState): String = {
        _mergeUnbinded {
          Iterator(
            strOptB.unbind  (ADN_ID_FN,             value.adnId),
            boolOptB.unbind (CAT_SCR_OPENED_FN,     value.searchScrOpenedOpt),
            boolOptB.unbind (GEO_SCR_OPENED_FN,     value.navScrOpenedOpt),
            longOptB.unbind (GENERATION_FN,         value.generationOpt),
            strOptB.unbind  (FADS_CURRENT_AD_ID_FN, value.fadOpenedIdOpt),
            intOptB.unbind  (FADS_OFFSET_FN,        value.fadsOffsetOpt),
            boolOptB.unbind (SEARCH_TAB_FN,         value.searchTabListOpt),
            strOptB.unbind  (PRODUCER_ADN_ID_FN,    value.fadsProdIdOpt),
            strOptB.unbind  (TILES_CAT_ID_FN,       value.tilesCatIdOpt),
            nglsMapB.unbind (NAV_NGLS_STATE_MAP_FN, if (value.navNglsMap.isEmpty) None else Some(value.navNglsMap) )
          )
        }
      }
    } // new QSB {}
  }   // def qsb()

  /** Выдать пустой инстанс. Всегда немного разный, чтобы был эффект тасования. */
  def empty = ScJsState()

  /** Очень часто-используемый вообще пустой инстанс. */
  val veryEmpty = ScJsState(generationOpt = None)

}


/**
 * Класс, отражающий состояние js-выдачи на клиенте.
 * @param adnId id текущего узла.
 * @param searchScrOpenedOpt Инфа об открытости поисковой панели.
 * @param navScrOpenedOpt Инфа об раскрытости навигационной панели.
 * @param generationOpt "Поколение".
 * @param fadOpenedIdOpt id текущей открытой карточки.
 * @param fadsOffsetOpt текущий сдвиг в просматриваемых карточках.
 * @param searchTabListOpt Выбранная вкладка на поисковой панели.
 * @param fadsProdIdOpt id продьюсера просматриваемой карточки.
 * @param tilesCatIdOpt id текущей категории в плитке категорий.
 * @param navNglsMap Карта недефолтовых состояний отображаемых гео-уровней на карте навигации по узлам.
 */
case class ScJsState(
  adnId               : Option[String]   = None,
  searchScrOpenedOpt  : Option[Boolean]  = None,
  navScrOpenedOpt     : Option[Boolean]  = None,
  generationOpt       : Option[Long]     = ScJsState.generationDflt,
  fadOpenedIdOpt      : Option[String]   = None,
  fadsOffsetOpt       : Option[Int]      = None,
  searchTabListOpt    : Option[Boolean]  = None,
  fadsProdIdOpt       : Option[String]   = None,
  tilesCatIdOpt       : Option[String]   = None,
  navNglsMap          : Map[NodeGeoLevel, Boolean] = Map.empty
) { that =>

  /** Содержаться ли тут какие-либо данные? */
  def nonEmpty: Boolean = {
    productIterator.exists {
      case opt: Option[_]           => opt.nonEmpty && !(opt eq generationOpt)
      case col: TraversableOnce[_]  => col.nonEmpty
      case _ => true
    }
  }
  /** Инстанс содержит хоть какие-нибудь полезные данные? */
  def isEmpty = !nonEmpty

  protected def orFalse(boolOpt: Option[Boolean]): Boolean = {
    boolOpt.isDefined && boolOpt.get
  }

  protected def orZero(intOpt: Option[Int]): Int = {
    if (intOpt.isDefined)  intOpt.get  else  0
  }

  protected def bool2boolOpt(bool: Boolean): Option[Boolean] = {
    if (bool) Some(bool) else None
  }

  // Пока что считывание geo-состояния из qs не нужно, т.к. HTML5 Geolocation доступно только в js-выдаче.
  def geo: GeoMode = GeoIp

  /** Экземпляр AdSearch для поиска карточек, отображаемых в плитке. */
  def tilesAdSearch(): AdSearch = new AdSearchImpl {
    override def randomSortSeed = that.generationOpt
    override def geo            = that.geo
    override def outEdges: Seq[ICriteria] = {
      val cr = Criteria(
        nodeIds   = that.adnId.toSeq,
        sls       = Seq(AdShowLevels.LVL_START_PAGE)
      )
      Seq(cr)
    }
  }

  /** Экземпляр AdSearch для поиска в текущей рекламной карточки. */
  def focusedAdSearch(_maxResultsOpt: Option[Int]): FocusedAdsSearchArgs = {
    new FocusedAdsSearchArgsImpl {
      // v1 выдача.
      override def firstIds       = that.fadOpenedIdOpt.toList
      override def offsetOpt      = that.fadsOffsetOpt
      // При синхронном рендере единственная карточка автоматом является целевой
      override def withHeadAd     = true

      // v2 выдача.
      override def lookupMode     = MLookupModes.Around
      override def lookupAdId     = that.fadOpenedIdOpt.get
      override def focJumpAllowed = false

      // common-параметры для выборки карточек.
      override def limitOpt  = _maxResultsOpt
      override def randomSortSeed  = that.generationOpt
      override def outEdges: Seq[ICriteria] = {
        val someTrue = Some(true)
        val rcvrCrOpt = for (nodeId <- that.adnId) yield {
          Criteria(
            nodeIds     = Seq(nodeId),
            predicates  = Seq(MPredicates.Receiver),
            must        = someTrue
          )
        }
        val prodCrOpt = for (nodeId <- that.fadsProdIdOpt) yield {
          Criteria(
            nodeIds     = Seq(nodeId),
            predicates  = Seq(MPredicates.OwnedBy),
            must        = someTrue
          )
        }
        (rcvrCrOpt ++ prodCrOpt).toSeq
      }

    }
  }

  def isSearchScrOpened : Boolean = orFalse( searchScrOpenedOpt )
  def isNavScrOpened    : Boolean = orFalse( navScrOpenedOpt )
  def isAnyPanelOpened  : Boolean = isSearchScrOpened || isNavScrOpened
  def isFadsOpened      : Boolean = fadOpenedIdOpt.isDefined
  def isSomethingOpened : Boolean = isAnyPanelOpened || isFadsOpened

  def isSearchTabList   : Boolean = searchTabListOpt.exists(identity)
  def isSearchTabCats   : Boolean = !isSearchTabList

  def fadsOffset        : Int     = orZero( fadsOffsetOpt )

  def generation: Long = generationOpt.getOrElse(System.currentTimeMillis)

  /** Уточнить значение состояния развернутости nav. гео-слоя. В синхронной выдаче возможны варианты. */
  def nglExpanded(gnl: GeoNodesLayer): Boolean = {
    navNglsMap.getOrElse(gnl.ngl, gnl.expanded)
  }

  /**
   * Переключить состояние поля navScrOpenedOpt, сгенерив новое состояние.
   * @return Копия текущего состояния с новым значением поля navScrOpenedOpt.
   */
  def toggleNavScreen = copy(
    navScrOpenedOpt = bool2boolOpt( !isNavScrOpened )
  )

  def toggleSearchScreen = copy(
    searchScrOpenedOpt = bool2boolOpt( !isSearchScrOpened )
  )


  /** Очень каноническое состояние выдачи без каких-либо уточнений. */
  def canonical: ScJsState = copy(
    searchScrOpenedOpt = None, navScrOpenedOpt = None, generationOpt = None, fadsOffsetOpt = None, searchTabListOpt = None,
    fadsProdIdOpt = None, tilesCatIdOpt = None, navNglsMap = Map.empty
  )

  /** Короткая сериализация экземпляра в открывок query string. */
  def toQs(qsb: QueryStringBindable[ScJsState] = ScJsState.qsbStandalone) = {
    qsb.unbind("", this)
  }

}


/** Некоторые асинхронные шаблоны выдачи при синхронном рендере требуют для себя js-состояние. */
abstract class JsStateRenderWrapper {

  /**
    * Запустить синхронный рендер шаблона используя указанное js-состояние выдачи.
    * @param jsStateOpt None - происходит асинхронный рендер. Some() - идёт синхронный рендер с указанным состоянием.
    * @return Отрендеренный HTML.
    */
  def apply(jsStateOpt: Option[ScJsState] = None): Html

}


