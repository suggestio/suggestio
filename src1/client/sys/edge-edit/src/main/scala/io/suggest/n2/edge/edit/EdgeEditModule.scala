package io.suggest.n2.edge.edit

import com.softwaremill.macwire._
import io.suggest.n2.edge.edit.v.EdgeEditFormR
import io.suggest.n2.edge.edit.v.inputs.PredicateEditR

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:48
  * Description: DI-module для формы файлозаливки.
  */
class EdgeEditModule {

  import io.suggest.ReactCommonModule._

  // view
  lazy val predicateEditR = wire[PredicateEditR]

  lazy val edgeEditFormR = wire[EdgeEditFormR]


  // circuit

  lazy val edgeEditCircuit = wire[EdgeEditCircuit]

}
