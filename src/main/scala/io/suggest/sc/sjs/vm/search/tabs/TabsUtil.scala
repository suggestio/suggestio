package io.suggest.sc.sjs.vm.search.tabs

import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.util.{IInitLayout, InitOnClickToFsmT}
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.sjs.vm.util.domvm.get.ChildElOrFind
import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.raw.{HTMLElement, HTMLDivElement}

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
trait TabRoot extends ISafe with ChildElOrFind with IInitLayout {

  override type T <: HTMLDivElement

  override type SubTagVm_t <: TabWrapper

  def wrapper: Option[SubTagVm_t] = _findSubtag()

  def adjust(tabHeight: Int): Unit = {
    // Кешируем анонимную фунцкию экстракции underlying-тегов между несколькими вызовами.
    val underF = ISafe.extractorF[HTMLElement]
    val _wrapper = wrapper
    // Достаём content container
    val containerOpt = _wrapper
      .flatMap(_.content)
      .map(underF)
    // Подбираем заворачивающие content div теги.
    val wrappersIter = (this :: _wrapper.toList)
      .iterator
      .map(underF)
    // Выставляем высоты для всех этих вещей.
    VUtil.setHeightRootWrapCont(tabHeight, containerOpt, wrappersIter)
  }

}


/** Трейт для vm'ок tab wrapper. */
trait TabWrapper extends ChildElOrFind {

  override type T <: HTMLDivElement

  override type SubTagVm_t <: TabContent

  def content: Option[SubTagVm_t] = _findSubtag()

}


/** Трейт для vm'ок tab content. */
trait TabContent extends ISafe {

  override type T <: HTMLDivElement

}


/** Трейт для vm'ки тела таба. */
trait TabBtn extends InitOnClickToFsmT {

  override type T <: HTMLDivElement

}
