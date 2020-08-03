package models.msc

import io.suggest.common.empty.EmptyProduct
import io.suggest.geo.MGeoPoint
import io.suggest.geo.GeoPoint.pipeDelimitedQsbOpt
import io.suggest.util.logs.MacroLogsImpl
import io.suggest.xplay.qsb.QueryStringBindableImpl
import play.api.mvc.QueryStringBindable
import util.qsb.QSBs.NglsStateMap_t
import util.qsb.QsbUtil

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 10.04.15 15:26
 * Description: Контейнер js-состояния выдачи (какой узел сейчас, какие панели открыты и т.д.).
 */

object ScJsState extends MacroLogsImpl {

  import io.suggest.sc.ScConstants.ScJsState._

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
                            nglsMapB     : QueryStringBindable[Option[NglsStateMap_t]]
                           ): QueryStringBindable[ScJsState] = {
    val geoPointOptB = pipeDelimitedQsbOpt
    new QueryStringBindableImpl[ScJsState] {
      override def bind(key: String, params: Map[String, Seq[String]]): Option[Either[String, ScJsState]] = {
        for {
          maybeAdnId            <- strOptB.bind (NODE_ID_FN,             params)
          maybeCatScreenOpened  <- boolOptB.bind(SEARCH_OPENED_FN,     params)
          maybeGeoScreenOpened  <- boolOptB.bind(MENU_OPENED_FN,     params)
          maybeGeneration       <- longOptB.bind(GENERATION_FN,         params)
          maybeFadsOpened       <- strOptB.bind (FOCUSED_AD_ID_FN, params)
          maybeProducerAdnId    <- strOptB.bind (PRODUCER_ADN_ID_FN,    params)
          geoPointOptEith       <- geoPointOptB.bind(LOC_ENV_FN,        params)
        } yield {
          val r = ScJsState(
            adnId               = strNonEmpty( QsbUtil.eitherOpt2option(maybeAdnId) ),
            searchScrOpenedOpt  = noFalse( QsbUtil.eitherOpt2option(maybeCatScreenOpened) ),
            menuOpenedOpt     = noFalse( QsbUtil.eitherOpt2option(maybeGeoScreenOpened) ),
            generationOpt       = QsbUtil.eitherOpt2option(maybeGeneration),
            fadOpenedIdOpt      = strNonEmpty( QsbUtil.eitherOpt2option(maybeFadsOpened) ),
            fadsProdIdOpt       = strNonEmpty( QsbUtil.eitherOpt2option(maybeProducerAdnId) ),
            geoPoint            = QsbUtil.eitherOpt2option( geoPointOptEith )
          )
          Right(r)
        }
      }

      override def unbind(key: String, value: ScJsState): String = {
        _mergeUnbinded1(
          strOptB.unbind  (NODE_ID_FN,            value.adnId),
          boolOptB.unbind (SEARCH_OPENED_FN,      value.searchScrOpenedOpt),
          boolOptB.unbind (MENU_OPENED_FN,        value.menuOpenedOpt),
          longOptB.unbind (GENERATION_FN,         value.generationOpt),
          strOptB.unbind  (FOCUSED_AD_ID_FN,      value.fadOpenedIdOpt),
          strOptB.unbind  (PRODUCER_ADN_ID_FN,    value.fadsProdIdOpt),
          geoPointOptB.unbind(LOC_ENV_FN,         value.geoPoint)
        )
      }
    } // new QSB {}
  }   // def qsb()

  /** Очень часто-используемый вообще пустой инстанс. */
  val veryEmpty = ScJsState(generationOpt = None)

}


/**
 * Класс, отражающий состояние js-выдачи на клиенте.
 * @param adnId id текущего узла.
 * @param searchScrOpenedOpt Инфа об открытости поисковой панели (справа).
 * @param menuOpenedOpt Инфа об раскрытости левой панели (меню).
 * @param generationOpt "Поколение" - random seed.
 * @param fadOpenedIdOpt id текущей открытой карточки.
 * @param fadsProdIdOpt id продьюсера просматриваемой карточки.
 * @param geoPoint Данные по текущему месту юзера на карте, если есть.
 */
// TODO Удалить древние поля следом за старой выдачей, унифицировать в Sc3Pages.MainScreen.
case class ScJsState(
                      adnId               : Option[String]   = None,
                      searchScrOpenedOpt  : Option[Boolean]  = None,
                      menuOpenedOpt       : Option[Boolean]  = None,
                      generationOpt       : Option[Long]     = None,
                      fadOpenedIdOpt      : Option[String]   = None,
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

  /** Очень каноническое состояние выдачи без каких-либо уточнений. */
  def canonical: ScJsState = copy(
    searchScrOpenedOpt  = None,
    menuOpenedOpt     = None,
    generationOpt       = None,
    fadsProdIdOpt       = None
  )

  /** Короткая сериализация экземпляра в открывок query string. */
  def toQs(qsb: QueryStringBindable[ScJsState] = ScJsState.qsbStandalone) = {
    qsb.unbind("", this)
  }

}
