package io.suggest.sc.sjs.vm.util.height3

import io.suggest.sc.sjs.v.vutil.VUtil
import io.suggest.sc.sjs.vm.util.domvm.get.{WrapperChildContent, ContentElT, RootChildWrapper}
import io.suggest.sjs.common.view.safe.ISafe
import org.scalajs.dom.raw.HTMLElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 07.08.15 16:57
 * Description: Есть места, где скроллинг имитируется через каскад из трёх вложенных div: root, wrapper, content.
 */
trait SetHeight3Raw extends RootChildWrapper {

  protected type ContentVm_t <: ContentElT

  override type SubTagVm_t <: WrapperChildContent { type SubTagVm_t <: ContentVm_t }

  /** Бывает нужно вместо content-div другой из него отковырять.
    * Можно сделать это перезаписав данный метод. */
  protected[this] def __getContentDiv(content: Option[ContentVm_t]): Option[ContentElT] = {
    content
  }

  protected def _setHeight3(height: Int): Unit = {
    // Кешируем анонимную функцию экстракции underlying-тегов между несколькими вызовами.
    val undF = ISafe.extractorF[HTMLElement]

    val wrapperOpt = wrapper
    val wrapperDivs = (this :: wrapperOpt.toList)
      .iterator
      .map(undF)

    val content1 = wrapperOpt.flatMap(_.content)
    val contentDivOpt = __getContentDiv(content1)
      .map(undF)

    VUtil.setHeightRootWrapCont(height, contentDivOpt, wrapperDivs)
  }

}


/** Обычно надо использовать данную реализацию SH3. В ней не надо шаманить с типом content-div,
  * и проблематично типобезопасно переопределить логику getContentDiv(). */
trait SetHeight3 extends SetHeight3Raw {
  override protected type ContentVm_t = ContentElT
}
