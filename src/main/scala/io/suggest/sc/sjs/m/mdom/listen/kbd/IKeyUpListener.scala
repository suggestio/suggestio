package io.suggest.sc.sjs.m.mdom.listen.kbd

import org.scalajs.dom._

/** Если контроллер должен принимать события клавиатуры, то нужно реализовать этот интерфейс. */
trait IKeyUpListener {

  def handleKeyUp(e: KeyboardEvent): Unit

}
