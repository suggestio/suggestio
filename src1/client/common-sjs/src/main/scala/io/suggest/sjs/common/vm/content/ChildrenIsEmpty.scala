package io.suggest.sjs.common.vm.content

import io.suggest.common.empty.NonEmpty
import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.Node

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 13.07.16 18:00
  * Description: Поддержка методов isEmpty/nonEmpty, сообщающих о наличии или отсутствии дочерних узлов у данной ноды.
  */
trait ChildrenIsEmpty extends IVm with NonEmpty {

  override type T <: Node

  override def isEmpty = _underlying.firstChild == null

}
