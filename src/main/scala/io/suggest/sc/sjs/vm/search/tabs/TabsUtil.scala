package io.suggest.sc.sjs.vm.search.tabs

import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.sjs.vm.util.domvm.get.ChildElOrFind
import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 14:43
 * Description: Утиль для полиморфной сборки моделей табов.
 */

/** Трейт объекта-компаньона root-div табов. */
trait TabCompanion extends FindDiv {

  override type T <: TabRoot

}


/** Трейт экземпляра модели div'а корневого таба поисковой вкладки. */
trait TabRoot extends ISafe with ChildElOrFind {

  override type T <: HTMLDivElement

  override type SubTagVm_t <: TabWrapper

  def wrapper: Option[SubTagVm_t] = _findSubtag()

}


trait TabWrapper extends ChildElOrFind {

  override type T <: HTMLDivElement

  override type SubTagVm_t <: TabContent

  def content: Option[SubTagVm_t] = _findSubtag()

}


trait TabContent extends ISafe {

  override type T <: HTMLDivElement

}
