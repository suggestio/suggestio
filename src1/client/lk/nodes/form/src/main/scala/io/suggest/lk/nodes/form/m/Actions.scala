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

/** Экшен, связанный с деревом узлов. Он всега содержит поле rcvrKey. */
sealed trait LkNodesTreeAction
  extends LkNodesAction
  with IRcvrKey


/** Юзер кликнул по узлу, необходимо развернуть узел. */
case class NodeNameClick(
                          override val rcvrKey: RcvrKey
                        )
  extends LkNodesTreeAction


/** Модель результата запроса к серверу по поводу под-узлов для указанного узла. */
case class HandleSubNodesOf(
                             override val rcvrKey   : RcvrKey,
                             subNodesRespTry        : Try[MLknNodeResp]
                           )
  extends LkNodesTreeAction


/** Юзер кликнул по кнопке добавления подузла.
  *
  * @param rcvrKey Ключ родительского узла.
  */
case class AddSubNodeClick(
                            override val rcvrKey: RcvrKey
                          )
  extends LkNodesTreeAction


sealed trait LkNodesTreeNameAction extends LkNodesTreeAction {
  val name: String
}

/** Добавление под-узла: Юзер вводит название узла (маячка). */
case class AddSubNodeNameChange(
                                 override val rcvrKey: RcvrKey,
                                 name: String
                               )
  extends LkNodesTreeNameAction


/** Добавление под-узла: Юзер вводит id узла (маячка). */
case class AddSubNodeIdChange(
                               override val rcvrKey : RcvrKey,
                               id                   : String
                             )
  extends LkNodesTreeAction


/** Добавление под-узла: Юзер нажимает "сохранить". */
case class AddSubNodeSaveClick(
                                override val rcvrKey: RcvrKey
                              )
  extends LkNodesTreeAction


/** Среагировать на ответ сервера по поводу успешного добавления нового узла. */
case class AddSubNodeResp( rcvrKey: RcvrKey, tryResp: Try[MLknNode] )
  extends LkNodesTreeAction


/** Добавление под-узла: юзер нажал "отмена". */
case class AddSubNodeCancelClick(
                                  override val rcvrKey: RcvrKey
                                )
  extends LkNodesTreeAction


/** Сигнал о клике по галочке узла. */
case class NodeIsEnabledChanged(
                                 override val rcvrKey : RcvrKey,
                                 isEnabled            : Boolean
                               )
  extends LkNodesTreeAction


/** Сигнал ответа сервера на апдейт флага isEnabled. */
case class NodeIsEnabledUpdateResp(
                                    override val rcvrKey : RcvrKey,
                                    resp                 : Try[MLknNode]
                                  )
  extends LkNodesTreeAction


/** Клик по кнопке удаления узла. */
case class NodeDeleteClick( override val rcvrKey: RcvrKey )
  extends LkNodesTreeAction

/** Клик по кнопке подтверждения удаления узла. */
case class NodeDeleteOkClick( override val rcvrKey: RcvrKey )
  extends LkNodesTreeAction

/** Результат запроса к серверу по поводу удаления узла. */
case class NodeDeleteResp(
                           override val rcvrKey: RcvrKey,
                           resp: Try[Boolean]
                         )
  extends LkNodesTreeAction

/** Клик по кнопке отмены удаления узла. */
case class NodeDeleteCancelClick( override val rcvrKey: RcvrKey )
  extends LkNodesTreeAction


/** Клик по кнопке редактирования узла в дереве узлов. */
case class NodeEditClick( override val rcvrKey: RcvrKey)
  extends LkNodesTreeAction

/** Редактирование названия узла: Юзер вводит название узла (маячка). */
case class NodeEditNameChange(
                               override val rcvrKey: RcvrKey,
                               name: String
                             )
  extends LkNodesTreeNameAction

/** Клик по кнопке отмены редактирования узла. */
case class NodeEditCancelClick(override val rcvrKey: RcvrKey)
  extends LkNodesTreeAction

/** Клик по кнопке подтверждения редактирования узла. */
case class NodeEditOkClick(override val rcvrKey: RcvrKey)
  extends LkNodesTreeAction

/** Ответ сервера по итогам запроса. */
case class NodeEditSaveResp(override val rcvrKey: RcvrKey, tryResp: Try[MLknNode])
  extends LkNodesTreeAction


// Управление размещением текущей карточки на указанном узле.

/** Изменилось состояние галочки, управляющей размещением текущей карточки на узле. */
case class AdvOnNodeChanged(override val rcvrKey: RcvrKey, isEnabled: Boolean)
  extends LkNodesTreeAction
/** Ответ сервера на тему управления размещением карточки на узле. */
case class AdvOnNodeResp(override val rcvrKey: RcvrKey, tryResp: Try[MLknNode])
  extends LkNodesTreeAction
