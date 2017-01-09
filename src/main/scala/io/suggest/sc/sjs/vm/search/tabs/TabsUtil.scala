package io.suggest.sc.sjs.vm.search.tabs

import io.suggest.sjs.common.vm.height3.SetHeight3
import io.suggest.sc.sjs.vm.util.OnClick
import io.suggest.sjs.common.model.browser.IBrowser
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.child.{ContentElT, WrapperChildContent}
import io.suggest.sjs.common.vm.find.FindDiv
import io.suggest.sjs.common.vm.style.{SetIsShown, ShowHideDisplayT}
import io.suggest.sjs.common.vm.util.IInitLayout
import org.scalajs.dom.raw.HTMLDivElement
import io.suggest.sc.ScConstants.Search.TAB_BTN_INACTIVE_CSS_CLASS
import io.suggest.sc.sjs.c.search.SearchFsm
import io.suggest.sc.sjs.m.msearch.MTab
import io.suggest.sjs.common.fsm.{InitOnClickToFsmT, SjsFsm}

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
trait TabRoot extends SetHeight3 with IInitLayout with ShowHideDisplayT with SetIsShown {

  override type T = HTMLDivElement
  //override protected type SubtagCompanion_t <: TabWrapperCompanion
  override type SubTagVm_t <: TabWrapper

  def adjust(tabHeight: Int, browser: IBrowser): Unit = {
    _setHeight3(tabHeight, browser)
  }

  def mtab: MTab

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
trait TabBtn extends VmT
  with InitOnClickToFsmT
  with OnClick
{

  override type T = HTMLDivElement

  override protected def FSM: SjsFsm = SearchFsm

  /** Визуальная активация текущей вкладки. */
  def activate(): Unit = {
    removeClass(TAB_BTN_INACTIVE_CSS_CLASS)
  }

  /** Визуальная активация текущей вкладки. */
  def deactivate(): Unit = {
    addClasses(TAB_BTN_INACTIVE_CSS_CLASS)
  }

}
