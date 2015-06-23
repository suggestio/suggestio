package io.suggest.sc.sjs.vm.layout

import io.suggest.sc.ScConstants.Layout
import io.suggest.sc.sjs.v.vutil.SetStyleDisplay
import io.suggest.sc.sjs.vm.SafeDoc
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.sjs.vm.util.domvm.create.CreateDiv
import io.suggest.sjs.common.model.dom.DomListIterator
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 9:50
 * Description: ViewModel для взаимодействия с layout-контейнером верхнего уровня.
 * Пришла на смену LayoutView и MLayoutDom.
 */
object LayRootVm extends FindDiv with CreateLayRootDiv {

  override type T = LayRootVm
  override protected def DOM_ID = Layout.ROOT_ID


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

sealed trait CreateLayRootDiv extends CreateDiv {
  abstract override protected def createNewEl(): HTMLDivElement = {
    val el = super.createNewEl()
    el.setAttribute("class", Layout.ROOT_CSS_CLASS)
    el
  }
}

/** Логика функционирования экземпляра вынесена сюда для возможности разных реализация динамической модели. */
trait LayRootVmT extends SafeElT {

  override type T = HTMLDivElement

  /** Найти content-div (sioMartLayout). */
  def content: Option[LayContentVm] = {
    DomListIterator( _underlying.children )
      .find { _.id == Layout.LAYOUT_ID }
      .map { node => LayContentVm( node.asInstanceOf[HTMLDivElement] ) }
      .orElse { LayContentVm.find() }
  }

  /** Затолкать в DOM этот model view. Используется при ручном создании layout'а. */
  def insertIntoDom(): Unit = {
    SafeDoc.body
      .appendChild( _underlying )
  }

  /** Из-за прозрачностей нужно очистить фон до блеска после приветствия. */
  def eraseBg(): Unit = {
    _erase( SafeDoc.body )
    _erase(_underlying)
  }

  private def _erase(el: HTMLElement): Unit = {
    el.style.backgroundColor = "#ffffff"
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

