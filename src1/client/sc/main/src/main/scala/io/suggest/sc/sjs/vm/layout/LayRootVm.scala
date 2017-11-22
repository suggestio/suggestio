package io.suggest.sc.sjs.vm.layout

import io.suggest.sc.ScConstants.Layout
import io.suggest.sjs.common.vm.child.{ChildElOrFind, SubTagFind}
import io.suggest.sjs.common.vm.create.CreateDivWithId
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.content.EraseBg
import io.suggest.sjs.common.vm.doc.DocumentVm
import io.suggest.sjs.common.vm.find.FindDiv
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 9:50
 * Description: ViewModel для взаимодействия с layout-контейнером верхнего уровня.
 * Пришла на смену LayoutView и MLayoutDom.
 */
object LayRootVm extends FindDiv with CreateLayRootDiv {

  override type T = LayRootVm
  override def DOM_ID = Layout.ROOT_ID


  /** Создать новый элемент в памяти (вне DOM). Созданный layout можно сохранить через вызов .insertIntoDom() */
  def createNew(): LayRootVmT = {
    val el = createNewEl()
    val contentVm = LayContentVm.createNew()
    el.appendChild( contentVm._underlying )
    new LayRootVmT {
      override def _underlying = el
      override def content: Option[LayContentVm] = Some(contentVm)
    }
  }

}

sealed trait CreateLayRootDiv extends CreateDivWithId {
  abstract override protected def createNewEl(): HTMLDivElement = {
    val el = super.createNewEl()
    el.setAttribute("class", Layout.ROOT_CSS_CLASS)
    el
  }
}


/** Логика функционирования экземпляра вынесена сюда для возможности разных реализация динамической модели. */
trait LayRootVmT extends VmT with SubTagFind with ChildElOrFind with EraseBg {

  override type T = HTMLDivElement

  // Поиск единственного субтега: content.
  override protected type SubtagCompanion_t = LayContentVm.type
  override type SubTagVm_t                  = LayContentVm.T
  override protected type SubTagEl_t        = LayContentVm.Dom_t
  override protected def _subtagCompanion   = LayContentVm

  /** Найти content-div (sioMartLayout). */
  def content = _findSubtag()

  /** Затолкать в DOM этот model view. Используется при ручном создании layout'а. */
  def insertIntoDom(): Unit = {
    DocumentVm()
      .body
      .appendChild( _underlying )
  }

}


/** Дефолтовая реализация экземпляра модели [[LayRootVmT]]. */
case class LayRootVm(
  override val _underlying: HTMLDivElement
)
  extends LayRootVmT
{

  override lazy val content = super.content

}

