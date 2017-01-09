package models.msc

import io.suggest.common.menum.EnumValue2Val
import io.suggest.sc.ScConstants.{Header, Search}
import models.{MHands, MHand}
import play.twirl.api.{HtmlFormat, Html, Template1}
import views.html.sc.hdr._
import views.html.sc.svg._

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 08.05.15 15:23
 * Description: Модель возможных кнопок в заголовке.
 */
object ScHdrBtns extends Enumeration with EnumValue2Val {

  /** Интерфейс экземпляра модели. */
  protected sealed trait ValT {
    def strId: String

    /** Рендер svg-верстки. */
    def renderSvg(fgColor: String): Html

    /** Дополнительные css для div-обертки. */
    def divCss: List[String] = Nil

    /** DOM id. */
    def domId: String

    /** Выравнивание. */
    def align: MHand

    /** Использовать css-класс "abs" для выравнивания? */
    def abs: Boolean = true

    /** Рендер кнопки вместе с div'ом. */
    def apply(args: IhBtnArgs): Html
  }


  /** Если используется рендер через svg-шаблон, то можно этот трейт заюзать для укорачивания кода. */
  protected sealed trait SvgTplVal extends ValT {
    /** SVG-шаблон для рендера. */
    def svgTpl: Template1[String, Html]

    /** Рендер svg-верстки. */
    override def renderSvg(fgColor: String): Html = {
      svgTpl.render(fgColor)
    }
  }


  /** Экземляр модели. */
  protected abstract sealed class Val(val strId: String) extends super.Val(strId) with ValT {

    override def toString() = strId

    override def apply(args: IhBtnArgs): Html = {
      _hdrBtnTpl(this, args)
    }
  }

  /** Есть несколько внешне одинаковых кнопок "назад". */
  protected sealed trait BackLeftSvgVal extends SvgTplVal with LeftBtnT {
    override def svgTpl = _backArrowLeft
  }
  
  protected sealed trait TileBtn extends SvgTplVal {
    override def svgTpl = _indexAdsButtonTpl
  }

  override type T = Val

  protected sealed trait LeftBtnT extends ValT {
    override def align  = MHands.Left
  }

  protected sealed trait TopLeftMainBtnT extends LeftBtnT {
    override def divCss = "sm-producer-header_geo-button" :: super.divCss
  }

  /** Кнопка отображения геосписка узлов для переключения между ними. */
  val NavPanelOpen: T = new Val("a") with SvgTplVal with TopLeftMainBtnT {
    override def svgTpl = _geoFillTpl
    override def domId: String = "smGeoScreenButton"
  }


  /** 2015.may.8: Кнопка "назад" для перехода узел верхнего уровня. */
  val Back2UpperNode: T = new Val("b") with BackLeftSvgVal with TopLeftMainBtnT {
    override def domId  = Header.PREV_NODE_BTN_ID
  }

  /** Прозрачная кнопка для сворачивания панели категорий. */
  val CatsIndexTransparent: T = new Val("c") {
    override def renderSvg(fgColor: String) = HtmlFormat.empty
    override def align  = MHands.Left
    override def domId  = "smCategoriesIndexButton"
  }

  /** Правая кнопка со стрелкой для сворачивания списка категорий. */
  val CatsPanelClose: T = new Val("d") with SvgTplVal {
    override def svgTpl = _backArrowRight
    override def align  = MHands.Right
    override def domId  = Search.HIDE_PANEL_BTN_ID
  }

  /** Когда раскрыта панель геонавигации (левая), то кнопка сворачивания панели назад слева. */
  val NavPanelClose: T = new Val("e") with BackLeftSvgVal {
    override def domId  = "smGeoScreenCloseButton"
  }

  /** Кнопка выхода из выдачи. Наверное будет выпилена в будущем. */
  val ScExit: T = new Val("f") with SvgTplVal {
    override def svgTpl = _exitButtonTpl
    override def align  = MHands.Left
    override def domId  = "smExitButton"
    override def divCss = "sm-producer-header_exit-button" :: super.divCss
  }

  /** Кнопка отображения основной выдачи карточек. Обычно скрыта другой кнопкой. */
  val ShowIndex: T = new Val("g") with TileBtn {
    override def svgTpl = _indexAdsButtonTpl
    override def align  = MHands.Left
    override def domId  = Header.SHOW_INDEX_BTN_ID
    override def divCss = "sm-producer-header_index-button" :: super.divCss
  }

  /** Поисковая кнопка в правом углу экрана: категории, магазины, полнотекстовый поиск. */
  val SearchPanelOpen: T = new Val("h") with SvgTplVal {
    override def svgTpl = _navLayerButtonTpl
    override def align  = MHands.Right
    override def domId  = Search.SHOW_PANEL_BTN_ID
    override def divCss = "sm-producer-header_search-button" :: super.divCss
  }

  /** Кнопка закрытия раскрытой карточки. */
  val CloseFocused: T = new Val("i") with BackLeftSvgVal {
    override def domId  = "closeFocusedAdsButton"
    override def divCss = "sm-producer-header_exit-button" :: super.divCss
  }

  /** Кнопка перехода на выдачу продьюсера. */
  val GoToNodeTile: T = new Val("j") with TileBtn {
    override def domId = Header.GO_TO_PRODUCER_INDEX_BTN_ID
    override def align = MHands.Right
  }

}
