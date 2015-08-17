package io.suggest.sc.sjs.vm.res

import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.layout.LayContentVm
import io.suggest.sc.sjs.vm.util.ClearT
import io.suggest.sc.sjs.vm.util.domvm.FindDiv
import io.suggest.sjs.common.view.safe.SafeElT
import org.scalajs.dom
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 21.05.15 15:51
 * Description: Resource-контейнеры используются для хранения и передачи заинлайненных ресурсов в DOM.
 * Например: style-теги со стилями полученной верстки.
 * Тут заготовка для приготовления ресурса.
 *
 * С точки зрения DOM, контейнеры -- это просто теги в начале body.
 */

trait ResStaticT extends FindDiv {

  override type T <: ResT

  protected def _insertDiv(lay: LayContentVm, div: HTMLDivElement): Unit
  
  def ensureCreated(): T = {
    find() getOrElse {
      val div = VUtil.newDiv()
      div.id = DOM_ID
      val lay = LayContentVm.find()
        .get
      _insertDiv(lay, div)
      apply(div)
    }
  }

}


trait ResT extends SafeElT with ClearT {

  override type T = HTMLDivElement

  /** Добавить css-ресурс в контейнер. */
  def appendCss(css: String): Unit = {
    val tag = dom.document.createElement("style")
    tag.innerHTML = css
    _underlying.appendChild(tag)
  }

}
