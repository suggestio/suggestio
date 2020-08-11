package io.suggest.lk.nodes.form

import com.softwaremill.macwire._
import io.suggest.lk.nodes.form.r.LkNodesFormR
import io.suggest.lk.nodes.form.r.menu.{NodeMenuBtnR, NodeMenuR}
import io.suggest.lk.nodes.form.r.pop.{CreateNodeR, EditTfDailyR, LknPopupsR}
import io.suggest.lk.nodes.form.r.tree.{NodeR, TreeR}

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 30.03.18 11:48
  * Description: compile-time DI линковка react LkNodes-формы.
  */
class LkNodesModule {

  import io.suggest.ReactCommonModule._

  lazy val lkNodesFormCircuit = wire[LkNodesFormCircuit]

  lazy val lkNodesFormR = wire[LkNodesFormR]

  lazy val lknPopupsR = wire[LknPopupsR]

  lazy val treeR = wire[TreeR]

  lazy val nodeR = wire[NodeR]

  lazy val editTfDailyR = wire[EditTfDailyR]

  lazy val createNodeR = wire[CreateNodeR]

  lazy val nodeMenuR = wire[NodeMenuR]

  lazy val nodeMenuBtnR = wire[NodeMenuBtnR]

}
