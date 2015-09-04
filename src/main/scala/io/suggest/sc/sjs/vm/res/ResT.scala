package io.suggest.sc.sjs.vm.res

import io.suggest.sc.sjs.vm.layout.LayContentVm
import io.suggest.sjs.common.view.VUtil
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.content.ClearT
import io.suggest.sjs.common.vm.create.{CreateVm, CreateDivWithId}
import io.suggest.sjs.common.vm.find.FindDiv
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

trait ResStaticT extends FindDiv with CreateDivWithId with CreateVm {

  override type T <: ResT

  protected def _insertDiv(lay: LayContentVm, div: HTMLDivElement): Unit

  def ensureCreated(): T = {
    find() getOrElse {
      val vm = createNew()
      val lay = LayContentVm.find()
        .get
      _insertDiv(lay, vm._underlying)
      vm
    }
  }

}


trait ResT extends VmT with ClearT {

  override type T = HTMLDivElement

  /** Добавить css-ресурс в контейнер. */
  def appendCss(css: String): Unit = {
    val tag = dom.document.createElement("style")
    tag.innerHTML = css
    _underlying.appendChild(tag)
  }

}
