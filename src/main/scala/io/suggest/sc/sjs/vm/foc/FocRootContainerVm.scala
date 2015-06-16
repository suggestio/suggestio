package io.suggest.sc.sjs.vm.foc

import io.suggest.sc.sjs.m.mdom.GetDivById
import io.suggest.sc.sjs.v.vutil.{SetStyleDisplay, VUtil}
import io.suggest.sc.sjs.vm.util.cont.{ShowHide, ContainerT}
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Focused.ROOT_ID

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 16.06.15 15:07
 * Description: ViewModel для взаимодейстия с корневым контейнером для выдачи focused ads.
 */

object FocRootContainerVm extends GetDivById {

  def apply(): FocRootContainerVm = {
    apply( getDivById(ROOT_ID).get )
  }

  def newSubContainer(): HTMLDivElement = {
    VUtil.newDiv()
  }

}


import FocRootContainerVm._


case class FocRootContainerVm(_underlying: HTMLDivElement)
  extends ContainerT
  with ShowHide
{

  override type T = HTMLDivElement

  /** Хелпер для дедубликации кода для методов создания суб-контейнеров. */
  protected def _newSubCont(f: HTMLDivElement => Unit): HTMLDivElement = {
    val subdiv = newSubContainer()
    f(subdiv)
    subdiv
  }

  /**
   * Создать новый субконтейнер в качестве первого child-элемента.
   * @return Созданный div.
   */
  def newHeadSubContainer(): HTMLDivElement = {
    _newSubCont { subdiv =>
      _underlying.insertBefore(subdiv, _underlying.firstChild)
    }
  }

  /**
   * Создать новый субконтейнер в конце child-списка.
   * @return Созданный div.
   */
  def newTailSubContainer(): HTMLDivElement = {
    _newSubCont { subdiv =>
      _underlying.appendChild(subdiv)
    }
  }

}
