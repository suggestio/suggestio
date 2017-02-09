package io.suggest.lk.nodes.form.m

import io.suggest.adv.rcvr.RcvrKey
import io.suggest.sjs.common.spa.DAction

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 21:48
  * Description: Экшены форму управления узлами.
  */
sealed trait LkNodesAction extends DAction


/** Юзер кликнул по узлу, необходимо развернуть узел. */
case class NodeShow(rcvrKey: RcvrKey) extends LkNodesAction
