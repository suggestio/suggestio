package io.suggest.sc.sjs.vm.grid

import io.suggest.sjs.common.vm.height3.SetHeight3Raw
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.child.{ContentElT, SubTagFind}
import io.suggest.sjs.common.vm.find.FindDiv
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Grid
import io.suggest.sc.sjs.m.msc.IScSd

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 24.06.15 15:08
 * Description: ViewModel корневого DIV'а выдачи-сетки карточек.
 *
 * Цепочка из root-wrapper-content используется для решения возможных проблем со скроллингом и
 * скроллбаром в некоторых браузерах.
 */
object GRoot extends FindDiv {

  override def DOM_ID = Grid.ROOT_DIV_ID
  override type T = GRoot

}


/** Логика и интерфейс экземпляра модели. */
trait GRootT extends VmT with SubTagFind with SetHeight3Raw {

  override type T = HTMLDivElement


  override protected type SubtagCompanion_t = GWrapper.type
  override type SubTagVm_t                  = GWrapper.T
  override protected type SubTagEl_t        = GWrapper.Dom_t
  override protected def _subtagCompanion   = GWrapper
  override protected type ContentVm_t       = GContent

  override protected[this] def __getContentDiv(content: Option[ContentVm_t]): Option[ContentElT] = {
    content.flatMap(_.container)
  }

  def reInitLayout(sd: IScSd): Unit = {
    val height = sd.common.screen.height
    _setHeight3(height, sd.common.browser)
  }

  /** Раняя инициализация, которая должна проходить однократно.
    * Используется после создания нового layout'а. */
  def initLayout(sd: IScSd): Unit = {
    reInitLayout(sd)
    for (gwrapper <- wrapper) {
      gwrapper.initLayout(sd)
    }
  }

  /** Отображать размыто. */
  def blur(): Unit = {
    addClasses( Grid.BLURRED_CLASS )
  }

  /** Убрать размывку */
  def unblur(): Unit = {
    removeClass( Grid.BLURRED_CLASS )
  }

}


/**
 * Экземпляр модели; дефолтовая реализация [[GRootT]].
  *
  * @param _underlying Соответствующий этой модели DOM div.
 */
case class GRoot(
  override val _underlying: HTMLDivElement
)
  extends GRootT {

  override lazy val wrapper = super.wrapper

}
