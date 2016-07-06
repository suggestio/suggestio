package io.suggest.sc.sjs.vm.foc.fad

import io.suggest.sc.sjs.m.magent.IMScreen
import io.suggest.sc.ScConstants.Focused.FAd.MAD_ID_ATTR
import io.suggest.sc.sjs.m.msc.IScCommon
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.content.{ApplyFromOuterHtml, GetInnerHtml, OuterHtml}
import io.suggest.sjs.common.vm.height3.SetHeight3Raw
import io.suggest.sjs.common.vm.of.OfDiv
import io.suggest.sjs.common.vm.style.{StyleLeft, StylePosition, StyleWidth}
import io.suggest.sjs.common.vm.util.OfHtmlElDomIdRelated

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 12.08.15 16:26
 * Description: Ячейка в карусели focused-выдачи, является одновременно корневым div'ом каждой
 * focused-карточки, пришедшей от сервера.
 * Создаётся как контейнер, позиционируется, заполняется контентом, прицепляется к focused-карусели.
 */

object FAdRoot
  extends FAdStatic
    with ApplyFromOuterHtml
    with OfDiv
    with OfHtmlElDomIdRelated
{

  override type T = FAdRoot
  override type DomIdArg_t = String

  /** Сборка focused root-vm из сырого HTML. */
  override def apply(outerHtml: String): FAdRoot = {
    val vm = super.apply(outerHtml)
    vm.positionAbsolute()
    vm
  }

  /** Приведение параметров экрана к ширине ячейки focused-выдачи. */
  def screen2cellWidth(screen: IMScreen): Int = {
    screen.width
  }

}


import FAdRoot.{Dom_t, screen2cellWidth}


trait FAdRootT
  extends VmT
    with StyleWidth
    with StyleLeft
    with StylePosition
    with _FAdFindSubtag
    with SetHeight3Raw
    with GetInnerHtml
    with OuterHtml
{

  override type T = Dom_t

  // protected -> public
  override def setLeftPx(leftPx: Int) = super.setLeftPx(leftPx)

  /** Выставить значение left в ячейках focused-выдачи. Т.к. в ширинах экрана. */
  def setLeft(cell: Int, screen: IMScreen): Unit = {
    setLeftPx(cell * screen2cellWidth(screen))
  }

  def initLayout(scc: IScCommon): Unit = {
    setWidthPx( screen2cellWidth(scc.screen) )
    _setHeight3( scc.screen.height, scc.browser )
  }

  /** id рекламной карточки, которая отрендерена внутри. */
  def madId = getAttribute( MAD_ID_ATTR )

  override type SubTagVm_t = FAdWrapper
  override protected type ContentVm_t = FAdContent
  override protected type SubTagEl_t = FAdWrapper.Dom_t
  override protected def _subtagCompanion = FAdWrapper
  override protected type SubtagCompanion_t = FAdWrapper.type

}


case class FAdRoot(
  override val _underlying: Dom_t
)
  extends FAdRootT
{
  override lazy val wrapper = super.wrapper
}
