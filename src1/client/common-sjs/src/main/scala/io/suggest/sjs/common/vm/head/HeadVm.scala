package io.suggest.sjs.common.vm.head

import io.suggest.sjs.common.vm.IVm
import io.suggest.sjs.common.vm.find.IApplyEl
import io.suggest.sjs.dom2.DomListIterator
import org.scalajs.dom.raw.HTMLHeadElement

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 12.09.16 15:01
  * Description: VM'ка для head-тега.
  * Получить инстансы можно с помощью [[io.suggest.sjs.common.vm.doc.DocumentVm]].
  */
object HeadVm extends IApplyEl {
  override type T = HeadVm
  override type Dom_t = HTMLHeadElement
}

import HeadVm.Dom_t


/** Класс для инстансов head vm. */
case class HeadVm(override val _underlying: Dom_t) extends IVm {

  override type T = Dom_t

  /** Вернуть link-теги. */
  def links: Iterator[HeadLink] = {
    DomListIterator( _underlying.getElementsByTagName("link") )
      .flatMap( HeadLink.ofNodeUnsafe )
  }

}
