package io.suggest.sc.sjs.vm.search.tabs

import io.suggest.sc.sjs.vm.util.height3.SetHeight3
import io.suggest.sc.sjs.vm.util.{IInitLayout, InitOnClickToFsmT}
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sc.sjs.vm.util.domvm.get.{ContentElT, WrapperChildContent}
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.view.safe.SafeElT
import io.suggest.sjs.common.view.safe.display.ShowHideDisplayEl
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Search.TAB_BTN_INACTIVE_CSS_CLASS

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 05.08.15 14:43
 * Description: Утиль для полиморфной сборки моделей табов.
 */

/** Трейт объекта-компаньона root-div табов. */
trait TabRootCompanion extends FindDiv {

  override type T <: TabRoot

}


/** Трейт экземпляра модели div'а корневого таба поисковой вкладки. */
trait TabRoot extends SetHeight3 with IInitLayout with ShowHideDisplayEl {

  override type T = HTMLDivElement
  //override protected type SubtagCompanion_t <: TabWrapperCompanion
  override type SubTagVm_t <: TabWrapper

  def adjust(tabHeight: Int, browser: IBrowser): Unit = {
    _setHeight3(tabHeight, browser)
  }

}


trait TabWrapperCompanion extends FindDiv {
  override type T <: TabWrapper
}
/** Трейт для vm'ок tab wrapper. */
trait TabWrapper extends WrapperChildContent {

  override type T = HTMLDivElement

 // override protected type SubtagCompanion_t <: TabContentCompanion
  override type SubTagVm_t <: TabContent

}


trait TabContentCompanion extends FindDiv {
  override type T <: TabContent
}
/** Трейт для vm'ок tab content. */
trait TabContent extends ContentElT {

  override type T = HTMLDivElement

}


trait TabBtnCompanion extends FindDiv {
  override type T <: TabBtn
}

/** Трейт для vm'ки тела таба. */
trait TabBtn extends InitOnClickToFsmT with SafeElT {

  override type T = HTMLDivElement

  /** Визуальная активация текущей вкладки. */
  def activate(): Unit = {
    removeClass(TAB_BTN_INACTIVE_CSS_CLASS)
  }

  /** Визуальная активация текущей вкладки. */
  def deactivate(): Unit = {
    addClasses(TAB_BTN_INACTIVE_CSS_CLASS)
  }

}
