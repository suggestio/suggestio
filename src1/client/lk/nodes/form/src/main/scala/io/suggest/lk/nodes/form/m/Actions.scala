package io.suggest.lk.nodes.form.m

import io.suggest.lk.nodes.{MLknNode, MLknNodeResp}
import io.suggest.scalaz.NodePath_t
import io.suggest.spa.DAction

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
{
  def nodePath: NodePath_t
}

sealed trait LkNodesTreeNameAction extends LkNodesTreeAction {
  val name: String
}


/** Юзер кликнул по узлу, необходимо развернуть узел. */
case class NodeNameClick(
                          override val nodePath: NodePath_t,
                        )
  extends LkNodesTreeAction


/** Модель результата запроса к серверу по поводу под-узлов для указанного узла. */
case class HandleSubNodesOf(
                             override val nodePath  : NodePath_t,
                             subNodesRespTry        : Try[MLknNodeResp],
                             tstampMs               : Long,
                           )
  extends LkNodesTreeAction


/** Юзер кликнул по кнопке добавления подузла.
  */
case object CreateNodeClick
  extends LkNodesAction



/** Добавление под-узла: Юзер вводит название узла (маячка). */
case class CreateNodeNameChange( name: String )
  extends LkNodesAction


/** Добавление под-узла: Юзер вводит id узла (маячка). */
case class CreateNodeIdChange( id: String )
  extends LkNodesAction


/** Добавление под-узла: Юзер нажимает "сохранить". */
case object CreateNodeSaveClick
  extends LkNodesAction


/** Среагировать на ответ сервера по поводу успешного добавления нового узла. */
case class CreateNodeResp(tryResp: Try[MLknNode] )
  extends LkNodesAction


/** Добавление под-узла: юзер нажал "отмена". */
case object CreateNodeCloseClick
  extends LkNodesAction



/** Сигнал о клике по галочке узла. */
case class NodeIsEnabledChanged(
                                 isEnabled            : Boolean,
                               )
  extends LkNodesAction


/** Сигнал ответа сервера на апдейт флага isEnabled. */
case class NodeIsEnabledUpdateResp(
                                    override val nodePath: NodePath_t,
                                    resp                 : Try[MLknNode]
                                  )
  extends LkNodesTreeAction


// Удалятельство узлов.

/** Клик по кнопке удаления узла. */
case object NodeDeleteClick
  extends LkNodesAction


// Выпадающая менюшка узла с доп.функциями.
/** Клик по кнопке меню узла. */
case object NodeMenuBtnClick
  extends LkNodesAction


/** Результат запроса к серверу по поводу удаления узла. */
case class NodeDeleteResp(
                           override val nodePath: NodePath_t,
                           resp: Try[Boolean]
                         )
  extends LkNodesTreeAction


// Редактирование узла (переименование).

/** Клик по кнопке редактирования узла в дереве узлов. */
case object NodeEditClick extends LkNodesAction

/** Редактирование названия узла: Юзер вводит название узла (маячка). */
case class NodeEditNameChange( name: String ) extends LkNodesAction

/** Клик по кнопке отмены редактирования узла. */
case object NodeEditCancelClick extends LkNodesAction

/** Клик по кнопке подтверждения редактирования узла. */
case object NodeEditOkClick extends LkNodesAction

/** Ответ сервера по итогам запроса. */
case class NodeEditSaveResp( nodeId: String, tryResp: Try[MLknNode] ) extends LkNodesAction


// Управление размещением текущей карточки на указанном узле.

/** Изменилось состояние галочки, управляющей размещением текущей карточки на узле. */
case class AdvOnNodeChanged(override val nodePath: NodePath_t, isEnabled: Boolean)
  extends LkNodesTreeAction
/** Ответ сервера на тему управления размещением карточки на узле. */
case class AdvOnNodeResp(override val nodePath: NodePath_t, tryResp: Try[MLknNode])
  extends LkNodesTreeAction


// Управление тарифом узла.
case object TfDailyShowDetails extends LkNodesAction

/** Клик по ссылке редактирования тарифа текущего узла. */
case object TfDailyEditClick
  extends LkNodesAction

/** Команда сохранения возможно обновлённого тарифа. */
case object TfDailySaveClick
  extends LkNodesAction
/** Сигнал ответа сервера по поводу обновления тарифа. */
case class TfDailySavedResp(tryResp: Try[MLknNode])
  extends LkNodesAction

/** Отмена редактирования тарифа текущего узла. */
case object TfDailyCancelClick
  extends LkNodesAction

/** Юзер редактирует значением amount для ручного тарифа. */
case class TfDailyManualAmountChanged(amount: String)
  extends LkNodesAction

/** Выбран режим унаследованного тарифа. */
case object TfDailyInheritedMode
  extends LkNodesAction

/** Выбран режим ручной тарификации. */
case object TfDailyManualMode
  extends LkNodesAction


/** Изменилось значение галочки раскрытости карточки по дефолту. */
case class AdvShowOpenedChange( isChecked: Boolean ) extends LkNodesAction

/** Ответ сервера на запрос изменения отображения рекламной карточки. */
case class AdvShowOpenedChangeResp(
                                    override val nodePath: NodePath_t,
                                    reason: AdvShowOpenedChange,
                                    tryResp: Try[_],
                                  )
  extends LkNodesTreeAction

/** Изменение галочки постоянной обводки. */
case class AlwaysOutlinedSet( isChecked: Boolean ) extends LkNodesAction
case class AlwaysOutlinedResp(
                               override val nodePath: NodePath_t,
                               reason: AlwaysOutlinedSet,
                               tryResp: Try[_],
                             )
  extends LkNodesTreeAction
