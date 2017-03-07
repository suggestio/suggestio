package io.suggest.lk.nodes.form.m

import io.suggest.adv.rcvr.{IRcvrKey, RcvrKey}
import io.suggest.lk.nodes.{ILknTreeNode, MLknNodeResp}
import io.suggest.sjs.common.spa.DAction

import scala.util.Try

/**
  * Suggest.io
  * User: Konstantin Nikiforov <konstantin.nikiforov@cbca.ru>
  * Created: 08.02.17 21:48
  * Description: Экшены формы управления узлами.
  */
sealed trait LkNodesAction extends DAction


/** Юзер кликнул по узлу, необходимо развернуть узел. */
case class NodeNameClick(
                          override val rcvrKey: RcvrKey
                        )
  extends LkNodesAction
  with IRcvrKey


/** Модель результата запроса к серверу по поводу под-узлов для указанного узла. */
case class HandleSubNodesOf(
                             override val rcvrKey   : RcvrKey,
                             subNodesRespTry        : Try[MLknNodeResp]
                           )
  extends LkNodesAction
  with IRcvrKey


/** Юзер кликнул по кнопке добавления подузла.
  *
  * @param rcvrKey Ключ родительского узла.
  */
case class AddSubNodeClick(
                            override val rcvrKey: RcvrKey
                          )
  extends LkNodesAction
  with IRcvrKey


/** Добавление под-узла: Юзер вводит название узла (маячка). */
case class AddSubNodeNameChange(
                                 override val rcvrKey: RcvrKey,
                                 name: String
                               )
  extends LkNodesAction
  with IRcvrKey


/** Добавление под-узла: Юзер вводит id узла (маячка). */
case class AddSubNodeIdChange(
                               override val rcvrKey : RcvrKey,
                               id                   : String
                             )
  extends LkNodesAction
  with IRcvrKey


/** Добавление под-узла: Юзер нажимает "сохранить". */
case class AddSubNodeSaveClick(
                                override val rcvrKey: RcvrKey
                              )
  extends LkNodesAction
  with IRcvrKey


/** Среагировать на ответ сервера по поводу успешного добавления нового узла. */
case class AddSubNodeResp( rcvrKey: RcvrKey, tryResp: Try[ILknTreeNode] )
  extends LkNodesAction
  with IRcvrKey


/** Добавление под-узла: юзер нажал "отмена". */
case class AddSubNodeCancelClick(
                                  override val rcvrKey: RcvrKey
                                )
  extends LkNodesAction
  with IRcvrKey
