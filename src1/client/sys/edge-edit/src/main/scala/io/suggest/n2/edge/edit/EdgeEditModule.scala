package io.suggest.n2.edge.edit

import com.softwaremill.macwire._
import io.suggest.n2.edge.edit.v.EdgeEditFormR
import io.suggest.n2.edge.edit.v.inputs.info.{InfoFlagR, InfoTextNiR}
import io.suggest.n2.edge.edit.v.inputs.{NodeIdsR, OrderR, PredicateR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:48
  * Description: DI-module для формы файлозаливки.
  */
class EdgeEditModule {

  import io.suggest.ReactCommonModule._

  // view
  lazy val predicateR = wire[PredicateR]
  lazy val edgeEditFormR = wire[EdgeEditFormR]
  lazy val nodeIdsR = wire[NodeIdsR]
  lazy val infoFlagR = wire[InfoFlagR]
  lazy val orderR = wire[OrderR]
  lazy val infoTextNiR = wire[InfoTextNiR]


  // circuit

  lazy val edgeEditCircuit = wire[EdgeEditCircuit]

}
