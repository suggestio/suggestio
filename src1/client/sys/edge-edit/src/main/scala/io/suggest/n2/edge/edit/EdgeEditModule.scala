package io.suggest.n2.edge.edit

import com.softwaremill.macwire._
import io.suggest.n2.edge.edit.v._
import io.suggest.n2.edge.edit.v.inputs.act._
import io.suggest.n2.edge.edit.v.inputs.info._
import io.suggest.n2.edge.edit.v.inputs.media._
import io.suggest.n2.edge.edit.v.inputs._

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 17.12.2019 9:48
  * Description: DI-module для формы файлозаливки.
  */
class EdgeEditModule {

  import io.suggest.ReactCommonModule._
  import io.suggest.sc.ScCommonModule._

  // edge
  lazy val predicateR = wire[PredicateR]
  lazy val nodeIdsR = wire[NodeIdsR]
  lazy val orderR = wire[OrderR]

  // edge.info
  lazy val edgeInfoR = wire[EdgeInfoR]
  lazy val flagR = wire[FlagR]
  lazy val textNiR = wire[TextNiR]
  lazy val extServiceR = wire[ExtServiceR]
  lazy val osFamilyR = wire[OsFamilyR]

  // edge.media
  lazy val mediaR = wire[MediaR]

  // act
  lazy val deleteBtnR = wire[DeleteBtnR]
  lazy val deleteDiaR = wire[DeleteDiaR]
  lazy val saveBtnR = wire[SaveBtnR]
  lazy val errorDiaR = wire[ErrorDiaR]
  lazy val fileExistDiaR = wire[FileExistDiaR]
  lazy val nodeLinkR = wire[NodeLinkR]

  // circuit
  lazy val edgeEditFormR = wire[EdgeEditFormR]
  lazy val edgeEditCircuit = wire[EdgeEditCircuit]

}
