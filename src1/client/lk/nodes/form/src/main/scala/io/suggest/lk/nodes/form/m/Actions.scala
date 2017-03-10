package io.suggest.lk.nodes.form.m

import io.suggest.adv.rcvr.{IRcvrKey, RcvrKey}
import io.suggest.lk.nodes.{MLknNode, MLknNodeResp}
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
case class AddSubNodeResp( rcvrKey: RcvrKey, tryResp: Try[MLknNode] )
  extends LkNodesAction
  with IRcvrKey


/** Добавление под-узла: юзер нажал "отмена". */
case class AddSubNodeCancelClick(
                                  override val rcvrKey: RcvrKey
                                )
  extends LkNodesAction
  with IRcvrKey


/** Сигнал о клике по галочке узла. */
case class NodeIsEnabledChanged(
                                 override val rcvrKey : RcvrKey,
                                 isEnabled            : Boolean
                               )
  extends LkNodesAction
  with IRcvrKey


/** Сигнал ответа сервера на апдейт флага isEnabled. */
case class NodeIsEnabledUpdateResp(
                                    override val rcvrKey : RcvrKey,
                                    resp                 : Try[MLknNode]
                                  )
  extends LkNodesAction
  with IRcvrKey


/** Клик по кнопке редактирования узла в дереве узлов. */
case class NodeEditClick( override val rcvrKey: RcvrKey)
  extends LkNodesAction
  with IRcvrKey

/** Клик по кнопке удаления узла. */
case class NodeDeleteClick( override val rcvrKey: RcvrKey )
  extends LkNodesAction
  with IRcvrKey

/** Клик по кнопке подтверждения удаления узла. */
case class NodeDeleteOkClick( override val rcvrKey: RcvrKey )
  extends LkNodesAction
  with IRcvrKey

/** Результат запроса к серверу по поводу удаления узла. */
case class NodeDeleteResp( override val rcvrKey: RcvrKey,
                           resp: Try[Boolean]
                         )
  extends LkNodesAction
  with IRcvrKey

/** Клик по кнопке отмены удаления узла. */
case class NodeDeleteCancelClick( override val rcvrKey: RcvrKey )
  extends LkNodesAction
  with IRcvrKey
