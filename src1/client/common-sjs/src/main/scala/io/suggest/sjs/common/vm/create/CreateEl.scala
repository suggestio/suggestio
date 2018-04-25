package io.suggest.sjs.common.vm.create

import io.suggest.sjs.common.vm.find.{IApplyEl, TypeDomT}
import io.suggest.sjs.common.vm.util.DomId
import org.scalajs.dom
import org.scalajs.dom.Element
import org.scalajs.dom.raw.HTMLDivElement

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 23.06.15 15:30
 * Description: Аддоны для быстрой сборки методов создания новых DOM-элементов под нужды ViewModel'ей.
 */
trait CreateEl extends TypeDomT {
  
  protected def TAG_NAME: String

  /** Собрать новый элемент. */
  protected def createNewEl(): Dom_t = {
    dom.document.createElement(TAG_NAME)
      .asInstanceOf[Dom_t]
  }

}


/** Сборка div'ов. */
trait CreateDiv extends CreateEl {

  override protected def TAG_NAME = "div"
  override type Dom_t = HTMLDivElement

}

