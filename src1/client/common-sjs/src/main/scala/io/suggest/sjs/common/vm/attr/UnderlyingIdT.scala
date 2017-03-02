package io.suggest.sjs.common.vm.attr

import io.suggest.primo.id.IId
import io.suggest.sjs.common.vm.IVm
import org.scalajs.dom.Element

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 07.10.16 17:56
  * Description: Проброс обязательного поля id в API vm'ки.
  * Если id не найден, то может быть null или пустая строка.
  */
trait UnderlyingIdT extends IVm with IId[String] {

  override type T <: Element

  override def id: String = {
    _underlying.id
  }

}


/** Проброс поля id с необязательным значением в API vm'ки. */
trait UnderlyingIdOptT extends IVm with IId[Option[String]] {

  override type T <: Element

  /** id элемента, если есть. Пустые строки и null отфильтровываются в None. */
  override def id: Option[String] = {
    Option( _underlying.id )
      .filter(_.nonEmpty)
  }

}
