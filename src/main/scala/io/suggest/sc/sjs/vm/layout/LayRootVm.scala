package io.suggest.sc.sjs.vm.layout

import io.suggest.sc.ScConstants.Layout
import io.suggest.sc.sjs.vm.util.domget.GetDivById
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 9:50
 * Description: ViewModel для взаимодействия с layout-контейнером верхнего уровня.
 */
object LayRootVm extends GetDivById {

  /** Создать модель из DOM, если возможно. */
  def find(): Option[LayRootVm] = {
    getDivById(Layout.ROOT_ID)
      .map(LayRootVm.apply)
  }

}


/** Логика функционирования экземпляра вынесена сюда для возможности разных реализация динамической модели. */
trait LayRootVmT extends SafeElT {

  override type T = HTMLDivElement

  def content: Option[LayContentVm] = {
    Option( _underlying.firstChild.asInstanceOf[HTMLDivElement] )
      .filter { _.id == Layout.LAYOUT_ID }
      .map {  }
  }

}


/** Дефолтовая реализация экземпляра модели [[LayRootVmT]]. */
case class LayRootVm(
  override val _underlying: HTMLDivElement
) extends LayRootVmT
