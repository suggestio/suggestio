package models.msc

import io.suggest.common.empty.EmptyProduct
import io.suggest.geo.MGeoPoint
import io.suggest.geo.GeoPoint.pipeDelimitedQsbOpt
import io.suggest.model.play.qsb.QueryStringBindableImpl
import io.suggest.util.logs.MacroLogsImpl
import play.api.mvc.QueryStringBindable
import play.twirl.api.Html
import util.qsb.QSBs.NglsStateMap_t
import util.qsb.QsbUtil

import scala.util.Random

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:26
 * Description: Контейнер js-состояния выдачи (какой узел сейчас, какие панели открыты и т.д.).
 */

object ScJsState extends MacroLogsImpl {

  import io.suggest.sc.ScConstants.ScJsState._

  def generationDflt: Option[Long] = {
    val l = new Random().nextLong()
    Some(l)
  }

  def qsbStandalone: QueryStringBindable[ScJsState] = {
    import QueryStringBindable._
    import util.qsb.QSBs._
    scJsStateQsb
  }

  private def noFalse(boolOpt: Option[Boolean]) = boolOpt.filter(identity)
  private def strNonEmpty(strOpt: Option[String]) = strOpt.filter(!_.isEmpty)

  implicit def scJsStateQsb(implicit
                            strOptB      : QueryStringBindable[Option[String]],
                            boolOptB     : QueryStringBindable[Option[Boolean]],
                            longOptB     : QueryStringBindable[Option[Long]],
                            intOptB      : QueryStringBindable[Option[Int]],
                            nglsMapB     : QueryStringBindable[Option[NglsStateMap_t]]
                           ): QueryStringBindable[ScJsState] = {
    val geoPointOptB = pipeDelimitedQsbOpt
    new QueryStringBindableImpl[ScJsState] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ScJsState]] = {
        for {
          maybeAdnId            <- strOptB.bind (NODE_ID_FN,             params)
          maybeCatScreenOpened  <- boolOptB.bind(CAT_SCR_OPENED_FN,     params)
          maybeGeoScreenOpened  <- boolOptB.bind(GEO_SCR_OPENED_FN,     params)
          maybeGeneration       <- longOptB.bind(GENERATION_FN,         params)
          maybeFadsOpened       <- strOptB.bind (FOCUSED_AD_ID_FN, params)
          maybeFadsOffset       <- intOptB.bind (FADS_OFFSET_FN,        params)
          maybeProducerAdnId    <- strOptB.bind (PRODUCER_ADN_ID_FN,    params)
          geoPointOptEith       <- geoPointOptB.bind(LOC_ENV_FN,        params)
        } yield {
          val r = ScJsState(
            adnId               = strNonEmpty( QsbUtil.eitherOpt2option(maybeAdnId) ),
            searchScrOpenedOpt  = noFalse( QsbUtil.eitherOpt2option(maybeCatScreenOpened) ),
            navScrOpenedOpt     = noFalse( QsbUtil.eitherOpt2option(maybeGeoScreenOpened) ),
            generationOpt       = QsbUtil.eitherOpt2option(maybeGeneration)
              .orElse(generationDflt),
            fadOpenedIdOpt      = strNonEmpty( QsbUtil.eitherOpt2option(maybeFadsOpened) ),
            fadsOffsetOpt       = QsbUtil.eitherOpt2option(maybeFadsOffset),
            fadsProdIdOpt       = strNonEmpty( QsbUtil.eitherOpt2option(maybeProducerAdnId) ),
            geoPoint            = QsbUtil.eitherOpt2option( geoPointOptEith )
          )
          Right(r)
        }
      }

      override def unbind(key: String, value: ScJsState): String = {
        _mergeUnbinded1(
          strOptB.unbind  (NODE_ID_FN,             value.adnId),
          boolOptB.unbind (CAT_SCR_OPENED_FN,     value.searchScrOpenedOpt),
          boolOptB.unbind (GEO_SCR_OPENED_FN,     value.navScrOpenedOpt),
          longOptB.unbind (GENERATION_FN,         value.generationOpt),
          strOptB.unbind  (FOCUSED_AD_ID_FN,      value.fadOpenedIdOpt),
          intOptB.unbind  (FADS_OFFSET_FN,        value.fadsOffsetOpt),
          strOptB.unbind  (PRODUCER_ADN_ID_FN,    value.fadsProdIdOpt),
          geoPointOptB.unbind(LOC_ENV_FN,         value.geoPoint)
        )
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
 * @param fadsProdIdOpt id продьюсера просматриваемой карточки.
 * @param geoPoint Данные по текущему месту юзера на карте, если есть.
 */
// TODO Удалить древние поля следом за старой выдачей, унифицировать в Sc3Pages.MainScreen.
case class ScJsState(
                      adnId               : Option[String]   = None,
                      searchScrOpenedOpt  : Option[Boolean]  = None,
                      navScrOpenedOpt     : Option[Boolean]  = None,
                      generationOpt       : Option[Long]     = ScJsState.generationDflt,
                      fadOpenedIdOpt      : Option[String]   = None,
                      fadsOffsetOpt       : Option[Int]      = None,
                      fadsProdIdOpt       : Option[String]   = None,
                      geoPoint            : Option[MGeoPoint] = None
)
  extends EmptyProduct
{ that =>

  /** Внутренний тест одного значения на nonEmpty. */
  override protected[this] def _nonEmptyValue(v: Any): Boolean = {
    v match {
      // Запрещаем проверять значение поля generationOpt: возвращать false даже для Some().
      case genOpt: Some[_] if genOpt eq generationOpt =>
        false
      case other =>
        super._nonEmptyValue(other)
    }
  }

  protected def orFalse(boolOpt: Option[Boolean]): Boolean = {
    boolOpt.isDefined && boolOpt.get
  }

  protected def orZero(intOpt: Option[Int]): Int = {
    if (intOpt.isDefined)  intOpt.get  else  0
  }

  protected def bool2boolOpt(bool: Boolean): Option[Boolean] = {
    if (bool) Some(bool) else None
  }

  def isSearchScrOpened : Boolean = orFalse( searchScrOpenedOpt )
  def isNavScrOpened    : Boolean = orFalse( navScrOpenedOpt )
  def isAnyPanelOpened  : Boolean = isSearchScrOpened || isNavScrOpened
  def isFadsOpened      : Boolean = fadOpenedIdOpt.isDefined
  def isSomethingOpened : Boolean = isAnyPanelOpened || isFadsOpened


  def fadsOffset        : Int     = orZero( fadsOffsetOpt )

  def generation: Long = generationOpt.getOrElse(System.currentTimeMillis)

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
    searchScrOpenedOpt  = None,
    navScrOpenedOpt     = None,
    generationOpt       = None,
    fadsOffsetOpt       = None,
    fadsProdIdOpt       = None
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


