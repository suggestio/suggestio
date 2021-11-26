package io.suggest

import diode.react.ModelProxy
import io.suggest.react.ComponentFunctionR
import io.suggest.sc.model.MScRoot
import _root_.scalaz.Tag

package object adt {

  /** Type-tag for render function of menu row.
    * Used by macwire in DI code logic, to detect correct rendering function. */
  sealed trait ScMenuLkEnterItem
  def ScMenuLkEnterItem[A](a: A) = Tag[A, ScMenuLkEnterItem]( a )

  /** Type-tag for render function of sc.search.SearchMapR() react-component. */
  sealed trait ScSearchMap
  def ScSearchMap[A](a: A) = Tag[A, ScSearchMap](a)

}
