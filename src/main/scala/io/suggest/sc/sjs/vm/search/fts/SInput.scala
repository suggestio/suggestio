package io.suggest.sc.sjs.vm.search.fts

import io.suggest.sc.ScConstants.Search.Fts.INPUT_ID
import io.suggest.sc.sjs.m.msearch.{FtsFieldBlur, FtsFieldKeyUp, FtsFieldFocus}
import io.suggest.sc.sjs.vm.util.{FindUsingAttachedEventT, OnEventToFsmUtilT}
import io.suggest.sjs.common.vm.VmT
import io.suggest.sjs.common.vm.find.FindElT
import io.suggest.sjs.common.vm.util.IInitLayout
import org.scalajs.dom.raw.{HTMLDivElement, HTMLInputElement}

/**
 * Suggest.io
 * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
 * Created: 06.08.15 14:29
 * Description: VM поля полнотекстового поиска.
 */
object SInput extends FindElT with FindUsingAttachedEventT {

  override type Dom_t = HTMLInputElement
  override type T = SInput
  override def DOM_ID: String = INPUT_ID

}


/** Логика экземпляра модели поискового input'а в этом трейте. */
trait SInputT extends VmT with IInitLayout with OnEventToFsmUtilT {
  override type T = HTMLInputElement

  override def initLayout(): Unit = {
    _addToFsmEventListener("keyup", FtsFieldKeyUp)
    _addToFsmEventListener("focus", FtsFieldFocus)
    _addToFsmEventListener("blur",  FtsFieldBlur)
    // TODO Нужно отрабатывать CTRL+V или иной копипаст мышкой (X11/Xorg middle click paste) в поле!
  }

  def setText(s: String): Unit = {
    _underlying.value = s
  }

  def getText: String = _underlying.value

  def getNormalized: String = {
    getText.trim
  }

  def container: Option[SInputContainer] = {
    val contOpt = for {
      wrapperNode   <- Option(_underlying.parentNode)
      containerNode <- Option(wrapperNode.parentNode)
    } yield {
      val contEl = containerNode.asInstanceOf[HTMLDivElement]
      SInputContainer(contEl)
    }
    contOpt orElse { SInputContainer.find() }
  }

}


/** Реализация модели поискового инпута. */
case class SInput(
  override val _underlying: HTMLInputElement
)
  extends SInputT
